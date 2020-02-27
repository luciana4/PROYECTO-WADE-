/*****************************************************************
WADE - Workflow and Agent Development Environment is a framework to develop 
multi-agent systems able to execute tasks defined according to the workflow
metaphor.
Copyright (C) 2008 Telecom Italia S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/
package com.tilab.wade.performer.interactivity;

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Result;
import jade.content.schema.ObjectSchema;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.tilab.wade.performer.BuildingBlock;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.HierarchyNode;
import com.tilab.wade.performer.Subflow;
import com.tilab.wade.performer.UnmanagedField;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.interactivity.ontology.Back;
import com.tilab.wade.performer.interactivity.ontology.GetSnapshot;
import com.tilab.wade.performer.interactivity.ontology.Go;
import com.tilab.wade.performer.interactivity.ontology.InteractivityCompleted;
import com.tilab.wade.performer.interactivity.ontology.InteractivityOntology;
import com.tilab.wade.performer.layout.WorkflowSkipped;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.utils.OntologyUtils;

@WorkflowSkipped
public class InteractiveWorkflowBehaviour extends WorkflowBehaviour {
	
	protected static final String UNGROUNDED_PARAMETERS_PANEL_ID = "UNGROUNDED_PARAMETERS_PANEL_ID";
	
	@UnmanagedField
	private boolean interactivityCompleted = false;
	@UnmanagedField
	private Interaction currentInteraction;
	
	@UnmanagedField
	private List<Display> toBeDisplayed = new ArrayList<Display>();
	@UnmanagedField
	protected String displayActivityName = "";

	@UnmanagedField
	private List<NotificationInfo> notifications = new ArrayList<NotificationInfo>();
	
	@UnmanagedField
	private boolean closeInteractivityOnSuspension = true;
	@UnmanagedField
	private String suspendMessage;

	@UnmanagedField
	private Stack<String> steps = new Stack<String>();

	// Display: activity -> parameterKey -> list of parts/labels
	@UnmanagedField
	private HashMap<String, HashMap<String, ArrayList>> inputDisplays = new HashMap<String, HashMap<String, ArrayList>>();
	@UnmanagedField
	private HashMap<String, HashMap<String, ArrayList>> outputDisplays = new HashMap<String, HashMap<String, ArrayList>>();

	// Info: activity -> parameterKey -> list of parts/infos
	@UnmanagedField
	private HashMap<String, HashMap<String, ArrayList>> inputInfos = new HashMap<String, HashMap<String, ArrayList>>();
	@UnmanagedField
	private HashMap<String, HashMap<String, ArrayList>> outputInfos = new HashMap<String, HashMap<String, ArrayList>>();
	
	public InteractiveWorkflowBehaviour() {
		super();
	}
	
	/**
	 * This constructor is used when this workflow is used as an inline subflow
	 * @param activityName The name of the subflow activity whose execution corresponds to the execution of this workflow
	 */
	public InteractiveWorkflowBehaviour(String activityName) {
		super(activityName);
	}
	
	private List getParamActions(HashMap<String, HashMap<String, ArrayList>> actionsMap, String activityName, String paramKey, boolean createIfNotPresent) {
		ArrayList actions4Param = null;
		
		HashMap<String, ArrayList> actions4Activity = actionsMap.get(activityName);
		if (actions4Activity == null && createIfNotPresent) {
			actions4Activity = new HashMap<String, ArrayList>();
			actionsMap.put(activityName, actions4Activity);
		}
		if (actions4Activity != null) {
			actions4Param = actions4Activity.get(paramKey);
			if (actions4Param == null && createIfNotPresent) {
				actions4Param = new ArrayList();
				actions4Activity.put(paramKey, actions4Param);
			}
		}
		return actions4Param;
	}
	
	private List getParamActions(HashMap<String, HashMap<String, ArrayList>> inputActionsMap, HashMap<String, HashMap<String, ArrayList>> outputActionsMap, String activityName, String paramKey, int paramMode, boolean createIfNotPresent) {
		List paramActions;
		if (paramMode == Constants.IN_MODE) {
			paramActions = getParamActions(inputActionsMap, activityName, paramKey, createIfNotPresent);
		} else {
			paramActions = getParamActions(outputActionsMap, activityName, paramKey, createIfNotPresent);
		}
		return paramActions;
	}
	
	/**
	 * Set an additional info to a given part of a parameter (possibly the whole parameter) 
	 * of a given activity. 
	 * @param activityName The name of the activity
	 * @param paramKey The key identifying the parameter i.e. <param-name>[@<building-block-name>].
	 * The building block indication is required only for activities that may execute more than one
	 * building block (e.g. ToolActivities). Note that the param-name on its turn may have the form
	 * perfix.name (for instance header.username).
	 * @param paramMode the mode of the parameter i.e. one of <code>Constants.IN_MODE, Constants.OUT_MODE,
	 * Constants.INOUT_MODE</code>
	 * @param paramPart The identifier of the part of the parameter to be displayed <slot>.<slot>. ...
	 * @param info application specific info 
	 */
	public void setAdditionalInfo(String activityName, String paramKey, int paramMode, String paramPart, String info) {
		List paramInfos = getParamActions(inputInfos, outputInfos, activityName, paramKey, paramMode, true);
		paramInfos.add(new AdditionalInfo(paramPart, info));
	}
	
	
	/**
	 * Declare that a given part of a parameter (possibly the whole parameter) of a given activity 
	 * must be displayed after the execution of the activity.
	 * @param activityName The name of the activity
	 * @param paramKey The key identifying the parameter i.e. <param-name>[@<building-block-name>].
	 * The building block indication is required only for activities that may execute more than one
	 * building block (e.g. ToolActivities). Note that the param-name on its turn may have the form
	 * perfix.name (for instance header.username).
	 * @param paramMode the mode of the parameter i.e. one of <code>Constants.IN_MODE, Constants.OUT_MODE,
	 * Constants.INOUT_MODE</code>
	 * @param paramPart The identifier of the part of the parameter to be displayed <slot>.<slot>. ...
	 * @param label The label to show when displaying the parameter (defaults to paramKey+paramPart) 
	 */
	public void display(String activityName, String paramKey, int paramMode, String paramPart, String label) {
		List paramDisplays = getParamActions(inputDisplays, outputDisplays, activityName, paramKey, paramMode, true);
		
		// If label is not present use paramKey+paramPart
		if (label == null) {
			label = paramKey;
			if (paramPart != null) {
				label += Constants.BB_PART_SEPARATOR+paramPart;
			}
		}
		
		paramDisplays.add(new Display(activityName, paramKey, paramMode, paramPart, label));
	}

	protected ListPanel prepareDisplaysPanel() {
		ListPanel displayPanel = null;
		if (!toBeDisplayed.isEmpty()) {
			displayPanel = new ListPanel();
			displayPanel.setLabel(getDisplaysTitle());
			int displayIndex = 0;
			
			for (Display display : toBeDisplayed) {
				String displayKey = "display#"+displayIndex;
				
				StructuredDataElement displayElement = new StructuredDataElement(displayKey);
				displayElement.setEditable(false);
				displayElement.setLabel(display.label);
				displayElement.setValue(display.value);
				
				// Check if display have info associated
				List<AdditionalInfo> paramInfos = getParamActions(inputInfos, outputInfos, display.activityName, display.paramKey, display.paramMode, false);
				if (paramInfos != null) {
					// Add additional-info to StructuredDataElement
					for (AdditionalInfo additionalInfo : paramInfos) {
	
						// Algoritmo per capire se l'additionalInfo è da applicare:
						// - se la parte del display è null (parametero intero) 
						//   -> qualsiasi sia la parte dell'additionalInfo 
						//      -> elaborare l'additionalInfo (senza modifica alla part)
						// - se la parte del display NON è null (parte di parametero es. person)
						//   -> se la parte dell'additionalInfo inizia con (oppure è uguale) a quella del display (es. person.address)
						//      -> elaborare l'additionalInfo (modificare la part con part-AddIndo - part-display, es. address)
						//   -> se la parte dell'additionalInfo NON inizia con (oppure è uguale) a quella del display (es. cognome)
						//      -> NON elaborare l'additionalInfo
						String paramPart = null;
						String displayPart = display.paramPart; 
						String infoPart = additionalInfo.paramPart;
						if (displayPart == null) {
							paramPart = infoPart!=null?infoPart:"";
						}
						else {
							if (infoPart != null && infoPart.startsWith(displayPart)) {
								if (infoPart.equals(displayPart)) {
									paramPart = "";
								} else {
									paramPart = infoPart.substring(displayPart.length()+1);
								}
							}
						}
						
						if (paramPart != null) {
							try {
								displayElement.addConstraint(new AdditionalInfoConstraint(additionalInfo.info), paramPart);
							} catch (OntologyException e) {
								myLogger.log(Logger.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				
				displayPanel.addComponent(displayElement);
				displayIndex++;
			}
			
			// Clear the list of display
			toBeDisplayed.clear();
		}
		return displayPanel;
	}
	
	private ListPanel perpareUngroundedParametersPanel(BuildingBlock bb, Map<String, AbsObject> ungroundedParameters) {
		ListPanel paramPanel = null;
		if (!ungroundedParameters.isEmpty()) {
			paramPanel = new ListPanel(UNGROUNDED_PARAMETERS_PANEL_ID);
			paramPanel.setLabel(getUngroundedParametersTitle());
			for (String ungroundedParameterName : ungroundedParameters.keySet()) {
				Parameter inputParameter = bb.getInputParameter(ungroundedParameterName);
				
				// Get abs template value
				AbsObject ungroundedParameterValue = ungroundedParameters.get(ungroundedParameterName);

				try {
					// Get and set parameter schema to inputParameter
					ObjectSchema paramSchema = OntologyUtils.getParameterSchema(inputParameter, bb.getOntology());
					inputParameter.setSchema(paramSchema);
					
					// Create StructuredDataElement
					StructuredDataElement ungroundedParameterElement = new StructuredDataElement(ungroundedParameterName);
					ungroundedParameterElement.setEditable(true);
					
					// Preset toBeRemove to false if mandatory or not mandatory and value is primitive
					// In all other case set to true (variable or concept)
					boolean paramMandatory = inputParameter.getMandatory();
					ungroundedParameterElement.markToBeRemove(!(paramMandatory || (!paramMandatory && ungroundedParameterValue instanceof AbsPrimitive)));
					
					ungroundedParameterElement.setLabel(ungroundedParameterName);
					ungroundedParameterElement.setValue(ungroundedParameterValue);
					ungroundedParameterElement.addNodeConstraints(paramSchema);
					ungroundedParameterElement.addConstraints(inputParameter);

					// Check if ungroundedParameter have info associated
					manageAdditionalInfoConstraint(ungroundedParameterElement, bb.getActivity().getBehaviourName(), ungroundedParameterName, inputParameter.getMode());
					
					// Add StructuredDataElement to panel
					paramPanel.addComponent(ungroundedParameterElement);

				} catch (Exception e) {
					myLogger.log(Logger.WARNING, "Impossible set value/constraints of parameter "+ungroundedParameterName+", value="+ungroundedParameterValue, e);
				}
			}
		}
		return paramPanel;
	}
	
	public void manageAdditionalInfoConstraint(StructuredDataElement sde, String activityName, String paramKey, int paramMode) throws OntologyException {
		List<AdditionalInfo> paramInfos = getParamActions(inputInfos, outputInfos, activityName, paramKey, paramMode, false);
		if (paramInfos != null) {
			// Add additional-info to StructuredDataElement
			for (AdditionalInfo additionalInfo : paramInfos) {
				sde.addConstraint(new AdditionalInfoConstraint(additionalInfo.info), additionalInfo.paramPart);
			}
		}
	}
	
	protected boolean skipParameter(BuildingBlock bb, List<String> prefixesToSkip, String parameterName) {
		if (prefixesToSkip != null) {
			// Get parameter prefix
			String prefix = null;
			int sepPos = parameterName.indexOf(Constants.BB_PREFIX_SEPARATOR);
			if (sepPos > 0) {
				prefix = parameterName.substring(0, sepPos-1);
			}
		
			// Check if ignore prefixed parameter
			return prefixesToSkip.contains(prefix);
		}		
		return false;
	}
	
	protected void handleUngroundedParameters(BuildingBlock bb) throws Exception {
		// Manage ungrounded parameters with interaction only if batch-mode is disable 
		if (isInteractiveMode()) {
			Map<String, AbsObject> ungroundedParameters = new HashMap<String, AbsObject>();
			List<String> prefixesToSkip = getParameterPrefixesToSkip(bb);
			
			// Get ungrounded parameters
			for (String parameterName : bb.getInputParameterNames()) {
				
				// Check if ignore parameters
				if (skipParameter(bb, prefixesToSkip, parameterName)) {
					continue;
				}
				
				Object parameterValue = bb.getInput(parameterName);
				
				// If parameter is an Abs check if is not grounded 
				if (parameterValue != null && parameterValue instanceof AbsObject) {
					AbsObject parameterAbs = (AbsObject)parameterValue;
					if (!parameterAbs.isGrounded() || 
						(parameterValue instanceof AbsAggregate && parameterAbs.getCount() == 0)) {
						ungroundedParameters.put(parameterName, parameterAbs);
					}
				}
			}
			
			// Call interact only if are present displays or ungrounded parameters
			if (!ungroundedParameters.isEmpty() || !toBeDisplayed.isEmpty()) {
	
				// Prepare interaction
				Interaction interaction = new Interaction(getInteractionTitle(bb));
	
				// Prepare main panel
				ListPanel mainPanel = new ListPanel();
				interaction.setMainPanel(mainPanel);
				
				// Prepare actions
				Action next = new Action(getNextButtonLabel());
				next.requireAllComponentsValidation();
				interaction.addAction(next);
	
				// Prepare displays
				ListPanel displaysPanel = prepareDisplaysPanel();
				if (displaysPanel != null) {
					mainPanel.addComponent(displaysPanel);
				}
				
				// Prepare dataElements for ungrounded parameters 
				ListPanel ungroundedParametersPanel = perpareUngroundedParametersPanel(bb, ungroundedParameters);
				if (ungroundedParametersPanel != null) {
					mainPanel.addComponent(ungroundedParametersPanel);
				}
				
				// Customize interaction before invoke
				customizeUngroundedParametersInteraction(bb, interaction);
				
				// Execute interaction
				interaction = interact(interaction);
				
				// Get filled parameters and set into building block
				for (String ungroundedParameterName : ungroundedParameters.keySet()) {
					StructuredDataElement groundedParameterElement = (StructuredDataElement)interaction.getComponent(ungroundedParameterName);
					if (groundedParameterElement != null) {
						bb.setInput(ungroundedParameterName, groundedParameterElement.getValue());
					}
				}
			}
		}
		
		// In case some variables are still there, nullify them 
		super.handleUngroundedParameters(bb);
	}

	protected void customizeUngroundedParametersInteraction(BuildingBlock bb, Interaction interaction) {
	}

	@Override
	public void onStart() {
		try {
			initRootExecutor();

			if (isInteractiveMode()) {
				if (!isInline() && getDescriptor().getDelegationChain() == null) {
					// Blocks until receive GO from EngineProxy
					try {
						blockUntilGo(getDescriptor().getSessionId(), null);
					} catch (InteractionException e) {
						// Should never happen
						e.printStackTrace();
					}
				}
			}
			
			super.onStart();
		} catch(Agent.Interrupted e) {
			// The workflow was killed while waiting for the first GO --> Do nothing
		}
	}

	@Override
	public int onEnd() {
		boolean failed = false;
		boolean interrupted = false;
		
		if (!frozen && !interactivityCompleted && isInteractiveMode()) {
			try {
				// If present displays -> prepare panel and do a special interaction
				ListPanel displaysPanel = prepareDisplaysPanel();
				if (displaysPanel != null) {
					
					// Prepare last displays interaction
					Interaction interaction = new Interaction(getLastDisplaysInteractionTitle());
	
					// Prepare main panel
					ListPanel mainPanel = new ListPanel();
					interaction.setMainPanel(mainPanel);
					mainPanel.addComponent(displaysPanel);
					
					// Prepare actions
					Action ok = new Action(getOkButtonLabel());
					interaction.addAction(ok);
					
					try {
						interact(interaction);
					} catch(Agent.Interrupted e) {
						// The workflow was killed/freezed while performing the interaction --> Just remember 
						// that we were killed/freezed to make the workflow fail/freezed
						interrupted = true;
					}
				}
				
				// If present call last interaction
				if (!interrupted && !isSubflow() && !isInteractivityCompleted()) {
					Interaction defaultLastInteraction = getDefaultLastInteraction();
					if (defaultLastInteraction != null) {
						defaultLastInteraction.setLast(true);
						interact(defaultLastInteraction);
					} else {
						markInteractivityCompleted();
					}
				}
			} catch (InteractionException e) {
				myLogger.log(Logger.WARNING, "Error performing interaction",e);
				failed = true;
			}
		}

		int ret = super.onEnd();

		if (frozen && ret==Constants.SUCCESS) {
			// The workflow is terminated correctly but was frozen while performing the interaction "last display" --> freeze
			ret = Constants.FROZEN;
		}
		
		return (failed ? Constants.FAILURE : ret);
	}

	private Go blockUntilGo(String sessionId, String expectedInteractionId) throws InteractionException {

		// Prepare interaction message template
		MessageTemplate interactionTemplate = MessageTemplate.and(
				MessageTemplate.and(
						MessageTemplate.MatchOntology(InteractivityOntology.getInstance().getName()), 
						MessageTemplate.MatchConversationId(sessionId)),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		
		boolean entered = false;
		try {
			entered = enterInterruptableSection();
			
			AgentAction agentAction = null;
			boolean receivedGoAction;
			do {
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "Begin BlockUntilGo for sessionId="+sessionId);
				}
				
				// Wait for message
				ACLMessage msg = myAgent.blockingReceive(interactionTemplate);
				
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "BlockUntilGo for sessionId="+sessionId+" receive msg="+msg);
				}
				
				// Check timeout
				if (msg == null) {
					return null;
				}
				
				// Extract agent action
				try {
					jade.content.onto.basic.Action action = (jade.content.onto.basic.Action)myAgent.getContentManager().extractContent(msg);
					agentAction = (AgentAction)action.getAction();
				} catch (Exception e) {
					// Should never happen
					throw new InteractionException("Error extracting action from message", e);					
				}
	
				// Check if action is a GetSnapshot
				if (agentAction instanceof GetSnapshot) {
					serveGetSnapshot((GetSnapshot)agentAction, msg.createReply());
					receivedGoAction = false;
				}

				// Check if action is a Back
				else if (agentAction instanceof Back) {
					serveBack((Back)agentAction, msg.createReply());
					receivedGoAction = false;
				}

				// Check if action is a Go
				else if (agentAction instanceof Go) {
					// Match interactionId to check if expected interaction is correct
					Interaction interaction = ((Go)agentAction).getInteraction();
					if ((expectedInteractionId == null && interaction == null) ||
						(expectedInteractionId == null && interaction != null && interaction.getId() == null) ||	
						(expectedInteractionId != null && interaction != null && expectedInteractionId.equals(interaction.getId()))) {
						receivedGoAction = true;
					} else {
						myLogger.log(Logger.WARNING, "BlockUntilGo for sessionId="+sessionId+" receive unexpected interaction, extected="+expectedInteractionId+", received="+(interaction!=null?interaction.getId():"null"));
						receivedGoAction = false;
					}
				}
				
				// otherwise
				else {
					myLogger.log(Logger.WARNING, "BlockUntilGo for sessionId="+sessionId+" receive unexpected msg="+msg);
					receivedGoAction = false;
				}
				
				// Exit only when receive a GO action
			} while(!receivedGoAction);			
			
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Exit from BlockUntilGo for sessionId="+sessionId);
			}
			return (Go)agentAction;
		} 
		catch(Agent.Interrupted e) {
			// If the workflow was killed while waiting for the next GO request, we must not
			// send any default last interaction as no one is waiting for it (since we are 
			// here, the Controller cannot be executing the go() method).
			interactivityCompleted = true;
			throw e;
		} 
		finally {
			if (entered) {
				exitInterruptableSection(null);
			}
		}
	}

	private void serveGetSnapshot(GetSnapshot im, ACLMessage reply) throws InteractionException {
		myLogger.log(Logger.INFO, "Serving GetSnapshot action");
		
		try {
			InteractivitySnapshot snapshot = new InteractivitySnapshot();
			snapshot.setInteraction(currentInteraction);
			snapshot.setNotifications(notifications);

			Result result = new Result(im, snapshot);
			informManagerAgent(reply, result);
		} catch (Exception e) {
			myLogger.log(Logger.WARNING, "Error sending snapshot to proxy for sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Error sending snapshot to proxy", e);
		}
	}

	private void serveBack(Back back, ACLMessage reply) throws InteractionException {
		myLogger.log(Logger.INFO, "Serving Back action");
		
		// Check if TAG are supporter
		if (!supportTags()) {
			myLogger.log(Logger.WARNING, "Workflow without tags support, sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Workflow without tags support");
		} else {
			// Check if previous step is available
			if (getSteps().isEmpty()) {
				myLogger.log(Logger.INFO, "Workflow without previous step available -> back to parent workflow, sessionId="+getDescriptor().getSessionId());
				
				// The current wf no have previous interactions available so must to go back to the workflow father.
				// To handle this situation must set the BackOnFirstInteractionException on current wf 
				// (so that it ends in error with the customizable message getBackOnFirstInteractonMessage()) 
				// and the throw the Agent.Interrupted so that the action method of the relative CodeExecutionBehaviour 
				// (activity suspended on blockUntilGo) exit without generating exceptions.  
				handleException(new BackOnFirstInteractionException(getBackOnFirstInteractonMessage()));
				throw new Agent.Interrupted();
			} else {
				// Do back
				try {
					back();
				} catch (Exception e) {
					myLogger.log(Logger.WARNING, "Error in go back for sessionId="+getDescriptor().getSessionId());
					throw new InteractionException("Error in go back", e);
				}
			}
		}
	}
	
	private String getTagName() {
		String tagName = null;
		WorkflowBehaviour wb = this;
		
		do {
			tagName = wb.getCurrent().getBehaviourName() + (tagName!=null?"."+tagName:"");
			wb = wb.getOwner();
		} while(wb != null);

		return tagName+"_"+System.currentTimeMillis();
	}

	private Stack<String> getSteps() {
		WorkflowBehaviour owner = getOwner();
		if (owner != null && owner instanceof InteractiveWorkflowBehaviour) {
			return ((InteractiveWorkflowBehaviour)owner).getSteps();
		}
		return steps;
	}
	
	private void flash() throws Exception {
		if (supportTags()) {
			String tagName = getTagName();
			tag(tagName);
			getSteps().push(tagName);
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Flashed tag "+tagName);
			}
		}
	}
	
	private void back() throws Exception {
		String tagName = getSteps().pop();
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Moving back to tag: "+tagName);
		}
		reloadTag(tagName);
	}
	
	public Interaction interact(Interaction interaction) throws InteractionException {
		if (interaction == null) {
			throw new InteractionException("Null interaction");
		}
		
		// Modify id to support loop activity and loop subflow (not inline)
		if (interaction.getId() != null) {
			interaction.setId(interaction.getId()+getExecutionId()+getCurrent().getRestartCounter());
		} 
		
		myLogger.log(Logger.INFO, "Begin interact (title="+interaction.getTitle()+", id="+interaction.getId()+", executionId="+getExecutionId()+", sessionId="+getDescriptor().getSessionId()+")");
		
		// Save current interaction
		currentInteraction = interaction;

		// If mode is not interactive -> skip interact
		AID interactionManagerAgent = getInteractionManagerAgent();
		if (interactionManagerAgent == null) {
			return null;
		}
		
		// Check that the interactivity isn't already finished
		if (interactivityCompleted) {
			myLogger.log(Logger.WARNING, "Interaction already finished for sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Interaction already finished");
		}
		if (interaction.isLast()) {
			// Set that is the last interaction 
			interactivityCompleted = true;
		}

		// Set back information
		if (supportTags() && getSteps().isEmpty() && getDescriptor().getId().equals(getWorkflowIdOfFirstInteraction())) {
			resetFirstInteractionAsExecuted();
		}
		interaction.setBackEnabled(isFirstInteractionExecuted());
		
		// Send request-interaction
		try {
			// Create a fictitious action without real request message (with request interaction object) 
			// to optimize the response message
			Result result = new Result(new Go(), interaction);
			
			informManagerAgent(result);
			
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Send INFORM(RESULT(GO)) for sessionId="+getDescriptor().getSessionId());
			}
		} catch (Exception e) {
			myLogger.log(Logger.WARNING, "Error sending interaction to proxy for sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Error sending interaction to proxy", e);
		}

		// If is last interaction receive nothing
		if (interactivityCompleted) {
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Interactivity completed for sessionId="+getDescriptor().getSessionId());
			}
			return null;
		}
		
		// Receive response-interaction
		Interaction newInteraction = null;

		Go goAction = blockUntilGo(getDescriptor().getSessionId(), interaction.getId());
		if(goAction!=null){				
			newInteraction = goAction.getInteraction();
		} else {
			myLogger.log(Logger.WARNING, "Interaction timeout for sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Interaction timeout");
		}
	
		// No interaction currently displayed on client -> reset current interaction 
		currentInteraction = null;

		// Flash current tag 
		try {
			flash();
		} catch (Exception e) {
			myLogger.log(Logger.WARNING, "Error flashing tag for sessionId="+getDescriptor().getSessionId());
			throw new InteractionException("Error flashing tag", e);
		}

		// Set back information
		if (!isFirstInteractionExecuted() && supportTags()) {
			markFirstInteractionAsExecuted();
		}
		
		if (newInteraction != null) {
			myLogger.log(Logger.INFO, "End interact (title="+newInteraction.getTitle()+", id="+newInteraction.getId()+", executionId="+getExecutionId()+", sessionId="+getDescriptor().getSessionId()+")");
		} else {
			myLogger.log(Logger.WARNING, "End interact, received null interaction (executionId="+getExecutionId()+", sessionId="+getDescriptor().getSessionId()+")");
		}
		
		return newInteraction;
	}
	
	protected boolean isInteractivityCompleted() {
		return interactivityCompleted;
	}

	private void setInteractivityCompleted() {
		interactivityCompleted = true;
		currentInteraction = null;
		
		Behaviour currentBehaviour = getCurrent();
		if (currentBehaviour != null && currentBehaviour instanceof InteractiveWorkflowBehaviour) {
			 ((InteractiveWorkflowBehaviour)currentBehaviour).setInteractivityCompleted();
		}
	}
	
	protected void markInteractivityCompleted() {
		if (!interactivityCompleted) {
			if (isSubflow() && parent instanceof InteractiveWorkflowBehaviour) {
				((InteractiveWorkflowBehaviour) parent).markInteractivityCompleted();
			} else {
				setInteractivityCompleted();
				
				try {
					InteractivityCompleted completed = new InteractivityCompleted(InteractivityCompleted.Reason.PROGRAMMATING);
					informManagerAgent(completed);
				} catch (Exception e) {
					// Should never happen
					e.printStackTrace();
				}
			}
		}
	}
	
	private void informManagerAgent(Predicate predicate) throws InteractionException {
		informManagerAgent(new ACLMessage(), predicate);
	}
	
	private void informManagerAgent(ACLMessage msg, Predicate predicate) throws InteractionException {
		AID interactionManagerAgent = getInteractionManagerAgent();
		if (interactionManagerAgent != null) {
			msg.setPerformative(ACLMessage.INFORM);
			msg.setLanguage(LEAPCodec.NAME);
			msg.setOntology(InteractivityOntology.getInstance().getName());
			msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// A FAILURE from the AMS would make the workflow fail --> If the interaction manager suddenly disappears 
			// we will block until a new one takes over
			msg.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
			msg.setConversationId(getDescriptor().getSessionId());
			msg.addReceiver(interactionManagerAgent);
			
			try {
				myAgent.getContentManager().fillContent(msg, predicate);
				myAgent.send(msg);
			} catch (Exception e) {
				myLogger.log(Logger.WARNING, "Error sending "+(predicate!=null?predicate:"FAILURE")+" to proxy for sessionId="+getDescriptor().getSessionId());
				throw new InteractionException("Error sending "+(predicate!=null?predicate:"FAILURE")+" to proxy", e);
			}
		}
		
	}
	
	/**
	 * Return the interaction title for specific building block
	 */
	protected String getInteractionTitle(BuildingBlock bb) {
		return "Activity "+bb.getActivity().getBehaviourName();
	}

	/**
	 * Return the interaction title for the last step (Workflow terminated) 
	 */
	protected String getLastInteractionTitle() {
		return "";
	}

	/**
	 * Return the interaction title for the last displays (displays of last activity) 
	 */
	protected String getLastDisplaysInteractionTitle() {
		return "";
	}

	/**
	 * Return the panel title for displays 
	 */
	protected String getDisplaysTitle() {
		return "Displays";
	}

	/**
	 * Return the panel title for ungrounded parameters 
	 */
	protected String getUngroundedParametersTitle() {
		return "Request parameters";
	}
	
	/**
	 * Return the label for OK button 
	 */
	protected String getOkButtonLabel() {
		return "OK";
	}

	/**
	 * Return the label for NEXT button 
	 */
	protected String getNextButtonLabel() {
		return "NEXT";
	}

	/**
	 * Return the label for an unknown value
	 */
	protected String getUnknownValueLabel() {
		return "UNKNOWN";
	}

	/**
	 * Return a list of prefixes of ungrounded parameters to skip in interaction
	 */
	protected List<String> getParameterPrefixesToSkip(BuildingBlock bb) {
		return null;
	}
	
	protected String getBackOnFirstInteractonMessage() {
		return "Back pressed on first interaction";
	}
	
	@Override
	protected void handleEndActivity(HierarchyNode activity) {
		prepareDisplays(activity, Constants.IN_MODE);
		prepareDisplays(activity, Constants.OUT_MODE);
		
		super.handleEndActivity(activity);
	}

	public void suspend(String message) {
		setInteractivityPolicyOnSuspension(true, message);
		suspend();
	}
	
	public void setInteractivityPolicyOnSuspension(boolean closeInteractivity, String suspendMessage) {
		this.closeInteractivityOnSuspension = closeInteractivity;
		this.suspendMessage = suspendMessage;
	}

	protected void prepareDisplays(HierarchyNode activity, int mode) {
		displayActivityName = activity.getBehaviourName();
		
		HashMap<String, HashMap<String, ArrayList>> displaysMap;
		if (mode == Constants.IN_MODE) {
			displaysMap = inputDisplays;
		} else {
			displaysMap = outputDisplays;
		}
		
		HashMap<String, ArrayList> displays4Activity = displaysMap.get(activity.getBehaviourName());
		if (displays4Activity != null) {
			for (String parameterKey : displays4Activity.keySet()) {
				ArrayList<Display> displays4Param = displays4Activity.get(parameterKey);
				if (displays4Param != null) {
					for (Display display : displays4Param) {

						// Get display value 
						try {
							display.value = activity.getBindingManager().getAbsValue(parameterKey, mode, display.paramPart);
						} catch (Exception e) {
							myLogger.log(Logger.WARNING, "Error getting AbsValue for parameter key= "+parameterKey+", mode= "+mode+", part= "+display.paramPart+". Error="+e.getMessage());
							display.value = null;
						}

						// Replace null value with message
						if (display.value == null) {
							display.value = AbsPrimitive.wrap(getUnknownValueLabel());
						}
						
						// Add into toBeDisplay list
						toBeDisplayed.add(display);
					}
				}
			}
		}		
	}
	
	protected Interaction getDefaultLastInteraction() {
		return null;
	}
	
	protected void performOutputInteraction(OutputInteraction oi) throws Exception {
		manageBindings(oi);

		// Output activity is enabled only when batch-mode is disabled
		if (isInteractiveMode()) {
			Interaction interaction = oi.getOutputInteraction();
			interact(interaction);
		}
	}

	@Override
	public boolean isLongRunning() {
		return false;
	}

	@Override
	public boolean supportEnqueuing() {
		return false;
	}

	@Override
	public boolean supportTags() {
		return true;
	}
	
	@Override
	public void onSuspended() {
		if (!interactivityCompleted && closeInteractivityOnSuspension) {
			try {
				InteractivityCompleted suspended = new InteractivityCompleted(InteractivityCompleted.Reason.SUSPENDED);
				suspended.setSuspendMessage(suspendMessage);
				informManagerAgent(suspended);
			} catch (Exception e) {
				// Should never happen
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onFrozen() {
		if (!interactivityCompleted) {
			try {
				InteractivityCompleted frozen = new InteractivityCompleted(InteractivityCompleted.Reason.FROZEN);
				informManagerAgent(frozen);
			} catch (Exception e) {
				// Should never happen
				e.printStackTrace();
			}
		}
	}

	@Override
	protected WorkflowEvent customizeEvent(String id, long time, String type, WorkflowEvent ev, jade.util.leap.List controllers) {
		// Add the event to the list of notifications only if managerAgent between controllers
		AID managerAgent = getInteractionManagerAgent();
		if (managerAgent != null && controllers.contains(managerAgent)) {
			NotificationInfo notificationInfo = new NotificationInfo(time, type, ev);
			notifications.add(notificationInfo);
		}
		return ev;
	}

	@Override
	protected void onThawed() {
		super.onThawed();

		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "OnThawed for sessionId="+getDescriptor().getSessionId());
		}
		
		// Register InteractivityOntology
		myAgent.getContentManager().registerOntology(InteractivityOntology.getInstance());
		
		if (isInteractiveMode()) {
			try {
				blockUntilGo(getDescriptor().getSessionId(), null);
			} catch (InteractionException e) {
				// Should never happen
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onResumed() {
		super.onResumed();

		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "OnResumed for sessionId="+getDescriptor().getSessionId());
		}
		
		if (isInteractiveMode() && closeInteractivityOnSuspension) {
			try {
				blockUntilGo(getDescriptor().getSessionId(), null);
			} catch (InteractionException e) {
				// Should never happen
				e.printStackTrace();
			}
		}
	}
	
	private void markFirstInteractionAsExecuted() {
		Modifier modifier = getModifier(Constants.INTERACTIVE_MODIFIER);
		if (modifier != null) {
			modifier.setProperty(Constants.INTERACTIVE_FIRST_INTERACTION_EXECUTED, getDescriptor().getId());
		}
		
	}

	private void resetFirstInteractionAsExecuted() {
		Modifier modifier = getModifier(Constants.INTERACTIVE_MODIFIER);
		if (modifier != null) {
			modifier.removeProperty(Constants.INTERACTIVE_FIRST_INTERACTION_EXECUTED);
		}
		
	}
	
	private String getWorkflowIdOfFirstInteraction() {
		Modifier modifier = getModifier(Constants.INTERACTIVE_MODIFIER);
		if (modifier != null) {
			return (String)modifier.getProperty(Constants.INTERACTIVE_FIRST_INTERACTION_EXECUTED);
		}
		return null;
	}
	
	private boolean isFirstInteractionExecuted() {
		return (getWorkflowIdOfFirstInteraction() != null);
	}
	
	/**
	 * The interaction manager agent is read every time because the modifier could change a runtime
	 */
	private AID getInteractionManagerAgent() {
		AID interactionManagerAgent = null;
		Modifier modifier = getModifier(Constants.INTERACTIVE_MODIFIER);
		if (modifier != null) {
			interactionManagerAgent = (AID)modifier.getProperty(Constants.INTERACTIVE_AID);
		}
		return interactionManagerAgent;
	}
	
	protected boolean isInteractiveMode() {
		return getInteractionManagerAgent() != null;
	}

	@Override
	protected  jade.util.leap.List propagateModifier(Subflow sbfl) {
		if (sbfl.getAsynch()) {
			// If present remove interactive modifier			
			jade.util.leap.List filteredModifiers = null;
			jade.util.leap.List superModifier = super.propagateModifier(sbfl);
			if (superModifier != null) {
				filteredModifiers = new jade.util.leap.ArrayList();
				Iterator it = superModifier.iterator();
				while (it.hasNext()) {
					Modifier m = (Modifier) it.next();
					if (!m.getName().equals(Constants.INTERACTIVE_MODIFIER)) {
						filteredModifiers.add(m);
					}				
				}
			}
			return filteredModifiers;
		} else {
			return super.propagateModifier(sbfl);
		}
	}

	@Override
	public void reset() {
		notifications.clear();
		super.reset();
	}

	@Override
	protected void handleSubflowFailure(String delegatedExecutionId, ACLMessage reply) throws Exception {
		try {
			ContentElement failureContent = myAgent.getContentManager().extractContent(reply);
			if (failureContent instanceof ExecutionError) {
				ExecutionError ee = (ExecutionError)failureContent;
				if (ee.getType().equals(BackOnFirstInteractionException.class.getName())) {
					serveBack(null, null);
					return;
				}
			}
		}
		catch (Exception e) {}
		
		super.handleSubflowFailure(delegatedExecutionId, reply);
	}

	
	private class Display implements Serializable {
		public String activityName;
		public String paramKey;
		public int paramMode;
		public String paramPart;
		public String label;
		public AbsObject value;
		
		public Display(String activityName, String paramKey, int paramMode, String paramPart, String label) {
			this.activityName = activityName;
			this.paramKey = paramKey;
			this.paramMode = paramMode;
			this.paramPart = paramPart;
			this.label = label;
		}
	}
	
	private class AdditionalInfo implements Serializable {
		public String info;
		public String paramPart;
		
		public AdditionalInfo(String paramPart, String info) {
			this.paramPart = paramPart;
			this.info = info;
		}
	}
}
