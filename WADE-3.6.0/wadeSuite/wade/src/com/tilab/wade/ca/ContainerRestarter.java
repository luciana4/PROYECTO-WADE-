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
package com.tilab.wade.ca;

import jade.content.lang.Codec.CodecException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.StartBackupMainContainer;
import com.tilab.wade.cfa.ontology.StartContainer;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.CAUtils;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.SimpleFipaRequestInitiator;

/**
 * Try to restart the whole container.
 * If this fails wait a bit and retry.
 * Do that N times (the host may have been restored)
 * At the end (unless we are dealing with a backup-main) try to restart the agents
 * one by one               | 
 *                          V                 OK
 *               ---->START-CONTAINER---------------------
 *              |           |                             |
 *              |           | KO                          |
 *              |           V           IS-BACKUP-MAIN    | 
 *              |     CHECK-GIVE-UP -----------           V       
 *              |           |      \           |       SUCCESS(F)
 *              |      MORE |       \          |          A
 *              |           V        \         V          |
 *               ------WAIT-A-BIT     \      ERROR(F)     |
 *                                     \       A          | 
 *                                     |       |KO        |
 *                                     V       |          |
 *                             START-AGENTS-VIA-RAA ------
 *                                                       OK  
 */
class ContainerRestarter extends FSMBehaviour {

	private static Logger myLogger = Logger.getMyLogger(ContainerRestarter.class.getName());

	private final static String START_CONTAINER = "START-CONTAINER";
	private final static String CHECK_GIVE_UP = "CHECK-GIVE-UP";
	private final static String WAIT_A_BIT = "WAIT-A-BIT";
	private final static String START_AGENTS_VIA_RAA = "START-AGENTS-VIA-RAA";
	private final static String SUCCESS = "SUCCESS";
	private final static String ERROR = "ERROR";

	private final static int CONTAINER_RESTART_OK = 0;
	private final static int CONTAINER_RESTART_KO = 1;
	private final static int START_AGENTS_VIA_RAA_OK = 2;
	private final static int START_AGENTS_VIA_RAA_KO = 3;
	private final static int RETRY = 4;
	private final static int RESTART_AGENTS = 5;
	private final static int GIVE_UP = 6;


	protected ContainerInfo cInfo;
	protected String containerName;
	protected AgentInfo controlAgentInfo;
	protected Ontology configurationOnto;
	private Boolean isMain;
	
	private int maxContainerRestartAttempts = ControllerAgent.MAX_CONTAINER_RESTART_ATTEMPTS_DEFAULT;
	private int containerRestartAttemptsCnt = 0;
	
	
	/**
	 * Inner class StartAgentsViaRAA
	 * Re-start agents in the dead container via RAA
	 */
	private class StartAgentsViaRAA extends SimpleBehaviour {

		private AID raa;
		private Iterator<AgentInfo> agentsIterator;
		private boolean error = false;
		private int successCnt = 0;

		public StartAgentsViaRAA() {
			super();
		}

		@Override
		public void onStart() {
			super.onStart();
			myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Try to restart agents of container "+cInfo.getName()+" using RAA");
			try {
				// Retrieve RuntimeAllocatorAgent AID
				raa = DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.RAA_AGENT_TYPE, null));
				if (raa == null) {
					myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - RAA not found. Cannot restart agents of container "+cInfo.getName());
					error = true;
				} 
				else {
					agentsIterator = cInfo.getAgents().iterator();
				}
			} catch (FIPAException fe) {
				myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - Error retrieving RAA from DF", fe);
				error = true;
			}
		}

		@Override
		public void action() {
			if (!error && agentsIterator.hasNext()) {
				AgentInfo ai = agentsIterator.next();
				try {
					myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Restarting agent "+ai.getName()+" via RAA ");
					String containerName = createAgentViaRAA(ai);	
					successCnt++;
					if (containerName.equals(ControlOntology.NO_CONTAINER)) {
						myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Agent "+ai.getName()+" must not be re-created");
					}
					else {
						myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Agent "+ai.getName()+" successfully restarted in container "+containerName);				
					}
				}
				catch (Exception e) {
					myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - Error restarting agent "+ai.getName()+" via RAA", e);
				}
			}
		}

		@Override
		public boolean done() {
			return error || (!agentsIterator.hasNext());
		}
		
		@Override
		public int onEnd() {
			if (successCnt < cInfo.getAgents().size()) {
				return START_AGENTS_VIA_RAA_KO;
			}
			else {
				// All agents successfully re-started
				return START_AGENTS_VIA_RAA_OK;
			}
		}

		/**
		 * Create an agent via the RAA  and return the name of the container the agent
		 * was re-created in or the ControlOntology.NO_CONTAINER constant if, according 
		 * to the RAA policies, the agent must not be re-created. 
		 */
		private String createAgentViaRAA(AgentInfo agentInfo) throws CodecException, OntologyException, FIPAException {
			// RAA uses the same ontology and the same CreateAgent action as CA
			// NOTE: There is no MessageTemplate conflict between this and the ManagementResponder
			// processing ControlOntology messages since this matches on an auto-generated in-reply-to
			// while the ManagementResponder matches REQUEST messages only.
			ACLMessage inform =  CAUtils.createAgent(myAgent, agentInfo, raa);
			if (inform != null) {
				try {
					return (String)((Result)myAgent.getContentManager().extractContent(inform)).getValue();
				} catch (Exception e) {
					myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - Error decoding CreateAgent response from RAA", e);
					return "UNKNOWN";
				}
			}
			else {
				// Timeout
				throw new FIPAException("Timeout waiting for response from RAA");
			}
		}
	} // END of inner class StartAgentsViaRAA
	
	
	/**
	 * Inner class StartContainerInHost
	 * Request the CFA to start a container (possibly a backup main) with a set of agents 
	 * in a given host
	 */
	private class StartContainerInHost extends SimpleFipaRequestInitiator {

		private String hostname;
		private int result;
		private AID cfa;

		public StartContainerInHost(AID cfa, String hostname) {
			super(null, null);
			this.hostname = hostname;
			this.cfa = cfa;
			result = CONTAINER_RESTART_KO;
		}

		protected ACLMessage prepareRequest(ACLMessage request) {
			if (cfa == null) {
				// CFA does not exist --> Go directly to the CreateAgentsViaRAA state
				containerRestartAttemptsCnt = maxContainerRestartAttempts;
				return null;
			}
			request = null;
			if (hostname != null) {
				if (isMain){
					myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Requesting cfa to restart backup-main container " + containerName + " in host " + hostname + ". Attempt # = "+containerRestartAttemptsCnt);
					StartBackupMainContainer startContainer = new StartBackupMainContainer();
					startContainer.setContainerName(containerName);
					startContainer.setHostName(hostname);
					try {
						request = ((ControllerAgent)myAgent).prepareRequest(cfa, startContainer, configurationOnto);
					} catch (Exception e) {
						myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - Error encoding StartBackUpMainContainer request", e);
					}
				}
				else{
					myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Requesting cfa to restart container " + containerName + " in host " + hostname + ". Attempt # = "+containerRestartAttemptsCnt);
					StartContainer startContainer = new StartContainer();
					startContainer.setContainerName(containerName);
					startContainer.setJavaProfile(getPropertyValue(controlAgentInfo, WadeAgent.JAVA_PROFILE));
					startContainer.setJadeProfile(getPropertyValue(controlAgentInfo, WadeAgent.JADE_PROFILE));
					startContainer.setProjectName(getPropertyValue(controlAgentInfo, WadeAgent.PROJECT_NAME));
					String tmp = getPropertyValue(controlAgentInfo, WadeAgent.JADE_ADDITIONAL_ARGS);
					if (tmp != null) {
						startContainer.setJadeAdditionalArgs(tmp.replace('#', ' '));
					}
					startContainer.setSplit("true".equals(getPropertyValue(controlAgentInfo, WadeAgent.SPLIT)));
					startContainer.setHostName(hostname);
					startContainer.setAgents(cInfo.getAgents());
					try {
						request = ((ControllerAgent)myAgent).prepareRequest(cfa, startContainer, configurationOnto);
					} catch (Exception e) {
						myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - Error encoding StartContainer request", e);
					}
				}
				
			} else {
				myLogger.log(Logger.WARNING, "CA "+myAgent.getName()+" - Missing hostname for container "+containerName);
			}
			return request;
		}

		protected void handleInform(ACLMessage inform) {
			result = CONTAINER_RESTART_OK;
			myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Container " + containerName + " successfully restarted in host " + hostname);
			try {
				Result result;
				result = (Result) myAgent.getContentManager().extractContent(inform);
				ContainerInfo container = (ContainerInfo)result.getValue();
				if (!container.getAgents().isEmpty()) {
					myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - Some agents in Container " + containerName + " could not be restarted correctly");
				}
			} catch (Exception e) {
				myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - error decoding StartContainer result", e);
			}
		}

		protected void handleError(ACLMessage msg) {
			myLogger.log(Level.SEVERE, "CA "+myAgent.getName()+" - "+ACLMessage.getPerformative(msg.getPerformative())+" message received as response from CFA to StartContainer request about container  " + cInfo.getName() +". Reason is "+msg.getContent());
		}

		protected void handleTimeout() {
			myLogger.log(Logger.WARNING, "CA "+myAgent.getName()+" - Timeout expired waiting for response from CFA to StartContainer request about container " + cInfo.getName() + ".");
		}

		@Override
		public int onEnd() {
			return result;
		}
	} // END of inner class StartContainerInHost
	

	private static AgentInfo removeCA(ContainerInfo ci) {
		AgentInfo result = null;
		Iterator<AgentInfo> it = ci.getAgents().iterator();
		AgentInfo ai;
		while(it.hasNext()) {
			ai = it.next();
			if(ai.getType().equals(WadeAgent.CONTROL_AGENT_TYPE)) {
				result = ai;
				it.remove();
				break;
			}
		}
		return result;
	}

	private void setRestartingAttribute(ContainerInfo ci) {
		Collection<AgentInfo> agents = ci.getAgents();
		for (AgentInfo agent : agents) {
			agent.addParameter(new AgentArgumentInfo(WadeAgent.RESTARTING, Boolean.toString(true)));
		}
	}

	private static String getPropertyValue(AgentInfo ai, String name) {
		String result = null;
		if (ai != null) {
			AgentArgumentInfo aai = ai.getParameter(name);
			if (aai != null) {
				result = (String)(aai.getValue());
			}
		}
		return result;
	}

	public ContainerRestarter(Agent a, AID cfa, ContainerInfo cInfo, String ipAddress) {
		super(a);
		this.cInfo = cInfo;
	
		configurationOnto = ConfigurationOntology.getInstance();
		Map extendedAttrs = cInfo.getExtendedAttributes();
		isMain = (Boolean)extendedAttrs.get(ControllerAgent.IS_MAIN);

		containerName = cInfo.getName();
		controlAgentInfo = removeCA(cInfo);
		setRestartingAttribute(cInfo);
		
		maxContainerRestartAttempts = ((ControllerAgent)myAgent).getIntConfig(ControllerAgent.MAX_CONTAINER_RESTART_ATTEMPTS_KEY, ControllerAgent.MAX_CONTAINER_RESTART_ATTEMPTS_DEFAULT);

		String hostname = null;
		if (controlAgentInfo != null) {
			// Try first with the Profile.EXPORT_HOST property.
			hostname = getPropertyValue(controlAgentInfo, Profile.EXPORT_HOST);
			if (hostname == null) {
				hostname = getPropertyValue(controlAgentInfo, WadeAgent.HOSTNAME);
			}
		} else {
			if(ipAddress != null){
				try {
					hostname = InetAddress.getByName(ipAddress).getHostName();
					myLogger.log(Level.INFO, "Retrieved hostname "+hostname + " for " + ipAddress);
				} catch (UnknownHostException e) {
					myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - Error retrieving host information for address "+ipAddress, e);
				}
			} else {
				myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - cannot retrieve host information for container "+containerName);
				// FIXME: We should exit immediately in this case
			}
		}
		
		registerFirstState(new StartContainerInHost(cfa, hostname), START_CONTAINER);
		registerState(new OneShotBehaviour() {
			@Override
			public void action() {
				containerRestartAttemptsCnt++;
			}
			
			public int onEnd() {
				if (containerRestartAttemptsCnt >= maxContainerRestartAttempts) {
					if (isMain) {
						// If we were trying to restart a backup-main, there is nothing more we can do 
						return GIVE_UP;
					}
					else {
						// If we were trying to restart a normal container, try to restart its agents one by one
						return RESTART_AGENTS;
					}
				}
				else {
					return RETRY;
				}
			}
			
		}, CHECK_GIVE_UP);
		registerState(new WakerBehaviour(a, 30000) {
			public void onStart() {
				super.onStart();
				myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Wait a bit before next attempt...");
			}
		}, WAIT_A_BIT);
		// StartAgentsViaRAA blocks waiting for response from RAA. In some cases however RAA may
		// ask ourselves to create an agent --> Use a ThreadedBehaviour to avoid deadlock
		registerState(((ControllerAgent) myAgent).tf.wrap(new StartAgentsViaRAA()), START_AGENTS_VIA_RAA);
		registerLastState(new OneShotBehaviour() {
			@Override
			public void action() {
				myLogger.log(Level.INFO, "CA "+myAgent.getName()+" - Container restart procedure for container "+containerName+" successfully completed");
			}
		}, SUCCESS);
		registerLastState(new OneShotBehaviour() {
			@Override
			public void action() {
				myLogger.log(Level.WARNING, "CA "+myAgent.getName()+" - Container restart procedure for container "+containerName+" failed");
			}
		}, ERROR);

		registerTransition(START_CONTAINER, SUCCESS, CONTAINER_RESTART_OK);
		registerDefaultTransition(START_CONTAINER, CHECK_GIVE_UP);
		registerTransition(CHECK_GIVE_UP, WAIT_A_BIT, RETRY);
		registerDefaultTransition(WAIT_A_BIT, START_CONTAINER, new String[]{START_CONTAINER, CHECK_GIVE_UP, WAIT_A_BIT});
		registerTransition(CHECK_GIVE_UP, START_AGENTS_VIA_RAA, RESTART_AGENTS);
		registerTransition(CHECK_GIVE_UP, ERROR, GIVE_UP);
		registerTransition(START_AGENTS_VIA_RAA, ERROR, START_AGENTS_VIA_RAA_KO);
		registerDefaultTransition(START_AGENTS_VIA_RAA, SUCCESS);
	}
}
