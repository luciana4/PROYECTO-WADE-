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
import jade.content.onto.annotations.Element;
import jade.content.onto.annotations.Slot;

@Element(name="workflow-detail")
public class WorkflowDetails implements Concept, Comparable {

	private String className;
	private String name;
	private String documentation;
	private Boolean component;
	private String category;
	private String icon;
	private String color;	
	private ModuleInfo moduleInfo;
	
	public WorkflowDetails() {
	}
	
	@Slot(name="class-name", mandatory=true)
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	@Slot(name="name", mandatory=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Slot(name="documentation", mandatory=false)
	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Slot(name="component", mandatory=false)
	public Boolean isComponent() {
		return component;
	}

	public void setComponent(Boolean component) {
		this.component = component;
	}
	
	@Slot(name="category", mandatory=false)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Slot(name="icon", mandatory=false)
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Slot(name="color", mandatory=false)
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Slot(name="module", mandatory=false)
	public ModuleInfo getModuleInfo() {
		return moduleInfo;
	}

	public void setModuleInfo(ModuleInfo moduleInfo) {
		this.moduleInfo = moduleInfo;
	}
	
	public int compareTo(Object obj) {
	    int result = name.compareTo(((WorkflowDetails)obj).getName());
	    if (result == 0) {
	    	result = className.compareTo(((WorkflowDetails)obj).getClassName());
	    }
	    return result;
	}

	@Override
	public String toString() {
		return "WorkflowDetails [className=" + className + ", name=" + name + ", documentation=" + documentation + ", category=" + category + "]";
	}
}
