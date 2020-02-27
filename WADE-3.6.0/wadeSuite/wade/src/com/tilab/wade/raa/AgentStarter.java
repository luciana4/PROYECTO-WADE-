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
package com.tilab.wade.raa;

import jade.content.ContentException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.Iterator;

import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.utils.CAUtils;

/**
 * This behaviour is responsible for activating an agent selecting the container according to the RAA policies.
 * More in details it<br>
 * - Gets the policy to apply to agent to be started<br>
 * - If present, run the policy allocation behaviour.
 * - Get the name of the container to start the agent into<br>
 * - Request the CA on the selected container to create the agent<br>
 * - If the policy is not able to select any container or the creation fails check if another policy applies and try again<br> 
 */
class AgentStarter extends FSMBehaviour {

	private final static Logger logger = Logger.getMyLogger(RuntimeAllocatorAgent.class.getName());

	// FST states
	private final static String GET_POLICY = "GET-POLICY";
	private final static String RUN_ALLOCATION_BEHAVIOUR = "RUN-ALLOCATION-BEHAVIOUR";
	private final static String CREATE_AGENT = "CREATE-AGENT";
	private final static String SUCCESS = "SUCCESS";
	private final static String ERROR = "ERROR";

	// States exit values
	public static final int POLICY_NOT_FOUND = 0;
	public static final int ALLOCATION_BEHAVIOUR_PRESENT = 1;
	public static final int ALLOCATION_BEHAVIOUR_NOT_PRESENT = 2;
	private final static int AGENT_STARTUP_OK = 0;
	private final static int AGENT_STARTUP_KO = 1;

	private RuntimeAllocatorAgent raa;
	private Iterator<AgentAllocationPolicy> allocationPoliciesIter;
	private AgentInfo info;
	private ACLMessage request;

	private Behaviour allocationBehaviour;
	private AgentAllocationPolicy allocationPolicy;
	private String containerName;
	private String errorMessage;

	
	protected AgentStarter(RuntimeAllocatorAgent raaAgent, ACLMessage createAgentRequest, final Action action, final AgentInfo info) {
		this.raa = raaAgent;
		allocationPoliciesIter = raa.getPolicies(info);
		this.info = info;		
		this.request = createAgentRequest;

		// STATES. Note that the RUN-ALLOCATION-BEHAVIOUR state is registered, if necessary, at runtime 
		registerFirstState(new GetPolicy(), GET_POLICY);
		registerState(new CreateAgent(), CREATE_AGENT);
		registerLastState(new OneShotBehaviour() {
			public void action() {
				ACLMessage reply = request.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				try {
					Result r = new Result(action, containerName);
					myAgent.getContentManager().fillContent(reply, r);
				}
				catch (ContentException ce) {
					logger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Error encoding CreateAgent result", ce);
				}
				reply.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
				myAgent.send(reply);
			}
		}, SUCCESS);

		registerLastState(new OneShotBehaviour() {
			public void action() {
				ACLMessage reply = request.createReply();
				reply.setPerformative(ACLMessage.FAILURE);
				reply.setContent("Cannot create agent " + info.getName()+ "["+errorMessage+"]");
				reply.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
				myAgent.send(reply);
			}
		}, ERROR);

		
		// TRANSITIONS
		registerTransition(GET_POLICY, RUN_ALLOCATION_BEHAVIOUR, ALLOCATION_BEHAVIOUR_PRESENT);
		registerTransition(GET_POLICY, CREATE_AGENT, ALLOCATION_BEHAVIOUR_NOT_PRESENT);
		registerDefaultTransition(GET_POLICY, ERROR);

		registerDefaultTransition(RUN_ALLOCATION_BEHAVIOUR, CREATE_AGENT);

		registerTransition(CREATE_AGENT, SUCCESS, AGENT_STARTUP_OK);
		registerDefaultTransition(CREATE_AGENT, GET_POLICY, new String[] {GET_POLICY, CREATE_AGENT});
	}

	
	/**
	 * Inner class GetPolicy
	 * Retrieve the first/next allocation policy suitable for the agent to be created and 
	 * register the allocation behaviour if any
	 */
	private class GetPolicy extends OneShotBehaviour {
		private int result;
		private boolean firstRun;

		private GetPolicy() {
			firstRun = true;
		}

		@Override
		public void action() {
			result = POLICY_NOT_FOUND;

			if (allocationPoliciesIter.hasNext()) {
				allocationPolicy = allocationPoliciesIter.next();
				logger.log(Logger.FINE, "Agent "+myAgent.getName()+": Trying to allocate agent "+info.getName()+" through policy "+allocationPolicy.getKey());
				allocationBehaviour = allocationPolicy.getAllocationBehaviour(info);
				if (allocationBehaviour != null) {
					// This policy requires the execution of a behaviour to identify the target container
					// Register it. 
					registerState(allocationBehaviour, RUN_ALLOCATION_BEHAVIOUR);
					result = ALLOCATION_BEHAVIOUR_PRESENT;
				}
				else {
					result = ALLOCATION_BEHAVIOUR_NOT_PRESENT;
				}
			}
			else {
				// No policy available --> Agent creation will fail 
				if (firstRun) {
					// Set a suitable error message. If this is not the first run, the error message is already 
					// filled with the last creation failure reason
					errorMessage = "No suitable policy found to allocate agent "+info.getName()+".";
					logger.log(Logger.SEVERE, "Agent " + raa.getName()+ " - "+errorMessage);
				}
			}
			firstRun = false;
		}

		@Override
		public int onEnd() {
			return result;
		}
	} // END of inner class GetPolicy

	
	/**
	 * Inner class CreateAgent
	 * Get the target container from the current allocation policy and create the agent 
	 * through the ControlAgent
	 */
	private class CreateAgent extends OneShotBehaviour {
		int result;

		public void action() {
			result = AGENT_STARTUP_KO;

			String failureReason = null;
			try {
				containerName = allocationPolicy.getContainer(info, allocationBehaviour);
				if (containerName != null) {
					AID ca = raa.getCA(containerName);
					if (ca != null) {
						logger.log(Logger.FINE, "Agent " + raa.getName()+ ": Creating agent " + info.getName()+ " in container " + containerName);
						ACLMessage response = CAUtils.createAgent(myAgent, info, ca);
						if (response != null) {
							// Agent successfully created
							logger.log(Logger.INFO, "Agent "+ raa.getName()+ ": Agent "+info.getName()+" successfully created in container "+ containerName);
							result = AGENT_STARTUP_OK;
						}
						else { 
							// Timeout expired 
							failureReason = "CA in container "+ containerName + " did not reply in due time";
						}
					}
					else {
						// CA does not exist
						failureReason = "CA in container "+ containerName + " does not exist";
					}
				}
				else {
					// No container selected
					failureReason = "No container selected";
				}
			}
			catch (DoNotCreateException dnce) {
				// The agent must not be (re)created
				logger.log(Logger.INFO, "Agent "+ raa.getName()+ ": Agent "+info.getName()+" must not be re-created");
				containerName = ControlOntology.NO_CONTAINER;
				result = AGENT_STARTUP_OK;
			}
			catch (Exception e) {
				// Error creating the agent
				failureReason = "Creation error: "+e.getMessage();
			}
			
			if (failureReason != null) {
				errorMessage = (errorMessage != null ? errorMessage+"; Policy "+allocationPolicy.getKey()+": "+failureReason : "Policy "+allocationPolicy.getKey()+": "+failureReason);
				logger.log(Logger.WARNING, "Agent "+ raa.getName()+ ": Cannot create agent "+info.getName()+" ["+failureReason+"]");
			}
		}

		public int onEnd(){
			return result;
		}
	} // END of inner class CreateAgent

}
