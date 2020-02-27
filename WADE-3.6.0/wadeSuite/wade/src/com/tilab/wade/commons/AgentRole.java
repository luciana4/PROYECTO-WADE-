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
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 1-giu-2006
 * Time: 17.25.27
 * To change this template use File | Settings | File Templates.
 */
public class AgentRole implements Serializable {
	private static String ANY_ROLE ="any-role";
	private static String NO_ROLE ="no-role";
	public static AgentRole ANY = new AgentRole(ANY_ROLE);
	public static AgentRole NONE = new AgentRole(NO_ROLE);
	private List properties = new ArrayList();
	private String description;
	private int code;
	private int hashcode;

	public AgentRole(){
	}
	public AgentRole(String description) {
		setDescription(description);
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
	 *  Required by Digester
	 *
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
				 return description.equals(((AgentRole)obj).description);
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
		 return "AgentRole [description=" + description + ", code=" + code + "]";
	 }
}
