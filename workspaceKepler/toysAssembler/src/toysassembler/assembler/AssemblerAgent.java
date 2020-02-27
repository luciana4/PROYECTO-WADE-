package toysassembler.assembler;

import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.dispatcher.DispatchingCapabilities;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.WorkflowEngineAgent;
import com.tilab.wade.performer.WorkflowException;
import com.tilab.wade.performer.descriptors.ElementDescriptor;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.ExecutionError;

/**
 * This is the agent that tries to assemble toys (PUPPET, WAGON)

 */
public class AssemblerAgent extends WorkflowEngineAgent {

	private AssemblerAgentGui myGui;
	private DispatchingCapabilities dc = new DispatchingCapabilities();
	private List searcherAgents = new ArrayList();	
	private int index = 0;

	/**
	 * Agent initialization
	 */
	protected void agentSpecificSetup() throws AgentInitializationException {
		super.agentSpecificSetup();

		// Create and show the gui
		myGui = new AssemblerAgentGui(this);
		myGui.initGui();
		myGui.setVisible(true);

		// Initialize the DispatchingCapabilities instance used 
		// to launch workflows
		dc.init(this);

		// Subscribe to the DF to keep the searchers list up to date
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Searcher-Agent");
		DFAgentDescription dfTemplate = new DFAgentDescription();
		dfTemplate.addServices(sd);
		SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults(new Long(-1));
		ACLMessage subscribe = DFService.createSubscriptionMessage(this, getDefaultDF(), dfTemplate, sc);
		addBehaviour(new SubscriptionInitiator(this, subscribe) {
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for (int i = 0; i < dfds.length; ++i) {
						AID aid = dfds[i].getName();
						if (dfds[i].getAllServices().hasNext()) {
							// Registration/Modification
							if (!searcherAgents.contains(aid)) {
								searcherAgents.add(aid);
								System.out.println("Searcher Agent "+aid.getLocalName()+" added to the list of searcher agents");
							}
						} else {
							// Deregistration
							searcherAgents.remove(aid);
							System.out.println("Searcher Agent "+aid.getLocalName()+" removed from the list of searcher agents");
						}
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		} );
	}

	/**
	 * Agent clean-up
	 */
	protected void takeDown() {
		// Turn off the GUI on agent termination 
		if (myGui != null) {
			myGui.dispose();
			myGui.setVisible(false);
		}
	}


	public AID getSearcherAgent() {
		if (searcherAgents.isEmpty()) {
			throw new RuntimeException("No SearcherAgent available");
		}
		if (index >=searcherAgents.size()) {
			index = 0;
		}
		return (AID) searcherAgents.get(index++);
	}


	/**
	 * The method invoked by the GUI when the user requests
	 * the assembling of a toy
	 */
	void assembleToy(final String type) {
		// Prepare the WorkflowDescriptor including the workflow class
		// and INPUT parameters
		Map params = new HashMap();
		params.put("toyType", type);
		WorkflowDescriptor wd = new WorkflowDescriptor("toysassembler.workflows.AssemblingToysWorkflow", params);
		try {
			// Dispatch the workflow to myself 
			dc.launchWorkflow(getAID(), wd, new WorkflowResultListener() {
				public void handleAssignedId(AID executor, String executionId) {
					// The workflow was properly loaded and a unique ID was assigned to it
					System.out.println("Workflow correctly loaded by performer "+executor.getLocalName());	
				}

				public void handleLoadError(String reason) {
					// The workflow could not be loaded
					System.out.println("Error loading the workflow");
					myGui.showMessage("Error loading the workflow. "+reason, "Error", JOptionPane.ERROR_MESSAGE);
				}

				public void handleNotificationError(AID executor, String executionId) {
					// There was a communication error receiving the notification from the executor
					System.out.println("Notification error ("+executionId+")");
					myGui.showMessage("Notification error received from performer "+executor.getName()+" for workflow "+executionId, "Error", JOptionPane.ERROR_MESSAGE);
				}

				public void handleExecutionError(ExecutionError er, AID executor, String executionId) {
					// The execution of the workflow failed
					System.out.println("Execution error ("+executionId+")");
					myGui.showMessage("Execution error received from performer "+executor.getName()+" for workflow "+executionId+" ["+er.getType()+": "+er.getReason()+"]", "Error", JOptionPane.ERROR_MESSAGE);
				}
				
				public void handleExecutionCompleted(jade.util.leap.List results, AID executor, String executionId) {
					// The workflow was successfully executed
					System.out.println("Execution OK ("+executionId+")");
					Map params = ElementDescriptor.paramListToMap(results);
					Boolean assembled = (Boolean) params.get("toyAssembled");
					String missingComponent = (String) params.get("missingComponent");
					if (!assembled.booleanValue()){
						myGui.showMessage("Can not assemble "+type+"!\n"+missingComponent+" not available!","Failure!!!",JOptionPane.WARNING_MESSAGE);
					}
					else{
						myGui.showMessage(type+" successfully assembled!","Success!!!",JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}, null);	
		} catch (WorkflowException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
