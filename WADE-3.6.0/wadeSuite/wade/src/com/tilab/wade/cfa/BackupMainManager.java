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
import test.common.JadeController;
import test.common.remote.RemoteManager;

import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.HostInfo;

/**
 * This behaviour is responsible for managing all startup operations related to a given backup main container, i.e.
 * - Creating the container
 * - Checking that a BCA is up and running
 */
class BackupMainManager extends FSMBehaviour {
	// FSM state names
	private static final String START_CONTAINER_STATE = "__Start_Container__";
	private static final String CHECK_BCA_STATE = "__Check_BCA__";

	private static final String DUMMY_FINAL_STATE = "__Dummy_final__";

	private static final String ERROR_STATE = "__Error__";

	// FSM state extit values
	private static final int CONTAINER_STARTED = 1;
	private static final int CONTAINER_NOT_STARTED = 2;

	private static final int BACKUP_CONTROLAGENT_STARTED = 3;
	private static final int BACKUP_CONTROLAGENT_NOT_STARTED = 4;

	// DataStore keys
	public static final String HOST_REMOTE_MANAGER_KEY = "__Host_remote_manager__";
	public static final String CONTAINER_INFO_KEY = "__Container_info__";

	private final static Logger logger = Logger.getMyLogger(BackupMainManager.class.getName());

	private ContainerInfo container;
	private RemoteManager hostRemoteManager;
	private HostInfo host;

	public BackupMainManager(HostInfo host) {
		this.host = host;
		registerFirstState(new BackupMainStarter(), START_CONTAINER_STATE);
		registerState(new BCAChecker(), CHECK_BCA_STATE);
		registerLastState(new OneShotBehaviour(myAgent) {
			public void action() {
				// Just do nothing

			}
		}, DUMMY_FINAL_STATE);
		registerLastState(new Error(), ERROR_STATE);

		registerTransition(START_CONTAINER_STATE, CHECK_BCA_STATE, CONTAINER_STARTED);
		registerDefaultTransition(START_CONTAINER_STATE, ERROR_STATE);
		registerTransition(CHECK_BCA_STATE, DUMMY_FINAL_STATE, BACKUP_CONTROLAGENT_STARTED);
		registerDefaultTransition(CHECK_BCA_STATE, ERROR_STATE);
	}

	public void onStart() {
		container = (ContainerInfo) getDataStore().get(CONTAINER_INFO_KEY);
		hostRemoteManager = (RemoteManager) getDataStore().get(HOST_REMOTE_MANAGER_KEY);
	}

	/**
	 * Inner class ContainerStarter.
	 * This behaviour is responsible for starting the currentContainer.
	 */
	private class BackupMainStarter extends OneShotBehaviour {
		private int result = CONTAINER_NOT_STARTED;

		public void action(){
			try {
				JadeController controller = ((ConfigurationAgent)myAgent).startBackupMain(hostRemoteManager, container.getName());
				container.setName(controller.getContainerName());
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
	 * This behaviour is responsible for checking if a BCA is up and running on the current Container.
	 */
	private class BCAChecker extends OneShotBehaviour {
		private static final long serialVersionUID = 11111123L;
		private int result = BACKUP_CONTROLAGENT_NOT_STARTED;

		public void action() {
			try {
				logger.log(Logger.FINE, "Searching for backup control agent of container " + container.getName() + "...");
				long bcaStartupTimeout = Long.parseLong(((ConfigurationAgent) myAgent).getCfaProperty(ConfigurationAgent.BACKUP_CONTROL_AGENT_STARTUP_TIMEOUT_KEY, ConfigurationAgent.BACKUP_CONTROL_AGENT_STARTUP_TIMEOUT_DEFAULT));
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(new AID("BCA-"+container.getName(), AID.ISLOCALNAME));
				DFAgentDescription[] descriptions = DFService.searchUntilFound(myAgent, myAgent.getDefaultDF(), dfd, null, bcaStartupTimeout);
				if(descriptions != null && descriptions.length == 1) {
					result = BACKUP_CONTROLAGENT_STARTED;
				}
				else {
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": BCA-"+container.getName()+ " did not start in due time");
					container.setErrorCode(CfaMessageCode.BCA_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "timeout");
				}
			} catch (FIPAException e) {
				logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Error searching for BCA-"+container.getName(),e);
				container.setErrorCode(CfaMessageCode.BCA_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}
		}

		public int onEnd() {
			return result;
		}

		public void reset() {
			super.reset();
			result = BACKUP_CONTROLAGENT_NOT_STARTED;
		}
	} // END of inner class BCAChecker

	/**
	 * Inner class Error.
	 * This behaviour is just a placeholder to make the ContainerManager behaviour abort.
	 */
	private class Error extends OneShotBehaviour {
		private static final long serialVersionUID = 11111126L;

		public void action() {
			host.addContainer(container);
			container.setJadeProfile("(internal main backup profile)");
			logger.log(Logger.SEVERE, "error starting backup main");
		}
	} // END of inner class Error

}
