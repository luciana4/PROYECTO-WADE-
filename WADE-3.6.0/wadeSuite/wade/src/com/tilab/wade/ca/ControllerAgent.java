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

import java.io.File;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.tilab.wade.ca.ontology.AskForExecutor;
import com.tilab.wade.ca.ontology.CACoordinationOntology;
import com.tilab.wade.ca.ontology.CAStatus;
import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.ca.ontology.CreateAgent;
import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.IsGlobalProperty;
import com.tilab.wade.ca.ontology.KillAgent;
import com.tilab.wade.ca.ontology.RestartingAgent;
import com.tilab.wade.ca.ontology.RestartingContainer;
import com.tilab.wade.ca.ontology.SetAutoRestart;
import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.commons.WadeAgentLoader;
import com.tilab.wade.commons.WadeBasicResponder;
import com.tilab.wade.commons.locale.MessageCode;
import com.tilab.wade.commons.ontology.Attribute;
import com.tilab.wade.commons.ontology.PrepareForShutdown;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.event.EventOntology;
import com.tilab.wade.utils.AMSUtils;
import com.tilab.wade.utils.CAUtils;
import com.tilab.wade.utils.DFUtils;

import jade.content.AgentAction;
import jade.content.Predicate;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.SerializableOntology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.Location;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.AddedContainer;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
import jade.domain.introspection.KillContainerRequested;
import jade.domain.introspection.MovedAgent;
import jade.domain.introspection.RemovedContainer;
import jade.domain.introspection.ShutdownPlatformRequested;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.misc.FileManagerServer;
import jade.misc.LeadershipManager;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;
import jade.util.ObjectManager;
import jade.util.leap.List;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;

/**
 * @author Marco Ughetti - TILAB
 * @author Elena Quarantotto - TILAB
 * @author Giovanni Caire - TILAB
 */
public class ControllerAgent extends WadeAgentImpl {

	////////////////////////////////////////////////
	// WADE class loader management configuration 
	////////////////////////////////////////////////
	/** The property to be specified in the types.xml file to indicate the root directory of WADE class loaders */
	public static final String CLASS_LOADER_ROOT_KEY = "class-loader-root";
	public static final String CLASS_LOADER_ROOT_DEFAULT = "./deploy";
	
	/** The property to be specified in the types.xml file to indicate the list of classpath jars containing workflows, web service descriptors and other relevant entities, in addition to those included in the deploy directory */
	public static final String CLASSPATH_RELEVANT_JARS_KEY = "classpathRelevantJars";
	/** This property is maintained for backward compatibility only. Use CLASSPATH_RELEVANT_JARS_KEY instead */
	public static final String CLASSPATH_WORKFLOW_JARS_KEY = "classpathWorkflowJars";
	private static final String CLASSPATH_RELEVANT_JARS_SEPARATOR = ";";
	
	/** The property to be specified in the types.xml file to indicate the interval between two successive performance measurements */
	public static final String MONITORING_INTERVAL_KEY = "monitoringInterval";
	public static final long MONITORING_INTERVAL_DEFAULT = 10000; // 10 sec
	
	/** The property to be specified in the types.xml file to indicate whether the CA must fail in case the class-loader root directory does not exist */
	public static final String FAIL_ON_MISSING_CLASS_LOADER_ROOT_DIR = "failOnMissingClassLoaderRootDir";

	/** The property to be specified in the types.xml file to indicate that the CA must ignore any WadeClassLoader initialization error */
	public static final String IGNORE_CLASS_LOADER_INIT_ERROR = "ignoreClassLoaderInitError";

	/** The property to be specified in the types.xml file to indicate the root directory of FileManagerServer */
	public static final String FILE_MANAGER_SERVER_ROOT_KEY = "fileManagerServerRoot";
	
	/** The property to be specified in the types.xml file to indicate the download block size of FileManagerServer */
	public static final String FILE_MANAGER_SERVER_DOWNLOAD_BLOCK_SIZE_KEY = "fileManagerServerDownloadBlockSize";
	

	// Attribute IDs
	public static final String CLASS_LOADER_TIMEOUT_ATTRIBUTE = "ClassloaderTimeout";
	public static final String CLASS_LOADER_CLEANUP_INTERVAL_ATTRIBUTE = "ClassloaderCleanupInterval";
	public static final String CLASS_LOADERS_ATTRIBUTE = "Classloaders";

	////////////////////////////////////////////////
	// Performance monitoring configuration
	////////////////////////////////////////////////
	/** The property to be specified in the types.xml file to indicate the CPU usage threshold (in percentage) */
	public static final String CPU_USAGE_THRESHOLD_KEY = "cpuUsageThreshold";
	public static final long CPU_USAGE_THRESHOLD_DEFAULT = 90;
	
	/** The property to be specified in the types.xml file to indicate the CPU usage reentrant threshold (in percentage). Defaults to the CPU usage threshold */
	public static final String CPU_USAGE_REENTRANT_TH_KEY = "cpuUsageReentrantThreshold";

	/** The property to be specified in the types.xml file to indicate the memory usage threshold (in percentage) */
	public static final String MEMORY_USAGE_THRESHOLD_KEY = "memoryUsageThreshold";
	public static final long MEMORY_USAGE_THRESHOLD_DEFAULT = 70;
	
	/** The property to be specified in the types.xml file to indicate the memory usage reentrant threshold (in percentage). Defaults to the memory usage threshold */
	public static final String MEMORY_USAGE_REENTRANT_TH_KEY = "memoryUsageReentrantThreshold";

	/** The property to be specified in the types.xml file to indicate the thread number threshold */
	public static final String THREAD_NUMBER_THRESHOLD_KEY = "threadNumberThreshold";
	public static final int THREAD_NUMBER_THRESHOLD_DEFAULT = 300;
	
	/** The property to be specified in the types.xml file to indicate the thread number reentrant threshold . Defaults to the thread number threshold */
	public static final String THREAD_NUMBER_REENTRANT_TH_KEY = "threadNumberReentrantThreshold";

	// Threshold crossing directions
	public static final boolean UP = true;
	public static final boolean DOWN = false;

	// Attribute IDs
	public static final String MEMORY_HEAP_INIT_ATTRIBUTE = "MemoryHeapInit";
	public static final String MEMORY_HEAP_USED_ATTRIBUTE = "MemoryHeapUsed";
	public static final String MEMORY_HEAP_COMMITTED_ATTRIBUTE = "MemoryHeapCommitted";
	public static final String MEMORY_HEAP_MAX_ATTRIBUTE = "MemoryHeapMax";
	public static final String MONITORING_INTERVAL_ATTRIBUTE = "MonitoringInterval";
	public static final String MEMORY_USAGE_THRESHOLD_ATTRIBUTE = "MemoryUsageThreshold";
	public static final String MEMORY_USAGE_REENTRANT_TH_ATTRIBUTE = "MemoryUsageReentrantThreshold";
	public static final String MEMORY_USAGE_ATTRIBUTE = "MemoryUsage";
	public static final String THREAD_NUMBER_THRESHOLD_ATTRIBUTE = "ThreadNumberThreshold";
	public static final String THREAD_NUMBER_REENTRANT_TH_ATTRIBUTE = "ThreadNumberReentrantThreshold";
	public static final String THREAD_NUMBER_ATTRIBUTE = "ThreadNumber";

	private static final long MBYTE = 1048576; // 1024 * 1024
	

	////////////////////////////////////////////////
	// Fault recovery configuration
	////////////////////////////////////////////////	
	/** The property to be specified in the types.xml file to indicate whether the component autorestart mode should be activated or not */
	public static final String AUTORESTART_KEY = "autorestart";
	public static final boolean AUTORESTART_DEFAULT = true;
	
	public static final String MAX_CONTAINER_RESTART_ATTEMPTS_KEY = "maxContainerRestartAttempts";
	public static final int MAX_CONTAINER_RESTART_ATTEMPTS_DEFAULT = 3;
	
	
	// Attribute IDs
	public static final String LEADERSHIP_ATTRIBUTE = "Leadership";
	public static final String AUTORESTART_ATTRIBUTE = "Autorestart";
	
	// ContainerInfo and AgentInfo extended attributes
	private static final String IS_TERMINATING = "IS-TERMINATING";
	static final String IS_MAIN = "IS-MAIN";
	private static final String ADDRESS = "ADDRESS";
	private static final String ORIGINAL_CONTAINER = "ORIGINAL-CONTAINER";

	private boolean autorestart;

	private ContainerMap containers = new ContainerMap();
	private Map<String, ContainerInfo> containersToRestart = new HashMap<String, ContainerInfo>();
	private Map<String, AgentInfo> agentsToRestart = new HashMap<String, AgentInfo>();
	private Map<AID, String> controlAgents = new HashMap<AID, String>(); 
	private String hostAddress;
	
	private LeadershipManager leadershipManager;
	
	// Ontologies
	private Ontology controlOnto = ControlOntology.getInstance();
	private Ontology configurationOnto = ConfigurationOntology.getInstance();
	private Ontology caCoordinationOnto = CACoordinationOntology.getInstance();
	private Ontology deploymentOnto = DeploymentOntology.getInstance();
	private Ontology evOnto = EventOntology.getInstance();

	private AMSEventListener amsListener;
	private PerformanceMonitorBehaviour performanceMonitor;
	private CAServices caServices;
	private Map envInfo;
	ThreadedBehaviourFactory tf = null;
	private WadeAgentLoader wal;

	protected void agentSpecificSetup() throws AgentInitializationException {
		// Check that the local host address is properly configured
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new AgentInitializationException("CA " + getLocalName() + " - Local host name could not be resolved.", e);
		}
		
		// Register ontologies and languages
		getContentManager().registerOntology(controlOnto);
		getContentManager().registerOntology(configurationOnto);
		getContentManager().registerOntology(caCoordinationOnto);
		getContentManager().registerOntology(JADEManagementOntology.getInstance()); // This must be registered to interact with the AMS
		getContentManager().registerOntology(evOnto);
		getContentManager().registerOntology(deploymentOnto);

		// Read CA type properties
		Map configProps = TypeManager.getInstance().getProperties(getType());

		
		////////////////////////////////////////////////
		// Local resources monitoring initialization
		////////////////////////////////////////////////
		envInfo = EnvironmentInfo.getInstance().getEnvironmentInfo();
		
		// Add the behaviour monitoring local container performances
		long monitoringInterval = TypeManager.getLong(configProps, MONITORING_INTERVAL_KEY, MONITORING_INTERVAL_DEFAULT);
		performanceMonitor = new PerformanceMonitorBehaviour(this, monitoringInterval, configProps);
		tf = new ThreadedBehaviourFactory();
		addBehaviour(tf.wrap(performanceMonitor));
		
		// Create and associate to this CA a FileManagerServer to support log files retrieval 
		FileManagerServer fileManagerServer = new FileManagerServer();
		String fmsRoot = getArgument(FILE_MANAGER_SERVER_ROOT_KEY, null);
		if (fmsRoot == null) {
			// Try as type property
			fmsRoot = TypeManager.getString(configProps, FILE_MANAGER_SERVER_ROOT_KEY, null);
		}
		fileManagerServer.init(this, fmsRoot);

		int downloadBlockSize = TypeManager.getInt(configProps, FILE_MANAGER_SERVER_DOWNLOAD_BLOCK_SIZE_KEY, 0);
		if (downloadBlockSize > 0) {
			fileManagerServer.setDownloadBlockSize(downloadBlockSize);
		}
		
		
		/////////////////////////////////////////////
		// CAServices and autorestart initialization
		/////////////////////////////////////////////
		caServices = CAServices.getInstance(this);
		caServices.setLocalCA(this);
		
		// Initialize the autorestart mode
		autorestart = TypeManager.getBoolean(configProps, AUTORESTART_KEY, AUTORESTART_DEFAULT);
		
		// Add myself to the list of control agents and the local container to the ContainerMap
		controlAgents.put(getAID(), hostAddress);
		
		Location myLocation = here();
		Boolean myContainerIsMain = ((ContainerID)myLocation).getMain();
		addContainer(myLocation.getName(), myContainerIsMain);

		// Add the behaviours subscribing to the AMS and DF to keep the map of containers and agents up to date
		activateDFSubscription();
		activateAMSSubscription();

		// Add the behaviour serving CA-coordination notifications
		addBehaviour(new CACoordinationServer());
		
		// Initiate the leader election mechanism
		try {
			leadershipManager = createLeadershipManager();
			leadershipManager.init(this);
			leadershipManager.updateLeadership();
		}
		catch (ServiceException se) {
			throw new AgentInitializationException("CA " + getName() + ": Error connecting to the TopicManagementService", se);
		}
		
		
		////////////////////////////////////////////////
		// Wade ClassLoader initialization
		////////////////////////////////////////////////
		String root = (String) configProps.get(CLASS_LOADER_ROOT_KEY);
		if (root == null) {
			root = CLASS_LOADER_ROOT_DEFAULT;
		}
		
		// Detect if this CA is the "host leader" (the first CA created in this host)
		// NOTE that the PlatformStarter works so that given a HOST the (N +1)th CA
		// is not activated until the Nth is not completely registered with the DF.
		boolean caHostLeader;
		try {
			DFAgentDescription dfad = DFUtils.searchAnyByType(this, WadeAgent.CONTROL_AGENT_TYPE, new Property(WadeAgent.HOSTADDRESS, hostAddress));
			caHostLeader = (dfad == null);
		} catch (Exception e) {
			throw new AgentInitializationException("CA " + getLocalName() + " - Impossible searching into DF.", e);
		}
		
		// If this is the CA host leader prepare the deploy folder
		if (caHostLeader) {
			// Check if deploy folder exists
			File f = new File(root);
			if (!f.exists()) {
				if (TypeManager.getBoolean(configProps, FAIL_ON_MISSING_CLASS_LOADER_ROOT_DIR, false)) {
					throw new AgentInitializationException("CA " + getLocalName() + " - ClassLoader root directory " + root + " does not exist.");
				}
				else {
					myLogger.log(Logger.INFO, "CA "+getName()+" - ClassLoader root directory "+root+" does not exist. Creating...");
					f.mkdir();
				}
			}
			
			// Get the platform-startup-time to be used as a unique identifier (on all hosts) 
			// of the new current classloader
			String platformStartuptime = getProperty(WadeAgent.PLATFORM_STARTUP_TIME, null);
			
			// Prepare the deploy folders
			try {
				caServices.getClassLoaderManager().prepare(root, platformStartuptime);
			} catch(Exception e) {
				if (!this.getBooleanTypeProperty(IGNORE_CLASS_LOADER_INIT_ERROR, false)) {
					throw new AgentInitializationException("CA " + getLocalName() + " - Error preparing deploy folders", e);
				}
			}
		}

		// Get list of jars containing workflows, applications, service-descriptor
		// Check first in classpathRelevantJars property and than in classpathWorkflowJars for backward compatibility
		String classpathRelevantJars = (String) configProps.get(CLASSPATH_RELEVANT_JARS_KEY);
		if (classpathRelevantJars == null) {
			// Try as a System property
			classpathRelevantJars = System.getProperty(CLASSPATH_RELEVANT_JARS_KEY);
			if (classpathRelevantJars == null) {
				// For backward compatibility only try with the old property
				classpathRelevantJars = (String) configProps.get(CLASSPATH_WORKFLOW_JARS_KEY);
			}
		}
		
		// Initialize the ClassLoaderManager (only set the current classloader)
		try {
			caServices.getClassLoaderManager().init(root, getClasspathRelevantJars(classpathRelevantJars));
			// Instruct the SerializableOntology to use the WADE ClassLoader when deserializing
			((SerializableOntology) SerializableOntology.getInstance()).setClassLoader(caServices.getDefaultClassLoader());
		} catch(Exception e) {
			if (!this.getBooleanTypeProperty(IGNORE_CLASS_LOADER_INIT_ERROR, false)) {
				throw new AgentInitializationException("CA " + getLocalName() + " - Error initializing ClassLoaderManager", e);
			}
		}
		
		// Initialize the WadeAgentLoader used to load agents classes from WADE classloader
		wal = new WadeAgentLoader(this);
		ObjectManager.addLoader(ObjectManager.AGENT_TYPE, wal);
		
		// Add the behaviour serving deploy requests
		addBehaviour(new DeploymentServer(this));
	}

	protected void takeDown() {
		super.takeDown();
		
		// Remove the WadeAgentLoader used to load agents classes from WADE classloader
		ObjectManager.removeLoader(ObjectManager.AGENT_TYPE, wal);
		
		if (amsListener != null) {
			// Unsubscribe from the AMS
			send(amsListener.getCancel());
		}
		if (performanceMonitor != null) {
			performanceMonitor.stop();
		}
	}
	
	protected void activateDFSubscription() {
		DFAgentDescription dfTemplate = new DFAgentDescription();
		ACLMessage subscriptionMsg = DFService.createSubscriptionMessage(this, getDefaultDF(), dfTemplate, null);
		addBehaviour(new DFSubscriber(this, subscriptionMsg));
	}
	
	protected void activateAMSSubscription() {
		amsListener = new AMSEventListener();
		addBehaviour(amsListener);
	}
	
	protected LeadershipManager createLeadershipManager() {
		return new LeadershipManager() {
			protected void leaderElected(AID leader) {
				myLogger.log(Level.INFO, "CA " + getName() + ": New CA leader is "+leader.getName());
				if (leader.equals(getAID())) {
					becomeLeader();
				}
			}
		};
		
	}
	
	////////////////////////////////////////////////////////
	// Agent attributes
	////////////////////////////////////////////////////////
	public List getAttributes() {
		// These attributes need to be managed all-together in order to be consistent
		List attributes = super.getAttributes();
		MemoryUsage memUsage = performanceMonitor.getMemoryUsage();
		
		Attribute attr = new Attribute(MEMORY_HEAP_INIT_ATTRIBUTE, memUsage.getInit() / MBYTE + " MB");
		attr.setName("Memory heap INIT");
		attributes.add(attr);
		
		attr = new Attribute(MEMORY_HEAP_USED_ATTRIBUTE, memUsage.getUsed() / MBYTE + " MB"); 
		attr.setName("Memory heap USED");
		attributes.add(attr);
		
		attr = new Attribute(MEMORY_HEAP_COMMITTED_ATTRIBUTE, memUsage.getCommitted() / MBYTE + " MB"); 
		attr.setName("Memory heap COMMITTED");
		attributes.add(attr);
		
		attr = new Attribute(MEMORY_HEAP_MAX_ATTRIBUTE, memUsage.getMax() / MBYTE + " MB"); 
		attr.setName("Memory heap MAX");
		attributes.add(attr);		
		
		return attributes;
	}
	
	@AttributeGetter
	public String getLeadership() {
		return StringUtils.capitalize(Boolean.toString(leadershipManager.isLeader()));
	}
	
	@AttributeGetter
	public String getAutorestart() {
		return StringUtils.capitalize(Boolean.toString(autorestart));
	}
	
	@AttributeGetter(name=EnvironmentInfo.OS_NAME)
	public String getOsName() {
		return (String)envInfo.get(EnvironmentInfo.OS_NAME);
	}

	@AttributeGetter(name=EnvironmentInfo.OS_ARCHITECTURE)
	public String getOsArchitecture() {
		return (String)envInfo.get(EnvironmentInfo.OS_ARCHITECTURE);
	}
	
	@AttributeGetter(name=EnvironmentInfo.OS_VERSION)
	public String getOsVersion() {
		return (String)envInfo.get(EnvironmentInfo.OS_VERSION);
	}
	
	@AttributeGetter(name=EnvironmentInfo.OS_AVAILABLE_PROCESSOR)
	public Integer getOsAvailableProcessor() {
		return (Integer)envInfo.get(EnvironmentInfo.OS_AVAILABLE_PROCESSOR);
	}
	
	@AttributeGetter(name=EnvironmentInfo.RUNTIME_NAME)
	public String getRuntimeName() {
		return (String)envInfo.get(EnvironmentInfo.RUNTIME_NAME);
	}
	
	@AttributeGetter(name=EnvironmentInfo.RUNTIME_VM_VERSION)
	public String getRuntimeVmVersion() {
		return (String)envInfo.get(EnvironmentInfo.RUNTIME_VM_VERSION);
	}
	
	@AttributeGetter(name=EnvironmentInfo.RUNTIME_UPTIME)
	public Date getRuntimeUptime() {
		return new Date(System.currentTimeMillis() - EnvironmentInfo.getInstance().getRuntimeUpTime());
	}
	
	@AttributeGetter(name="Monitoring interval (ms)")
	public long getMonitoringInterval() {
		return performanceMonitor.getMonitoringInterval();
	}

	@AttributeGetter(name="Memory usage threshold (% of Max Heap)")
	public long getMemoryUsageThreshold() {
		return performanceMonitor.getMemoryUsageThreshold(UP);
	}
	
	@AttributeGetter(name="Memory usage reentrant threshold (% of Max Heap)")
	public long getMemoryUsageReentrantThreshold() {
		return performanceMonitor.getMemoryUsageThreshold(DOWN);
	}

	@AttributeGetter(name="Memory usage (% of Max Heap)")
	public String getMemoryUsage() {
		MemoryUsage memUsage = performanceMonitor.getMemoryUsage();
		DecimalFormat format = new DecimalFormat("00.00");
		return format.format((memUsage.getUsed() * 100.0) / memUsage.getMax());
	}
	
	@AttributeGetter(name="Thread number threshold")
	public int getThreadNumberThreshold() {
		return performanceMonitor.getThreadNumberThreshold(UP);
	}

	@AttributeGetter(name="Thread number reentrant threshold")
	public int getThreadNumberReentrantThreshold() {
		return performanceMonitor.getThreadNumberThreshold(DOWN);
	}

	@AttributeGetter(name="Active thread number")
	public int getThreadNumber() {
		return performanceMonitor.getThreadNumber();
	}

	/* CAs should not monitor CPU 
	@AttributeGetter(name=CPU_USAGE_THRESHOLD_KEY)
	public String getCpuUsageThreshold() {
		return performanceMonitor.getCpuUsageThreshold(UP) + " %";
	}

	@AttributeGetter(name=CPU_USAGE_REENTRANT_TH_KEY)
	public String getCpuUsageReentrantThreshold() {
		return performanceMonitor.getCpuUsageThreshold(DOWN) + " %";
	}

	@AttributeGetter(name="CPU USAGE (%)")
	public long getCpuUsagePercent() {
		return performanceMonitor.getCpuUsage();
	}
	*/

	@AttributeGetter(name="WADE ClassLoader instances")
	public String getClassloaders() {
		return caServices.getClassLoaderManager().getClassLoadersInfo();
	}
	
	public DFAgentDescription getDFDescription() {
		// Add all configuration options necessary to properly restart the local container
		// after an unexpected fault to the DF Description 
		Map<String, Object> props = new HashMap<String, Object>();
		String jadeProfile = getProperty(WadeAgent.JADE_PROFILE, null);
		if (jadeProfile != null) {
			props.put(WadeAgent.JADE_PROFILE, jadeProfile);
		}
		String javaProfile = getProperty(WadeAgent.JAVA_PROFILE, null);
		if (javaProfile != null) {
			props.put(WadeAgent.JAVA_PROFILE, javaProfile);
		}
		String split = getProperty(WadeAgent.SPLIT, null);
		if (split != null) {
			props.put(WadeAgent.SPLIT, split);
		}
		
		// PROJECT_NAME is not null ONLY if the container belongs to a different project (child-project)
		// NOTE: Do not confuse the PROJECT-NAME Jade argument (used to support child-projects container 
		// restart) with the -Dproject-name and -Dparent-project-name system variables used by the CFA and
		// BootDaemon
		String projectName = getProperty(WadeAgent.PROJECT_NAME, null);
		props.put(WadeAgent.MAIN_PROJECT, projectName==null);
		
		// If the Profile.EXPORT_HOST property is set add it to the DF description properties
		String exportHost = getProperty(Profile.EXPORT_HOST, null);
		if (exportHost != null) {
			props.put(Profile.EXPORT_HOST, exportHost);
		}
		
		return DFUtils.createDFAgentDescription(this, props);
	}

	// Since the Control Ontology extends the WADE-Management Ontology, we serve both ontologies with a 
	// single behaviour.
	public WadeBasicResponder getManagementResponder() {
		WadeBasicResponder responder = super.getManagementResponder();
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.or(
						MessageTemplate.MatchOntology(ControlOntology.getInstance().getName()),
						MessageTemplate.MatchOntology(WadeManagementOntology.getInstance().getName())
				),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
		);
		responder.setTemplate(template);
		responder.registerHandler(AskForExecutor.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return serveAskForExecutor((AskForExecutor) act, aExpr, request);
			}
		});
		responder.registerHandler(SetAutoRestart.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return serveSetAutoRestart((SetAutoRestart) act, aExpr, request);
			}
		});
		responder.registerHandler(KillAgent.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return serveKillAgent((KillAgent) act, aExpr, request);
			}
		});
		responder.registerHandler(CreateAgent.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return serveCreateAgent((CreateAgent) act, aExpr, request);
			}
		});
		// Override the default handler for the PrepareForShutdown action since the CA serves such action asynchronously
		// by means of a behaviour.
		responder.registerHandler(PrepareForShutdown.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return servePrepareForShutdown((PrepareForShutdown) act, aExpr, request);
			}
		});

		return responder;
	}

	////////////////////////////////////////////////////////
	// Performance monitoring callback methods
	////////////////////////////////////////////////////////
	public void memoryThresholdCrossed(boolean direction) {
		myLogger.log(direction ? Logger.WARNING : Logger.INFO, "CA "+getName()+": Memory-Threshold crossed " + (direction ? "UP" : "DOWN") + ". Used = "+performanceMonitor.getMemoryUsage().getUsed()+", committed = "+performanceMonitor.getMemoryUsage().getCommitted());
	}

	public void threadCountThresholdCrossed(boolean direction) {
		myLogger.log(direction ? Logger.WARNING : Logger.INFO, "CA "+getName()+": Thread-count-Threshold crossed " + (direction ? "UP" : "DOWN") + ". N-threads = "+performanceMonitor.getThreadNumber());
	}

	public void cpuUsageThresholdCrossed(boolean direction) {
		// FIXME: Do we actually need this method: CPU is per host, not per JVM. 
	}

	// cpu monitoring hook
	public long getCpuUsage() {
		return 0;
	}
	
	
	////////////////////////////////////////////////////////
	// DF/AMS Event handling callback methods
	////////////////////////////////////////////////////////
	protected void handleDFRegistration(DFAgentDescription dfd, ServiceDescription sd) {
		AID aid = dfd.getName();
		String serviceType = sd.getType();
		if (serviceType.equals(WadeAgent.CONTROL_AGENT_TYPE)) {
			// CONTROL AGENT
			String hostAddress = "UNKNOWN";
			jade.util.leap.Iterator propsIt = sd.getAllProperties();
			while (propsIt.hasNext()) {
				Property prop = (Property)propsIt.next();
				if (prop.getName().equalsIgnoreCase(WadeAgent.HOSTADDRESS)) {
					hostAddress = (String)prop.getValue();
					break;
				}
			}
			controlAgents.put(aid, hostAddress);
			if (leadershipManager.isLeader()) {
				// I'm the leader --> Notify the current status to the new CA
				CAStatus caStatus = new CAStatus(autorestart, CAServices.getInstance(this).getGlobalProperties());
				notifyControlAgents(ACLMessage.INFORM, caStatus);
			}
		}
		updateContainerMap(aid, dfd, sd);
	}

	protected void handleDFDeregistration(DFAgentDescription dfd) {
		// Nothing to do, cleanup is done by AMSEventListener (see DEADAGENT handler)
	}

	protected void handleBornAgent(BornAgent event) {
		agentsToRestart.remove(event.getAgent().getLocalName());
	}

	protected void handleDeadAgent(DeadAgent event) {
		AID aid = event.getAgent();
		String agentName = aid.getLocalName();
		ContainerID cid = event.getWhere();
		String containerName = cid.getName();
		myLogger.log(Level.FINE, "CA " + getName() + " - DeadAgent event received. Agent is " + agentName + " (container = " + containerName+")");

		// Handle CA removal
		if (controlAgents.remove(aid) != null) {
			myLogger.log(Level.INFO, "CA " + getName() + " - Control agent " + agentName + " removed.");
			if (aid.equals(leadershipManager.getLeader())) {
				// CA leader dead --> Update leadership
				myLogger.log(Level.INFO, "CA " + getName() + " - Updating leadership...");
				leadershipManager.updateLeadership();
			}
		}

		caServices.handleDeadAgent(aid);

		ContainerInfo ci = containers.getContainerInfo(containerName);
		if (ci != null) {
			Boolean b = event.getContainerRemoved();
			boolean containerRemoved = b != null ? b.booleanValue() : false;
			boolean isTerminating = ((Boolean) ci.getExtendedAttributes().get(IS_TERMINATING)).booleanValue();
			if (!isTerminating && !containerRemoved) {
				// The agent did NOT die due to a container termination. Check if we need to restart it
				myLogger.log(Level.INFO, "CA " + getName() + " - Agent " + agentName + " in container " + containerName + " dead");
				AgentInfo ai = ci.getAgent(agentName);
				if (ci.removeAgent(ai)) {
					Boolean tmp = (Boolean) ai.getExtendedAttributes().get(IS_TERMINATING);
					boolean isAgentTerminating = tmp != null ? tmp.booleanValue() : false;  
					if (autorestart && !isAgentTerminating) { // IS_TERMINATING attribute is set by CAServices.setTerminating()
						ai.addParameter(new AgentArgumentInfo(WadeAgent.RESTARTING, Boolean.toString(true)));
						if (here().getName().equals(containerName)) {
							// This is a local agent --> Restart it locally
							restartLocalAgent(ai);
						} else if (requiresRemoteRestart(ai)) {
							// This is an agent (such as the CFA or another CA) that must be remotely restarted by the CA leader 
							if (leadershipManager.isLeader()) {
								// I'm the leader --> Restart it
								restartRemoteAgentViaAMS(ai, containerName);
							}
							else {
								// I'm not the leader --> Cache it util either the leader restarts it or I become the new leader
								ai.getExtendedAttributes().put(ORIGINAL_CONTAINER, ci.getName());
								agentsToRestart.put(ai.getName(), ai);
							}
						}
					}
				}
			} else {
				// The agent died due to a container termination. Just do nothing.
			}
		}
	}

	protected void handleMovedAgent(MovedAgent event) {
		String agentName = event.getAgent().getLocalName();
		String fromContainer = event.getFrom().getName();
		String toContainer = event.getTo().getName();
		ContainerInfo cifrom = containers.getContainerInfo(fromContainer);
		ContainerInfo cito = containers.getContainerInfo(toContainer);
		AgentInfo ai = cifrom.getAgent(agentName);
		cifrom.removeAgent(ai);
		caServices.handleMovedAgent(event.getAgent());
		// The new DF-registration done by the moved agent may be received before this event --> We don't want to override
		// the AgentInfo
		if (cito.getAgent(agentName) == null) {
			cito.addAgent(ai);
		}
		myLogger.log(Level.FINE, "CA " + getName() + ": MovedAgent event received. Agent is " + agentName + " (Source = " + fromContainer + ", destination = " + toContainer + ")");
	}

	protected void handleAddedContainer(AddedContainer event) {
		Location cid = event.getContainer();
		String containerName = cid.getName();
		String containerAddress = cid.getAddress();

		containersToRestart.remove(containerName);
		
		boolean isAux = CAUtils.isAuxiliary(containerName);
		boolean containerInfoAlreadyPresent = containers.containsContainerInfo(containerName); 
		Boolean isMain = ((ContainerID) cid).getMain();
		myLogger.log(Level.FINE, "CA " + getName() + ": AddedContainer event received. Container is " + containerName + " (address = " + containerAddress + ", main = " + isMain + ")");
		if (!isAux) {
			// Note that the container info may be already present in the container map due to a DF-registration 
			// notification received before the AddedContainer event or a master Main Containener re-election. 
			// If this is the case we don't want to overwrite it
			if (containerInfoAlreadyPresent) {
				myLogger.log(Level.INFO, "CA " + getName() + ": updating existing container info");
				containers.getContainerInfo(containerName).getExtendedAttributes().put(IS_MAIN, isMain);
			} else {
				myLogger.log(Level.INFO, "CA " + getName() + ": creating new container info");
				addContainer(containerName, isMain);
			}
		}
	}

	private ContainerInfo addContainer(String containerName, Boolean isMain) {
		Map extAttrs = new HashMap(2);
		extAttrs.put(IS_MAIN, isMain);
		extAttrs.put(IS_TERMINATING, new Boolean(false));
		ContainerInfo ci = new ContainerInfo(containerName);
		ci.setExtendedAttributes(extAttrs);
		containers.putContainerInfo(containerName, ci);
		return ci;
	}

	protected void handleRemovedContainer(RemovedContainer event) {
		Location l = event.getContainer();

		String containerName = l.getName();

		myLogger.log(Level.INFO, "CA " + getName() + " - RemovedContainer event received. Container is " + containerName);

		ContainerInfo ci = containers.removeContainerInfo(containerName);
		if (ci != null) {
			boolean isTerminating = ((Boolean) ci.getExtendedAttributes().get(IS_TERMINATING)).booleanValue();
			boolean isMain = ((Boolean) ci.getExtendedAttributes().get(IS_MAIN)).booleanValue();
			if (!isTerminating && !CAUtils.isAuxiliary(containerName)) {
				myLogger.log(Logger.WARNING, "CA " + getName() + " - Container " + containerName + " unexpectedly dead!");
				if (autorestart) {
					if (leadershipManager.isLeader()) {
						// I'm the leader --> Restart the container
						myLogger.log(Logger.INFO, "CA " + getName() + " - Autorestart ON. I'm the leader --> Take care of the container restart procedure ...");
						restartContainer(ci, l.getAddress());
					} else {
						// I'm not the leader --> Cache the container until either the leader restarts it or I become the new leader
						myLogger.log(Logger.INFO, "CA " + getName() + " - Autorestart ON. I'm NOT the leader --> store container information until the leader takes care of restarting it");
						ci.getExtendedAttributes().put(ADDRESS, l.getAddress());
						containersToRestart.put(ci.getName(), ci);
					}
				}
				else {
					myLogger.log(Logger.INFO, "CA " + getName() + " - Autorestart OFF. Ignore it ...");
				}
			} 
		}
	}

	protected void handleKillContainerRequested(KillContainerRequested event) {
		ContainerID cid = event.getContainer();
		String containerName = cid.getName();
		myLogger.log(Level.FINE, "CA " + getName() + ": KillContainerRequested event received. Container is " + containerName);
		ContainerInfo ci = containers.getContainerInfo(containerName);
		if (ci != null) {
			ci.getExtendedAttributes().put(IS_TERMINATING, new Boolean(true));
		}
	}

	protected void handleShutdownPlatformRequested(ShutdownPlatformRequested event) {
		myLogger.log(Level.FINE, "CA " + getName() + ": ShutdownPlatformRequested event received");
		autorestart = false;
		Iterator it = containers.getContainers().iterator();
		while (it.hasNext()) {
			ContainerInfo ci = (ContainerInfo) it.next();
			ci.getExtendedAttributes().put(IS_TERMINATING, new Boolean(true));
		}
	}

	/**
	 * Allow extended classes to know whether or not this CA is the current leader
	 */
	public boolean isLeader() {
		return leadershipManager.isLeader();
	}

	// Returns a randomly CA for each host
	Collection<AID> getOneCAByHosts() {
		Map<String, AID> caMap = new HashMap<String, AID>();
		Iterator<Entry<AID, String>> it = controlAgents.entrySet().iterator();
		while(it.hasNext()) {
			Entry<AID, String> caEntry = it.next();
			caMap.put(caEntry.getValue(), caEntry.getKey());
		}
		return caMap.values();
	}

	// Returns the CAs by host (excluded this)
	Collection<AID> getOtherCAsInThisHost() {
		Collection<AID> cas = new ArrayList<AID>();
		Iterator<Entry<AID, String>> it = controlAgents.entrySet().iterator();
		while(it.hasNext()) {
			Entry<AID, String> caEntry = it.next();
			if (Profile.compareHostNames(caEntry.getValue(), hostAddress) && !caEntry.getValue().equals(getAID())) {
				cas.add(caEntry.getKey());
			}
		}
		return cas;
	}
	
	void setTerminating(Agent a) {
		ContainerInfo ci = containers.getContainerInfo(a.here().getName());
		if (ci != null) {
			AgentInfo ai = ci.getAgent(a.getLocalName());
			if (ai != null) {
				ai.getExtendedAttributes().put(IS_TERMINATING, true);
			}
		}
	}
	
	///////////////////////////////////////////////////////////
	// Control-Ontology actions serving section
	///////////////////////////////////////////////////////////
	private ACLMessage serveAskForExecutor(AskForExecutor act, Action aExpr, final ACLMessage request) {
		myLogger.log(Level.INFO, "CA " + getName() + ": AskForExecutor request received from " + request.getSender().getName());
		if (performanceMonitor.isCpuBelowThreshold() && performanceMonitor.isThreadNumberBelowThreshold()) {
			myLogger.log(Level.FINE, "CA " + getName() + ": AskForExecutor request accepted");
			return prepareStringReply(request, ACLMessage.INFORM, null);
		} else {
			myLogger.log(Logger.WARNING, "CA " + getName() + ": AskForExecutor request refused");
			return prepareStringReply(request, ACLMessage.REFUSE, null);
		}
	}

	private ACLMessage serveSetAutoRestart(SetAutoRestart act, Action aExpr, final ACLMessage request) {
		autorestart = act.isAutorestart();
		myLogger.log(Level.INFO, "CA " + getName() + ": SetAutoRestart request received from " + request.getSender().getName() + ". Autorestart = " + autorestart);
		return prepareStringReply(request, ACLMessage.INFORM, null);
	}

	// Handle a request to create a new agent locally.
	private ACLMessage serveCreateAgent(CreateAgent act, Action aExpr, ACLMessage request) {
		String argsMsg = "";
		Object[] args = act.getArguments();
		if (args != null && args.length == 1 && args[0] instanceof Map) {
			argsMsg = " arguments = " + args[0];
		}
		myLogger.log(Level.INFO, "CA " + getName() + ": CreateAgent request received from " + request.getSender().getName() + ". Name = " + act.getName() + " class = " + act.getClassName() + argsMsg);
		try {
			String agentName = createAgent(act.getName(), act.getClassName(), act.getArguments());
			myLogger.log(Level.INFO, "CA " + getName() + ": Local agent " + agentName + " successfully started");
			return prepareStringReply(request, ACLMessage.INFORM, null);
		} catch (Exception e) {
			myLogger.log(Level.SEVERE, "CA " + getName() + ": Error starting local agent " + act.getName(), e);
			return prepareStringReply(request, ACLMessage.FAILURE, MessageCode.UNEXPECTED_ERROR + MessageCode.ARGUMENT_SEPARATOR + e.getMessage());
		}
	}

	private ACLMessage serveKillAgent(KillAgent act, Action aExpr, final ACLMessage request) {
		AID aid = act.getAgent();
		myLogger.log(Level.INFO, "CA " + getName() + ": KillAgent request received from " + request.getSender().getName() + ". Agent is " + aid.getName());
		if (getAID().equals(aid)) {
			// Avoid killing myself
			return prepareStringReply(request, ACLMessage.REFUSE, MessageCode.ACTION_NOT_SUPPORTED + MessageCode.ARGUMENT_SEPARATOR + "KillAgent" + MessageCode.ARGUMENT_SEPARATOR + "CA");
		} else {
			String localName = aid.getLocalName();
			try {
				AgentController ac = getAgentController(localName);
				if (ac != null) {
					ac.kill();
					// Remove the AgentInfo of the killed agent to avoid restarting it.
					ContainerInfo ci = containers.getContainerInfo(here().getName());
					if (ci != null) {
						ci.removeAgent(ci.getAgent(localName));
					}
					myLogger.log(Level.INFO, "CA " + getName() + ": agent " + aid.getName() + " successfully killed");
					return prepareStringReply(request, ACLMessage.INFORM, null);
				} else {
					myLogger.log(Logger.WARNING, "CA " + getName() + ": agent " + aid.getName() + " not found on local container");
					// FIXME: Use a proper error code
					return prepareStringReply(request, ACLMessage.REFUSE, "agent " + aid.getName() + " not found on local container");
				}
			} catch (Exception e) {
				myLogger.log(Level.SEVERE, "CA " + getName() + ": error killing agent " + aid.getName(), e);
				return prepareStringReply(request, ACLMessage.FAILURE, MessageCode.UNEXPECTED_ERROR + MessageCode.ARGUMENT_SEPARATOR + e.getMessage());
			}
		}
	}
	
	private ACLMessage servePrepareForShutdown(PrepareForShutdown act, Action aExpr, final ACLMessage request) {
		ACLMessage reply = null;
		myLogger.log(Level.INFO, "CA " + getName() + ": PrepareForShutdown request received from " + request.getSender().getName());

		Collection<AgentInfo> agents = getLocalAgentsWithoutCA();
		if (agents.size() == 0) {
			// There are no agents in this container
			reply = prepareContentReply(aExpr, request, ACLMessage.INFORM, new Boolean(false));
		} else {
			try {
				ACLMessage myRequest = prepareRequest(getAID(), act, WadeManagementOntology.getInstance()); // Dummy actor
				myRequest.setReplyByDate(request.getReplyByDate());
				myRequest.clearAllReceiver();
				// Broadcast the request to all local agents (except myself)
				Iterator<AgentInfo> it = agents.iterator();
				int agentsCnt = 0;
				while (it.hasNext()) {
					AgentInfo ai = it.next();
					if (!ai.getName().equals(getLocalName())) {
						myRequest.addReceiver(new AID(ai.getName(), AID.ISLOCALNAME));
						agentsCnt++;
					}
				}

				addBehaviour(new ContainerActivitiesTerminationChecker(this, myRequest, agentsCnt, aExpr, request));
				reply = prepareStringReply(request, ACLMessage.AGREE, null);
			} catch (Exception e) {
				// Should never happen
				myLogger.log(Level.SEVERE, "CA " + getName() + ": Error encoding request to prepare for shutdown", e);
				reply = prepareStringReply(request, ACLMessage.FAILURE, "Unexpected error");
			}
		}
		return reply;
	}

	//////////////////////////////////////////////
	// CAServices section
	//////////////////////////////////////////////
	protected void registerCAServicesExtension(String extensionName, Object extension) {
		CAServices.getInstance(this).registerExtension(extensionName, extension);
	}

	
	//////////////////////////////////////////////
	// Utility section
	//////////////////////////////////////////////
	private void restartLocalAgent(AgentInfo aInfo) {
		String name = aInfo.getName();
		String className = aInfo.getClassName();
		Map<String, Object> arg = propertyCollection2Map(aInfo.getParameters());
		arg.put(WadeAgent.AGENT_TYPE, aInfo.getType());
		arg.put(WadeAgent.AGENT_OWNER, aInfo.getOwner());

		myLogger.log(Level.INFO, "CA " + getName() + ": Re-starting local agent " + name + ". Class = " + className + " arguments = " + arg);
		Object[] args = new Object[]{arg};
		try {
			String agentName = createAgent(name, className, args);
			myLogger.log(Level.INFO, "CA " + getName() + ": Local agent " + agentName + " successfully re-started");
		} catch (Exception e) {
			myLogger.log(Level.SEVERE, "CA " + getName() + ": Error re-starting local agent " + name, e);
		}
	}
	
	private void restartRemoteAgentViaAMS(final AgentInfo ai, final String containerName) {
		// Wait a bit before activating the restart procedure to allow AMS and DF notification messages to be processed
		addBehaviour(new WakerBehaviour(this, 1000) {
			public void onWake() {
				myLogger.log(Level.INFO, "CA " + getName() + ": Re-creating agent " + ai.getName());
				try {
					notifyControlAgents(ACLMessage.CONFIRM, new RestartingAgent(ai.getName()));
					Map<String, Object> properties = propertyCollection2Map(ai.getParameters());
					properties.put(WadeAgent.AGENT_TYPE, ai.getType());
					properties.put(WadeAgent.AGENT_OWNER, ai.getOwner());
					AMSUtils.createAgent(ControllerAgent.this, ai.getName(), ai.getClassName(), new Object[]{properties}, containerName);
					myLogger.log(Level.INFO, "CA " + getName() + ": Agent " + ai.getName() + " successfully re-created");
				}
				catch (Exception e) {
					myLogger.log(Level.SEVERE, "CA " + getName() + ": Error re-creating agent " + ai.getName(), e);
				}
			}
		});
	}

	private void restartContainer(final ContainerInfo cInfo, final String ipAddress) {
		// Wait a bit before activating the restart procedure to allow AMS and DF notification messages to be processed
		addBehaviour(new WakerBehaviour(this, 10000) {
			public void onWake() {
				try {
					String containerName = cInfo.getName();
					notifyControlAgents(ACLMessage.CONFIRM, new RestartingContainer(containerName));
					myLogger.log(Level.INFO, "CA " + getName() + " - Activating restart procedure for container " + containerName + "...");

					// Retrieve ConfigurationAgent AID
					AID cfa = DFUtils.getAID(DFUtils.searchAnyByType(ControllerAgent.this, WadeAgent.CONFIGURATION_AGENT_TYPE, null));

					if (cfa == null) {
						myLogger.log(Level.SEVERE, "CA " + getName() + " - Cannot restart container, missing Configuration Agent");
					} else {
						ContainerRestarter cr = new ContainerRestarter(ControllerAgent.this, cfa, cInfo, ipAddress);
						addBehaviour(cr);
					}
				}
				catch (Exception e) {
					myLogger.log(Level.SEVERE, "CA " + getName() + " - Unexpected error activating container restart procedure.", e);
				}
			}
		});
	}

	/**
	 * Check if a given just-dead agent must be restarted even if it was not living in the local container.
	 * This is the case for CAs and CFA
	 */
	private boolean requiresRemoteRestart(AgentInfo aInfo) {
		return aInfo.getType().equals(TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE).getDescription()) ||
		aInfo.getType().equals(TypeManager.getInstance().getType(WadeAgent.CONFIGURATION_AGENT_TYPE).getDescription());
	}

	private void updateContainerMap(AID id, DFAgentDescription dfd, ServiceDescription sd) {
		String location = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
		if (location != null) {
			ContainerInfo ci = containers.getContainerInfo(location);
			if (ci == null) {
				// Add the container info for this location and go on. Note in fact that this notification may
				// be received before the ADDED_CONTAINER event is received
				myLogger.log(Level.INFO, "CA " + getName() + ": Container " + location + " for agent " + id.getName() + " not found. Add it!");
				ci = addContainer(location, Boolean.FALSE);
			}
			String agentClassName = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_CLASSNAME);
			String agentType = sd.getType();
			String agentOwner = sd.getOwnership();
			String agentGroup = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_GROUP);
			Collection<AgentArgumentInfo> agentProperties = getAgentProperties(sd);

			AgentInfo info = new AgentInfo(id.getLocalName(), agentType, agentClassName, agentOwner, agentGroup, agentProperties);
			ci.addAgent(info);
			String msgExt = "";
			if (myLogger.isLoggable(Level.FINE)) {
				msgExt = " [className: " + agentClassName + " type: " + agentType + " owner: " + agentOwner
				+ " properties: " + stringifyProperties(agentProperties) + "]";
			}
			myLogger.log(Level.CONFIG, "CA " + getName() + ": Agent " + id.getName() + " added to container " + location + msgExt);
		} else {
			myLogger.log(Level.WARNING, "CA " + getName() + ": LOCATION property not found in DF registration of agent " + id.getName());
		}
	}

	private void becomeLeader() {
		myLogger.log(Level.INFO, "CA " + getName() + ": Leadership acquired");
		if (autorestart) {
			if (agentsToRestart.size() > 0) {
				myLogger.log(Level.INFO, "CA " + getName() + ": there are "+agentsToRestart.size()+" agents to restart");
				Iterator<AgentInfo> it = agentsToRestart.values().iterator();
				while (it.hasNext()) {
					AgentInfo ai = it.next();
					String containerName = (String) ai.getExtendedAttributes().get(ORIGINAL_CONTAINER);
					restartRemoteAgentViaAMS(ai, containerName);
				}
			}
			if (containersToRestart.size() > 0) {
				myLogger.log(Level.INFO, "CA " + getName() + ": there are "+containersToRestart.size()+" containers to restart");
				Iterator<ContainerInfo> it = containersToRestart.values().iterator();
				while (it.hasNext()) {
					ContainerInfo ci = it.next();
					restartContainer(ci, (String)ci.getExtendedAttributes().get(ADDRESS));
				}
			}
		}
	}
	
	/**
	 * Notify other CAs a given fact of the CACoordination ontology
	 * This is synchronized as it is called by CAServices#setGlobalProperty()
	 */ 
	synchronized void notifyControlAgents(int performative, Predicate fact) {
		try {
			ACLMessage msg = new ACLMessage(performative);
			msg.setOntology(caCoordinationOnto.getName());
			msg.setLanguage(codec.getName());
			
			Iterator<AID> it = controlAgents.keySet().iterator();
			while (it.hasNext()) {
				msg.addReceiver(it.next());
			}
			getContentManager().fillContent(msg, fact);
			msg.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
			send(msg);
		}
		catch (Exception e) {
			// Should never happen
			myLogger.log(Logger.WARNING, "CA " + getName() + ": Error encoding CA coordination message", e);
		}
	}

	// TO BE REMOVED
	/**
	 * Propagate the current Auto-restart setting to the new CA
	 *
	private void propagateAutoRestart(final AID aid) {
		ACLMessage myRequest = new ACLMessage(ACLMessage.REQUEST);
		myRequest.setOntology(controlOnto.getName());
		myRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		myRequest.setLanguage(codec.getName());
		myRequest.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		myRequest.addReceiver(aid);
		SetAutoRestart sar = new SetAutoRestart(autorestart);
		Action a = new Action(aid, sar);
		try {
			getContentManager().fillContent(myRequest, a);
			addBehaviour(new SimpleAchieveREInitiator(this, myRequest) {
				protected void handleInform(ACLMessage inform) {
					myLogger.log(Level.FINE, "CA " + getName() + ": Autorestart correctly propagated to " + aid.getLocalName());
				}

				protected void handleFailure(ACLMessage failure) {
					myLogger.log(Logger.WARNING, "CA " + getName() + ": Autorestart not propagated to " + aid.getLocalName());
				}
			});
		} catch (Exception e) {
			myLogger.log(Level.SEVERE, "CA " + getName() + ": Error propagating autorestart to " + aid.getLocalName(), e);
		}
	}*/

	/**
	 * Return all agents in this container excluding the local CA
	 */ 
	private Collection<AgentInfo> getLocalAgentsWithoutCA() {
		ContainerInfo ci = containers.getContainerInfo(here().getName());
		AgentInfo localCa = ci.getAgent(getLocalName());
		Collection<AgentInfo> result = new java.util.ArrayList<AgentInfo>(ci.getAgents());
		result.remove(localCa);
		return result;
	}

	/**
	 * Prepare the reply to a given message using the Result or Done predicate depending on whether the result is != null
	 * @param actExpr The Action expression that embedded the served action
	 * @param request The message that embedded the request to serve the action
	 * @param performative The ACL performative to use in the reply
	 * @param result The result (if any) produced by the action. 
	 */
	protected ACLMessage prepareContentReply(Action actExpr, ACLMessage request, int performative, Object result) {
		ACLMessage reply = request.createReply();
		reply.setPerformative(performative);
		try {
			if (result != null) {
				Result r = new Result(actExpr, result);
				getContentManager().fillContent(reply, r);
			} else {
				Done d = new Done(actExpr);
				getContentManager().fillContent(reply, d);
			}
		}
		catch (Exception e) {
			myLogger.log(Level.SEVERE, "CA " + getName() + ": Exception encoding result of action " + actExpr.getAction().getClass().getName(), e);
		}
		return reply;
	}

	/**
	 * Prepare the reply to a given message with the result (if any) inserted in the content slot as a plain string 
	 */
	protected final static ACLMessage prepareStringReply(ACLMessage request, int performative, String result) {
		ACLMessage reply = request.createReply();
		reply.setPerformative(performative);
		reply.setContent(result);
		return reply;
	}

	/**
	 * Prepare a suitable message to request a given action to a given agent
	 */
	protected ACLMessage prepareRequest(AID receiver, AgentAction act, Ontology onto) throws CodecException, OntologyException {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setOntology(onto.getName());
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.setLanguage(codec.getName());
		request.addReceiver(receiver);
		Action a = new Action(receiver, act);
		getContentManager().fillContent(request, a);
		return request;
	}

	/**
	 * Converts an array of <code>Property</code> into a <code>Map</code>
	 *
	 * @param props
	 * @return Map<String, Object>
	 */
	private static Map<String, Object> propertyCollection2Map(Collection<AgentArgumentInfo> props) {
		Map<String, Object> result = null;
		if (props != null) {
			result = new HashMap<String, Object>();
			Iterator<AgentArgumentInfo> it = props.iterator();
			while (it.hasNext()) {
				AgentArgumentInfo aai = it.next();
				result.put(aai.getKey(), aai.getValue());
			}
		}
		return result;
	}

	private Collection<AgentArgumentInfo> getAgentProperties(ServiceDescription sd) {
		java.util.List<AgentArgumentInfo> plist = new java.util.ArrayList<AgentArgumentInfo>();
		Iterator it = sd.getAllProperties();
		while (it.hasNext()) {
			Property p = (Property) it.next();
			plist.add(new AgentArgumentInfo(p.getName(), p.getValue()));
		}
		return plist;
	}

	private String stringifyProperties(Collection<AgentArgumentInfo> pp) {
		StringBuffer sb = new StringBuffer("[");
		Iterator<AgentArgumentInfo> it = pp.iterator();
		int i = 0;
		while (it.hasNext()) {
			AgentArgumentInfo aai = it.next();
			sb.append(aai.getKey());
			sb.append(": ");
			sb.append(aai.getValue());
			if (i < pp.size() - 1) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]");
		return sb.toString();
	}
	
	private String[] getClasspathRelevantJars(String classpathRelevantJars) {
		String[] result = null;
		if(classpathRelevantJars != null) {
			result = classpathRelevantJars.split(CLASSPATH_RELEVANT_JARS_SEPARATOR);
			for(int i=0; i<result.length; i++) {
				result[i] = result[i].trim();
			}
		}
		return result;
	}
	

	///////////////////////////////////////////////////////
	// Inner classes
	///////////////////////////////////////////////////////
	/**
	 * Inner class ContainerActivitiesTerminationChecker
	 */
	private class ContainerActivitiesTerminationChecker extends AchieveREInitiator {
		private int nReceivers;
		private Action aExpr;
		private ACLMessage origRequest;

		public ContainerActivitiesTerminationChecker(Agent agent, ACLMessage request, int nReceivers, Action aExpr, ACLMessage origRequest) {
			super(agent, request);
			this.nReceivers = nReceivers;
			this.origRequest = origRequest;
			this.aExpr = aExpr;
		}

		protected void handleAllResultNotifications(Vector vector) {
			int busyAgentsCnt = 0;
			if (vector.size() < nReceivers) {
				myLogger.log(Logger.WARNING, "CA " + myAgent.getName() + ": " + (nReceivers - vector.size()) + " agent(s) did not reply in due time");
			}
			for (int i = 0; i < vector.size(); i++) {
				ACLMessage msg = (ACLMessage) vector.get(i);
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						Result r = (Result) myAgent.getContentManager().extractContent(msg);
						Boolean working = (Boolean) r.getValue();
						if (working.booleanValue()) {
							busyAgentsCnt++;
						}
					}
					catch (Exception e) {
						myLogger.log(Level.WARNING, "CA " + myAgent.getName() + ": Error decoding reply from agent " + msg.getSender().getName(), e);
					}
				} else {
					if (!msg.getSender().equals(getAMS())) {
						myLogger.log(Logger.WARNING, "CA " + myAgent.getName() + ": Agent " + msg.getSender().getName() + " replied with unexpected message " + ACLMessage.getPerformative(msg.getPerformative()) + ". Content is " + msg.getContent());
					}
				}
			}
			send(prepareContentReply(aExpr, origRequest, ACLMessage.INFORM, new Boolean(busyAgentsCnt > 0)));
		}
	} // END of inner class ContainerActivitiesTerminationChecker

	
	/**
	 * Inner class AMSEventListener
	 * This behaviour listens to AMS events and, if the autorestart mode is turned on, automatically
	 * restarts agents and containers that unexpectedly die.
	 * Moreover this class forwards platform events as WANTS-Events
	 */
	private class AMSEventListener extends AMSSubscriber {
		private static final long serialVersionUID = 1111111112;

		protected void installHandlers(Map handlersTable) {
			handlersTable.put(IntrospectionVocabulary.BORNAGENT, new EventHandler() {
				public void handle(Event ev) {
					handleBornAgent((BornAgent) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.DEADAGENT, new EventHandler() {
				public void handle(Event ev) {
					handleDeadAgent((DeadAgent) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.MOVEDAGENT, new EventHandler() {
				public void handle(Event ev) {
					handleMovedAgent((MovedAgent) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.ADDEDCONTAINER, new EventHandler() {
				public void handle(Event ev) {
					handleAddedContainer((AddedContainer) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.REMOVEDCONTAINER, new EventHandler() {
				public void handle(Event ev) {
					handleRemovedContainer((RemovedContainer) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.KILLCONTAINERREQUESTED, new EventHandler() {
				public void handle(Event ev) {
					handleKillContainerRequested((KillContainerRequested) ev);
				}
			});
			handlersTable.put(IntrospectionVocabulary.SHUTDOWNPLATFORMREQUESTED, new EventHandler() {
				public void handle(Event ev) {
					handleShutdownPlatformRequested((ShutdownPlatformRequested) ev);
				}
			});
		}
	} // END of inner class AMSEventListener

	
	/**
	 * Inner class DFSubscriber
	 */
	private class DFSubscriber extends SubscriptionInitiator {

		private DFSubscriber(Agent a, ACLMessage msg) {
			super(a, msg);
		}

		protected void handleInform(ACLMessage inform) {
			try {
				DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
				for (int i = 0; i < dfds.length; ++i) {
					Iterator services = dfds[i].getAllServices();
					if (services.hasNext()) {
						// Registration/Modification
						ServiceDescription sd = (ServiceDescription) services.next();
						myLogger.log(Level.FINE, "CA " + myAgent.getName() + ": Registration notification received from DF. Type = " + sd.getType());
						handleDFRegistration(dfds[i], sd);
					} else {
						// Deregistration
						myLogger.log(Level.FINE, "CA " + myAgent.getName() + ": De-registration notification received from DF");
						handleDFDeregistration(dfds[i]);
					}
				}
			}
			catch (Exception e) {
				myLogger.log(Level.SEVERE, "CA " + myAgent.getName() + ": Error decoding notification from DF. Message content: \"" + inform.getContent() + "\"", e);
			}
		}

	} // END of inner class DFSubscriber


	/**
	 * Inner class CACoordinationServer
	 * This behaviour serves all messages that Control Agents exchange to coordinate themselves 
	 */
	private class CACoordinationServer extends CyclicBehaviour {
		private MessageTemplate template = MessageTemplate.MatchOntology(CACoordinationOntology.getInstance().getName());
		
		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				try {
					Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
					if (msg.getPerformative() == ACLMessage.CONFIRM) {
						if (p instanceof RestartingContainer) {
							// CA leader is taking care of restarting a dead container --> Get rid of it
							String containerName = ((RestartingContainer) p).getContainerName();
							containersToRestart.remove(containerName);
						}
						else if (p instanceof RestartingAgent) {
							// CA leader is taking care of restarting a dead agent --> Get rid of it
							String agentName = ((RestartingAgent) p).getAgentName();
							agentsToRestart.remove(agentName);
						}
					}
					else if (msg.getPerformative() == ACLMessage.INFORM) {
						if (p instanceof IsGlobalProperty) {
							// Global property set
							setCAGlobalProperty(((IsGlobalProperty) p).getProperty());
						}
						else if (p instanceof CAStatus) {
							// CA common information alignment
							CAStatus caStatus = (CAStatus) p;
							autorestart = caStatus.getAutorestart();
							Iterator it = caStatus.getGlobalProperties().iterator();
							while (it.hasNext()) {
								setCAGlobalProperty((Property) it.next());
							}
						}
					}
					else if (msg.getPerformative() == ACLMessage.DISCONFIRM) {
						if (p instanceof IsGlobalProperty) {
							// Global property removed
							Property prop = ((IsGlobalProperty) p).getProperty();
							CAServices.getInstance(ControllerAgent.this).getGlobalProperties().remove(prop.getName());
						}
					}
				}
				catch (Exception e) {
					myLogger.log(Logger.WARNING, "CA " + getLocalName() + " - error processing CA-Coordination message "+ACLMessage.getPerformative(msg.getPerformative())+" from "+msg.getSender().getName()+". Content is <"+msg.getContent()+">", e);
				}
			}
			else {
				block();
			}
		}
		
		private void setCAGlobalProperty(Property prop) {
			String name = prop.getName();
			String value = (String) prop.getValue();
			CAServices.getInstance(ControllerAgent.this).getGlobalProperties().put(name, value);
		}
	} // END of inner class CACoordinationServer
	

	///////////////////////////////////////////////////////////
	// Methods masking the differences in underlying runtime
	///////////////////////////////////////////////////////////
	AgentController getAgentController(String name) throws ControllerException {
		ContainerController cc = getContainerController();
		if (cc != null) {
			return cc.getAgent(name);
		}
		else {
			// ContainerController is null --> We are on a split container
			return MicroRuntime.getAgent(name);
		}
	}

	private String createAgent(String agentName, String agentClassName, Object[] args) throws Exception {
		ContainerController cc = getContainerController();
		String realAgentName = agentName;
		if (cc != null) {
			AgentController agent = cc.createNewAgent(agentName, agentClassName, args);
			agent.start();
			realAgentName = agent.getName();
		}
		else {
			// ContainerController null --> We are in a split container
			MicroRuntime.startAgent(agentName, agentClassName, args);
		}
		return realAgentName;
	}
}
