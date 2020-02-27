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

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.AgentPoolInfo;
import com.tilab.wade.cfa.beans.ConfigurationLoader;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.UriUtils;

/**
 * This behaviour reconstructs the currently running configuration in form of a PlatformInfo object
 * @author Giovanni Caire - TILAB
 * @author Marco Ughetti - TILAB
 */
public class PlatformConfigurationReconstructor extends SequentialBehaviour {
	private ConfigurationLoader confLoader;

	private PlatformInfo result = null;
	private int exitCode = ConfigurationOntology.KO;
	private String errorReason;
	private Logger logger = Logger.getMyLogger(getClass().getName());
	private Map<String, AgentPoolInfo> agentPools = new HashMap<String, AgentPoolInfo>();	
	protected final Logger myLogger = Logger.getMyLogger(getClass().getName());;

	public PlatformConfigurationReconstructor(final Agent a, ConfigurationLoader cl) {
		super(a);
		confLoader = cl;

		// Step 1: Retrieve all hosts and containerProfiles
		addSubBehaviour(new OneShotBehaviour(a) {
			public void action() {
				// Retrieve the complete list of hosts. This is necessary not to loose hosts with no containers
				try {
					result = confLoader.loadConfiguration();
					Collection<HostInfo> hosts = result.getHosts();

					for(HostInfo singleHost : hosts) {
						singleHost.removeAllContainers();
					}
					
					
					Collection<AgentPoolInfo> pools = result.getAgentPools();
					for(AgentPoolInfo pool: pools) {
						pool.setSize(0);
						agentPools.put(pool.getName(), pool);
					}

				}
				catch (Exception e) {
					errorReason = CfaMessageCode.GET_HOSTS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage();
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorReason,e);
					skipNext();
				}
			}
		});

		// Step 2: Retrieve all containers and "assign" them to hosts if they are not
		addSubBehaviour(new OneShotBehaviour(a) {
			public void action() {
				// Retrieve the list of CA from the DF. Each CA represents a container
				try {
					DFAgentDescription[] dfds = DFUtils.searchAllByType(myAgent, TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE), null);
					for (int i = 0; i < dfds.length; ++i) {
						ServiceDescription caService = (ServiceDescription) dfds[i].getAllServices().next();
						String containerName = (String) DFUtils.getPropertyValue(caService, WadeAgent.AGENT_LOCATION);
						String hostName = (String)DFUtils.getPropertyValue(caService, WadeAgent.HOSTNAME);
						String jadeProfileName = (String)DFUtils.getPropertyValue(caService, WadeAgent.JADE_PROFILE);
						String javaProfileName = (String)DFUtils.getPropertyValue(caService, WadeAgent.JAVA_PROFILE);
						String split = (String)DFUtils.getPropertyValue(caService, WadeAgent.SPLIT);
						ContainerInfo container = new ContainerInfo();
						container.setName(containerName);
						container.setJadeProfile(jadeProfileName);
						container.setJavaProfile(javaProfileName);
						if ("true".equals(split)) {
							container.setSplit(true);
						}
						HostInfo host = getHost(result, hostName);
						if (host == null) {							
							// We found a container on a host that is not registered in the DB. Just add it
							host = new HostInfo();
							host.setName(hostName);
							result.addHost(host);
						}						
						host.addContainer(container);
					}
				}
				catch (Exception e) {
					errorReason = CfaMessageCode.ERROR_RETRIEVING_CONTAINERS_FROM_DF;
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorReason,e);
					skipNext();
				}
			}
		});

		// Step 3: For each container retrieve all agents in that container
		addSubBehaviour(new SimpleBehaviour(a) {
			private Iterator containersIt;
			private boolean failed = false;

			public void onStart() {
				Collection<ContainerInfo> containers = new ArrayList<ContainerInfo>(); 
				for (HostInfo hostInfo : result.getHosts()) {
					for (ContainerInfo containderInfo : hostInfo.getContainers()) {
						containers.add(containderInfo);
					}
				}
				containersIt = containers.iterator();
			}

			public void action() {
				if (containersIt.hasNext()) {
					ContainerInfo container = (ContainerInfo) containersIt.next();					
					try {					
						DFAgentDescription[] dfds = DFUtils.searchAllByType(myAgent, (String) null, new Property(WadeAgent.AGENT_LOCATION, container.getName()));
						String name;
						for (int i = 0; i < dfds.length; ++i) {
							name = dfds[i].getName().getLocalName();
							ServiceDescription service = (ServiceDescription) dfds[i].getAllServices().next();
							if(!isAgentToSkip(service)) {
								String poolName = getAgentPoolName(service);
								if (poolName != null) {
									AgentPoolInfo api = getAgentPoolInfo(poolName);
									if (api != null) {
										// increment size
										api.setSize(api.getSize()+1);
									}
								} else {
									AgentInfo agentInfo = ((ConfigurationAgent)myAgent).generateAgentInfo();
									AgentInfo agent = createAgentInfo(agentInfo, name, service);
									container.addAgent(agent);
								}
							}
						}
					}
					catch (Exception e) {
						errorReason = CfaMessageCode.ERROR_RETRIEVING_AGENTS_IN_CONTAINER_FROM_DF+CfaMessageCode.ARGUMENT_SEPARATOR+container.getName();
						logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorReason,e);
						skipNext();
					}
				}
			}

			public boolean done() {
				return (failed || (!containersIt.hasNext()));
			}
		});

		// Step 4: Retrieve all backup main containers
		addSubBehaviour(new OneShotBehaviour(a) {
			public void action() {
				// Retrieve the list of BCA from the DF. Each BCA represents a backup main container
				try {
					DFAgentDescription[] dfds = DFUtils.searchAllByType(myAgent, TypeManager.getInstance().getType(WadeAgent.BCA_AGENT_TYPE), null);
					result.setBackupsNumber(dfds.length);
				}
				catch (Exception e) {
					// FIXME assign errorReason a more appropriate error code
					errorReason = CfaMessageCode.ERROR_RETRIEVING_CONTAINERS_FROM_DF;
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorReason, e);
				}
			}
		});

		// Step 5: Manage main agents
		addSubBehaviour(new OneShotBehaviour(a) {
			public void action() {
								
				try {
					result.removeMainAgents();
					DFAgentDescription [] ads = DFUtils.searchAllByType(a, (AgentType)null, new Property(WadeAgent.MAIN_AGENT, "true"));
					for (int i = 0; i < ads.length; i++) {
						String agentName = ads[i].getName().getLocalName();
						ServiceDescription service = (ServiceDescription) ads[i].getAllServices().next();
											
						AgentInfo agentInfo = ((ConfigurationAgent)myAgent).generateAgentInfo();
						AgentInfo agent = createAgentInfo(agentInfo, agentName, service);
						result.addMainAgents(agent);						
					}
				}
				catch (Exception e) {
					myLogger.log(Logger.WARNING, "Agent " + a.getLocalName() + ": Error retrieving Main Container agents", e);
				}								
			}
		});

		// Step 6: Set the exitCode to OK
		addSubBehaviour(new OneShotBehaviour(a) {
			public void action() {
				exitCode = ConfigurationOntology.OK;
			}
		});
	}

	public int getExitCode() {
		return exitCode;
	}
	public String getErrorReason() {
		return errorReason;
	}
	public Object getResult() {
		return (exitCode == ConfigurationOntology.OK ? (Object) result : (Object) errorReason);
	}	

	private String getAgentPoolName(ServiceDescription service) {

		Iterator props = service.getAllProperties();
		while (props.hasNext()) {
			Property prop = (Property) props.next();
			String propName = prop.getName();
			if (propName.equals(WadeAgent.AGENT_POOL)) {
				return (String)prop.getValue();
			}
		}
		return null;
	}

	private HostInfo getHost(PlatformInfo platform, String hostName) {
		Iterator it = platform.getHosts().iterator(); 
		while (it.hasNext()) {
			HostInfo host = (HostInfo) it.next();
			if (UriUtils.compareHostNames(hostName, host.getName())) {
				return host;
			}
		}
		return null;
	}

	private AgentPoolInfo getAgentPoolInfo(String name) {
		AgentPoolInfo api = agentPools.get(name);
		if (api == null) {
			// if you are in the right mood and don't have nothing better to do,
			// feel free to modify this method in order to build a new AgentPoolInfo
			// using infos in the original AgentInfo...
		}
		return api;
	}

	private AgentInfo createAgentInfo(AgentInfo agent, String name, ServiceDescription service) {
		agent.setName(name);

		agent.setType(service.getType());
		agent.setOwner(service.getOwnership());
		Collection<AgentArgumentInfo> args = new HashSet<AgentArgumentInfo>();
		Iterator props = service.getAllProperties();
		while (props.hasNext()) {
			Property prop = (Property) props.next();
			String propName = prop.getName();
			if (propName.equals(WadeAgent.AGENT_CLASSNAME)) {
				agent.setClassName((String) prop.getValue());
			}
			else if (propName.equals(WadeAgent.AGENT_GROUP)) {
				agent.setGroup((String) prop.getValue());
			}
			else {
				// Discard properties not to be saved 
				if(!(propName.equals(WadeAgent.HOSTNAME) || propName.equals(WadeAgent.AGENT_LOCATION) || 
						propName.equals(WadeAgent.HOSTADDRESS) || propName.equals(WadeAgent.AGENT_ROLE)  ||
						propName.startsWith(WadeAgent.TRANSIENT_AGENT_ARGUMENT))) {
					args.add(new AgentArgumentInfo(prop.getName(),prop.getValue()));
				} 
			}
		}
		agent.setParameters(args);

		return agent;
	}

	private boolean isAgentToSkip(ServiceDescription caService) {
		String agentType = caService.getType();
		if (TypeManager.getInstance().getType(agentType).equals(TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE))
				|| TypeManager.getInstance().getType(agentType).equals(TypeManager.getInstance().getType(WadeAgent.CONFIGURATION_AGENT_TYPE))
				/*
			|| TypeManager.getInstance().getType(agentType).equals(TypeManager.getInstance().getType(WadeAgent.RAA_AGENT_TYPE))	
				 */
				) {
			return true;
		}
		return false;
	}
}
