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
package com.tilab.wade.performer.ontology;

import com.tilab.wade.event.EventVocabulary;

/**
 * Vocabulary interface for the WorkflowManagementOntology
 * @author Giovanni Caire - TILAB
 */
public interface WorkflowManagementVocabulary extends ErrorsVocabulary, EventVocabulary {
	public static final String ONTOLOGY_NAME = "Workflow-management-ontology";
	
	// Kill scope constants
	public static final int SCOPE_TARGET_ONLY = 0;
	public static final int SCOPE_INNER_MOST = 1;
	public static final int SCOPE_ALL = 2;
	
	// Concepts
	public static final String WORKFLOW = "workflow";
	public static final String WORKFLOW_NAME = "name";
	public static final String WORKFLOW_ID = "id";
	public static final String WORKFLOW_PACKAGES = "packages";
	public static final String WORKFLOW_PARAMETERS = "parameters";
	public static final String WORKFLOW_EXECUTION = "execution";
	public static final String WORKFLOW_PRIORITY = "priority";
	public static final String WORKFLOW_TIMEOUT = "timeout";
	public static final String WORKFLOW_TRANSACTIONAL = "transactional";
	public static final String WORKFLOW_REQUESTER = "requester";
	public static final String WORKFLOW_SID = "session-id";
	public static final String WORKFLOW_DELEGATION_CHAIN = "delegation-chain";
	public static final String WORKFLOW_SESSION_STARTUP = "session-startup";
	public static final String WORKFLOW_CLASSLOADER_IDENTIFIER = "classloader-identifier";
	public static final String WORKFLOW_FORMAT = "format";
	public static final String WORKFLOW_REPRESENTATION = "representation";
	
	public static final String CONTROL_INFO = "control-info";
	public static final String CONTROL_INFO_TYPE = "type";
	public static final String CONTROL_INFO_SYNCH = "synch";
	public static final String CONTROL_INFO_VERBOSITY_LEVEL = "verbosity-level";
	public static final String CONTROL_INFO_CONTROLLERS = "controllers";
	public static final String CONTROL_INFO_SELF_CONFIG = "self-config";
	
	public static final String CONTROL_INFO_CHANGES = "control-info-changes";
	public static final String CONTROL_INFO_CHANGES_TYPE = "type";
	public static final String CONTROL_INFO_CHANGES_SYNCH = "synch";
	public static final String CONTROL_INFO_CHANGES_VERBOSITY_LEVEL = "verbosity-level";
	public static final String CONTROL_INFO_CHANGES_SELF_CONFIG = "self-config";
	public static final String CONTROL_INFO_CHANGES_CONTROLLERS = "controllers";
	public static final String CONTROL_INFO_CHANGES_CONTROLLERS_TO_REMOVE = "controllers-to-remove";
	public static final String CONTROL_INFO_CHANGES_CONTROLLERS_TO_ADD = "controllers-to-add";

	public static final String PARAMETER = "parameter";
	public static final String PARAMETER_NAME = "name";
	public static final String PARAMETER_TYPE = "type";
	public static final String PARAMETER_ELEMENT_TYPE = "element-type";
	public static final String PARAMETER_MODE = "mode";
	public static final String PARAMETER_WRAPPED_VALUE = "wrapped-value";
	public static final String PARAMETER_DOCUMENTATION = "documentation";
	public static final String PARAMETER_SCHEMA = "schema";
	public static final String PARAMETER_DEFAULT_VALUE = "default-value";
	public static final String PARAMETER_MANDATORY = "mandatory";
	public static final String PARAMETER_REGEX = "regex";
	public static final String PARAMETER_CARD_MIN = "card-min";
	public static final String PARAMETER_CARD_MAX = "card-max";
	public static final String PARAMETER_PERMITTED_VALUES = "permitted-values";
	
	public static final String MODIFIER = "modifier";
	public static final String MODIFIER_NAME = "name";
	public static final String MODIFIER_PROPERTIES = "properties";
	
	public static final String EXECUTOR = "Executor";
	public static final String EXECUTOR_ID = "id";
	public static final String EXECUTOR_SESSION_ID = "session-id";
	public static final String EXECUTOR_DELEGATION_CHAIN = "delegation-chain";
	public static final String EXECUTOR_EXECUTOR_STATUS = "executor-status";
	public static final String EXECUTOR_EXECUTOR_FSM_STATE = "executor-fsm-state";
	public static final String EXECUTOR_WORKFLOW_FSM_STATE = "workflow-fsm-state";
	public static final String EXECUTOR_ABORT_CONDITION = "abort-condition";
	public static final String EXECUTOR_WATCHDOG_EXPIRED = "watchdog-expired";
	public static final String EXECUTOR_WATCHDOG_STARTTIME = "watchdog-starttime";
	public static final String EXECUTOR_WATCHDOG_WAKEUPTIME = "watchdog-wakeuptime";
	public static final String EXECUTOR_SUBFLOWS = "subflows";
	
	public static final String SUBFLOW = "Subflow";
	public static final String SUBFLOW_ID = "id";
	public static final String SUBFLOW_DELEGATED_PERFORMER = "delegated-performer";
	public static final String SUBFLOW_EXECUTION_ID = "execution-id";
	public static final String SUBFLOW_STATUS = "status";
	
	// Actions
	public static final String EXECUTE_WORKFLOW = "execute-workflow";
	public static final String EXECUTE_WORKFLOW_WHAT = "what";
	public static final String EXECUTE_WORKFLOW_HOW = "how";
	public static final String EXECUTE_WORKFLOW_MODIFIERS = "modifiers";
	// When a workflow must be interpreted its package SL definition is 
	// directly included in the ExecuteWorkflow action
	public static final String EXECUTE_WORKFLOW_PKGDEF = "package-definition";
	
	public static final String THAW_WORKFLOW = "thaw-workflow";
	public static final String THAW_WORKFLOW_SERIALIZED_STATE = "workflow-serialized-state";
	public static final String THAW_WORKFLOW_EXECUTION = "execution";
	public static final String THAW_WORKFLOW_CONTROL_INFOS = "control-infos";
	public static final String THAW_WORKFLOW_MODIFIERS = "modifiers";

	public static final String RECOVER_WORKFLOW = "recover-workflow";
	public static final String RECOVER_WORKFLOW_EXECUTION_ID = "execution-id";
	public static final String RECOVER_WORKFLOW_EXECUTION = "execution";
	public static final String RECOVER_WORKFLOW_CONTROL_INFOS = "control-infos";
	public static final String RECOVER_WORKFLOW_MODIFIERS = "modifiers";

	public static final String KILL_WORKFLOW = "kill-workflow";
	public static final String KILL_WORKFLOW_EXECUTION_ID = "execution-id";
	public static final String KILL_WORKFLOW_SMOOTH = "smooth";
	public static final String KILL_WORKFLOW_FREEZE = "freeze";
	public static final String KILL_WORKFLOW_SCOPE = "scope";
	public static final String KILL_WORKFLOW_MESSAGE = "message";
	
	public static final String SET_CONTROL_INFO = "set-control-info";
	public static final String SET_CONTROL_INFO_EXECUTION_ID = "execution-id";
	public static final String SET_CONTROL_INFO_INFO = "info";
	
	public static final String UPDATE_CONTROL_INFO = "update-control-info";
	public static final String UPDATE_CONTROL_INFO_EXECUTION_ID = "execution-id";
	public static final String UPDATE_CONTROL_INFO_INFO = "info";
	
	public static final String GET_WRD = "get-wrd";
	public static final String GET_WRD_EXECUTION_ID = "execution-id";
	public static final String GET_WRD_WRD = "wrd";
	
	public static final String SET_WRD = "set-wrd";
	public static final String SET_WRD_EXECUTION_ID = "execution-id";
	public static final String SET_WRD_WRD = "wrd";
	public static final String SET_WRD_VALUE = "value";
	
	public static final String GET_SESSION_STATUS = "get-session-status";
	public static final String GET_SESSION_STATUS_SESSION_ID = "session-id";
	
	public static final String RESET_MODIFIERS = "reset-modifiers";
	public static final String RESET_MODIFIERS_EXECUTION_ID = "execution-id";
	public static final String RESET_MODIFIERS_MODIFIERS = "modifiers";

	public static final String RESET_CONTROL_INFOS = "reset-control-infos";
	public static final String RESET_CONTROL_INFOS_EXECUTION_ID = "execution-id";
	public static final String RESET_CONTROL_INFOS_CONTROL_INFOS = "control-infos";
	
	public static final String GET_POOL_SIZE = "get-pool-size";
	
	// Predicates
	public static final String EXECUTION_ERROR = "execution-error";
	public static final String EXECUTION_ERROR_TYPE = "type";
	public static final String EXECUTION_ERROR_REASON = "reason";
	public static final String EXECUTION_ERROR_PARAMETERS = "parameters";
	public static final String FROZEN = "frozen";
}