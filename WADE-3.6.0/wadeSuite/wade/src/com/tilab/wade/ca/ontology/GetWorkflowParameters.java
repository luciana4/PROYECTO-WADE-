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
package com.tilab.wade.ca.ontology;

import jade.content.AgentAction;
import jade.content.onto.annotations.Element;
import jade.content.onto.annotations.Slot;

@Element(name="get-workflow-parameters")
public class GetWorkflowParameters implements AgentAction {
	private static final long serialVersionUID = 6180500098391071149L;

	private String name;
	private String classloaderId = null;

	public GetWorkflowParameters() {
		// default constructor
	}

	public GetWorkflowParameters(String name) {
		this.name = name;
	}

	@Slot(name="name", mandatory=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Slot(name="classloader-id", mandatory=false)
	public String getClassloaderId() {
		return classloaderId;
	}

	public void setClassloaderId(String classloaderId) {
		this.classloaderId = classloaderId;
	}
}
