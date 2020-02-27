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
package com.tilab.wade.dispatcher;

import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OntologyServer;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.WrapperBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.commons.ontology.GetCurrentLoad;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.performer.ontology.ExecuteWorkflow;
import com.tilab.wade.performer.ontology.ThawWorkflow;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;

/**
 * An agent able to dispatch workflow execution requests to a set of 
 * executors according to a given policy.
 * Currently 2 policies are available:
 * - ROUND_ROBIN (the default policy)
 * - CURRENT_LOAD: all possible executors are asked for the current load 
 * according to the GetCurrentLoad action of the WadeManagement Ontology 
 * and the agent with the minimum load is selected.
 * Which policy to use is specified by means of the <code>DispatcherPolicy</code>
 * attribute that can by specified either as an agent argument or as a type
 * property.
 */
public class WorkflowDispatcherAgent extends WadeAgentImpl {

	private static final long serialVersionUID = 4442383458482501817L;

	private static final String DISPATCHER_POLICY_ATTRIBUTE = "DispatcherPolicy";
	private static final String DEFAULT_DISPATCHER_POLICY = DispatcherPolicyType.ROUND_ROBIN.name();
	private static final String GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE = "GetCurrentLoadTimeout";
	private static final long DEFAULT_GET_CURRENT_LOAD_TIMEOUT = 5000;
	
	private enum DispatcherPolicyType { ROUND_ROBIN, CURRENT_LOAD };
	
	private List<AID> executors = new ArrayList<AID>();
	private int currentExecutorIndex = 0;
	private long getCurrentLoadTimeout;
	private DispatcherPolicyType dispatcherPolicyType; 
	
	
	@Override
	protected void agentSpecificSetup() throws AgentInitializationException {
		getContentManager().registerOntology(WorkflowManagementOntology.getInstance());

		// Add the behaviour listening to WorkflowManagementOntology events request
		addBehaviour(new OntologyServer(this, WorkflowManagementOntology.getInstance(), new int[] {ACLMessage.REQUEST}, this));

		// Subscribe to the DF in order to receive workflow executor agent registrations/deregistration
		ACLMessage subscriptionMsg = DFService.createSubscriptionMessage(this, getDefaultDF(), getExecutorTemplate(), null);
		addBehaviour(new SubscriptionInitiator(this, subscriptionMsg) {

			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for (int i = 0; i < dfds.length; ++i) {
						AID agent = dfds[i].getName();
						if (dfds[i].getAllServices().hasNext()) {
							WorkflowDispatcherAgent.this.addExecutor(agent);
						} else {
							WorkflowDispatcherAgent.this.removeExecutor(agent);
						}
					}
				}
				catch (FIPAException e) {
					myLogger.log(Level.WARNING, "Agent "+WorkflowDispatcherAgent.this.getName()+" - Error deconding df executor notification", e);
				}
			}
		});
		
		// Get dispatcher-policy-type, try before in configuration arguments and then in types file   
		String dispatcherPolicyTypeAttribute = getArgument(DISPATCHER_POLICY_ATTRIBUTE, null);
		if (dispatcherPolicyTypeAttribute == null) {
			dispatcherPolicyTypeAttribute = getTypeProperty(DISPATCHER_POLICY_ATTRIBUTE, DEFAULT_DISPATCHER_POLICY);
		}
		dispatcherPolicyType = DispatcherPolicyType.valueOf(dispatcherPolicyTypeAttribute);

		// Get dispatcher-policy-type, try before in configuration arguments and then in types file   
		getCurrentLoadTimeout = getLongArgument(GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE, -1);
		if (getCurrentLoadTimeout == -1) {
			getCurrentLoadTimeout = getLongTypeProperty(GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE, DEFAULT_GET_CURRENT_LOAD_TIMEOUT);
		}
	}

	/**
	 * Specify the DF template to identify the executors to dispatch workflow
	 * execution requests to.
	 * By default all agents with ROLE = WorkflowExecutor are taken into account
	 */
	protected DFAgentDescription getExecutorTemplate() {
		ServiceDescription sd = new ServiceDescription();
		sd.addProperties(new Property(WadeAgent.AGENT_ROLE, WadeAgent.WORKFLOW_EXECUTOR_ROLE));
		
		DFAgentDescription executorTemplate = new DFAgentDescription();  
		executorTemplate.addServices(sd);

		return executorTemplate;
	}
	
	public void serveExecuteWorkflowRequest(ExecuteWorkflow ew, ACLMessage msg) throws Exception {
		dispatch(msg);
	}
	
	public void serveThawWorkflowRequest(ThawWorkflow tw, ACLMessage msg) throws Exception {
		dispatch(msg);
	}

	private void addExecutor(AID agent) {
		if (!executors.contains(agent)) {
			executors.add(agent);
			myLogger.log(Logger.CONFIG, "Agent "+getName()+" - Added executor "+agent);
		}
	}

	private void removeExecutor(AID agent) {
		executors.remove(agent);
		myLogger.log(Logger.CONFIG, "Agent "+getName()+" - Removed executor "+agent);
	}
	
	private void checkExecutors() {
		// Check if executors list is empty -> search directly on DF
		if (executors.size() == 0) {
			
			// Search executor agents with specific template 
			try {
				DFAgentDescription[] dfds = DFService.search(this, getDefaultDF(), getExecutorTemplate());
				for (int i = 0; i < dfds.length; ++i) {
					AID agent = dfds[i].getName();
					addExecutor(agent);
				}
			} catch (FIPAException e) {
				myLogger.log(Logger.SEVERE, "Agent "+getName()+" - Error searching executor agents", e);
			}
		}
	}

	private AID getExecutor() throws AsynchSelection {
		AID executor = null;
		
		checkExecutors();
		
		if (executors.size() != 0) {
			if (dispatcherPolicyType == DispatcherPolicyType.ROUND_ROBIN) {
				// Synchronous selection policy --> Directly return an executor
				executor = getRoundRobinExecutor();
			} 
			else if (dispatcherPolicyType == DispatcherPolicyType.CURRENT_LOAD) {
				// Asynchronous selection policy --> Provide a Behaviour (implementing the AgentSelector interface) that will select an executor
				throw new AsynchSelection(new CurrentLoadBasedSelectionBehaviour(executors));
			}
			else {
				myLogger.log(Logger.WARNING, "Agent "+getName()+" - Dispatcher policy "+dispatcherPolicyType+" not supported");
			}
		}
		
		return executor;
	}	
	
	private void dispatch(final ACLMessage msg) {
		try {
			AID executor = getExecutor();
			forward(executor, msg);
		}
		catch (AsynchSelection as) {
			final Behaviour selector = as.getSelectionBehaviour();
			SequentialBehaviour sb = new SequentialBehaviour() {
				public int onEnd() {
					// When the selection behaviour has completed forward the 
					// request to the selected agent
					AID executor = ((AgentSelector) selector).getSelectedAgent();
					forward(executor, msg);
					return super.onEnd();
				}
			};
			sb.addSubBehaviour(selector);
			addBehaviour(sb);
		}
	}
	
	private void forward(AID executor, ACLMessage msg) {
		if (executor != null) {
			msg.clearAllReceiver();
			msg.addReceiver(executor);
			if (!msg.getAllReplyTo().hasNext()) {
				msg.addReplyTo(msg.getSender());
			}
			msg.setSender(getAID());
			
			send(msg);
			
			myLogger.log(Logger.CONFIG, "Agent "+getName()+" - Dispatched "+msg.getConversationId()+" to "+executor);

		} else {
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.FAILURE);
			reply.setContent("No workflow executors available");
			send(reply);
			
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - No workflow executors available to execute "+msg.getConversationId());
		}
	}
	
	
	/**
	 * Inner class AsynchSelection
	 * This exception class is used to indicate that the selection policy 
	 * requires a behaviour to be executed. Such behaviour MUST implement
	 * the AgentSelector interface.
	 */
	private class AsynchSelection extends Exception {
		private Behaviour selectionBehaviour;
		
		AsynchSelection(Behaviour b) {
			super();
			selectionBehaviour = b;
		}
		
		private Behaviour getSelectionBehaviour() {
			return selectionBehaviour;
		}
	} // END of inner class AsynchSelection
	
	
	/**
	 * Inner interface AgentSelector
	 */
	private interface AgentSelector {
		AID getSelectedAgent();
	} // END of inner interface AgentSelector
	
	
	////////////////////////////////////////////////////
	// Round-Robin policy section
	////////////////////////////////////////////////////
	private AID getRoundRobinExecutor() {
		AID executorAgent = null;
		int executorsSize = executors.size();
		if (executorsSize != 0){
			executorAgent = executors.get(currentExecutorIndex++ % executorsSize);

			if (currentExecutorIndex >= executorsSize) {
				currentExecutorIndex = 0;
			}
		}
		
		return  executorAgent;
	}
	
	////////////////////////////////////////////////////
	// Current-Load policy section
	////////////////////////////////////////////////////
	private class CurrentLoadBasedSelectionBehaviour extends AchieveREInitiator implements AgentSelector {
		private AID[] candidates;
		private AID selectedAgent = null;
		
		CurrentLoadBasedSelectionBehaviour(List<AID> ids) {
			super(null, null);
			candidates = ids.toArray(new AID[0]);
		}
		
		@Override
		public Vector prepareRequests(ACLMessage msg) {
			Vector v = new Vector(1);
			msg = new ACLMessage(ACLMessage.REQUEST);
			for (AID id : candidates) {
				msg.addReceiver(id);
			}
			// Wait at most x sec for responses
			if (getCurrentLoadTimeout > 0) {
				msg.setReplyByDate(new Date(System.currentTimeMillis() + getCurrentLoadTimeout)); 
			}
			msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			msg.setOntology(WadeManagementOntology.getInstance().getName());
			// Dummy Actor
			Action action = new Action(new AID("YOU", AID.ISLOCALNAME), new GetCurrentLoad());
			try {
				getContentManager().fillContent(msg, action);
				v.add(msg);
			}
			catch (Exception e) {
				// Should never happen
				e.printStackTrace();
			}
			return v;
		}

		@Override
		public void handleAllResultNotifications(Vector resultNotifications) {
			if (resultNotifications.size() < candidates.length) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Only "+resultNotifications.size()+" replies to GetCurrentLoad request received in due time while "+candidates.length+" were expected");
			}
			int minimumLoad = WadeAgent.CURRENT_LOAD_UNKNOWN;
			Iterator it = resultNotifications.iterator();
			while (it.hasNext()) {
				ACLMessage msg = (ACLMessage) it.next();
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						Result r = (Result) getContentManager().extractContent(msg);
						int load = ((Integer) r.getValue()).intValue();
						if (load != WadeAgent.CURRENT_LOAD_UNKNOWN) {
							if (minimumLoad == WadeAgent.CURRENT_LOAD_UNKNOWN ||
								load < minimumLoad) {
								minimumLoad = load;
								selectedAgent = msg.getSender();
							}
						}
					}
					catch (Exception e) {
						// Should never happen
						e.printStackTrace();
					}
				}
			}
		}
		public AID getSelectedAgent() {
			return selectedAgent;
		}
	}
}
