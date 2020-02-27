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
package com.tilab.wade.cfa;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.util.Logger;

import java.util.Collection;

import test.common.remote.RemoteManager;

import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.ContainerProfileInfo;

/**
 * This behaviour is responsible for managing all startup operations related to a given container, i.e. 
 * - Creating the container
 * - Checking that a CA is up and running on the created container
 * - Starting all required agents through the CA.
 */
class ContainerManager extends FSMBehaviour {			
	// FSM state names
	private static final String START_CONTAINER_STATE = "__Start_Container__";
	private static final String CHECK_CA_STATE = "__Check_CA__";
	private static final String START_AGENTS_STATE = "__Start_Agents__";	
	
	private static final String ERROR_STATE = "__Error__";
	
	// FSM state extit values
	private static final int CONTAINER_STARTED = 0;
	private static final int CONTAINER_NOT_STARTED = 1;	
	
	private static final int CONTROLAGENT_STARTED = 0;
	private static final int CONTROLAGENT_NOT_STARTED = 1;
	
	// DataStore keys
	public static final String HOST_REMOTE_MANAGER_KEY = "__Host_remote_manager__";
	public static final String CONTAINER_INFO_KEY = "__Container_info__";

	private final static Logger logger = Logger.getMyLogger(ContainerManager.class.getName());

	private String hostName;
	private ContainerInfo container;
	private RemoteManager hostRemoteManager;
	private Collection<ContainerProfileInfo> containerProfiles;
	private AgentsStarter agentsStarter;

	public ContainerManager(String hostName, Collection<ContainerProfileInfo> containerProfiles, AgentStartingListener asl) {
		this.hostName = hostName;
		this.containerProfiles = containerProfiles;
		agentsStarter = new AgentsStarter(asl);

		registerFirstState(new ContainerStarter(), START_CONTAINER_STATE);
		registerState(new CAChecker(), CHECK_CA_STATE);
		registerLastState(agentsStarter, START_AGENTS_STATE);
		registerLastState(new Error(), ERROR_STATE);
		
		registerTransition(START_CONTAINER_STATE, CHECK_CA_STATE, CONTAINER_STARTED);
		registerDefaultTransition(START_CONTAINER_STATE, ERROR_STATE);
		registerTransition(CHECK_CA_STATE, START_AGENTS_STATE, CONTROLAGENT_STARTED);
		registerDefaultTransition(CHECK_CA_STATE, ERROR_STATE);
	}

	public ContainerManager(String hostName, Collection<ContainerProfileInfo> containerProfiles) {
		this(hostName, containerProfiles, null);
	}

	public void onStart() {
		container = (ContainerInfo) getDataStore().get(CONTAINER_INFO_KEY);
		agentsStarter.setContainer(container);
		
		hostRemoteManager = (RemoteManager) getDataStore().get(HOST_REMOTE_MANAGER_KEY);
	}
	
	
	/**
	 * Inner class ContainerStarter.
	 * This behaviour is responsible for starting the currentContainer.
	 */
	private class ContainerStarter extends OneShotBehaviour {	
		private static final long serialVersionUID = 11111122L;
		private int result = CONTAINER_NOT_STARTED;
		
		public void action(){
			try {
				((ConfigurationAgent)myAgent).startContainer(hostName, container, containerProfiles, hostRemoteManager);
				result = CONTAINER_STARTED;
			}
			catch (Exception e) {
				logger.log(Logger.SEVERE, "Unable to start container "+container.getName(), e);	
				container.setErrorCode(CfaMessageCode.CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}				
		}			

		public int onEnd() {
			return result;
		}
		
		public void reset() {
			super.reset();
			result = CONTAINER_NOT_STARTED;
		}
	} // END of inner class ContainerStarter
	

	/**
	 * Inner class CAChecker.
	 * This behaviour is responsible for checking if a CA is up and running on the current Container.
	 */
	private class CAChecker extends OneShotBehaviour {
		private static final long serialVersionUID = 11111123L;
		private int result = CONTROLAGENT_NOT_STARTED;
		
		public void action() {
			try {
				logger.log(Logger.FINE, "Searching for control agent of container " + container.getName() + "...");
				long caStartupTimeout = Long.parseLong(((ConfigurationAgent) myAgent).getCfaProperty(ConfigurationAgent.CONTROL_AGENT_STARTUP_TIMEOUT_KEY, ConfigurationAgent.CONTROL_AGENT_STARTUP_TIMEOUT_DEFAULT));
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(new AID("CA-"+container.getName(), AID.ISLOCALNAME));
				DFAgentDescription[] descriptions = DFService.searchUntilFound(myAgent, myAgent.getDefaultDF(), dfd, null, caStartupTimeout);
				if(descriptions != null && descriptions.length == 1) {
					result = CONTROLAGENT_STARTED;
				}
				else {
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": CA-"+container.getName()+ " did not start in due time");
					container.setErrorCode(CfaMessageCode.CA_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "timeout");
				}
			} catch (FIPAException e) {
				logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Error searching for CA-"+container.getName(),e);
				container.setErrorCode(CfaMessageCode.CA_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}
		}

		public int onEnd() {
			return result;
		}
		
		public void reset() {
			super.reset();
			result = CONTROLAGENT_NOT_STARTED;
		}
	} // END of inner class CAChecker
	
	

	/**
	 * Inner class Error.
	 * This behaviour is just a placeholder to make the ContainerManager behaviour abort.
	 */
	private class Error extends OneShotBehaviour {
		private static final long serialVersionUID = 11111126L;
		
		public void action() {
		}
	} // END of inner class Error
} 
