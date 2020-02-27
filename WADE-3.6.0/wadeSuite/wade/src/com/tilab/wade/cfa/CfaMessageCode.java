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

import com.tilab.wade.commons.locale.MessageCode;

// FIXME: Make it package-scoped as soon as the CFA will be fully wadeizated
public interface CfaMessageCode extends MessageCode {
	public static final String LOAD_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "LOAD_CONFIGURATION_ERROR";
	public static final String IMPORT_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "IMPORT_CONFIGURATION_ERROR";
	public static final String EXPORT_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "EXPORT_CONFIGURATION_ERROR";
	public static final String REMOVE_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "REMOVE_CONFIGURATION_ERROR";
	public static final String GET_HOSTS_ERROR = MSGCODE_START_CHARACTER + "GET_HOSTS_ERROR";
	public static final String GET_MAIN_AGENTS_ERROR = MSGCODE_START_CHARACTER + "GET_MAIN_AGENTS_ERROR";
	public static final String GET_AGENT_POOLS_ERROR = MSGCODE_START_CHARACTER + "GET_AGENT_POOLS_ERROR";
	public static final String COMPARE_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "COMPARE_CONFIGURATION_ERROR";
	public static final String GET_CONTAINER_PROFILES_ERROR = MSGCODE_START_CHARACTER + "GET_CONTAINER_PROFILES_ERROR";
	public static final String KILL_CONTAINER_ERROR = MSGCODE_START_CHARACTER + "KILL_CONTAINER_ERROR";
	public static final String KILL_CONTAINER_REFUSE = MSGCODE_START_CHARACTER + "KILL_CONTAINER_REFUSE";;
	public static final String CA_REQUEST_ENCODING_ERROR = MSGCODE_START_CHARACTER + "CA_REQUEST_ENCODING_ERROR";
	public static final String GET_CONFIGURATIONS_ERROR = MSGCODE_START_CHARACTER + "GET_CONFIGURATIONS_ERROR";
	public static final String SAVE_CONFIGURATION_ERROR = MSGCODE_START_CHARACTER + "SAVE_CONFIGURATION_ERROR";
	public static final String CONFIGURATION_LOADING_ERROR = MSGCODE_START_CHARACTER + "CONFIGURATION_LOADING_ERROR";
	public static final String CONTAINER_STARTUP_ERROR = MSGCODE_START_CHARACTER + "CONTAINER_STARTUP_ERROR";
	public static final String BACKUP_MAIN_CONTAINER_STARTUP_ERROR = MSGCODE_START_CHARACTER + "BACKUP_MAIN_CONTAINER_STARTUP_ERROR";
	public static final String NOT_ACTIVE_MAIN_REPLICATION_SERVICE = MSGCODE_START_CHARACTER + "NOT_ACTIVE_MAIN_REPLICATION_SERVICE";
	public static final String CA_STARTUP_ERROR = MSGCODE_START_CHARACTER + "CA_STARTUP_ERROR";
	public static final String BCA_STARTUP_ERROR = MSGCODE_START_CHARACTER + "BCA_STARTUP_ERROR";
	public static final String AGENT_STARTUP_ERROR = MSGCODE_START_CHARACTER + "AGENT_STARTUP_ERROR";
	public static final String HOST_NOT_REACHABLE = MSGCODE_START_CHARACTER + "HOST_NOT_REACHABLE";
	public static final String HOST_NOT_FOUND = MSGCODE_START_CHARACTER + "HOST_NOT_FOUND";
	public static final String INVALID_HOST_NAME = MSGCODE_START_CHARACTER + "INVALID_HOST_NAME";
	public static final String BOOTDAEMON_NOT_AVAILABLE = MSGCODE_START_CHARACTER + "BOOTDAEMON_NOT_AVAILABLE";
	public static final String PLATFORM_STATUS_ERROR = MSGCODE_START_CHARACTER + "PLATFORM_STATUS_ERROR";
	public static final String ADD_HOST_ERROR = MSGCODE_START_CHARACTER + "ADD_HOST_ERROR";
	public static final String REMOVE_HOST_ERROR = MSGCODE_START_CHARACTER + "REMOVE_HOST_ERROR";
	public static final String GROUP_STATUS_ERROR = MSGCODE_START_CHARACTER + "GROUP_STATUS_ERROR";
	public static final String GROUP_NAME_ERROR = MSGCODE_START_CHARACTER + "GROUP_NAME_ERROR";
	public static final String SHUTDOWN_GROUP_ERROR = MSGCODE_START_CHARACTER + "SHUTDOWN_GROUP_ERROR";
}
