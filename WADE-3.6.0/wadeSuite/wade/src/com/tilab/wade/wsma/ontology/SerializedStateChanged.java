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
package com.tilab.wade.wsma.ontology;

import jade.content.Predicate;

public class SerializedStateChanged implements Predicate {

	private static final long serialVersionUID = -3632613540709502488L;

	private String executionId;
	private String currentActivity;
	private byte[] serializedState;

	
	public SerializedStateChanged() {
	}

	public SerializedStateChanged(String executionId, String currentActivity, byte[] serializedState) {
		this.executionId = executionId;
		this.currentActivity = currentActivity;
		this.serializedState = serializedState;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String sessionId) {
		this.executionId = sessionId;
	}

	public String getCurrentActivity() {
		return currentActivity;
	}

	public void setCurrentActivity(String currentActivity) {
		this.currentActivity = currentActivity;
	}

	public byte[] getSerializedState() {
		return serializedState;
	}

	public void setSerializedState(byte[] serializedState) {
		this.serializedState = serializedState;
	}

	@Override
	public String toString() {
		return "Store [executionId=" + executionId + ", currentActivity=" + currentActivity + "]";
	}
}
