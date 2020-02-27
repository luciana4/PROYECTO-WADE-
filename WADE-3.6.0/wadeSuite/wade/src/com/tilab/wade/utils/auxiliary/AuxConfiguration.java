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
package com.tilab.wade.utils.auxiliary;

import jade.content.onto.annotations.Slot;
import jade.content.onto.annotations.SuppressSlot;
import jade.domain.FIPAAgentManagement.Property;

import java.util.List;

import com.tilab.wade.cfa.beans.AgentInfo;

public class AuxConfiguration {
	private String name;
	private List<Property> properties;
	private List<AgentInfo> agents;
	
	@Slot(mandatory=true)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Property> getProperties() {
		return properties;
	}
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
	public List<AgentInfo> getAgents() {
		return agents;
	}
	public void setAgents(List<AgentInfo> agents) {
		this.agents = agents;
	}
	
	@SuppressSlot
	public jade.util.leap.Properties getJadeProperties() {
		jade.util.leap.Properties pp = new jade.util.leap.Properties();
		if (properties != null) {
			for (Property p : properties) {
				Object value = p.getValue();
				if (value != null) {
					pp.setProperty(p.getName(), p.getValue().toString());
				}
			}
		}
		return pp;
	}
}
