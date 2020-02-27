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

import jade.content.Concept;
import jade.util.leap.List;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.domain.FIPAAgentManagement.Property;

/**
 */
public class Modifier implements Concept {
	private String name;
	private List properties;
	
	public Modifier() {
	}
	
	public Modifier(String name) {
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setProperties(List properties) {
		this.properties = properties;
	}
	
	public List getProperties() {
		return properties;
	}
	
	
	/////////////////////////////////////
	// Utility methods
	/////////////////////////////////////
	public void setProperty(String name, Object value) {
		Property p = getPropertyObject(name);
		if (p == null) {
			p = new Property(name, value);
			properties.add(p);
		}
		else {
			p.setValue(value);
		}
	}
	
	public Object getProperty(String name) {
		Property p = getPropertyObject(name);
		if (p != null) {
			return p.getValue();
		}
		else {
			return null;
		}
	}
	
	public Object removeProperty(String name) {
		Property p = getPropertyObject(name);
		if (p != null) {
			properties.remove(p);
		}
		return p;
	}
	
	private Property getPropertyObject(String name) {
		if (properties == null) {
			properties = new ArrayList();
		}
		for (int i = 0; i < properties.size(); ++i) {
			Property p = (Property) properties.get(i);
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}
	
	public static final Modifier getModifier(String name, List l) {
		if (l != null) {
			Iterator it = l.iterator();
			while (it.hasNext()) {
				Modifier m = (Modifier) it.next();
				if (m.getName().equals(name)) {
					return m;
				}				
			}
		}
		return null;
	}
}
