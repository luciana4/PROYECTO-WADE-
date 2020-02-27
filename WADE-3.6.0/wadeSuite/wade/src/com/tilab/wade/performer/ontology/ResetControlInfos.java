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

//#MIDP_EXCLUDE_FILE

public class ResetControlInfos implements AgentAction {

	private String executionId;
	private List controlInfos;

	public ResetControlInfos() {
	}
	
	public ResetControlInfos(String executionId, List controlInfos) {
		this.executionId = executionId;
		this.controlInfos = controlInfos;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public List getControlInfos() {
		return controlInfos;
	}

	public void setControlInfos(List controlInfos) {
		this.controlInfos = controlInfos;
	}
}
