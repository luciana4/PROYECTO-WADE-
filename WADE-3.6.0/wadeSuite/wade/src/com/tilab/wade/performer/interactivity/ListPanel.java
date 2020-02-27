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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Panel with a vertical/horizontal list layout
 * +-----+
 * |     |  
 * +-----+
 * |     |  
 * +-----+
 * |     |  
 * |     |
 * +-----+
 */
//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class ListPanel extends Panel {

	private static final long serialVersionUID = -6058799097779555502L;

	public static final int VERTICAL_ORIENTATION = 0;
	public static final int HORIZONTAL_ORIENTATION = 1;
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "components")
	@XmlElement(name = "component")
	//#ANDROID_EXCLUDE_END
	private List<Component> components = new ArrayList<Component>();

	private int orient = VERTICAL_ORIENTATION;

	public ListPanel() {
		this(null);
	}
	
	public ListPanel(String id) {
		this.id = id;
	}

	public void addComponent(Component comp) {
		components.add(comp);
		
	}

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
	}

	@Override
	public List<Component> getComponents() {
		return components;
	}
	
	@Override
	public Component getComponent(String id) {
		for (Component comp : components) {
			Component c = getComponentById(comp, id);
			if (c != null) {
				return c;
			}
		}
		return null;
	}
	
	@Override
	public void doValidate() throws ConstraintException {
		for (Component comp : components) {
			comp.validate();
		}
	}
	
	@Override
	public void stamp() {
		for (Component comp : components) {
			comp.stamp();
		}
	}

	@Override
	void fillCacheData(Map<String, Object> userData) {
		for (Component comp : components) {
			comp.fillCacheData(userData);
		}
	}

	@Override
	void setCacheData(Map<String, Object> userData) {
		for (Component comp : components) {
			comp.setCacheData(userData);
		}
	}
}
