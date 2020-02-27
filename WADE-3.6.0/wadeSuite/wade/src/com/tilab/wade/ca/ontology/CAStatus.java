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

import java.util.Map;

import jade.content.Predicate;
import jade.domain.FIPAAgentManagement.Property;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

public class CAStatus implements Predicate {
	private boolean autorestart;
	private List globalProperties;
	
	public CAStatus() {
		globalProperties = new ArrayList();
	}
	
	public CAStatus(boolean autorestart, Map<String, String> props) {
		setAutorestart(autorestart);
		setGlobalPropertiesMap(props);
	}
	
	public boolean getAutorestart() {
		return autorestart;
	}
	
	public void setAutorestart(boolean autorestart) {
		this.autorestart = autorestart;
	}
	
	public List getGlobalProperties() {
		return globalProperties;
	}
	
	public void setGlobalProperties(List globalProperties) {
		this.globalProperties = globalProperties;
	}
	
	public void setGlobalPropertiesMap(Map<String, String> props) {
		globalProperties = new ArrayList(props.size());
		for (String name : props.keySet()) {
			Property p = new Property(name, props.get(name));
			globalProperties.add(p);
		}
	}
}
