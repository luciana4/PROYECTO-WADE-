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
import java.util.Iterator;
import java.util.HashSet;


public class ContainerProfileInfo extends PlatformElement{

	private static final long serialVersionUID = -3004187837092158670L;

	public static final String JADE_PROFILE = "JADE";
	public static final String JAVA_PROFILE = "JAVA";
	
	private String name;
	private String type; // JADE or JAVA
	private Collection<ContainerProfilePropertyInfo> properties = new HashSet<ContainerProfilePropertyInfo>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	//////////////////////////////////////
	// Properties field
	//////////////////////////////////////
	
	// Required by Hibernate & betwixt
	public Collection<ContainerProfilePropertyInfo> getProperties() {
		return properties;
	}
	
	// Required by Hibernate 
	public void setProperties(Collection<ContainerProfilePropertyInfo> properties) {
		this.properties = properties;
	}
	
	// Required by Digester & Betwixt
	public void addProperty(ContainerProfilePropertyInfo property) {
		this.properties.add(property);
	}



}
