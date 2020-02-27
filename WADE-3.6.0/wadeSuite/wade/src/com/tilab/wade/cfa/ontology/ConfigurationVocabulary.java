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
package com.tilab.wade.cfa.ontology;


/**
 * The vocabulary of the symbols used in the PlatformOntology
 * 
 * @author max
 *
 */
public interface ConfigurationVocabulary {
	public static final String ONTOLOGY_NAME = "Configuration-Ontology";	
	public static final String PLATFORM_MANAGER_SERVICE = "PLATFORM-MANAGER";

	public static final String PLATFORM_LIFE_CYCLE_TOPIC = "Platform-Life-Cycle";
	public static final String GROUPS_LIFE_CYCLE_TOPIC = "Platform-Life-Cycle.Groups";

	//////////////
	// Concepts 
	//////////////

	public static final String PLATFORM_ELEMENT = "platformElement";
	public static final String PLATFORM_ELEMENT_ERROR_CODE = "errorCode";

	// Platform
	public static final String PLATFORM = "platform";
	public static final String PLATFORM_NAME = "name";
	public static final String PLATFORM_DESCRIPTION = "description";
	public static final String BACKUP_NUMBER = "backupsNumber";
	public static final String HOSTS = "hosts";
	public static final String MAIN_AGENTS = "mainAgents";	
	public static final String AGENT_POOLS = "agentPools";
	public static final String CONTAINER_PROFILES = "containerProfiles";

	// Host
	public static final String HOST = "host";
	public static final String HOST_NAME = "name";
	public static final String HOST_IPADDRESS = "ipAddress";
	public static final String HOST_AVAILABILITY = "availability";
	public static final String HOST_REACHABILITY = "reachability";
	public static final String HOST_CPU_NUMBER = "cpuNumber";
	public static final String HOST_RAM = "ram";
	public static final String HOST_ADMIN_STATE = "adminState";
	public static final String HOST_BENCHMARK_SPEC_INT = "benchmarkSpecInt";
	public static final String HOST_BENCHMAR_WF = "benchmarkWf";
	public static final String HOST_CONTAINERS = "containers";	
	public static final String HOST_BACKUP_ALLOWED = "backupAllowed";

	// Container
	public static final String CONTAINER = "container";
	public static final String CONTAINER_NAME = "name";
	public static final String CONTAINER_PROJECT_NAME = "projectName";
	public static final String CONTAINER_JAVA_PROFILE = "javaProfile";
	public static final String CONTAINER_JADE_PROFILE = "jadeProfile";
	public static final String CONTAINER_JADE_ADDITIONAL_ARGS = "jadeAdditionalArgs";
	public static final String CONTAINER_SPLIT = "split";
	public static final String CONTAINER_AGENTS = "agents";	

	// AgentBase
	public static final String AGENT_BASE = "agentBase";
	public static final String AGENT_BASE_NAME = "name";
	public static final String AGENT_BASE_TYPE = "type";
	public static final String AGENT_BASE_CLASSNAME = "className";
	public static final String AGENT_BASE_OWNER = "owner";
	public static final String AGENT_BASE_GROUP = "group";
	public static final String AGENT_BASE_PARAMETERS = "parameters";	

	// Agent
	public static final String AGENT = "agent";

	// AgentPool
	public static final String AGENT_POOL = "agentPool";
	public static final String AGENT_POOL_SIZE = "size";

	// VirtualAgent
	public static final String VIRTUAL_AGENT = "virtualAgent";
	public static final String VIRTUAL_AGENT_NUMBER_OF_REPLICAS = "numberOfReplicas";
	public static final String VIRTUAL_AGENT_REPLICATION_TYPE = "replicationType";

	// Agent Argument
	public static final String AGENT_ARGUMENT = "parameter";
	public static final String AGENT_ARGUMENT_KEY = "key";
	public static final String AGENT_ARGUMENT_VALUE = "value";

	// Container Profile
	public static final String CONTAINER_PROFILE = "containerProfile";
	public static final String CONTAINER_PROFILE_NAME = "name";
	public static final String CONTAINER_PROFILE_TYPE = "type";
	public static final String CONTAINER_PROFILE_PROPERTIES = "properties";	

	// Container Profile Property Info
	public static final String CONTAINER_PROFILE_PROPERTY = "property";
	public static final String CONTAINER_PROFILE_PROPERTY_KEY = "key";
	public static final String CONTAINER_PROFILE_PROPERTY_VALUE = "value";

	// Versions info
	public static final String VERSIONS = "versions";
	public static final String VERSIONS_JADE_VERSION = "jadeVersion";
	public static final String VERSIONS_JADE_REVISION = "jadeRevision";
	public static final String VERSIONS_JADE_DATE = "jadeDate";
	public static final String VERSIONS_WADE_VERSION = "wadeVersion";
	public static final String VERSIONS_WADE_REVISION = "wadeRevision";
	public static final String VERSIONS_WADE_DATE = "wadeDate";
	public static final String VERSIONS_PROJECT_VERSION = "projectVersion";
	public static final String VERSIONS_PROJECT_REVISION = "projectRevision";
	public static final String VERSIONS_PROJECT_ORIGINAL_REVISION = "projectOriginalRevision";
	public static final String VERSIONS_PROJECT_DATE = "projectDate";
	
	// Group status
	public static final String GROUP_STATUS = "groupStatus";
	public static final String GROUP_STATUS_NAME = "name";
	public static final String GROUP_STATUS_STATUS = "status";
	

	/////////////
	// Actions 
	/////////////
	public static final String STARTUP_PLATFORM = "startup-platform";	
	public static final String SHUTDOWN_PLATFORM = "shutdown-platform";	
	public static final String SHUTDOWN_PLATFORM_HARD_TERMINATION = "hard-termination";	
	public static final String SAVE_CONFIGURATION = "save-configuration";
	public static final String IMPORT_CONFIGURATION = "import-configuration";	
	public static final String IMPORT_CONFIGURATION_NAME = "name";
	public static final String EXPORT_CONFIGURATION = "export-configuration";	
	public static final String EXPORT_CONFIGURATION_NAME = "name";
	public static final String EXPORT_CONFIGURATION_DESCRIPTION = "description";
	public static final String EXPORT_CONFIGURATION_OVERRIDE = "override";
	public static final String REMOVE_CONFIGURATION = "remove-configuration";	
	public static final String REMOVE_CONFIGURATION_NAME = "name";	
	public static final String COMPARE_RUNNING_WITH_TARGET_CONFIGURATION = "compare-running-with-target-configuration";
	public static final String GET_CONFIGURATIONS = "get-configurations";
	public static final String GET_PLATFORM_STATUS = "get-platform-status";
	public static final String GET_STATUS_DETAIL = "get-status-detail";
	public static final String GET_HOSTS = "get-hosts";
	public static final String GET_MAIN_AGENTS = "get-main-agents";
	public static final String GET_AGENT_POOLS = "get-agent-pools";
	public static final String GET_CONTAINER_PROFILES = "get-container-profiles";
	public static final String START_CONTAINER = "start-container";
	public static final String START_CONTAINER_PROJECT_NAME = "project-name";
	public static final String START_CONTAINER_CONTAINER_NAME = "container-name";
	public static final String START_CONTAINER_JAVA_PROFILE = "java-profile";
	public static final String START_CONTAINER_JADE_PROFILE = "jade-profile";
	public static final String START_CONTAINER_JADE_ADDITIONAL_ARGS = "jade-additional-args";
	public static final String START_CONTAINER_HOST_NAME = "host-name";
	public static final String START_CONTAINER_AGENTS = "agents";
	public static final String START_AGENT = "start-agent";
	public static final String START_AGENT_AGENT = "agent";
	public static final String START_AGENT_CONTAINER_NAME = "container-name";
	public static final String START_BACKUP_MAIN_CONTAINER = "start-backup-main-container";
	public static final String START_BACKUP_MAIN_CONTAINER_CONTAINER_NAME = "container-name";
	public static final String START_BACKUP_MAIN_CONTAINER_HOST_NAME = "host-name";
	public static final String KILL_CONTAINER = "kill-container";
	public static final String KILL_CONTAINER_CONTAINER_NAME = "container-name";
	public static final String KILL_CONTAINER_HARD_TERMINATION = "hard-termination";	
	public static final String KILL_CONTAINER_HOST_NAME = "host-name";	
	public static final String RESET_ERROR_STATUS = "reset-error-state";
	public static final String ADD_HOST = "add-host";
	public static final String ADD_HOST_INFO = "host-info";	
	public static final String REMOVE_HOST = "remove-host";
	public static final String REMOVE_HOST_NAME = "host-name";
	public static final String GET_VERSIONS_INFO = "get-versions-info";
	public static final String STARTUP_GROUP = "startup-group";
	public static final String SHUTDOWN_GROUP = "shutdown-group";
	public static final String GROUP_NAME = "name";
	public static final String GET_GROUPS_STATUS = "get-groups-status";
}
