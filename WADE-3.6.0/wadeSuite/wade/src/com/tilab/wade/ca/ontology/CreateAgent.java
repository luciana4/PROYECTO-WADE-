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
import jade.util.leap.List;
import jade.util.leap.ArrayList;

public class CreateAgent implements AgentAction {
	private String name;
	private String className;
	private Object[] arguments;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public Object[] getArguments() {
		return arguments;
	}
	public List getAgentArguments() {
		List args = null;
		if (arguments != null) {
			args = new ArrayList(arguments.length);
			for (int i = 0; i < arguments.length; ++i) {
				args.add(arguments[i]);
			}
		}
		return args;
	}
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}	
	
	public void setAgentArguments(List args) {
		if (args != null) {
			arguments = args.toArray();
		}
		else {
			arguments = null;
		}
	}
}
