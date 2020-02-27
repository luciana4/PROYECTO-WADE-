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
package com.tilab.wade.dispatcher;

import jade.core.AID;

import com.tilab.wade.performer.ontology.ExecutionError;

public class WorkflowExecutionException extends Exception {
	private static final long serialVersionUID = 5083071716446126402L;

	private ExecutionError error;

	private AID executor;
	
	private String executionId;
	
	public WorkflowExecutionException(ExecutionError error, AID executor, String executionId) {
		super("Execution exception");
		
		this.error = error;
		
		this.executionId = executionId;
		
		this.executor = executor;
	}
	
	public ExecutionError getExecutionError() {
		return error;
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public AID getExecutor() {
		return executor;
	}
}
