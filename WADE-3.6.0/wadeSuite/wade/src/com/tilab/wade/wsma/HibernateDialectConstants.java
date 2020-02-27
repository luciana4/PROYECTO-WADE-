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

public interface HibernateDialectConstants {

	public static final String DIALECT = "SQL";

	// Workflow-Execution
	public static final String EXECUTION_ID_FIELD = "wi.executionId";
	public static final String NAME_FIELD = "wi.name";
	public static final String DOCUMENTATION_FIELD = "wi.documentation";
	public static final String PARENT_EXECUTION_ID_FIELD = "wi.parentExecutionid";
	public static final String WORKFLOW_ID_FIELD = "wi.workflowId";
	public static final String SESSION_ID_FIELD = "wi.sessionId";
	public static final String REQUESTER_FIELD = "wi.requester";
	public static final String EXECUTOR_FIELD = "wi.executorName";
	public static final String LONG_RUNNING_FIELD = "wi.longRunning";
	public static final String TRANSACTIONAL_FIELD = "wi.transactional";
	public static final String STATUS_FIELD = "wi.statusName";
	public static final String START_TIME_FIELD = "wi.startTime";
	public static final String LAST_UPDATE_TIME_FIELD = "wi.lastUpdateTime";
	public static final String RESULT_FIELD = "wi.resultName";
	public static final String ERROR_MESSAGE_FIELD = "wi.errorMessage";
	public static final String WF_CURRENT_ACTIVITY_FIELD = "wi.workflowCurrentActivity";
	public static final String PARAMETERS_FIELD = "wi.parameters";

	// Workflow-Parameter
	public static final String PARAMETER_NAME_FIELD = "pi.name";
	public static final String PARAMETER_TYPE_FIELD = "pi.type";
	public static final String PARAMETER_MODE_FIELD = "pi.mode";
	public static final String PARAMETER_VALUE_FIELD = "pi.value";
	public static final String PARAMETER_DOCUMENTATION_FIELD = "pi.documentation";
}
