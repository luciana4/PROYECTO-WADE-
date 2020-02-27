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

import com.tilab.wade.performer.ontology.ExecutionError;

/**
 * The exception thrown by a subflow and subflow-join activity when there is any type of failure 
 * If the failure comes from an execution error in the subflow, the <code>ExecutionError</code> 
 * object providing details about the error can be retrieved by means of the <code>getExecutionError()</code>
 * method. 
 */
public class FailedSubflow extends WorkflowException {
	private ExecutionError executionError;

	public FailedSubflow(String message) {
		super(message);
	}

	public FailedSubflow(String message, Throwable nested) {
		super(message, nested);
	}
	
	void setExecutionError(ExecutionError er) {
		executionError = er;
	}
	
	/**
	 * Retrieve the <code>ExecutionError</code> object describing the error occurred during the execution of the subflow
	 * @return the <code>ExecutionError</code> object describing the error occurred during the execution of the subflow
	 */
	public ExecutionError getExecutionError() {
		return executionError;
	}
}
