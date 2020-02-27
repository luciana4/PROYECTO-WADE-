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

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class ModuleInfo implements Concept, Comparable {
	
	public static final String BUNDLE_NAME = "Bundle-Name";
	public static final String BUNDLE_VERSION = "Bundle-Version";
	public static final String BUNDLE_DATE = "Bundle-Date";
	public static final String BUNDLE_CATEGORY = "Bundle-Category";
	public static final String BUNDLE_DESCRIPTION = "Bundle-Description";
	
	public static final String SERVICE_CATEGORY = "SERVICE";
	public static final String GENERIC_CATEGORY = "GENERIC";
	
	public static enum ModuleState { ACTIVE, NEW, DELETED, MODIFIED };
	
	private String name;
	private String version;
	private long date;
	private String category;
	private String description;
	private String fileName;
	private ModuleState state;
	
	public ModuleInfo() {
	}
	
	@Slot(mandatory=true)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Slot(mandatory=false)
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	@Slot(mandatory=true)
	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	@Slot(mandatory=true)
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	@Slot(mandatory=false)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Slot(mandatory=true)
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Slot(mandatory=false)
	public ModuleState getState() {
		return state;
	}

	public void setState(ModuleState state) {
		this.state = state;
	}
	
	@Override
	public int compareTo(Object obj) {
	    return name.compareTo(((ModuleInfo)obj).getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ModuleInfo &&  name.equalsIgnoreCase(((ModuleInfo)obj).getName()));
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public String toString() {
		return "ModuleInfo [name=" + name + ", version=" + version + ", category=" + category + ", fileName=" + fileName + "]";
	}
}
