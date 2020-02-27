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
package com.tilab.wade.commons;

import jade.util.leap.List;
import jade.domain.FIPAAgentManagement.DFAgentDescription;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 1-giu-2006
 * Time: 17.14.27
 * To change this template use File | Settings | File Templates.
 */
public interface WadeAgent { 

	// WADE agent type names
	public static final String CONTROL_AGENT_TYPE = "Control Agent";
	public static final String CONFIGURATION_AGENT_TYPE = "Configuration Agent";
	public static final String MDB_AGENT_TYPE = "ModelDB Manager"; // FIXME: To be changed in "MDB Agent"
	public static final String WFENGINE_AGENT_TYPE = "Workflow Engine Agent";
	public static final String RAA_AGENT_TYPE = "Runtime Allocator Agent";
	public static final String BCA_AGENT_TYPE = "Backup Controller Agent";
	public static final String WSMA_AGENT_TYPE = "Workflow Status Manager Agent";
	public static final String ESA_AGENT_TYPE = "Event System Agent"; 
	public static final String DISPATCHER_AGENT_TYPE = "Workflow Dispatcher Agent";
	public static final String LM_AGENT_TYPE = "Lock Manager Agent";

	// WADE agent role names
	public static final String ADMINISTRATOR_ROLE = "Administrator";
	public static final String WORKFLOW_EXECUTOR_ROLE = "Workflow Executor";
	
	// DF Properties/Agent arguments common to all WadeAgent-s (automatically managed in DFUtils.createDFAgentDescription()
	public static final String AGENT_TYPE = "AGENT-TYPE";
	public static final String AGENT_ROLE = "AGENT-ROLE";
	public static final String AGENT_OWNER = "AGENT-OWNER";
	public static final String AGENT_CLASSNAME = "AGENT-CLASSNAME";
	public static final String AGENT_LOCATION = "AGENT-LOCATION";
	public static final String AGENT_GROUP = "AGENT-GROUP";
	public static final String HOSTNAME = "HOSTNAME";
	public static final String HOSTADDRESS = "HOSTADDRESS";
	public static final String AGENT_POOL = "AGENT_POOL";
	public static final String MAIN_AGENT = "MAIN_AGENT"; // Property set to "true" for Main Container agents necessary to identify agents living in the Main Container to be killed at platform shutdown 
	public static final String AGENT_MASTER = "AGENT-MASTER";
	public static final String VIRTUAL_NAME = "VIRTUAL-NAME"; // Argument passed as argument to the master replica at creation time
	public static final String VIRTUAL = "VIRTUAL"; // Property included in the DF registration of the virtual agent
	public static final String REPLICATION_TYPE = "REPLICATION-TYPE";
	public static final String NUMBER_OF_REPLICAS = "NUMBER-OF-REPLICAS";
	
	
	// Transient agent arguments.  
	public static final String TRANSIENT_AGENT_ARGUMENT = "_";
	public static final String RESTARTING = TRANSIENT_AGENT_ARGUMENT+"RESTARTING";
	public static final String DUMP_ARGUMENTS = TRANSIENT_AGENT_ARGUMENT+"DUMP-ARGUMENTS";
	
	// DF Properties for the ControllerAgent
	public static final String JADE_PROFILE = "JADE-PROFILE";
	public static final String JAVA_PROFILE = "JAVA-PROFILE";
	public static final String JADE_ADDITIONAL_ARGS = "JADE-ADDITIONAL-ARGS";
	public static final String SPLIT = "SPLIT";
	public static final String PROJECT_NAME = "PROJECT-NAME"; // ONLY used in case of a container belonging to a different (child) project. Allows to restart the container as part of the original child project in case of fault
	public static final String MAIN_PROJECT = "MAIN-PROJECT";
	public static final String PLATFORM_STARTUP_TIME = "platform-startup-time";
	
	// Common attribute IDs
	public static final String MESSAGE_QUEUE_SIZE_ATTRIBUTE = "MessageQueueSize";
	public static final String STARTUP_TIME_ATTRIBUTE = "StartupTime";

	// Predefined owner values 
	public static final String NONE_OWNER = "NONE";

	public static final String NULL = "__NULL__";
	
	public static final int CURRENT_LOAD_UNKNOWN = -1;
	
	/**
	 * Returns the type of this agent
	 * @return the type of this agent
	 */
	public AgentType getType();
	
	/**
	 * Returns the role of this agent
	 * @return the role of this agent
	 */
	public AgentRole getRole();
	
	/**
	 * Returns the name of the owner (if any) of this agent
	 * @return The name of the owner (if any) of this agent
	 */
	public String getOwner();
	
	/**
	 * Returns the list of attributes (each one represented by an Attribute object) of this agent 
	 * @return the list of attributes of this agent
	 */
	public List getAttributes();
	
	
	/**
	 * Set the attributes specified in a given list to this agent
	 * @param attributes The list of Attribute objects to set
	 */
	public void setAttributes(List attributes);
	
	/**
	 * Returns the DFAgentDescription to be registered with the DF.
	 * @return the DFAgentDescription to be registered with the DF
	 */
	public DFAgentDescription getDFDescription();
	
	/**
	 * This method is automatically invoked when a WADE-based application is shutting down in a smooth way.
	 * The return value indicates that this agent is still working and needs more time before the shut-down 
	 * can actually take place.
	 * It should be noticed that if some agents is still working the whole prepare-for-shutdown procedure is 
	 * repeated. As a consequence this method may be invoked more than one time.
	 * @return a boolean value indicating that this agent is still working and needs more time before the shut-down 
	 * can actually take place.
	 */
	public boolean prepareForShutdown();
	
	/**
	 * Returns the current load of the agent or CURRENT_LOAD_UNKNOWN if not available 
	 * @return Current load of the agent
	 */
	public int getCurrentLoad();
}
