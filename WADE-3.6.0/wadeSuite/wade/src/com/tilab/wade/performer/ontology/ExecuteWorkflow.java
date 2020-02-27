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
import jade.util.leap.ArrayList;
import jade.util.leap.List;

import com.tilab.wade.performer.descriptors.WorkflowDescriptor;


/**
 */
public class ExecuteWorkflow implements AgentAction {
	private WorkflowDescriptor what;
	private List how;
	private List modifiers;
	
	public ExecuteWorkflow() {
	}

	public ExecuteWorkflow(WorkflowDescriptor what, List how, List modifiers) {
		this.what = what;
		this.how = how;
		this.modifiers = modifiers;
	}
	
	public ExecuteWorkflow(WorkflowDescriptor what, List how) {
		this.what = what;
		this.how = how;
	}
	
	public void setWhat(WorkflowDescriptor what) {
		this.what = what;
	}
	
	public WorkflowDescriptor getWhat() {
		return what;
	}
	
	public void setHow(List how) {
		this.how = how;
	}
	
	public List getHow() {
		return how;
	}
	
	public void setModifiers(List modifiers) {
		this.modifiers = modifiers;
	}
	
	public List getModifiers() {
		return modifiers;
	}
	
	public void addModifier(Modifier m) {
		if (modifiers == null) {
			modifiers = new ArrayList();
		}
		modifiers.add(m);
	}
}
