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
package com.tilab.wade.performer;

/**
   Constant values used throughout the Workflow Engine package
   @author Giovanni Caire - TILAB
 */
public interface Constants {
	// XPDL types
	public static final int INTEGER = 0;
	public static final int BOOLEAN = 1;
	public static final int FLOAT = 2;
	public static final int STRING = 3;
	public static final int DATETIME = 4;
	public static final int REFERENCE = 5;
	
	//Java Types
	public static final String INTEGER_TYPE = "java.lang.Integer";
	public static final String BOOLEAN_TYPE = "java.lang.Boolean";
	public static final String STRING_TYPE = "java.lang.String";
	public static final String FLOAT_TYPE = "java.lang.Float";
	public static final String DATE_TYPE = "java.lang.Date";
	
	// Parameters mode
	public static final int NO_MODE = -1;
	public static final int IN_MODE = 0;
	public static final int OUT_MODE = 1;
	public static final int INOUT_MODE = 2;
	
	public static final String IN_MODE_LABEL = "IN";
	public static final String OUT_MODE_LABEL = "OUT";
	public static final String INOUT_MODE_LABEL = "INOUT";
	
	// Worflow execution types
	public static final int SYNCH = 0;
	public static final int ASYNCH = 1;
	
	// Workflow execution predefined modifiers
	public static final String VERIFY_MODIFIER = "VERIFY";
	public static final String MOCK_MODIFIER = "MOCK";

	// Worflow execution exit values
	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;
	public static final int FROZEN = 2;
	
	// Predefined worflow execution error types
	public static final String ABORTED = "Aborted";
	public static final String WORKFLOW_DEFINED = "Workflow-Defined";
	public static final String UNKNOWN = "Unknown";
	
	// Activity execution exit values
	public static final int DEFAULT_OK_EXIT_VALUE = -68392; // Strange value
	public static final int UNCAUGHT_EXCEPTION_EXIT_VALUE = -68393; // Strange value
	
	// Priority levels
	public static final int NO_PRIORITY = -1;
	public static final int DEFAULT_PRIORITY = 0;

	// Timeout values
	public static final long INFINITE_TIMEOUT = -1;
	
	// WRD null value
	public static final String NULL_VALUE = "null";
	
	// Condition types
	public static final int CONDITION = 0;
	public static final int OTHERWISE = 1;
	public static final int EXCEPTION = 2;
	public static final int DEFAULT_EXCEPTION = 3;

	public static final String CONDITION_LABEL = "CONDITION";
	public static final String OTHERWISE_LABEL = "OTHERWISE";
	public static final String EXCEPTION_LABEL = "EXCEPTION";
	public static final String DEFAULT_EXCEPTION_LABEL = "DEFAULT_EXCEPTION";
	
	/**
	 * User defined parameter key specifying, when set to "true", that this message carries a WADE event.
	 */
	public static final String EVENT_MESSAGE = "WADE-event-message";
	
	// Event types
	public static final String WARNING_TYPE = "warning";
	public static final String FLOW_TYPE = "flow";
	public static final String TERMINATION_TYPE = "termination";
	public static final String TRANSACTION_TYPE = "transaction";
	public static final String TRACING_TYPE = "tracing";
	
	// Common verbosity level
	public static final int OFF_LEVEL = 0;
	public static final int SEVERE_LEVEL = 1;
	public static final int WARNING_LEVEL = 2;
	public static final int INFO_LEVEL = 3;
	public static final int CONFIG_LEVEL = 4;
	public static final int FINE_LEVEL = 5;
	public static final int FINER_LEVEL = 6;
	public static final int FINEST_LEVEL = 7;
	public static final int ALL_LEVEL = 10;
	
	public static final int DEFAULT_LEVEL = INFO_LEVEL;
	
	// Flow events verbosity levels
	public static final int WORKFLOW_LEVEL = INFO_LEVEL;	
	public static final int ACTIVITY_LEVEL = CONFIG_LEVEL;
	public static final int APPLICATION_LEVEL = FINE_LEVEL;	
	
	// WebServices
	public static final String WS_HEADER_PREFIX = "header";
	public static final String WS_PREFIX_SEPARATOR = ".";

	// RestServices
	public static final String REST_HEADER_PREFIX = "header";
	public static final String REST_TEMPLATE_PREFIX = "template";
	public static final String REST_MATRIX_PREFIX = "matrix";
	public static final String REST_QUERY_PREFIX = "query";
	public static final String REST_BODY_PREFIX = "body";
	public static final String REST_FAULT_PREFIX = "fault";
	public static final String REST_HTTP_PREFIX = "http";
	public static final String REST_BODY_REQUEST = "request";
	public static final String REST_BODY_RESPONSE = "response";
	public static final String HTTP_STATUS_CODE = "statusCode";
	public static final String REST_PREFIX_SEPARATOR = ".";
	
	// Building block
	public static final String BB_PREFIX_SEPARATOR = ".";
	public static final String BB_PART_SEPARATOR = ".";
	public static final String BB_ID_SEPARATOR = "@";

	// Interactive modifier
	public static final String INTERACTIVE_MODIFIER = "INTERACTIVE_MODIFIER";
	public static final String INTERACTIVE_AID = "INTERACTIVE_AID";
	public static final String INTERACTIVE_FIRST_INTERACTION_EXECUTED = "INTERACTIVE_FIRST_INTERACTION_EXECUTED";
}
	
