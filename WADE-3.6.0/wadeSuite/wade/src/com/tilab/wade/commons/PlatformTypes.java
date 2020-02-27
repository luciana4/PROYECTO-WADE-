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
package com.tilab.wade.commons;

import jade.content.onto.annotations.Element;
import jade.content.onto.annotations.Slot;
import jade.domain.FIPAAgentManagement.Property;

import java.util.List;
import java.util.ArrayList;

/** 
 * @author marco ughetti
 */
@Element(name="platform")
public class PlatformTypes {
	private List<Property> properties = new ArrayList<Property>();
	private List<AgentRole> roles = new ArrayList<AgentRole>();
	private List<AgentType> types = new ArrayList<AgentType>();
	private List<EventType> customEventTypes = new ArrayList<EventType>();

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	@Slot(name="agentTypes")
	public List<AgentType> getTypes() {
		return types;
	}

	public void setTypes(List<AgentType> types) {
		this.types = types;
	}

	@Slot(name="agentRoles")
	public List<AgentRole> getRoles() {
		return roles;
	}

	public void setRoles(List<AgentRole> roles) {
		this.roles = roles;
	}

	public List<EventType> getCustomEventTypes() {
		return customEventTypes;
	}

	public void setCustomEventTypes(List<EventType> customEventTypes) {
		this.customEventTypes = customEventTypes;
	}

	@Override
	public String toString() {
		return "PlatformTypes [properties=" + properties + ", roles=" + roles + ", types=" + types + ", customEventTypes=" + customEventTypes + "]";
	}
}
