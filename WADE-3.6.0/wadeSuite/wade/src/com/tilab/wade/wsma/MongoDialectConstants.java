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
package com.tilab.wade.wsma;

public interface MongoDialectConstants {
	
	public static final String DIALECT = "MONGO"; 
	
	public static final String ID_KEY = "_id";
	public static final String EXECUTION_ID_KEY = "executionId";
	public static final String NAME_KEY = "name";
	public static final String DOCUMENTATION_KEY = "documentation";
	public static final String PARENT_EXECUTION_ID_KEY = "parentExecutionid";
	public static final String WORKFLOW_ID_KEY = "workflowId";
	public static final String SESSION_ID_KEY = "sessionId";
	public static final String REQUESTER_KEY = "requester";
	public static final String EXECUTOR_KEY = "executor";
	public static final String LONG_RUNNING_KEY = "longRunning";
	public static final String INTERACTIVE_KEY = "interactive";
	public static final String TRANSACTIONAL_KEY = "transactional";
	public static final String STATUS_KEY = "status";
	public static final String START_TIME_KEY = "startTime";
	public static final String LAST_UPDATE_TIME_KEY = "lastUpdateTime";
	public static final String RESULT_KEY = "result";
	public static final String ERROR_MESSAGE_KEY = "errorMessage";
	public static final String CURRENT_ACTIVITY_KEY = "currentActivity";
	public static final String SERIALIZED_STATE_KEY = "serializedState";
	public static final String PARAMETERS_KEY = "parameters";
	public static final String PARAMETER_NAME_KEY = "name";
	public static final String PARAMETER_TYPE_KEY = "type";
	public static final String PARAMETER_MODE_KEY = "mode";
	public static final String PARAMETER_VALUE_KEY = "value";
	public static final String PARAMETER_DOCUMENTATION_KEY = "documentation";
	
	public static final String COUNT_COMMAND = "count";
}
