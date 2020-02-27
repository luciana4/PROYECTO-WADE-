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

import jade.domain.FIPAAgentManagement.Property;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * This class describes a WADE agent type and allows accessing the associated class, role (if any) and configuration properties
 */
public class AgentType implements Serializable {
	private static String ANY_TYPE ="any-type";
	private static String NO_TYPE ="no-type";
	public static AgentType ANY = new AgentType(ANY_TYPE, null, null);
	public static AgentType NONE = new AgentType(NO_TYPE, null,  null);
	private String className;
	private String role;
	private String description; //unique key
	private List properties = new ArrayList();
	private int hashcode;

	public AgentType() {
	}

	AgentType(String description, String className, String role) {
		setDescription(description);
		setClassName(className);
		setRole(role);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		hashcode = description.hashCode();
	}

	public List getProperties() {
		return properties;
	}

	public void setProperties(List properties) {
		this.properties = properties;
	}

	/**
	 * Required by Digester
	 * @param property
	 */
	public void addProperty(Property property) {
		properties.add(property);
	}

	public boolean equals(Object obj) {
		if (hashcode == obj.hashCode()) {
			if(!this.getClass().equals(obj.getClass())) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			else {
				return description.equals(((AgentType)obj).description);
			}
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return hashcode;
	}

	@Override
	public String toString() {
		return "AgentType [className=" + className + ", role=" + role + ", description=" + description + "]";
	}
}
