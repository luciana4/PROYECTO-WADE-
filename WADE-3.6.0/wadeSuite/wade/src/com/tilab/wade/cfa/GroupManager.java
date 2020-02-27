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

import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.cfa.beans.AgentBaseInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.AgentPoolInfo;
import com.tilab.wade.cfa.beans.ConfigurationLoaderException;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.GetGroupsStatus;
import com.tilab.wade.cfa.ontology.GroupStatus;
import com.tilab.wade.utils.CAUtils;

public class GroupManager {

	private final Logger myLogger = Logger.getMyLogger(getClass().getName());;
	
	private ConfigurationAgent cfa;
	private AID groupsLifeCycleTopic;
	private Map<String, GroupStatus> groupsStatus = new HashMap<String, GroupStatus>(); 

	
	GroupManager(ConfigurationAgent cfa) throws ServiceException {
		this.cfa = cfa;
		
		TopicManagementHelper topicHelper = (TopicManagementHelper) cfa.getHelper(TopicManagementHelper.SERVICE_NAME);
		groupsLifeCycleTopic = topicHelper.createTopic(ConfigurationOntology.GROUPS_LIFE_CYCLE_TOPIC);
	}
	
	void initGroupsStatus() throws ConfigurationLoaderException {
		groupsStatus.clear();
		for (String group : cfa.confLoader.getGroups()) {
			groupsStatus.put(group, new GroupStatus(group, ConfigurationOntology.DOWN_STATUS));
		}
		
		// Send a special notification to indicate the changing of groups
		notifyGroupLifecycleEvent(null);
	}

	Collection<GroupStatus> getGroupsStatus() {
		Collection<GroupStatus> gs = new ArrayList<GroupStatus>();
		gs.addAll(groupsStatus.values());
		return gs;
	}
	
	void startupGroup(final String groupName, final String platformStatus, final Object statusDetail, final Action actExpr, final ACLMessage request) {
		GroupStatus gs = getGroupStatus(groupName);
		if (gs != null) {
			String groupStatus = gs.getStatus();
			if (groupStatus.equals(ConfigurationOntology.DOWN_STATUS)) {
				PlatformInfo platformInfo = null;
				try {
					platformInfo = cfa.confLoader.loadConfiguration();
				} catch (ConfigurationLoaderException e){
					myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ":Cannot load configuration.", e);
					String errorReason = CfaMessageCode.LOAD_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage();
					cfa.sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
					return;
				}
				
				setGroupStatus(groupName, ConfigurationOntology.STARTING_STATUS);
				
				// Filter the platform-info for group
				filterByGroup(platformInfo, groupName, false);
				
				// Clean the detail from group info
				if (statusDetail instanceof PlatformInfo) {
					filterByGroup((PlatformInfo) statusDetail, groupName, true);
				}
				
				// Start group
				try{
					GroupStarter groupStarter = new GroupStarter(cfa, platformInfo, groupName) {

						@Override
						protected PlatformInfo getGlobalDetail(PlatformInfo groupDetail) {
							if (statusDetail instanceof String) {
								return groupDetail;
							} else {
								return mergeDetail((PlatformInfo)statusDetail, groupDetail);
							}
						}
					};
					cfa.addBehaviour(groupStarter);
					// This action may take some time --> We send back an AGREE
					cfa.sendNotification(actExpr, request, ACLMessage.AGREE, null);
				}
				catch (Exception e) {
					myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ":Cannot load configuration.", e);
					String errorReason = CfaMessageCode.LOAD_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage();
					cfa.sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
					
					cfa.getGroupManager().setGroupStatus(groupName, ConfigurationOntology.ERROR_STATUS);
					cfa.setGlobalStatus(ConfigurationOntology.ERROR_STATUS, errorReason);
				}
			} else {
				String errorReason = CfaMessageCode.GROUP_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "StartupGroup" + CfaMessageCode.ARGUMENT_SEPARATOR + groupName + CfaMessageCode.ARGUMENT_SEPARATOR + groupStatus;
				myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": " + errorReason);
				cfa.sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
			}
		} else {
			String errorReason = CfaMessageCode.GROUP_NAME_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "StartupGroup" + CfaMessageCode.ARGUMENT_SEPARATOR + groupName;
			myLogger.log(Logger.WARNING, "Agent " + cfa.getName() + ": " + errorReason);
			cfa.sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}				
	}
	
	void shutdownGroup(final String groupName, final String platformStatus, final Object statusDetail, final Action actExpr, final ACLMessage request) {
		GroupStatus gs = getGroupStatus(groupName);
		if (gs != null) {
			String groupStatus = gs.getStatus();
			if (groupStatus.equals(ConfigurationOntology.ACTIVE_STATUS) || groupStatus.equals(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS)) {
				
				setGroupStatus(groupName, ConfigurationOntology.SHUTDOWN_IN_PROGRESS);
				
				// 1) Ricostruisce la situazione attuale di agenti e pool
				// 2) Filtra agenti e pool per gruppo
				// 3) Uccide tutti gli agenti selezionati
				// 4) Se tutte le kill sono andate bene -> lo stato del gruppo diventa DOWN_STATUS
				//    altrimenti diventa ERROR_STATUS
				// 5) Lo stato globale diventa ACTIVE_INCOMPLETE se era ACTIVE, altrimenti resta invariato
				// 6) Se lo statusDetail era un PlatformInfo (la piattaforma era in ACTIVE_WITH_WORNING) 
				//    elimino (se presenti) tutti gli agenti/pool uccisi dallo statusDetail  
				cfa.addBehaviour(new PlatformConfigurationReconstructor(cfa, cfa.confLoader) {
					public int onEnd() {
						int exitCode = getExitCode();
						String errorReason = getErrorReason();
						if (exitCode == ConfigurationOntology.OK) {
							PlatformInfo pi = (PlatformInfo) getResult();

							// Filter the platform-info for group
							filterByGroup(pi, groupName, false);
							
							// Kill all group agents
							boolean error = killAgents(myAgent, pi);
							
							Object newStatusDetail = statusDetail;
							if (newStatusDetail instanceof PlatformInfo) {
								PlatformInfo statusPlatformInfo = (PlatformInfo) newStatusDetail;
								filterByGroup(statusPlatformInfo, groupName, true);
								
								if (statusPlatformInfo.getMainAgents().isEmpty() && 
									statusPlatformInfo.getHosts().isEmpty() &&
									statusPlatformInfo.getAgentPools().isEmpty()) {
									newStatusDetail = ConfigurationAgent.NO_DETAIL;
								}
							}
							
							if (error) {
								cfa.setGlobalStatus(ConfigurationOntology.ERROR_STATUS, ConfigurationAgent.NO_DETAIL);
								setGroupStatus(groupName, ConfigurationOntology.ERROR_STATUS);
								cfa.sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.SHUTDOWN_GROUP_ERROR);
							} else {
								String globalStatus = platformStatus;
								if (platformStatus.equals(ConfigurationOntology.ACTIVE_STATUS) ||
									platformStatus.equals(ConfigurationOntology.ACTIVE_INCOMPLETE_STATUS) ||
									(platformStatus.equals(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS) && newStatusDetail instanceof String && newStatusDetail.equals(ConfigurationAgent.NO_DETAIL))) {
									globalStatus = ConfigurationOntology.ACTIVE_INCOMPLETE_STATUS;
								} 
								cfa.setGlobalStatus(globalStatus, newStatusDetail);
								setGroupStatus(groupName, ConfigurationOntology.DOWN_STATUS);
								cfa.sendNotification(actExpr, request, ACLMessage.INFORM, null);
							}
						} else {
							myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": " + errorReason);
							setGroupStatus(groupName, ConfigurationOntology.ERROR_STATUS);
							cfa.setGlobalStatus(ConfigurationOntology.ERROR_STATUS, ConfigurationAgent.NO_DETAIL);
							cfa.sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.SHUTDOWN_GROUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + errorReason);
						}
						return 0;
					}
				});
			} else {
				String errorReason = CfaMessageCode.GROUP_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "ShutdownGroup" + CfaMessageCode.ARGUMENT_SEPARATOR + groupName + CfaMessageCode.ARGUMENT_SEPARATOR + groupStatus;
				myLogger.log(Logger.WARNING, "Agent " + cfa.getName() + ": " + errorReason);
				cfa.sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
			}
		} else {
			String errorReason = CfaMessageCode.GROUP_NAME_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "ShutdownGroup" + CfaMessageCode.ARGUMENT_SEPARATOR + groupName;
			myLogger.log(Logger.WARNING, "Agent " + cfa.getName() + ": " + errorReason);
			cfa.sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}			
	}
	
	void manageGroupStatus(String status, Object detail) {
		// If the detail is a PlatformInfo object (only at the end of platform-start behaviour)
		// set the status for single group; in other case the status is equals for all groups
		if (detail instanceof PlatformInfo) {
			// From PlatformInfo get the failed groups, for this groups set the passed 
			// status (ACTIVE_WITH_WARNINGS_STATUS or ERROR_STATUS); for the others groups 
			// set the ACTIVE_STATUS  
			Collection<String> failedGroups = getGroups((PlatformInfo) detail);
			for (String group : groupsStatus.keySet()) {
				if (failedGroups.contains(group)) {
					setGroupStatus(group, status);
				} else {
					setGroupStatus(group, ConfigurationOntology.ACTIVE_STATUS);
				}
			}
		} else {
			setAllGroupsStatus(status);
		}
	}
	
	public static Collection<String> getGroups(PlatformInfo pi) {
		Collection<String> groups = new java.util.ArrayList<String>();
		for (AgentInfo ai : pi.getMainAgents()) {
			addAgentToGroup(ai, groups);
		}
		for (HostInfo hi : pi.getHosts()) {
			for (ContainerInfo ci : hi.getContainers()) {
				for (AgentInfo ai : ci.getAgents()) {
					addAgentToGroup(ai, groups);
				}
			}
		}
		for (AgentPoolInfo api : pi.getAgentPools()) {
			addAgentToGroup(api, groups);
		}
		return groups;
	}
	
	private GroupStatus getGroupStatus(String group) {
		return groupsStatus.get(group);
	}
	
	void setGroupStatus(String group, String status) {
		GroupStatus gs = getGroupStatus(group);
		if (gs.getStatus() != status) {
			gs.setStatus(status);
			notifyGroupLifecycleEvent(group);
		}
	}
	
	private void filterByGroup(PlatformInfo pi, String groupName, boolean deleteGroup) {
		Iterator mainAgentIter = pi.getMainAgents().iterator();
		while (mainAgentIter.hasNext()) {
			filterAgentInfo(mainAgentIter, groupName, deleteGroup);
		}
		Iterator hostIter = pi.getHosts().iterator();
		while (hostIter.hasNext()) {
			HostInfo hi = (HostInfo) hostIter.next();
			Iterator containerIter = hi.getContainers().iterator();
			while (containerIter.hasNext()) {
				ContainerInfo ci = (ContainerInfo)containerIter.next();
				Iterator agentIter = ci.getAgents().iterator();
				while (agentIter.hasNext()) {
					filterAgentInfo(agentIter, groupName, deleteGroup);
				}
				if (ci.getAgents().size() == 0) {
					containerIter.remove();
				}
			}
			if (hi.getContainers().size() == 0) {
				hostIter.remove();
			}
		}
		Iterator agentPoolIter = pi.getAgentPools().iterator();
		while (agentPoolIter.hasNext()) {
			filterAgentInfo(agentPoolIter, groupName, deleteGroup);
		}
	}

	private void filterAgentInfo(Iterator it, String groupName, boolean deleteGroup) {
		AgentBaseInfo abi = (AgentBaseInfo) it.next();
		boolean belongsGroup = groupName.equals(abi.getGroup());
		if ((deleteGroup && belongsGroup) ||
			(!deleteGroup && !belongsGroup)) {
			it.remove();
		}
	}
	
	private void setAllGroupsStatus(String status) {
		for (String group : groupsStatus.keySet()) {
			setGroupStatus(group, status);
		}
	}

	private void notifyGroupLifecycleEvent(String group) {
		if (group == null) {
			myLogger.log(Logger.FINE, "Agent " + cfa.getName() + ": Issuing all groups lifecycle event");
		} else {
			myLogger.log(Logger.FINE, "Agent " + cfa.getName() + ": Issuing group <" + group + "> lifecycle event");
		}
		ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
		notification.addReceiver(groupsLifeCycleTopic);
		notification.setOntology(ConfigurationOntology.getInstance().getName());
		notification.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		Action actExpr = new Action(cfa.getAID(), new GetGroupsStatus());
		Object value;
		if (group != null) {
			value = getGroupStatus(group);
		} else {
			value = new jade.util.leap.ArrayList((ArrayList) getGroupsStatus());
		}
		Result r = new Result(actExpr, value);
		try {
			cfa.getContentManager().fillContent(notification, r);
			cfa.send(notification);
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": Error encoding group life cycle notification message.", e);
		}
	}
	
	private static void addAgentToGroup(AgentBaseInfo abi, Collection<String> groups) {
		String group = abi.getGroup();
		if (group != null && !groups.contains(group)) {
			groups.add(group);
		}
	}

	private PlatformInfo mergeDetail(PlatformInfo globalDetail, PlatformInfo groupDetail) {
		for (AgentInfo mainAgent : groupDetail.getMainAgents()) {
			// Update
			globalDetail.getMainAgents().remove(mainAgent);
			globalDetail.getMainAgents().add(mainAgent);
		}
		
		for (HostInfo groupHost : groupDetail.getHosts()) {
			HostInfo globalHost = globalDetail.getHost(groupHost.getName());
			if (globalHost != null) {
				for (ContainerInfo groupContainer : groupHost.getContainers()) {
					ContainerInfo globalContainer = globalHost.getContainer(groupContainer.getName());
					if (globalContainer != null) {
						for (AgentInfo groupAgent : groupContainer.getAgents()) {
							// Update
							globalContainer.getAgents().remove(groupAgent);
							globalContainer.getAgents().add(groupAgent);
						}
					} else {
						globalHost.addContainer(groupContainer);
					}
				}				
			} else {
				globalDetail.addHost(groupHost);
			}
		}
		
		for (AgentPoolInfo poolAgent : groupDetail.getAgentPools()) {
			// Update
			globalDetail.getAgentPools().remove(poolAgent);
			globalDetail.getAgentPools().add(poolAgent);
		}

		return globalDetail;
	}

	private boolean killAgents(Agent a, PlatformInfo pi) {
		boolean error = false;
		Iterator mainAgentIter = pi.getMainAgents().iterator();
		while (mainAgentIter.hasNext()) {
			AgentInfo ai = (AgentInfo) mainAgentIter.next();
			try {
				AgentController ac = cfa.getContainerController().getAgent(ai.getName());
				ac.kill();
			} catch (Exception e) {
				myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": Error killing main-agent " + ai.getName());
				error = true;
			} 
		}
		Iterator hostIter = pi.getHosts().iterator();
		while (hostIter.hasNext()) {
			HostInfo hi = (HostInfo) hostIter.next();
			Iterator containerIter = hi.getContainers().iterator();
			while (containerIter.hasNext()) {
				ContainerInfo ci = (ContainerInfo)containerIter.next();
				Iterator agentIter = ci.getAgents().iterator();
				while (agentIter.hasNext()) {
					AgentInfo ai = (AgentInfo) agentIter.next();
					AID aid = new AID(ai.getName(), AID.ISLOCALNAME);
					try {
						CAUtils.killAgent(a, aid);
					} catch (Exception e) {
						myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": Error killing agent " + aid);
						error = true;
					} 
				}
			}
		}
		Iterator agentPoolIter = pi.getAgentPools().iterator();
		while (agentPoolIter.hasNext()) {
			AgentPoolInfo api = (AgentPoolInfo) agentPoolIter.next();
			List<String> poolAgentNames = AgentPoolsManager.getAgentNames(api);
			for (String agentName : poolAgentNames) {
				AID aid = new AID(agentName, AID.ISLOCALNAME);
				try {
					CAUtils.killAgent(a, aid);
				} catch (Exception e) {
					myLogger.log(Logger.SEVERE, "Agent " + cfa.getName() + ": Error killing pool agent " + aid);
					error = true;
				} 
			}
		}
		return error;
	}
}
