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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.Vector;

import com.tilab.wade.ProjectVersionManager;
import com.tilab.wade.boot.BootDaemon;
import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.cfa.beans.AgentBaseInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.AgentPoolInfo;
import com.tilab.wade.cfa.beans.ConfigurationLoader;
import com.tilab.wade.cfa.beans.ConfigurationLoaderException;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.ContainerProfileInfo;
import com.tilab.wade.cfa.beans.ContainerProfilePropertyInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformElement;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.beans.VersionsInfo;
import com.tilab.wade.cfa.ontology.AddHost;
import com.tilab.wade.cfa.ontology.CompareRunningWithTargetConfiguration;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.ExportConfiguration;
import com.tilab.wade.cfa.ontology.GetAgentPools;
import com.tilab.wade.cfa.ontology.GetConfigurations;
import com.tilab.wade.cfa.ontology.GetContainerProfiles;
import com.tilab.wade.cfa.ontology.GetEventType;
import com.tilab.wade.cfa.ontology.GetEventTypes;
import com.tilab.wade.cfa.ontology.GetGlobalProperties;
import com.tilab.wade.cfa.ontology.GetGroupsStatus;
import com.tilab.wade.cfa.ontology.GetHosts;
import com.tilab.wade.cfa.ontology.GetMainAgents;
import com.tilab.wade.cfa.ontology.GetPlatformStatus;
import com.tilab.wade.cfa.ontology.GetRole;
import com.tilab.wade.cfa.ontology.GetRoleProperties;
import com.tilab.wade.cfa.ontology.GetRoles;
import com.tilab.wade.cfa.ontology.GetStatusDetail;
import com.tilab.wade.cfa.ontology.GetType;
import com.tilab.wade.cfa.ontology.GetTypeProperties;
import com.tilab.wade.cfa.ontology.GetTypes;
import com.tilab.wade.cfa.ontology.GetVersionsInfo;
import com.tilab.wade.cfa.ontology.ImportConfiguration;
import com.tilab.wade.cfa.ontology.KillContainer;
import com.tilab.wade.cfa.ontology.RemoveConfiguration;
import com.tilab.wade.cfa.ontology.RemoveHost;
import com.tilab.wade.cfa.ontology.ResetErrorStatus;
import com.tilab.wade.cfa.ontology.SaveConfiguration;
import com.tilab.wade.cfa.ontology.ShutdownGroup;
import com.tilab.wade.cfa.ontology.ShutdownPlatform;
import com.tilab.wade.cfa.ontology.StartAgent;
import com.tilab.wade.cfa.ontology.StartBackupMainContainer;
import com.tilab.wade.cfa.ontology.StartContainer;
import com.tilab.wade.cfa.ontology.StartupGroup;
import com.tilab.wade.cfa.ontology.StartupPlatform;
import com.tilab.wade.cfa.ontology.TypeManagementOntology;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AgentRole;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.EventType;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.commons.ontology.PrepareForShutdown;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.utils.AMSUtils;
import com.tilab.wade.utils.AgentUtils;
import com.tilab.wade.utils.CAUtils;
import com.tilab.wade.utils.CFAUtils;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.FileUtils;

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.AgentContainer;
import jade.core.ContainerID;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.ServiceException;
import jade.core.ServiceNotActiveException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OntologyServer;
import jade.core.messaging.TopicManagementFEService;
import jade.core.messaging.TopicManagementHelper;
import jade.core.messaging.TopicManagementService;
import jade.core.nodeMonitoring.UDPNodeMonitoringService;
import jade.core.replication.MainReplicationService;
import jade.domain.DFService;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.imtp.leap.LEAPIMTPManager;
import jade.imtp.leap.JICP.JICPPeer;
import jade.imtp.leap.JICP.JICPProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import test.common.JadeController;
import test.common.TestException;
import test.common.TestUtility;
import test.common.remote.RemoteManager;
import test.common.remote.TSDaemon;

public class ConfigurationAgent extends WadeAgentImpl {

	public static final String NO_DETAIL = "None";
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ConfigurationAgent configuration properties
	// These are specified either as Type properties or as Main Container configuration options prefixed with "cfa_"
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static String CFA_PREFIX = "cfa_"; 
	public static final String CONFIGURATION_LOADER_CLASS_KEY = "loader_class";
	public static final String CONFIGURATION_LOADER_CLASS_DEFAULT = "com.tilab.wade.cfa.beans.ConfigurationLoaderImpl";
	public static final String CONTROL_AGENT_STARTUP_TIMEOUT_KEY = "control_agent_startup_timeout";
	public static final String CONTROL_AGENT_STARTUP_TIMEOUT_DEFAULT = "30000";
	public static final String BACKUP_CONTROL_AGENT_STARTUP_TIMEOUT_KEY = "backup_control_agent_startup_timeout";
	public static final String BACKUP_CONTROL_AGENT_STARTUP_TIMEOUT_DEFAULT = "30000";
	public static final String HOST_REACHABLE_TIMEOUT_KEY = "host_reachable_timeout";
	public static final String HOST_REACHABLE_TIMEOUT_DEFAULT = "10000";
	public static final String AGENTS_STARTED_TIMEOUT_KEY = "agents_started_timeout";
	public static final String AGENTS_STARTED_TIMEOUT_DEFAULT = "15000";
	/** The key to retrieve the timeout in ms for the KillContainer request sent to the AMS to kill a container */
	public static final String CONTAINER_TERMINATION_TIMEOUT_KEY = "container_termination_timeout";
	public static final String CONTAINER_TERMINATION_TIMEOUT_DEFAULT = "10000";
	/** The key to retrieve the timeout in ms for giving a chance to agents to complete their ongoing activities when a smooth shutdown (platform or container) is requested */
	public static final String ACTIVITIES_COMPLETION_TIMEOUT_KEY = "shutdown_timeout";
	public static final String ACTIVITIES_COMPLETION_TIMEOUT_DEFAULT = "60000";
	/** The key to retrieve the indication whether to activate the automatic startup. When this property is specified, its value, if different from '*', it is interpreted as the name of a configuration to automatically import before platform startup */
	public static final String AUTOMATIC_STARTUP_KEY = "automatic_startup";
	public static final String AUTOMATIC_STARTUP_TARGET_CONFIGURATION = "*";
	public static final String BOOT_DAEMON_PORT_KEY = "bootdaemon_port";
	public static final String BOOT_DAEMON_NAME_KEY = "bootdaemon_name";	
	// Boot daemon port and name can be set, using the TestSuite style, as environment variables. 
	// Their setting according to the CFA style takes the precedence 
	public static final String TSBOOT_DAEMON_PORT_KEY = "tsdaemon.port";
	public static final String TSBOOT_DAEMON_NAME_KEY = "tsdaemon.name";

	// This default may be overridden by specifying a different class-name for the "Control Agent" type
	public static final String CONTROL_AGENT_CLASS_DEFAULT = "com.tilab.wade.ca.ControllerAgent";
	public static final String BACKUP_CONTROL_AGENT_CLASS_DEFAULT = "com.tilab.wade.bca.BackupControllerAgent";

	public static final String DEFAULT_PROFILE_PREFIX = "DEFAULT_PROFILE_";
	public static final String DEFAULT_JADE_PROFILE_PREFIX = "DEFAULT_PROFILE_JADE_";
	public static final String DEFAULT_JAVA_PROFILE_PREFIX = "DEFAULT_PROFILE_JAVA_";

	public static final String PROJECT_VERSION_MANAGER_CLASS_KEY = "project_version_manager_class";
	public static final String PROJECT_VERSIONS_FILE_KEY = "project_versions_file";
	public static final String DEFAULT_PROJECT_VERSIONS_FILE = "../versions.properties";
	
	ConfigurationLoader confLoader;
	private String controlAgentClass = CONTROL_AGENT_CLASS_DEFAULT;
	private String backupControlAgentClass = BACKUP_CONTROL_AGENT_CLASS_DEFAULT;

	private String platformName, platformDescription;

	private AID platformLifeCycleTopic;
	
	private VersionsInfo versionsInfo;
	
	private GroupManager groupManager;
	private OntologyServer typeManagementOntologyServer;
	
	///////////////////////////////////////////////////
	// Agent attributes
	///////////////////////////////////////////////////
	private String platformStatus = ConfigurationOntology.DOWN_STATUS;
	private Object statusDetail = NO_DETAIL;

	@AttributeGetter
	public String getPlatformStatus() {
		return platformStatus;
	}

	///////////////////////////////////////////////////
	// WadeAgent methods
	///////////////////////////////////////////////////
	protected void agentSpecificSetup() throws AgentInitializationException {
		// Register ontologies and codecs
		getContentManager().registerOntology(ConfigurationOntology.getInstance()); // To serve configuration requests
		getContentManager().registerOntology(ControlOntology.getInstance()); // To request CA the agent creations and other actions
		getContentManager().registerOntology(JADEManagementOntology.getInstance()); // To request the AMS to kill containers
		getContentManager().registerOntology(TypeManagementOntology.getInstance()); // To serve TypeManager requests

		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
			platformLifeCycleTopic = topicHelper.createTopic(ConfigurationOntology.PLATFORM_LIFE_CYCLE_TOPIC);
		} catch (ServiceException se) {
			throw new AgentInitializationException("Agent " + getName() + ": Error getting topic management helper", se);
		}

		// Initialize the ConfigurationLoader
		String confLoaderClass = getCfaProperty(CONFIGURATION_LOADER_CLASS_KEY, CONFIGURATION_LOADER_CLASS_DEFAULT);
		try {
			confLoader = (ConfigurationLoader) Class.forName(confLoaderClass).newInstance();
			confLoader.init(this);
		}
		catch (ConfigurationLoaderException cle) {
			throw new AgentInitializationException("Agent " + getName() + ": Error initializing ConfigurationLoader of class " + confLoaderClass + ". ", cle);
		}
		catch (Exception e) {
			throw new AgentInitializationException("Agent " + getName() + ": Error creating ConfigurationLoader of class " + confLoaderClass + ". ", e);
		}
		myLogger.log(Logger.INFO, "Agent " + getName() + ": ConfigurationLoader class is " + confLoader.getClass().getName());

		// Prepare group manager
		try {
			groupManager = new GroupManager(this);
			groupManager.initGroupsStatus();
		}
		catch (Exception cle) {
			throw new AgentInitializationException("Agent " + getName() + ": Error initializing configuration groups.", cle);
		}
		
		AgentType caType = TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE);
		if (caType != null) {
			controlAgentClass = caType.getClassName();
		}		
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Control-Agent class is " + controlAgentClass);
		AgentType bcaType = TypeManager.getInstance().getType(WadeAgent.BCA_AGENT_TYPE);
		if (bcaType != null) {
			backupControlAgentClass = bcaType.getClassName();
		}
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Backup-Main-Control-Agent class is " + backupControlAgentClass);
		
		// Initialize the WadeAgentLoader used to load agents classes from WADE classloader
		// FIX-ME: dovrebbe essere in grado di gestire il deploy
		//ObjectManager.addLoader(ObjectManager.AGENT_TYPE, new WadeAgentLoader(this));
				
		// Populate versionsInfo
		populateVersionsInfo();

		// Add initial behaviours
		addBehaviour(new ConfigurationRequestServer());
		
		// Add the behaviour listening to TypeManagementOntology events request
		typeManagementOntologyServer = new OntologyServer(this, TypeManagementOntology.getInstance(), ACLMessage.REQUEST, this);
		addBehaviour(typeManagementOntologyServer);

		if (isRestarting()) {
			// If we are restarting on a backup Main Container (elected as new master) the platform must be active.  
			platformStatus = ConfigurationOntology.ACTIVE_STATUS;
		}
		else {
			String automaticStartupConfiguration = getCfaProperty(AUTOMATIC_STARTUP_KEY, null);
			if (automaticStartupConfiguration != null) {
				// Activate automatic startup 
				if (!automaticStartupConfiguration.equals(AUTOMATIC_STARTUP_TARGET_CONFIGURATION)) {
					// Import the specified configuration first
					ImportConfiguration ic = new ImportConfiguration();
					ic.setName(automaticStartupConfiguration);
					serveImportConfiguration(ic, null, null);
				}
				serveStartupPlatform(new StartupPlatform(), null, null);
			}
		}
	}
	
	GroupManager getGroupManager() {
		return groupManager;
	}
	
	/**
	 * Utility method that allows getting CFA configuration properties either as Type properties 
	 * or as Main Container configuration options prefixed with "cfa_" 
	 */
	public String getCfaProperty(String key, String defaultValue) {
		String value = getTypeProperty(key, null);
		if (value == null) {
			// Try as a Main Container configuration option prefixed with "cfa_"
			value = getProperty(CFA_PREFIX+key, defaultValue);
		}
		return value;
	}
	
	
	public AgentInfo generateAgentInfo() {
		return new AgentInfo();
	}

	public void setQueueSize(int i) throws IllegalArgumentException {
		super.setQueueSize(i);	
	}

	protected String getPlatformName() {
		return platformName;
	}

	protected String getPlatformDescription() {
		return platformDescription;
	}

	/**
	 * Inner class ConfigurationRequestServer
	 * This behaviour is responsible for serving platform configuration requests (typically) from the Console
	 */
	private class ConfigurationRequestServer extends CyclicBehaviour {

		private MessageTemplate template = MessageTemplate.MatchOntology(ConfigurationOntology.getInstance().getName());

		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				try {
					Action actExpr = (Action) myAgent.getContentManager().extractContent(msg);
					AgentAction action = (AgentAction) actExpr.getAction();
					if (action instanceof StartupPlatform) {
						serveStartupPlatform((StartupPlatform) action, actExpr, msg);
					} else if (action instanceof ShutdownPlatform) {
						serveShutdownPlatform((ShutdownPlatform) action, actExpr, msg);
					} else if (action instanceof SaveConfiguration) {
						serveSaveConfiguration((SaveConfiguration) action, actExpr, msg);
					} else if (action instanceof AddHost) {
						serveAddHost((AddHost) action, actExpr, msg);
					} else if (action instanceof RemoveHost) {
						serveRemoveHost((RemoveHost) action, actExpr, msg);
					} else if (action instanceof StartContainer) {
						serveStartContainer((StartContainer) action, actExpr, msg);
					} else if (action instanceof StartBackupMainContainer) {
						serveStartBackupMainContainer((StartBackupMainContainer) action, actExpr, msg);
					} else if (action instanceof KillContainer) {
						serveKillContainer((KillContainer) action, actExpr, msg);
					} else if (action instanceof StartAgent) {
						serveStartAgent((StartAgent) action, actExpr, msg);
					} else if (action instanceof GetPlatformStatus) {
						serveGetPlatformStatus((GetPlatformStatus) action, actExpr, msg);
					} else if (action instanceof GetStatusDetail) {
						serveGetStatusDetail((GetStatusDetail) action, actExpr, msg);
					} else if (action instanceof CompareRunningWithTargetConfiguration) {
						serveCompareRunningWithTargetConfiguration((CompareRunningWithTargetConfiguration) action, actExpr, msg);
					} else if (action instanceof GetConfigurations) {
						serveGetConfigurations((GetConfigurations) action, actExpr, msg);
					} else if (action instanceof GetHosts) {
						serveGetHosts((GetHosts) action, actExpr, msg);
					}else if (action instanceof GetMainAgents) {
						serveGetMainAgents((GetMainAgents) action, actExpr, msg);
					} else if (action instanceof GetAgentPools) {
						serveGetAgentPools((GetAgentPools) action, actExpr, msg);
					} else if (action instanceof GetContainerProfiles) {
						serveGetContainerProfiles((GetContainerProfiles) action, actExpr, msg);
					} else if (action instanceof ImportConfiguration) {
						serveImportConfiguration((ImportConfiguration) action, actExpr, msg);
					} else if (action instanceof ExportConfiguration) {
						serveExportConfiguration((ExportConfiguration) action, actExpr, msg);
					} else if (action instanceof RemoveConfiguration) {
						serveRemoveConfiguration((RemoveConfiguration) action, actExpr, msg);
					} else if (action instanceof ResetErrorStatus) {
						serveResetErrorStatus((ResetErrorStatus) action, actExpr, msg);
					} else if (action instanceof GetVersionsInfo) {
						serveGetVersionsInfo((GetVersionsInfo) action, actExpr, msg);
					} else if (action instanceof StartupGroup) {
						serveStartupGroup((StartupGroup) action, actExpr, msg);
					} else if (action instanceof ShutdownGroup) {
						serveShutdownGroup((ShutdownGroup) action, actExpr, msg);
					} else if (action instanceof GetGroupsStatus) {
						serveGetGroupsStatus((GetGroupsStatus) action, actExpr, msg);
					} else {
						sendNotification(actExpr, msg, ACLMessage.REFUSE, CfaMessageCode.ACTION_NOT_SUPPORTED + CfaMessageCode.ARGUMENT_SEPARATOR + action.getClass().getName() + CfaMessageCode.ARGUMENT_SEPARATOR + "CONFIGURATION AGENT");
					}
				}
				catch (OntologyException oe) {
					myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Unable to extract request content", oe);
					sendNotification(null, msg, ACLMessage.NOT_UNDERSTOOD, CfaMessageCode.REQUEST_NOT_UNDERSTOOD + CfaMessageCode.ARGUMENT_SEPARATOR + oe.getMessage());
				}
				catch (CodecException ce) {
					myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Unable to extract request content", ce);
					sendNotification(null, msg, ACLMessage.NOT_UNDERSTOOD, CfaMessageCode.REQUEST_NOT_UNDERSTOOD + CfaMessageCode.ARGUMENT_SEPARATOR + ce.getMessage());
				}
				catch (Exception e) {
					myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Unexpected exception", e);
					sendNotification(null, msg, ACLMessage.FAILURE, CfaMessageCode.UNEXPECTED_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
				}
			} else {
				block();
			}
		}
	}


	///////////////////////////////////////////////////
	// ConfigurationOntology action serving methods
	///////////////////////////////////////////////////
	private void serveStartupPlatform(StartupPlatform sp, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.STARTUP_PLATFORM);
		if (platformStatus.equals(ConfigurationOntology.DOWN_STATUS)) {
			PlatformInfo platformInfo = null;
			try {
				platformInfo = confLoader.loadConfiguration();
			}catch (ConfigurationLoaderException e){
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ":Cannot load configuration.", e);
				String errorReason = CfaMessageCode.LOAD_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage();
				sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
				return;
			}
			
			setPlatformStatus(ConfigurationOntology.STARTING_STATUS, NO_DETAIL);
			try{
				// set platformName and platformDescription to be used in saveConfiguration
				platformName = platformInfo.getName();
				platformDescription = platformInfo.getDescription();
				PlatformStarter platformStarter = new PlatformStarter(this, platformInfo);
				addBehaviour(platformStarter);
				// This action may take some time --> We send back an AGREE
				sendNotification(actExpr, request, ACLMessage.AGREE, null);
			}
			catch (Exception e) {
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ":Cannot load configuration.", e);
				String errorReason = CfaMessageCode.LOAD_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage();
				sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
				setPlatformStatus(ConfigurationOntology.ERROR_STATUS, errorReason);
			}
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "StartupPlatform" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveShutdownPlatform(ShutdownPlatform sp, final Action actExpr, final ACLMessage request) throws Exception {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.SHUTDOWN_PLATFORM);
		if (CFAUtils.isPlatformActive(platformStatus))
		{
			setPlatformStatus(ConfigurationOntology.SHUTDOWN_IN_PROGRESS, NO_DETAIL);
			Vector<AID> controlAgents = new Vector<AID>();
			
			// While checking platform activities with CAs, also retrieve containers (and related hosts) to successively kill them 
			java.util.List<String> containers = new java.util.ArrayList<String>();
			java.util.List<String> hosts = new java.util.ArrayList<String>(); // Element i contains the host of ith container 
			final String [] containerNames;
			final String [] containerHosts;

			try {
				DFAgentDescription [] ads = DFUtils.searchAllByType(this, TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE), null);
				for (int i = 0; i < ads.length; i++) {
					ServiceDescription sd = (ServiceDescription) ads[i].getAllServices().next();
					String containerName = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
					if (here().getName().equals(containerName)){
						continue;
					}
					containers.add(containerName);
					// Note that if we have 2 containers in the same host, the hostname will appear 2 times in this list. This is exactly what we want as we need the correspondence container <--> host 
					hosts.add((String) DFUtils.getPropertyValue(sd, WadeAgent.HOSTNAME));

					if (!sp.getHardTermination()) {
						// If a hard termination is requested we don't send any termination request, but we kill all containers
						// in the onEnd() method in any case to have a uniform handling of exception
						controlAgents.add(ads[i].getName());
					}
				}

				ads = DFUtils.searchAllByType(this, TypeManager.getInstance().getType(WadeAgent.BCA_AGENT_TYPE), null);
				for (int i = 0; i < ads.length; i++) {
					ServiceDescription sd = (ServiceDescription) ads[i].getAllServices().next();
					String containerName = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
					if (here().getName().equals(containerName)){
						continue;
					}
					containers.add(containerName);
					hosts.add((String) DFUtils.getPropertyValue(sd, WadeAgent.HOSTNAME));
				}
				containerNames = containers.toArray(new String[] {});
				containerHosts = hosts.toArray(new String[] {});
			}
			catch (Exception e) {
				String errorReason = CfaMessageCode.UNEXPECTED_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Can't retrieve ControlAgents. " + e.getMessage();
				setPlatformStatus(ConfigurationOntology.ERROR_STATUS, errorReason);
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason, e);
				sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
				return;
			}

			try {
				int timeoutWorkflow = Integer.parseInt(getCfaProperty(ConfigurationAgent.ACTIVITIES_COMPLETION_TIMEOUT_KEY, ConfigurationAgent.ACTIVITIES_COMPLETION_TIMEOUT_DEFAULT));
				ACLMessage askAllCaRequest = createPrepareForShutdownRequest(controlAgents, timeoutWorkflow);

				addBehaviour(new ActivitiesTerminationWaiter(this, askAllCaRequest) {
					public int onEnd() {
						if (getExitCode() == ConfigurationOntology.KO) {
							myLogger.log(Logger.WARNING, "Agent " + myAgent.getName() + ": Timeout expired while waiting for activities termination on Container(s) " + getResult());
						}
						int timeoutKillContainer = Integer.parseInt(getCfaProperty(ConfigurationAgent.CONTAINER_TERMINATION_TIMEOUT_KEY, ConfigurationAgent.CONTAINER_TERMINATION_TIMEOUT_DEFAULT));
						for (int i = 0; i < containerNames.length; i++) {
							try {
								myLogger.log(Logger.FINEST, "CFA container is " + here().getName() + ". CA container is "+ containerNames[i]);
								if (here().getName().equals(containerNames[i])){
									myLogger.log(Logger.FINEST, "Discarded this container because is mine");
									continue;
								}
								myLogger.log(Logger.INFO, "Agent " + myAgent.getName() + ": Killing container " + containerNames[i]);
								killContainer(containerNames[i], containerHosts[i], timeoutKillContainer);
								myLogger.log(Logger.INFO, "Agent " + myAgent.getName() + ": Container " + containerNames[i] + " successfully killed");
							}
							catch (Exception e) {
								myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Error killing Container " + containerNames[i], e);
							}
						}
						
						killMainAgents();
						
						setPlatformStatus(ConfigurationOntology.DOWN_STATUS, NO_DETAIL);
						return 0;
					}

				});
				// This action may take some time --> We send back an AGREE
				sendNotification(actExpr, request, ACLMessage.AGREE, null);
			}
			catch (Exception e) {
				String errorReason = CfaMessageCode.UNEXPECTED_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "WorkflowTerminationRequest encoding failed. " + e.getMessage();
				setPlatformStatus(ConfigurationOntology.ERROR_STATUS, errorReason);
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason, e);
				sendNotification(actExpr, request, ACLMessage.FAILURE, errorReason);
			}
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "ShutdownPlatform" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void killMainAgents() {
		jade.wrapper.AgentContainer cc = getContainerController();
		try {
			DFAgentDescription [] ads = DFUtils.searchAllByType(this, (AgentType)null, new Property(WadeAgent.MAIN_AGENT, "true"));
			for (int i = 0; i < ads.length; i++) {
				String agentName = ads[i].getName().getLocalName();
				ServiceDescription sd = (ServiceDescription) ads[i].getAllServices().next();
				String containerName = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
				if (here().getName().equals(containerName)) {
					// This is a "Main Container agent" that actually lives in the local container (the Main) --> Kill it through the AgentController 
					try {
						AgentController ac = cc.getAgent(agentName);
						ac.kill();
					}
					catch (Exception e) {
						myLogger.log(Logger.WARNING, "Agent " + getLocalName() + ": Cannot kill Main Container agent " + agentName, e);
					}
				}
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent " + getLocalName() + ": Error retrieving Main Container agents", e);
		}
	}
	
	private void serveStartupGroup(StartupGroup sg, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.STARTUP_GROUP + " " + sg.getName());
		groupManager.startupGroup(sg.getName(), platformStatus, statusDetail, actExpr, request);
	}

	private void serveShutdownGroup(ShutdownGroup sg, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.SHUTDOWN_GROUP);
		groupManager.shutdownGroup(sg.getName(), platformStatus, statusDetail, actExpr, request);
	}
	
	private void serveGetGroupsStatus(GetGroupsStatus ggs, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_GROUPS_STATUS);
		sendNotification(actExpr, request, ACLMessage.INFORM, groupManager.getGroupsStatus());
	}
	
	private void serveAddHost(AddHost addHost, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.ADD_HOST);

		try {
			HostInfo hostInfo = addHost.getHostInfo();

			if (hostInfo == null) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.ADD_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Input data not valid, host null");
			}

			if (hostInfo.getName() == null || hostInfo.getName().equals("")) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.ADD_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Unable to add a host without hostname");
			}

			PlatformInfo platformInfo = confLoader.loadConfiguration();
			Collection<HostInfo> hosts = platformInfo.getHosts();

			if (hosts.contains(hostInfo)) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.ADD_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Unable to add a host with this name: " + hostInfo.getName() + "; host already present");
			}

			platformInfo.addHost(hostInfo);
			confLoader.storeConfiguration(platformInfo);
			sendNotification(actExpr, request, ACLMessage.INFORM, null);

		} catch (ConfigurationLoaderException e) {
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.ADD_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}

	}

	private void serveRemoveHost(RemoveHost removeHost, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.REMOVE_HOST);

		try {
			String hostname = removeHost.getHostname();

			if (hostname == null || hostname.equals("")) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.REMOVE_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Unable to remove a host without hostname");
			}

			boolean notFound = true;

			PlatformInfo platformInfo = confLoader.loadConfiguration();

			Iterator it = platformInfo.getHosts().iterator();
			while (it.hasNext()) {
				HostInfo host = (HostInfo) it.next();
				if (host.getName().equalsIgnoreCase(hostname)) {
					it.remove();
					notFound = false;
				}
			}

			if (notFound) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.REMOVE_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "Unable to remove a host with this hostname :" + hostname + "; hostname not present in the configuration");
			}

			confLoader.storeConfiguration(platformInfo);
			sendNotification(actExpr, request, ACLMessage.INFORM, null);

		} catch (ConfigurationLoaderException e) {
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.REMOVE_HOST_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}
	
	/**
	 * Extends the ContainerManager behaviour used to launch a container at platform startup to send back 
	 * a suitable notification to the requester on completion
	 */
	private class RuntimeContainerStarter extends ContainerManager {

		private Action actExpr;
		private ACLMessage request;

		public RuntimeContainerStarter(String hostName, Collection<ContainerProfileInfo> profileInfos, Action actExpr, ACLMessage request) {
			super(hostName, profileInfos);
			this.actExpr = actExpr;
			this.request = request;
		}

		public int onEnd() {
			int performative;
			Object result;
			ContainerInfo container = (ContainerInfo) getDataStore().get(ContainerManager.CONTAINER_INFO_KEY);

			String cec = container.getErrorCode();
			if (cec == null) {
				performative = ACLMessage.INFORM;
				result = container;
			} else {
				performative = ACLMessage.FAILURE;
				result = CfaMessageCode.CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + cec;
			}
			sendNotification(actExpr, request, performative, result);
			return 0;
		}
	}  // END if inner class RuntimeContainerStarter
	

	private void serveStartContainer(StartContainer sc, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.START_CONTAINER);

		if (CFAUtils.isPlatformActive(platformStatus)) {
			try {
				// Retrieve the RemoteManager for the host where to start the container and check host availability/reachability
				HostInfo host = new HostInfo();
				host.setName(sc.getHostName());

				int timeOut = Integer.parseInt(getCfaProperty(ConfigurationAgent.HOST_REACHABLE_TIMEOUT_KEY, ConfigurationAgent.HOST_REACHABLE_TIMEOUT_DEFAULT));
				RemoteManager hostManager = checkHostAvailability(host, timeOut);
				if (hostManager != null) {
					// Initialize a ContainerInfo object for the new container
					ContainerInfo container = new ContainerInfo(sc.getContainerName());
					container.setProjectName(sc.getProjectName());
					container.setJavaProfile(sc.getJavaProfile());
					container.setJadeProfile(sc.getJadeProfile());
					container.setJadeAdditionalArgs(sc.getJadeAdditionalArgs());
					container.setAgents(sc.getAgents());

					// Add the behaviour actually launching the container. This gets the host RemoteManager and the ContainerInfo in the DataStore  
					ContainerManager cm = new RuntimeContainerStarter(host.getName(), confLoader.getContainerProfiles(), actExpr, request);
					DataStore ds = new DataStore();
					ds.put(ContainerManager.CONTAINER_INFO_KEY, container);
					ds.put(ContainerManager.HOST_REMOTE_MANAGER_KEY, hostManager);
					cm.setDataStore(ds);
					addBehaviour(cm);
				} 
				else {
					sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + host.getErrorCode());
				}
			}
			catch (ConfigurationLoaderException cle) {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.LOAD_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + cle.getMessage());
			}
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "StartContainer" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveStartAgent(StartAgent sa, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.START_AGENT);

		if (CFAUtils.isPlatformActive(platformStatus)) {
			try {
				AgentInfo ai = sa.getAgent();
				if (ai.getClassName() == null) {
					ai.setClassName(TypeManager.getSafeClassName(ai));
				}
				
				if (sa.getContainerName() == null) {
					// No container specified -> send request to RAA
				
					// Search the RAA
					AID raaAID;
					DFAgentDescription raaTemplate  = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(WadeAgent.RAA_AGENT_TYPE);
					raaTemplate.addServices(sd);
					
					DFAgentDescription dfds[] = DFService.search(this, raaTemplate);
					if (dfds != null && dfds.length > 0) {
						raaAID = dfds[0].getName();
						
						CAUtils.createAgent(this, ai, raaAID);
					} 
					else {
						throw new Exception("RAA agent not found on DF");
					}
				}
				else {
					// Container specified -> send request to CA of the container
					AID aidCA = CAUtils.getCAOnLocation(this, sa.getContainerName());
					if (aidCA != null) {
						CAUtils.createAgent(this, ai, aidCA);
					}
					else {
						throw new Exception("CA agent not found on container "+sa.getContainerName());
					}
				}
				
				sendNotification(actExpr, request, ACLMessage.INFORM, null);
			} 
			catch (Exception e) {
				myLogger.log(Logger.SEVERE, "Agent "+getName()+": Unable to start agent "+sa.getAgent().getName()+(sa.getContainerName()!=null?(" on conatiner "+sa.getContainerName()):""), e);
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}
		} 
		else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "StartAgent" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}
	
	private RemoteManager getAvailableHost(HostInfo originalHost) {
		RemoteManager manager = null;
		int timeOut = Integer.parseInt(getCfaProperty(ConfigurationAgent.HOST_REACHABLE_TIMEOUT_KEY, ConfigurationAgent.HOST_REACHABLE_TIMEOUT_DEFAULT));
		Collection<HostInfo> hosts;
		try {
			hosts = confLoader.getHosts();
			for (Iterator<HostInfo> it = hosts.iterator(); (it.hasNext() && manager == null);) {
				HostInfo host = it.next();
				if (!host.getName().equals(originalHost.getName()))
					manager = checkHostAvailability(host, timeOut);
			}
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot get available host ", e);
		}
		return manager;
	}

	private void serveStartBackupMainContainer(StartBackupMainContainer sbm, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.START_BACKUP_MAIN_CONTAINER);
		HostInfo host = new HostInfo();
		host.setName(sbm.getHostName());
		
		if (!getMainReplication()){
			myLogger.log(Logger.SEVERE, "Backup main container won't be launched because MainReplication service is not active");
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.BACKUP_MAIN_CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + CfaMessageCode.NOT_ACTIVE_MAIN_REPLICATION_SERVICE);
		}else{
			int timeOut = Integer.parseInt(getCfaProperty(ConfigurationAgent.HOST_REACHABLE_TIMEOUT_KEY, ConfigurationAgent.HOST_REACHABLE_TIMEOUT_DEFAULT));
		
			RemoteManager hostManager = checkHostAvailability(host, timeOut);
		
			if (hostManager == null) {
				//find an available host
				hostManager = getAvailableHost(host);
			}

			if (hostManager != null) {
				BackupMainManager cm = new BackupMainManager(host) {
					public int onEnd() {
						int performative;
						Object result;
						ContainerInfo container = (ContainerInfo) getDataStore().get(ContainerManager.CONTAINER_INFO_KEY);
						String cec = container.getErrorCode();
						if (cec == null) {
							myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+": Backup MainContainer "+container.getName()+" correctly started");
							performative = ACLMessage.INFORM;
							result = container;
						} else {
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+": Backup MainContainer startup failed ["+cec+"]");
							performative = ACLMessage.FAILURE;
							result = CfaMessageCode.BACKUP_MAIN_CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + cec;
						}
						sendNotification(actExpr, request, performative, result);
						return 0;
					}
				};

				DataStore ds = new DataStore();
				ds.put(ContainerManager.CONTAINER_INFO_KEY, new ContainerInfo(sbm.getContainerName()));
				ds.put(ContainerManager.HOST_REMOTE_MANAGER_KEY, hostManager);
				cm.setDataStore(ds);
		
				addBehaviour(cm);
		
			} else {
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.BACKUP_MAIN_CONTAINER_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + host.getErrorCode());
			}
		}
	}


	private void serveKillContainer(final KillContainer kc, final Action actExpr, final ACLMessage request) throws Exception {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.KILL_CONTAINER + ". Container is " + kc.getContainerName());

		if (!kc.getContainerName().equals(AgentContainer.MAIN_CONTAINER_NAME)) {
			if (CFAUtils.isPlatformActive(platformStatus))
			{
				Vector<AID> controlAgents = new Vector<AID>();
				AID ca = CAUtils.getCAOnLocation(this, kc.getContainerName());

				try {
					int timeoutWorkflow = Integer.parseInt(getCfaProperty(ConfigurationAgent.ACTIVITIES_COMPLETION_TIMEOUT_KEY, ConfigurationAgent.ACTIVITIES_COMPLETION_TIMEOUT_DEFAULT));
					if (!kc.getHardTermination()) {
						// If an hard termination is requested we don't send any termination request, but we kill the container
						// in the onEnd() method in any case to have a uniform handling of exception
						controlAgents.add(ca);
					}
					ACLMessage caRequest = createPrepareForShutdownRequest(controlAgents, timeoutWorkflow);

					addBehaviour(new ActivitiesTerminationWaiter(this, caRequest) {
						public int onEnd() {
							if (getExitCode() == ConfigurationOntology.KO) {
								myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Timeout expired while waiting for workflow termination on Container " + kc.getContainerName() + ". " + getResult());
							}
							try {
								int timeout = Integer.parseInt(getCfaProperty(ConfigurationAgent.CONTAINER_TERMINATION_TIMEOUT_KEY, ConfigurationAgent.CONTAINER_TERMINATION_TIMEOUT_DEFAULT));
								// FIXME: Pass the correct host-name
								killContainer(kc.getContainerName(), null, timeout);
								sendNotification(actExpr, request, ACLMessage.INFORM, null);
							}
							catch (Exception e) {
								myLogger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": Error killing Container " + kc.getContainerName(), e);
								sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.KILL_CONTAINER_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + kc.getContainerName() + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
							}
							return 0;
						}
					});

				}
				catch (Exception e) {
					// Should never happen
					myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Error encoding CA request.", e);
					sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.CA_REQUEST_ENCODING_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
				}
			} else {
				String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "KillContainer" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
				myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
				sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
			}
		} else {
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": Unable to kill Container " + kc.getContainerName());
			String errorReason = CfaMessageCode.KILL_CONTAINER_REFUSE + CfaMessageCode.ARGUMENT_SEPARATOR + "Main-Container";
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveGetPlatformStatus(GetPlatformStatus getPlatformStatus, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_PLATFORM_STATUS);
		sendNotification(actExpr, request, ACLMessage.INFORM, platformStatus);
	}

	private void serveGetStatusDetail(GetStatusDetail gsd, Action actExpr, ACLMessage request) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_STATUS_DETAIL);
		sendNotification(actExpr, request, ACLMessage.INFORM, statusDetail);
	}

	private void serveCompareRunningWithTargetConfiguration(CompareRunningWithTargetConfiguration compareRunningWithTargetConfiguration, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.COMPARE_RUNNING_WITH_TARGET_CONFIGURATION);
		
		// Check if platform is active
		if (CFAUtils.isPlatformActive(platformStatus))
		{
			// Reconstruct running configuration
			addBehaviour(new PlatformConfigurationReconstructor(this, confLoader) {
				public int onEnd() {
					int exitCode = getExitCode();
					if (exitCode == ConfigurationOntology.OK) {
						
						// Get target configuration
						PlatformInfo targetPlatformInfo = null;
						try {
							targetPlatformInfo = confLoader.loadConfiguration();
						} catch (ConfigurationLoaderException e) {
							myLogger.log(Logger.SEVERE, "Agent " + getName() + " Cannot get target configuration ", e);
							sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.COMPARE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
							return 1;
						}

						PlatformInfo runningPlatformInfo = (PlatformInfo)getResult();
						runningPlatformInfo.setName("Actual configuration");
						// Compare configurations
						PlatformInfo comparedConfiguration = compareConfiguration(runningPlatformInfo, targetPlatformInfo);
						sendNotification(actExpr, request, ACLMessage.INFORM, comparedConfiguration);
						return 0;
					
					} else {
						// Error reconstructing configuration
						String errorReason = getErrorReason();
						myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason);
						sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.COMPARE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + errorReason);
						return 2;
					}
				}
			});
		} else {
			// Error: platform not active
			String errorReason = CfaMessageCode.COMPARE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "platform not active";
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}
	
	private void serveResetErrorStatus(ResetErrorStatus resetErrorState, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.RESET_ERROR_STATUS + ".");
		if (platformStatus.equals(ConfigurationOntology.ERROR_STATUS)) {
			setPlatformStatus(ConfigurationOntology.DOWN_STATUS, NO_DETAIL);
			sendNotification(actExpr, request, ACLMessage.INFORM, null);
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "ResetErrorState" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveGetHosts(GetHosts getHosts, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_HOSTS);
		try {
			int timeOut = Integer.parseInt(getCfaProperty(ConfigurationAgent.HOST_REACHABLE_TIMEOUT_KEY, ConfigurationAgent.HOST_REACHABLE_TIMEOUT_DEFAULT));
			Collection<HostInfo> hosts = confLoader.getHosts();
			for (HostInfo host : hosts) {
				//the following method already set the errorCode if the related host is not available or reachable
				checkHostAvailability(host, timeOut);
				// FIXME: Remove this when lazy initialization of collections will be added
				host.removeAllContainers();
			}
			sendNotification(actExpr, request, ACLMessage.INFORM, hosts);
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot get hosts ", e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.GET_HOSTS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}

	private void serveGetMainAgents(GetMainAgents getMainAgents, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_MAIN_AGENTS);
		try {
			Collection<AgentInfo> mainAgents = confLoader.getMainAgents();
			sendNotification(actExpr, request, ACLMessage.INFORM, mainAgents);
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + " Cannot get main container agents ", e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.GET_MAIN_AGENTS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}
	
	private void serveGetAgentPools(GetAgentPools getAgentPools, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_AGENT_POOLS);
		try {
			Collection<AgentPoolInfo> agentPools = confLoader.getAgentPools();
			sendNotification(actExpr, request, ACLMessage.INFORM, agentPools);
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + " Cannot get Agent Pools ", e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.GET_AGENT_POOLS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}
		
	private void serveGetContainerProfiles(GetContainerProfiles gcp, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_CONTAINER_PROFILES);
		try {
			Collection<ContainerProfileInfo> containerProfiles = confLoader.getContainerProfiles();
			sendNotification(actExpr, request, ACLMessage.INFORM, containerProfiles);
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + " Cannot get Container Profiles ", e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.GET_CONTAINER_PROFILES_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}

	private void serveSaveConfiguration(final SaveConfiguration sc, final Action actExpr, final ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.SAVE_CONFIGURATION);

		if (CFAUtils.isPlatformActive(platformStatus))
		{
			addBehaviour(new PlatformConfigurationReconstructor(this, confLoader) {
				public int onEnd() {
					int exitCode = getExitCode();
					String errorReason = getErrorReason();
					if (exitCode == ConfigurationOntology.OK) {
						try {
							PlatformInfo platformInfo = (PlatformInfo) getResult();
							confLoader.storeConfiguration(platformInfo);
							sendNotification(actExpr, request, ACLMessage.INFORM, null);
						}
						catch (Exception e) {
							errorReason = "Error saving current configuration";
							myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason, e);
							sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.SAVE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + errorReason);
						}
					} else {
						myLogger.log(Logger.SEVERE, "Agent " + getName() + ": " + errorReason);
						sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.SAVE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + errorReason);
					}
					return 0;
				}
			});
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "SaveConfiguration" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveGetConfigurations(GetConfigurations getConfigurations, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_CONFIGURATIONS);
		try {
			Collection<String> configurations = confLoader.listConfigurations();
			sendNotification(actExpr, request, ACLMessage.INFORM, configurations);
		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot return configurations", e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.GET_CONFIGURATIONS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}

	private void serveImportConfiguration(ImportConfiguration importConfiguration, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.IMPORT_CONFIGURATION + ". Configuration is " + importConfiguration.getName());
		if (platformStatus.equals(ConfigurationOntology.DOWN_STATUS)) {
			try {
				confLoader.importConfiguration(importConfiguration.getName());
				groupManager.initGroupsStatus();
				sendNotification(actExpr, request, ACLMessage.INFORM, null);
			} catch (ConfigurationLoaderException e) {
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot import configuration " + importConfiguration.getName(), e);
				sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.IMPORT_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + importConfiguration.getName() + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}
		} else {
			String errorReason = CfaMessageCode.PLATFORM_STATUS_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "ImportConfiguration" + CfaMessageCode.ARGUMENT_SEPARATOR + platformStatus;
			myLogger.log(Logger.WARNING, "Agent " + getName() + ": " + errorReason);
			sendNotification(actExpr, request, ACLMessage.REFUSE, errorReason);
		}
	}

	private void serveExportConfiguration(ExportConfiguration exportConfiguration, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.EXPORT_CONFIGURATION + ". Configuration is " + exportConfiguration.getName());
		try {
			confLoader.exportConfiguration(exportConfiguration.getName(), exportConfiguration.getDescription(), exportConfiguration.getOverride());
			sendNotification(actExpr, request, ACLMessage.INFORM, null);

		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot export configuration " + exportConfiguration.getName(), e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.EXPORT_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + exportConfiguration.getName() + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}

	}

	private void serveRemoveConfiguration(RemoveConfiguration removeConfiguration, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Serving action " + ConfigurationOntology.REMOVE_CONFIGURATION + ". Configuration is " + removeConfiguration.getName());
		try {
			confLoader.deleteConfiguration(removeConfiguration.getName());
			sendNotification(actExpr, request, ACLMessage.INFORM, null);

		} catch (ConfigurationLoaderException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Cannot remove configuration " + removeConfiguration.getName(), e);
			sendNotification(actExpr, request, ACLMessage.FAILURE, CfaMessageCode.REMOVE_CONFIGURATION_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + removeConfiguration.getName() + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}

	}

	private void serveGetVersionsInfo(GetVersionsInfo getVersionsInfo, Action actExpr, ACLMessage request) {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action " + ConfigurationOntology.GET_VERSIONS_INFO);
		sendNotification(actExpr, request, ACLMessage.INFORM, versionsInfo);
	}
	
	private void populateVersionsInfo() {
		versionsInfo = new VersionsInfo();
		
		// Jade version
		jade.core.VersionManager jvm = new jade.core.VersionManager();
		versionsInfo.setJadeVersion(jvm.getVersion());
		versionsInfo.setJadeRevision(jvm.getRevision());
		versionsInfo.setJadeDate(jvm.getDate());
		
		// Wade version
		com.tilab.wade.VersionManager wvm = new com.tilab.wade.VersionManager();
		versionsInfo.setWadeVersion(wvm.getVersion());
		versionsInfo.setWadeRevision(wvm.getRevision());
		versionsInfo.setWadeDate(wvm.getDate());
		
		// Project version
		String projectVersion = ProjectVersionManager.UNKNOWN;
		String projectRevision = ProjectVersionManager.UNKNOWN;
		String projectDate = ProjectVersionManager.UNKNOWN;
		
		// Try to use OLD project version managing
		String projectVersionManagerClass = getCfaProperty(PROJECT_VERSION_MANAGER_CLASS_KEY, null);
		if (projectVersionManagerClass != null) {
			try {
				Object versionManager = Class.forName(projectVersionManagerClass).newInstance();
				Class c = versionManager.getClass();
				java.lang.reflect.Method m = c.getMethod("getVersion", new Class[0]);
				projectVersion = (String) m.invoke(versionManager, new Object[0]);
				projectVersion = projectVersion.startsWith("$")?"snapshot":projectVersion;
				m = c.getMethod("getRevision", new Class[0]);
				projectRevision = (String) m.invoke(versionManager, new Object[0]);
				m = c.getMethod("getDate", new Class[0]);
				projectDate = (String) m.invoke(versionManager, new Object[0]);
			}
			catch (Exception e) {
				// VersionManager not available: keep defaults
			}
		}
		else {
			// Use NEW project version managing
			ProjectVersionManager pvm = new ProjectVersionManager();
			projectVersion = pvm.getVersion();
			projectRevision = pvm.getRevision();
			projectDate = pvm.getDate();
		}
		versionsInfo.setProjectVersion(projectVersion);
		versionsInfo.setProjectRevision(projectRevision);
		versionsInfo.setProjectDate(projectDate);
		versionsInfo.setProjectOriginalRevision(getProjectOriginalRevision(projectVersion, projectRevision));
	}
	
	private String getProjectOriginalRevision(String projectVersion, String projectRevision) {
		// Sanity check
		if (projectVersion == null || projectVersion.isEmpty() || projectVersion.equals(ProjectVersionManager.UNKNOWN) ||
			projectRevision == null || projectRevision.isEmpty() || projectRevision.equals(ProjectVersionManager.UNKNOWN)) {
			return null;
		}
		
		// Try to read file versions.properties
		String projectVersionsFilename = getCfaProperty(PROJECT_VERSIONS_FILE_KEY, DEFAULT_PROJECT_VERSIONS_FILE);
		Properties props = new Properties();
		FileInputStream fis = null; 
		try {
			fis = new FileInputStream(projectVersionsFilename);
			props.load(fis);
		} 
		catch (FileNotFoundException fnfe) {
			// versions.properties not present
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Error reading " + projectVersionsFilename, e);
			return null;
		}
		finally {
			if (fis != null) {
				try { fis.close(); } catch (IOException e) {}
			}
		}
		
		// Get original revision for the projectVersion
		String projectOriginalRevision = props.getProperty(projectVersion);
		if (projectOriginalRevision == null) {
			// Version not present -> set this as original revision
			props.setProperty(projectVersion, projectRevision);
			projectOriginalRevision = projectRevision;
			
			// Write the file versions.properties
			FileOutputStream fos = null; 
			try {
				fos = new FileOutputStream(projectVersionsFilename);
				props.store(fos, null);
			} 
			catch (Exception e) {
				myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Error writing " + projectVersionsFilename, e);
			}
			finally {
				if (fos != null) {
					try { fos.close(); } catch (IOException e) {}
				}
			}
		}

		return projectOriginalRevision;
	}

	///////////////////////////////////////////////////
	// Utility methods
	///////////////////////////////////////////////////

	/**
	 * This method sends back to the requester the result of an action in a uniform way
	 * regardless of whether or not the action succeeded.
	 *
	 * @param actExpr	  The Action expression that embedded the served action
	 * @param request	  The message that embedded the request to serve the action
	 * @param performative The ACL performative to use in the reply
	 * @param result	   The result (if any) produced by the action in case of success or an error code
	 *                     in case of failure.
	 */
	void sendNotification(Action actExpr, ACLMessage request, int performative, Object result) {
		// The request message is null in the case of self-initiated actions. Don't send any notification in these cases 
		if (request != null) {
			// Send back a proper reply to the requester
			ACLMessage reply = request.createReply();
			if (performative == ACLMessage.INFORM) {
				reply.setPerformative(ACLMessage.INFORM);
				try {
					ContentElement ce = null;
					if (result != null) {
						// If the result is a java.util.List, convert it into a jade.util.leap.List to make the ontology "happy"
						if (result instanceof java.util.List) {
							ArrayList l = new ArrayList();
							l.fromList((java.util.List) result);
							result = l;
						}
						ce = new Result(actExpr, result);
					} else {
						ce = new Done(actExpr);
					}
					getContentManager().fillContent(reply, ce);
				}
				catch (OntologyException oe) {
					myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Unable to send notification" + oe);
					oe.printStackTrace();
				}
				catch (CodecException ce) {
					myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Unable to send notification" + ce);
				}
			} else {
				reply.setPerformative(performative);
				if (result != null)
					reply.setContent(result.toString());
			}
			reply.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
			send(reply);
		}
	}

	/**
	 * Create a message to REQUEST a set of agents the CheckWorking action
	 */
	private ACLMessage createPrepareForShutdownRequest(Vector<AID> receivers, long timeout) throws Exception {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		for (AID receiver : receivers) {
			request.addReceiver(receiver);
		}
		request.setLanguage(codec.getName());
		request.setOntology(WadeManagementOntology.getInstance().getName());
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.setReplyByDate(new Date(System.currentTimeMillis() + timeout));

		Action actExpr = new Action(this.getAID(), new PrepareForShutdown());
		getContentManager().fillContent(request, actExpr);
		return request;
	}

	/**
	 * Actually start a remote container
	 * This is package-scoped as it is called by the PlatformStarter behaviour
	 */
	JadeController startContainer(String hostName, ContainerInfo container, Collection<ContainerProfileInfo> profiles, RemoteManager hostManager) throws TestException {
		String jvmArgs = getProfileArgs(container.getJavaProfile(), profiles, false);
		String jadeArgs = getJadeArgs(container, profiles);
		// If not explicitly set, set the export-host property to the name of the host as it appears in the configuration file 
		if (jadeArgs.indexOf(" -"+Profile.EXPORT_HOST) < 0) {
			jadeArgs = jadeArgs+" -"+Profile.EXPORT_HOST+" "+hostName;
		}
		
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Launching container " + container.getName() + " with JavaArgs " + jvmArgs + " and JadeArgs " + jadeArgs);
		JadeController controller = start(hostManager, container.getName(), jvmArgs, jadeArgs, container.getProjectName());
		container.setName(controller.getContainerName());
		return controller;
	}
	
	JadeController startBackupMain(RemoteManager hostManager, String containerName) throws TestException {
		String jvmArgs = getProfileArgs(null, null, false);
		// FIXME: Add to jvmArgs suitable JVM args of the local process (the master Main Container)
		String jadeArgs = getBackupMainArgs(containerName);
		
		myLogger.log(Logger.INFO, "Agent " + getName() + ": Launching backup-main-container " + containerName + " with JavaArgs " + jvmArgs + " and JadeArgs " + jadeArgs);
		return start(hostManager, containerName, jvmArgs, jadeArgs, null);
	}
	
	private JadeController start(RemoteManager hostManager, String instanceName, String jvmArgs, String jadeArgs, String projectName) throws TestException {
		// If a projectName is specified (either explicitly passed - container belonging to a child project - or 
		// set in the project-name system property - standard WADE-project startup) pass it to the new container
		// as a system property. 
		// In case of a child project also set the parent-project-name system property to 
		// the current project-name.
		// Furthermore include +${projName-classpath} in the classpath.
		// ${projName-classpath} will be replaced by the WADE BootDaemon with the indicated project classpath
		// + indicates that this is an additional classpath (managed by JADE TestUtility) 
		// 
		// If no project-name is set (pure WADE container startup) do not specify any
		// classpath. The new container will be launched with the Bootdaemon classpath (WADE
		// classes only)
		// 
		// See also comment in Bootdaemon.launchJadeInstance()
		String classpath = null;
		String parentProjectName = null;
		if (projectName != null) {
			parentProjectName = System.getProperty(BootDaemon.PROJECT_NAME);
		}
		else {
			projectName = System.getProperty(BootDaemon.PROJECT_NAME);
		}
		if (projectName != null) {
			if (parentProjectName == null) {
				// Standard WADE-project container startup
				myLogger.log(Logger.INFO, "Agent "+getName()+" - Standard WADE-project container startup: Project-name="+projectName);
				jvmArgs = jvmArgs + " "+BootDaemon.PROJECT_NAME_DEF+projectName;
			}
			else {
				// Child-project container startup
				myLogger.log(Logger.INFO, "Agent "+getName()+" - Child-project container startup: Project-name="+projectName+", Parent-project-name="+parentProjectName);
				jvmArgs = jvmArgs + " "+BootDaemon.PROJECT_NAME_DEF+projectName + " "+BootDaemon.PARENT_PROJECT_NAME_DEF+parentProjectName;
			}
			classpath = "+${"+projectName+"-classpath}";
		}
		else {
			myLogger.log(Logger.INFO, "Agent "+getName()+" - WADE-only container startup: No project-name specified");
		}

		String bootstrapClass = null;
		if (jadeArgs.contains(WadeAgent.SPLIT+" true")) { 
			// Split container
			bootstrapClass = "com.tilab.wade.SplitBoot";
		}
		else {
			// Normal container
			bootstrapClass = "com.tilab.wade.Boot";
		}
		return TestUtility.launch(hostManager, instanceName, classpath, jvmArgs, bootstrapClass, jadeArgs, null, null);
	}

	/**
	 * @return The command line to start a container specified by a given ContainerInfo.
	 *         Proper options are added to create a ControlAgent and to pass it the JADE and JAVA profile names if any.
	 * @throws ConfigurationLoaderException
	 */
	private String getJadeArgs(ContainerInfo container, Collection<ContainerProfileInfo> profiles) {
		StringBuffer jadeArgs = new StringBuffer();

		jadeArgs.append(getContainerArgs(container));
		if (container.getSplit()) {
			jadeArgs.append(getSplitContainerProfileArgs(container.getJadeProfile(), profiles));
			jadeArgs.append(" -").append(WadeAgent.SPLIT).append(" true");
		}
		else {
			jadeArgs.append(getProfileArgs(container.getJadeProfile(), profiles, true));
		}

		if (container.getJadeProfile() != null && !container.getJadeProfile().equals("")) {
			jadeArgs.append(" -").append(WadeAgent.JADE_PROFILE).append(" ").append(container.getJadeProfile());
		}
		if (container.getJavaProfile() != null && !container.getJavaProfile().equals("")) {
			jadeArgs.append(" -").append(WadeAgent.JAVA_PROFILE).append(" ").append(container.getJavaProfile());
		}
		if (container.getJadeAdditionalArgs() != null) {
			jadeArgs.append(" ").append(container.getJadeAdditionalArgs());
			jadeArgs.append(" -").append(WadeAgent.JADE_ADDITIONAL_ARGS).append(" ").append(container.getJadeAdditionalArgs().replace(' ', '#'));
		}
		if (container.getProjectName() != null) {
			jadeArgs.append(" -").append(WadeAgent.PROJECT_NAME).append(" ").append(container.getProjectName());
		}
		jadeArgs.append(" -").append(WadeAgent.PLATFORM_STARTUP_TIME).append(" ").append(getStartupTime().getTime());

		// Specify the -container option unless this is a backup Main Container or a split container
		if (container.getSplit() || jadeArgs.indexOf("-backupmain ") < 0) {
			jadeArgs.append(" ").append("-container");	
		}

		// If the connection-timeout property is specified locally, propagate it unless explicitly set 
		if ((jadeArgs.indexOf(JICPPeer.CONNECTION_TIMEOUT) < 0) && getProperty(JICPPeer.CONNECTION_TIMEOUT, null) != null ) {
			jadeArgs.append(" -").append (JICPPeer.CONNECTION_TIMEOUT).append(" ").append(getProperty(JICPPeer.CONNECTION_TIMEOUT, null) );		
		}
		jadeArgs.append(" -agents ").append("CA-%C:").append(controlAgentClass);
		return jadeArgs.toString();
	}

	private String getBackupMainArgs(String containerName) {
		StringBuffer args = new StringBuffer();

		Properties myBootProps = getBootProperties();
		for (Enumeration e = myBootProps.keys(); e.hasMoreElements();){
			String propKey = (String)e.nextElement();
			// Handle switch properties and remove local-port, container-name, agents and other instance-related properties
			if (propKey.equals("nomtp")) {
				args.append(" -").append(propKey);
			} else if (!(propKey.equals(Profile.LOCAL_PORT) || 
				         propKey.equals(Profile.LOCAL_HOST) || 
				         propKey.equals(Profile.MAIN_PORT) || 
				         propKey.equals(Profile.MAIN_HOST) || 
					     propKey.equals(Profile.AGENTS) || 
					     propKey.equals("container-name") || 
					     propKey.equals("gui") || 
					     propKey.equals("backupmain") || 
					     propKey.equals(LEAPIMTPManager.CHANGE_PORT_IF_BUSY))) {
				String value = myBootProps.getProperty(propKey);
				args.append(" -").append(propKey).append(" ").append(value);
			}
		}
		args.append(" -").append(LEAPIMTPManager.CHANGE_PORT_IF_BUSY).append(" true");
		if (containerName != null) {
			args.append(" -container-name "+containerName);
		}
		args.append(" -backupmain");
		args.append(" -").append(Profile.MAIN_HOST).append(" ").append(getPlatformHost());
		args.append(" -").append(Profile.MAIN_PORT).append(" ").append(getPlatformPort());
		args.append(" BCA-%C:").append(backupControlAgentClass); //To check
		return args.toString();
	}

	private String getContainerArgs(ContainerInfo currentContainer) {
		StringBuffer containerArgs = new StringBuffer("");
		if (currentContainer.getName() != null) {
			if (currentContainer.getSplit()) {
				// Split container: use the -msisdn option
				containerArgs.append("-"+JICPProtocol.MSISDN_KEY+" ").append(currentContainer.getName());
			}
			else {
				// Normal container: use the -container-name option
				containerArgs.append("-"+Profile.CONTAINER_NAME+" ").append(currentContainer.getName());
			}
		}

		if (!currentContainer.getSplit()) {
			containerArgs.append(" -").append(Profile.PLATFORM_ID).append(" ").append(getPlatformId());
		}
		
		// FIXME: if this is a split container we should enable connecting to the BEManagement service 
		containerArgs.append(" -").append(Profile.MAIN_HOST).append(" ").append(getPlatformHost());
		containerArgs.append(" -").append(Profile.MAIN_PORT).append(" ").append(getPlatformPort());
		return containerArgs.toString();
	}

	/**
	 * Convert the properties in a given container profile into a string of type -key value.
	 * For JADE profiles, if active on the Main Container, the UDPNodeMonitoring service is automatically added to the services option.
	 * For JAVA profiles, if not explicitly specified, append the Java-logging option of the Main Container
	 *
	 * @param profile The Container profile to use.
	 */
	private String getProfileArgs(String profileName, Collection<ContainerProfileInfo> profiles, boolean isJadeProfile) {
		Collection<ContainerProfilePropertyInfo> properties = null;
		if (profileName != null) {
			ContainerProfileInfo profileInfo = getContainerProfile(profileName, profiles);
			if (profileInfo != null) {
				properties = profileInfo.getProperties();
			}
		}

		StringBuffer profileArgs = new StringBuffer();
		boolean servicesOptionMissing = true;
		boolean loggingOptionMissing = true;
		if (properties != null) {
			for (ContainerProfilePropertyInfo currentProperty : properties) {
				String key = currentProperty.getKey();
				String value = currentProperty.getValue();
				if (isJadeProfile) {
					if (key.equals(Profile.SERVICES)) {
						servicesOptionMissing = false;
						if (getUdpMonitoring() && !value.contains(UDPNodeMonitoringService.class.getName())) {
							value = value + ";" + UDPNodeMonitoringService.class.getName();
						}
						if (getTopicManagement() && !value.contains(TopicManagementService.class.getName())) {
							value = value + ";" + TopicManagementService.class.getName();
						}
					}
				}
				else {
					// java profile
					if (key.startsWith("-Djava.util.logging.config.file")) {
						loggingOptionMissing = false;
					}
				}
				profileArgs.append(" -").append(key);
				if (value != null) {
					profileArgs.append(" ").append(value);
				}
			}
		}

		if (isJadeProfile) {
			if (servicesOptionMissing) {
				profileArgs.append(" -services jade.core.mobility.AgentMobilityService;jade.core.event.NotificationService;");
				if (getUdpMonitoring()) {
					profileArgs.append("jade.core.nodeMonitoring.UDPNodeMonitoringService;");
				}
				if (getTopicManagement()) {
					profileArgs.append("jade.core.messaging.TopicManagementService;");
				}
			}
		} else {
			// java profile
			if (loggingOptionMissing) {
				String loggingOption = System.getProperty("java.util.logging.config.file");
				if (loggingOption != null) {
					profileArgs.append(" -Djava.util.logging.config.file="+ loggingOption);
				}
			}
		}
		return profileArgs.toString();
	}

	private String getSplitContainerProfileArgs(String profileName, Collection<ContainerProfileInfo> profiles) {
		Collection<ContainerProfilePropertyInfo> properties = null;
		if (profileName != null) {
			ContainerProfileInfo profileInfo = getContainerProfile(profileName, profiles);
			if (profileInfo != null) {
				properties = profileInfo.getProperties();
			}
		}
		
		StringBuffer profileArgs = new StringBuffer();
		boolean servicesOptionMissing = true;
		if (properties != null) {
			for (ContainerProfilePropertyInfo currentProperty : properties) {
				String key = currentProperty.getKey();
				String value = currentProperty.getValue();
				// If the -services option is specified, be sure TopicManagement is there 
				if (key.equals(MicroRuntime.SERVICES_KEY)) {
					servicesOptionMissing = false;
					if (getTopicManagement() && !value.contains(TopicManagementFEService.class.getName())) {
						value = value + ";" + TopicManagementFEService.class.getName();
					}
				}
				profileArgs.append(" -").append(key);
				if (value != null) {
					profileArgs.append(" ").append(value);
				}
			}
		}

		// If the -services option is NOT specified, add TopicManagement 
		if (servicesOptionMissing) {
			if (getTopicManagement()) {
				profileArgs.append(" -"+MicroRuntime.SERVICES_KEY+" "+TopicManagementFEService.class.getName());
			}
		}
		return profileArgs.toString();
	}

	private ContainerProfileInfo getContainerProfile(String profileName, Collection<ContainerProfileInfo> containerProfiles) {
		for (ContainerProfileInfo currentProfile : containerProfiles) {
			if (currentProfile.getName().equals(profileName)) {
				return currentProfile;
			}
		}
		return null;
	}

	private void killContainer(String containerName, String containerHost, int timeout) throws Exception {
		jade.domain.JADEAgentManagement.KillContainer kc = new jade.domain.JADEAgentManagement.KillContainer();
		kc.setContainer(new ContainerID(containerName, null));
		try {
			AMSUtils.requestAMSAction(this, kc, timeout);
		}
		catch (Exception e) {
			if (containerHost != null) {
				myLogger.log(Logger.WARNING, "Agent " + getName() + ": Error killing container " + containerName + " via AMS.", e);
				myLogger.log(Logger.INFO, "Agent " + getName() + ": Try to kill container " + containerName + " via BootDaemon.");
				HostInfo hi = new HostInfo();
				hi.setName(containerHost);
				RemoteManager rm = checkHostAvailability(hi, timeout);
				int id = rm.getJadeInstanceId(containerName);
				rm.killJadeInstance(id);
			} else {
				throw e;
			}
		}
	}

	// This is package-scoped as it is called by the PlatformStarter behaviour.
	RemoteManager checkHostAvailability(HostInfo host, int timeout) {
		host.setAvailability(false);
		host.setReachability(false);
		try {
			// controllo la raggiungibilita` dell'host
			InetAddress inetAddress = InetAddress.getByName(host.getName());
			if (inetAddress.isReachable(timeout)) {
				host.setReachability(true);
				
				String tsDaemonPort = System.getProperty(TSBOOT_DAEMON_PORT_KEY, String.valueOf(TSDaemon.DEFAULT_PORT));
				tsDaemonPort = getCfaProperty(BOOT_DAEMON_PORT_KEY, tsDaemonPort);

				String tsDaemonName = System.getProperty(TSBOOT_DAEMON_NAME_KEY, TSDaemon.DEFAULT_NAME);
				tsDaemonName = getCfaProperty(BOOT_DAEMON_NAME_KEY, tsDaemonName);
				
				RemoteManager rm = TestUtility.createRemoteManager(host.getName(), Integer.parseInt(tsDaemonPort), tsDaemonName);
				host.setAvailability(true);
				return rm;
			} else {
				host.setErrorCode(CfaMessageCode.HOST_NOT_REACHABLE);
			}
		} catch (UnknownHostException e) {
			host.setErrorCode(CfaMessageCode.HOST_NOT_FOUND);
			myLogger.log(Logger.WARNING, "", e);
		} catch (IOException e) {
			host.setErrorCode(CfaMessageCode.HOST_NOT_REACHABLE);
			myLogger.log(Logger.WARNING, "", e);
		} catch (TestException e) {
			// Eccezione tirata se il BootDaemon non risponde
			host.setErrorCode(CfaMessageCode.BOOTDAEMON_NOT_AVAILABLE);
			myLogger.log(Logger.WARNING, "", e);
		}
		return null;
	}

	private int getPlatformPort() {
		return Integer.parseInt(((ContainerID) this.here()).getPort());
	}

	private String getPlatformHost() {
		String hostName = null;
		try {
			// If a local-host has explicitly been specified use it. Otherwise read the local host address from 
			// the network stack.
			hostName = getProperty(Profile.LOCAL_HOST, null);
			if (hostName == null) {
				hostName = InetAddress.getLocalHost().getHostAddress();
			}
		} catch (UnknownHostException e) {
			hostName = Profile.LOCALHOST_CONSTANT;
		}
		return hostName;
	}

	private String getPlatformId() {
		return getProperty(Profile.PLATFORM_ID, null);
	}

	private boolean getUdpMonitoring() {
		try {
			getHelper(UDPNodeMonitoringService.NAME);
		}
		catch (ServiceNotActiveException e) {
			// The UDPNodeMonitoring service is NOT installed
			return false;
		}
		catch (ServiceException e) {
			// Service is installed, but there were problems creating the helper (ignore them)
		}
		return true;
	}
	
	private boolean getTopicManagement() {
		try {
			getHelper(TopicManagementService.NAME);
		}
		catch (ServiceNotActiveException e) {
			// The UDPNodeMonitoring service is NOT installed
			return false;
		}
		catch (ServiceException e) {
			// Service is installed, but there were problems creating the helper (ignore them)
		}
		return true;
	}	

	protected boolean getMainReplication() {
		try {
			getHelper(MainReplicationService.NAME);
		}
		catch (ServiceNotActiveException e) {
			// The MainReplication service is NOT installed
			return false;
		}
		catch (ServiceException e) {
			// Service is installed, but there were problems creating the helper (ignore them)
		}
		return true;
	}

	protected void setPlatformStatus(String status, Object detail) {
		// Manage the global status
		setGlobalStatus(status, detail);
		
		// Manage the status of groups
		groupManager.manageGroupStatus(status, detail);
	}

	void setGlobalStatus(String status, Object detail) {
		String oldStatus = platformStatus;
		platformStatus = status;
		statusDetail = detail;
		
		if (!oldStatus.equals(status)) {
			// The status changed --> Notify a proper event on the PlatformLifeCycle topic
			notifyPlatformLifecycleEvent();
		}		
	}
	
	private void notifyPlatformLifecycleEvent() {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Issuing platform lifecycle event");
		ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
		notification.addReceiver(platformLifeCycleTopic);
		notification.setOntology(ConfigurationOntology.getInstance().getName());
		notification.setLanguage(codec.getName());
		Action actExpr = new Action(getAID(), new GetPlatformStatus());
		Result r = new Result(actExpr, platformStatus);
		try {
			getContentManager().fillContent(notification, r);
			send(notification);
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": Error encoding platform life cycle notification message.", e);
		}
	}

    private static void appendPropertyDescription(StringBuilder sb, String label1, String val1) {
    	sb.append(label1);
    	sb.append(':');
    	if (val1 == null) {
    		sb.append("null");
    	} else {
    		sb.append(val1);
    	}
    }

    private static String buildPropertiesComparisonDescription(String label1, String label2, String name, String val1, String val2) {
    	StringBuilder sb = new StringBuilder();
    	if (val1 != null || val2 != null) {
    		sb.append(name);
	    	sb.append(' ');
	    	appendPropertyDescription(sb, label1, val1);
	    	sb.append(", ");
	    	appendPropertyDescription(sb, label2, val2);
	    	sb.append("; ");
    	}
    	return sb.toString();
    }

    private static String prepareResult(StringBuilder descr) {
		if (descr.length() > 2) {
			descr.setLength(descr.length()-2);
		}
		if (descr.length() > 0) {
			return descr.toString();
		} else {
			return null;
		}
    }

	private static String compareHosts(String label1, String label2, HostInfo hi1, HostInfo hi2) {
		if (hi2 == null) {
			return PlatformElement.COMPARISON_MISSING;
		}

		StringBuilder descr = new StringBuilder();
		String ip1 = hi1.getIpAddress();
		String ip2 = hi2.getIpAddress();
		if (!FileUtils.compareObject(ip1, ip2)) {
		    descr.append(buildPropertiesComparisonDescription(label1, label2, "ipAddress", ip1, ip2));
		}
		boolean ba1 = hi1.getBackupAllowed();
		boolean ba2 = hi2.getBackupAllowed();
		if (ba1 != ba2){
		    descr.append(buildPropertiesComparisonDescription(label1, label2, "backupAllowed",
		    		Boolean.toString(ba1), Boolean.toString(ba2)));
		}
		return prepareResult(descr);
	}

	private static String compareContainers(String label1, String label2, ContainerInfo ci1, ContainerInfo ci2) {
		if (ci2 == null) {
			return PlatformElement.COMPARISON_MISSING;
		}
		
		StringBuilder descr = new StringBuilder();
		String s1 = ci1.getJavaProfile();
		String s2 = ci2.getJavaProfile();
		if (!FileUtils.compareObject(s1, s2)){
		    descr.append(buildPropertiesComparisonDescription(label1, label2, "javaProfile", s1, s2));
		}
		s1 = ci1.getJadeProfile();
		s2 = ci2.getJadeProfile();
		if (!FileUtils.compareObject(s1, s2)){
		    descr.append(buildPropertiesComparisonDescription(label1, label2, "jadeProfile", s1, s2));
		}
		s1 = ci1.getJadeAdditionalArgs();
		s2 = ci2.getJadeAdditionalArgs();
		if (!FileUtils.compareObject(s1, s2)){
		    descr.append(buildPropertiesComparisonDescription(label1, label2, "jadeAdditionalArgs", s1, s2));
		}
		return prepareResult(descr);
	}

	private static String compareABIs(String label1, String label2, AgentBaseInfo abi1, AgentBaseInfo abi2) {
		StringBuilder descr = new StringBuilder();
		SortedMap<String, String> abi1Params = abi1.getParametersForComparison();
		SortedMap<String, String> abi2Params = abi2.getParametersForComparison();

		// Add "className" outside AgentBaseInfo because TypeManger in not in wadeInterface.jar
		abi1Params.put("className", TypeManager.getSafeClassName(abi1));
		abi2Params.put("className", TypeManager.getSafeClassName(abi2));

		Iterator<Entry<String, String>> abiParamsIterator = abi1Params.entrySet().iterator();
		Entry<String, String> entry;
		String key;
		String value1;
		String value2;
		while (abiParamsIterator.hasNext()) {
			entry = abiParamsIterator.next();
			key = entry.getKey();
			value1 = entry.getValue();
			value2 = abi2Params.get(key);
			if (!value1.equals(value2)) {
				descr.append(buildPropertiesComparisonDescription(label1, label2, key, value1, value2));
			}
			abiParamsIterator.remove();
			abi2Params.remove(key);
		}

		abiParamsIterator = abi2Params.entrySet().iterator();
		while (abiParamsIterator.hasNext()) {
			entry = abiParamsIterator.next();
			key = entry.getKey();
			value2 = entry.getValue();
		    descr.append(buildPropertiesComparisonDescription(label1, label2, key, null, value2));
		}
		return prepareResult(descr);
	}


	private static class AgentKey {
		private String name;
		private String type;
		private int hashCode;

		public AgentKey(String name, String type) {
			this.name = name;
			if (type == null) {
				type = AgentType.NONE.getDescription();
			}
			this.type = type;
			computeHashCode();
		}

		private void computeHashCode() {
			hashCode = 37;
			if (name != null) {
				hashCode ^= name.hashCode();
			}
			if (type != null) {
				hashCode ^= type.hashCode();
			}
		}

		private final static boolean compare(String s1, String s2) {
			if (s1 == null && s2 == null) {
				return true;
			}
			if (s1 != null && s2 != null) {
					return s1.equals(s2);
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof AgentKey)) {
				return false;
			}
			return compare(name, ((AgentKey)obj).name) && compare(type, ((AgentKey)obj).type);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			computeHashCode();
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
			computeHashCode();
		}
	}

	private static class AgentData {
		String host;
		String container;
		AgentInfo ai;

		public AgentData(String host, String container, AgentInfo ai) {
			this.host = host;
			this.container = container;
			this.ai = ai;
		}
	}

	private static PlatformInfo compareConfiguration(PlatformInfo actual, PlatformInfo reference) {
		Map<AgentKey, AgentData> actualAgents = new HashMap<AgentKey, AgentData>();
		AgentKey ak;
		AgentData ad;
		String mismatch;
		String lblReference = reference.getName();
		String lblActual = actual.getName();

		Map<String, HostInfo> actualHostInfos = new HashMap<String, HostInfo>(reference.getHosts().size());
		Map<String, ContainerInfo> actualContainerInfos = new HashMap<String, ContainerInfo>();

		// get all agent in actual configuration and put them into actualAgent indexed by name & type
		for(HostInfo hi: actual.getHosts()) {
			actualHostInfos.put(hi.getName(), hi);
			for (ContainerInfo ci: hi.getContainers()) {
				actualContainerInfos.put(ci.getName(), ci);
				for (AgentInfo ai: ci.getAgents()) {
					ak = new AgentKey(ai.getName(), ai.getType());
					ad = new AgentData(hi.getName(), ci.getName(), ai);
					actualAgents.put(ak, ad);
				}
			}
		}

		Map<String, HostInfo> referenceHostInfos = new HashMap<String, HostInfo>(reference.getHosts().size());
		Map<String, ContainerInfo> referenceContainerInfos = new HashMap<String, ContainerInfo>();

		// iterate through reference hosts
		for(HostInfo hi: reference.getHosts()) {
			// Cache reference hosts, we'll need them later
			referenceHostInfos.put(hi.getName(), hi);

			// compare the two host infos 
			hi.setErrorCode(compareHosts(lblReference, lblActual, hi, actualHostInfos.get(hi.getName())));

			for (ContainerInfo ci: hi.getContainers()) {
				// Cache also reference containers
				referenceContainerInfos.put(ci.getName(), ci);

				// compare the two container infos
				ci.setErrorCode(compareContainers(lblReference, lblActual, ci, actualContainerInfos.get(ci.getName())));

				Iterator<AgentInfo> refAgentInfoIter = ci.getAgents().iterator();
				while (refAgentInfoIter.hasNext()) {
					AgentInfo ai = refAgentInfoIter.next();
					ak = new AgentKey(ai.getName(), ai.getType());
					ad = actualAgents.get(ak);
					if (ad == null) {
						// agent is missing
						ai.setErrorCode(PlatformElement.COMPARISON_MISSING);
					} else {
						// we found agent in actual configuration; compare agent properties
						mismatch = compareABIs(lblReference, lblActual, ai, ad.ai);
						if (mismatch != null) {
							// agent is not the same
							ai.setErrorCode(PlatformElement.COMPARISON_MISMATCH+": "+mismatch);
						} else {
							// AgentInfo in reference and in actual are the same
							if (hi.getName().equals(ad.host) && ci.getName().equals(ad.container)) {
								// they are also in the same place => no differences, we can remove it
								refAgentInfoIter.remove();
							} else {
								// agent was moved
								ai.setErrorCode(PlatformElement.COMPARISON_MOVED+": host "+hi.getName()+", container "+ci.getName());
							}
						}
						// we can remove agent from actual
						actualAgents.remove(ak);
					}
				}
			}
		}

		// now we need to iterate through remaining agents in actual configuration
		for (AgentData aad: actualAgents.values()) {
			HostInfo hi = referenceHostInfos.get(aad.host);
			if (hi == null) {
				// host is new
				hi = new HostInfo();
				hi.setName(aad.host);
				reference.addHost(hi);
				hi.setErrorCode(PlatformElement.COMPARISON_NEW);
				referenceHostInfos.put(aad.host, hi);
			}
			ContainerInfo ci = referenceContainerInfos.get(aad.container);
			if (ci == null) {
				// container is new
				ci = new ContainerInfo();
				ci.setName(aad.container);
				hi.addContainer(ci);
				ci.setErrorCode(PlatformElement.COMPARISON_NEW);
				referenceContainerInfos.put(aad.container, ci);
			}
			ci.addAgent(aad.ai);
			aad.ai.setErrorCode(PlatformElement.COMPARISON_NEW);
		}

		// Add to reference hosts and containers empty
		for(HostInfo ahi: actual.getHosts()) {
			HostInfo hi = referenceHostInfos.get(ahi.getName());
			if (hi == null) {
				// host is new
				hi = new HostInfo();
				hi.setName(ahi.getName());
				reference.addHost(hi);
				hi.setErrorCode(PlatformElement.COMPARISON_NEW);
			}
			for (ContainerInfo aci: ahi.getContainers()) {
				ContainerInfo ci = referenceContainerInfos.get(aci.getName());
				if (ci == null) {
					// container is new
					ci = new ContainerInfo();
					ci.setName(aci.getName());
					hi.addContainer(ci);
					ci.setErrorCode(PlatformElement.COMPARISON_NEW);
				}
			}
		}
		
		// last pass: removal of empty containers and hosts
		Iterator<HostInfo> refHostInfoIter = reference.getHosts().iterator();
		while (refHostInfoIter.hasNext()) {
			HostInfo hi = refHostInfoIter.next();
			Iterator<ContainerInfo> refContainerInfoIter = hi.getContainers().iterator();
			while (refContainerInfoIter.hasNext()) {
				ContainerInfo ci = refContainerInfoIter.next();
				// if error code for container is not null, we keep its ContainerInfo
				if (ci.getErrorCode() == null) {
					if (ci.getAgents().size() < 1) {
						// container is empty, we must remove it
						refContainerInfoIter.remove();
					}
				}
			}
			// as we do with hosts, if error code for container info is not null, we keep it even if it's empty
			if (hi.getErrorCode() == null) {
				if (hi.getContainers().size() < 1) {
					refHostInfoIter.remove();
				}
			}
		}

		// now let's take care of agent pools

		// fill a map with all agent pools in actual configuration
		Map<String, AgentPoolInfo> actualAgentPools = new HashMap<String, AgentPoolInfo>(actual.getAgentPools().size());
		for (AgentPoolInfo aapi: actual.getAgentPools()) {
			actualAgentPools.put(aapi.getName(), aapi);
		}

		// iterate through actual agent pools
		Iterator<AgentPoolInfo> referenceAgentPools = reference.getAgentPools().iterator();
		while (referenceAgentPools.hasNext()) {
			AgentPoolInfo rapi = referenceAgentPools.next();
			AgentPoolInfo aapi = actualAgentPools.get(rapi.getName());
			if (aapi == null) {
				// AgentPoolInfo is present in reference but not in actual, it has been removed
				rapi.setErrorCode(PlatformElement.COMPARISON_MISSING);
			} else {
				// agent pool comes from reference, check its properties
				mismatch = compareABIs(lblReference, lblActual, rapi, aapi);
				if (mismatch != null) {
					// properties are different, configuration changed from reference
					rapi.setErrorCode(PlatformElement.COMPARISON_MISMATCH+": "+mismatch);
				} else {
					// AgentPoolInfo equals -> remove from target e running
					referenceAgentPools.remove();
				}
				actualAgentPools.remove(aapi.getName());
			}
		}

		// Add all agent pools only-in-second
		for (AgentPoolInfo aapi: actualAgentPools.values()) {
			reference.addAgentPool(aapi);
			aapi.setErrorCode(PlatformElement.COMPARISON_NEW);
		}
		
		
		// now let's take care of main container agents
		
		// fill a map with all main container agents in actual configuration
		Map<String, AgentInfo> actualMainAgents = new HashMap<String, AgentInfo>(actual.getMainAgents().size());
		for (AgentInfo aai: actual.getMainAgents()) {
			actualMainAgents.put(aai.getName(), aai);
		}
		

		// iterate through actual main container agents
		Iterator<AgentInfo> referenceMainAgents = reference.getMainAgents().iterator();
		while (referenceMainAgents.hasNext()) {
			AgentInfo rai = referenceMainAgents.next();
			AgentInfo aai = actualMainAgents.get(rai.getName());
			if (aai == null) {
				// AgentInfo is present in reference but not in actual, it has been removed
				rai.setErrorCode(PlatformElement.COMPARISON_MISSING);
			} else {
				// main agent comes from reference, check its properties
				mismatch = compareABIs(lblReference, lblActual, rai, aai);
				if (mismatch != null) {
					// properties are different, configuration changed from reference
					rai.setErrorCode(PlatformElement.COMPARISON_MISMATCH+": "+mismatch);
				} else {
					// agentInfo equals -> remove from target e running
					referenceMainAgents.remove();
				}
				actualMainAgents.remove(aai.getName());
			}
		}

		// Add all main container agents only-in-second
		for (AgentInfo aai: actualMainAgents.values()) {
			reference.addMainAgents(aai);
			aai.setErrorCode(PlatformElement.COMPARISON_NEW);
		}
		
		return reference;
	}
	
	// TypeManagementOntology requests managing
	public void serveGetGlobalPropertiesRequest(GetGlobalProperties ggp, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetGlobalProperties");
		List<Property> properties = TypeManager.getInstance().getPropertiesList();
		AgentUtils.sendResult(this, ggp, msg, properties);
	}

	public void serveGetRoleRequest(GetRole gr, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetRole");
		AgentRole ar = TypeManager.getInstance().getRole(gr.getRole());
		AgentUtils.sendResult(this, gr, msg, ar);
	}

	public void serveGetRolePropertiesRequest(GetRoleProperties grp, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetRoleProperties");
		List<Property> properties = TypeManager.getInstance().getPropertiesList(grp.getAgentRole());
		AgentUtils.sendResult(this, grp, msg, properties);
	}

	public void serveGetRolesRequest(GetRoles grs, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetRoles");
		List<AgentRole> roles = TypeManager.getInstance().getRoles();
		AgentUtils.sendResult(this, grs, msg, roles);
	}
	
	public void serveGetTypeRequest(GetType gt, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetType");
		AgentType at = TypeManager.getInstance().getType(gt.getType());
		AgentUtils.sendResult(this, gt, msg, at);
	}

	public void serveGetTypePropertiesRequest(GetTypeProperties gtp, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetTypeProperties");
		List<Property> properties = TypeManager.getInstance().getPropertiesList(gtp.getAgentType());
		AgentUtils.sendResult(this, gtp, msg, properties);
	}

	public void serveGetTypesRequest(GetTypes gts, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetTypes");
		List<AgentType> types = TypeManager.getInstance().getTypes();
		AgentUtils.sendResult(this, gts, msg, types);
	}
	
	public void serveGetEventTypesRequest(GetEventTypes gets, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetEventTypes");
		List<EventType> eventTypes = TypeManager.getInstance().getCustomEventTypes();
		AgentUtils.sendResult(this, gets, msg, eventTypes);
	}

	public void serveGetEventTypeRequest(GetEventType get, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent " + getName() + ": Serving action GetEventType");
		EventType et = TypeManager.getInstance().getCustomEventType(get.getType());
		AgentUtils.sendResult(this, get, msg, et);
	}
}
