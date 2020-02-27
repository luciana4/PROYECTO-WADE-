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
package com.tilab.wade.cfa.beans;

import java.util.Collection;

public class AgentInfo extends AgentBaseInfo {

	private static final long serialVersionUID = -9061257078875779362L;

	public AgentInfo() {
		super();
	}
	
	public AgentInfo(String name, String type, String className, String owner, Collection parameters) {
		super(name, type, className, owner, parameters);
	}
	
	public AgentInfo(String name, String type, String className, String owner) {
		super(name, type, className, owner);
	}
	
	public AgentInfo(String name, String type, String className, String owner, String group, Collection parameters) {
		super(name, type, className, owner, group, parameters);
	}
	
	public AgentInfo(String name, String type, String className, String owner, String group) {
		super(name, type, className, owner, group);
	}
}
