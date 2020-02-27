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
package com.tilab.wade.performer.interactivity;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import java.io.Serializable;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Action.class, Component.class})
//#ANDROID_EXCLUDE_END
public abstract class VisualElement implements Serializable {
	
	abstract void fillCacheData(Map<String, Object> userData);
	abstract void setCacheData(Map<String, Object> userData);
	
	// Unique element identifier
	// Mandatory for InformationElement
	// Optional for Panel and Action
	protected String id;
	
	// Element label
	protected String label;
	
	// Element graphical information
	// Specific attributes (eg width="100%" height="10px" style="border=1px; font=...")
	private String height;
	private String width;
	private String style;
	
	// Generic attributes 
	public static final String READ_ONLY = "read-only";
	private Map<String, Object> properties = new HashMap<String, Object>();

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}
	
	protected Component getComponentById(Component comp, String id) {
		if (comp == null || id == null) {
			return null;
		}
		
		if (comp.getId() != null && comp.getId().equals(id)) {
			return comp;
		}
		
		if (comp instanceof Panel) {
			return ((Panel)comp).getComponent(id);
		}
		
		return null;
	}
}
