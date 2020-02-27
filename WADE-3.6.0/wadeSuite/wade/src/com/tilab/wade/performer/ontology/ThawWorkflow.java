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

import jade.content.AgentAction;
import jade.util.leap.List;

public class ThawWorkflow implements AgentAction {

	private byte[] workflowSerializedState;
	private Integer execution = null;
	private List controlInfos;
	private List modifiers;

	
	public ThawWorkflow() {
	}
	
	public ThawWorkflow(byte[] workflowSerializedState, List controlInfos, List modifiers) {
		this.workflowSerializedState = workflowSerializedState;
		this.controlInfos = controlInfos;
		this.modifiers = modifiers;
	}

	public byte[] getWorkflowSerializedState() {
		return workflowSerializedState;
	}

	public void setWorkflowSerializedState(byte[] workflowSerializedState) {
		this.workflowSerializedState = workflowSerializedState;
	}

	public Integer getExecution() {
		return execution;
	}

	public void setExecution(Integer execution) {
		this.execution = execution;
	}

	public List getModifiers() {
		return modifiers;
	}

	public void setModifiers(List modifiers) {
		this.modifiers = modifiers;
	}

	public List getControlInfos() {
		return controlInfos;
	}

	public void setControlInfos(List controlInfos) {
		this.controlInfos = controlInfos;
	}
}
