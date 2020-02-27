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

//#MIDP_EXCLUDE_FILE

import jade.content.Concept;
import jade.util.leap.List;

import java.util.Date;

/**
 * Bean-like class embedding relevant information about a WorkflowExecutor
 * @author Giovanni Caire - TILAB
 */
public class ExecutorInfo implements Concept {
	private String id;
	private String sessionId;
	private String delegationChain;
	private int executorStatus;
	private String executorFSMState;
	private String workflowFSMState;
	private int abortCondition;
	private List subflows;
	private Boolean watchDogExpired;
	private Date watchDogStartTime;
	private Date watchDogWakeupTime;
	
	
	public ExecutorInfo() {
	}
	
	public ExecutorInfo(String id) {
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public void setDelegationChain(String delegationChain) {
		this.delegationChain = delegationChain;
	}
	
	public String getDelegationChain() {
		return delegationChain;
	}
	
	public void setExecutorStatus(int executorStatus) {
		this.executorStatus = executorStatus;
	}
	
	public int getExecutorStatus() {
		return executorStatus;
	}
	
	public void setExecutorFSMState(String executorFSMState) {
		this.executorFSMState = executorFSMState;
	}
	
	public String getExecutorFSMState() {
		return executorFSMState;
	}
	
	public void setWorkflowFSMState(String workflowFSMState) {
		this.workflowFSMState = workflowFSMState;
	}
	
	public String getWorkflowFSMState() {
		return workflowFSMState;
	}
	
	public void setAbortCondition(int abortCondition) {
		this.abortCondition = abortCondition;
	}
	
	public int getAbortCondition() {
		return abortCondition;
	}
	
	public void setSubflows(List subflows) {
		this.subflows = subflows;
	}
	
	public List getSubflows() {
		return subflows;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("(Executor ");
		sb.append(":id ");
		sb.append(value(id));
		sb.append(' ');
		sb.append(":sessionId ");
		sb.append(value(sessionId));
		sb.append(' ');
		sb.append(":delegationChain ");
		sb.append(value(delegationChain));
		sb.append(' ');
		sb.append(":executorStatus ");
		sb.append(executorStatus);
		sb.append(' ');
		sb.append(":executorFSMState ");
		sb.append(value(executorFSMState));
		sb.append(' ');
		sb.append(":workflowFSMState ");
		sb.append(value(workflowFSMState));
		sb.append(' ');
		sb.append(":abortCondition ");
		sb.append(abortCondition);
		sb.append(' ');
		sb.append(":watchdog-expired ");
		sb.append(value(watchDogExpired));
		sb.append(' ');
		sb.append(":watchdog-starttime ");
		sb.append(value(watchDogStartTime));
		sb.append(' ');
		sb.append(":watchdog-wakeuptime ");
		sb.append(value(watchDogWakeupTime));
		sb.append(' ');
		if (subflows != null) {
			sb.append(":subflows (sequence ");
			for (int i = 0; i < subflows.size(); ++i) {
				sb.append(subflows.get(i).toString());
				sb.append(' ');
			}
			sb.append(')');
		}
		sb.append(')');
		return sb.toString();
	}
	
	private String value(Object v) {
		return (v != null ? v.toString() : "null");
	}

	public Boolean getWatchDogExpired() {
		return watchDogExpired;
	}

	public void setWatchDogExpired(Boolean watchDogExpired) {
		this.watchDogExpired = watchDogExpired;
	}

	public Date getWatchDogStartTime() {
		return watchDogStartTime;
	}

	public void setWatchDogStartTime(Date watchDogStartTime) {
		this.watchDogStartTime = watchDogStartTime;
	}

	public Date getWatchDogWakeupTime() {
		return watchDogWakeupTime;
	}

	public void setWatchDogWakeupTime(Date watchDogWakeupTime) {
		this.watchDogWakeupTime = watchDogWakeupTime;
	}
}
