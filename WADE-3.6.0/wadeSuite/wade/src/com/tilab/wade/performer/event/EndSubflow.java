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
package com.tilab.wade.performer.event;

//#MIDP_EXCLUDE_BEGIN

/**
   The event generated at the end of a subflow
 */
public class EndSubflow extends WorkflowEvent {
	private String executionId;
	private int exitValue;
	
	public EndSubflow() {
	}
	
	public EndSubflow(String executionId, int exitValue) {
		this.executionId = executionId;
		this.exitValue = exitValue;
	}
	
	public EndSubflow(String executionId, int exitValue, String workflowId, String sessionId) {
		this.executionId = executionId;
		this.exitValue = exitValue;
		setWorkflowId(workflowId);
		setSessionId(sessionId);
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
	
	/**
	 * @deprecated Use getExecutionId() instead
	 */
	public String getName() {
		return getExecutionId();
	}
	
	/**
	 * @deprecated Use setExecutionId() instead
	 */
	public void setName(String name) {
		setExecutionId(name);
	}
	
	public int getExitValue() {
		return exitValue;
	}
	
	public void setExitValue(int exitValue) {
		this.exitValue = exitValue;
	}
	
	public String toString() {
		return getClass().getName()+": execution-id = "+executionId+", exit-value = "+exitValue;
	}
}
