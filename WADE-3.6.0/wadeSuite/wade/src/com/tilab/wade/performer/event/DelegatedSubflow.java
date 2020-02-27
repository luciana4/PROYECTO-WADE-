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
   The event generated when a subflow is delegated
 */
public class DelegatedSubflow extends WorkflowEvent {
	private String id;
	private String executor;
	private String executionId;
	
	public DelegatedSubflow() {
	}
	
	public DelegatedSubflow(String id, String executor, String executionId) {
		this.id = id;
		this.executor = executor;
		this.executionId = executionId;
	}
	
	public DelegatedSubflow(String id, String executor, String executionId, String workflowId, String sessionId) {
		this.id = id;
		this.executor = executor;
		this.executionId = executionId;
		setWorkflowId(workflowId);
		setSessionId(sessionId);
	}
	
	/**
	 * @return the workflow ID of the delegated workflow
	 */
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getExecutor() {
		return executor;
	}
	
	public void setExecutor(String executor) {
		this.executor = executor;
	}
	
	/**
	 * @return the execution-id of the delegated workflow
	 */
	public String getExecutionId() {
		return executionId;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
}
