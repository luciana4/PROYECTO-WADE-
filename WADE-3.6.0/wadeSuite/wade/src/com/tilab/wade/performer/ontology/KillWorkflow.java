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

import jade.content.AgentAction;

/**
 */
public class KillWorkflow implements AgentAction {
	private String executionId;
	private Boolean smooth;
	private Boolean freeze;
	private int scope = WorkflowManagementVocabulary.SCOPE_TARGET_ONLY;
	private String message;
	

	public KillWorkflow() {
	}
	
	public KillWorkflow(String executionId) {
		this.executionId = executionId;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public Boolean getSmooth() {
		return smooth;
	}
	
	public void setSmooth(Boolean smooth) {
		this.smooth = smooth;
	}

	public void setFreeze(Boolean freeze) {
		this.freeze = freeze;
	}
	
	public Boolean getFreeze() {
		return freeze;
	}

	public void setScope(int scope) {
		this.scope = scope;
	}

	public int getScope() {
		return scope;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
