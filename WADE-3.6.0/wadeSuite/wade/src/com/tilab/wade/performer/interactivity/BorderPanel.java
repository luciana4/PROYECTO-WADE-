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
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Panel with this layout:
 * +----------------------+
 * |        NORTH         |
 * +------+--------+------+
 * | WEST | CENTER | EAST |
 * +------+--------+------+
 * |        SOUTH         |
 * +----------------------+
 */
//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class BorderPanel extends Panel {

	private static final long serialVersionUID = -5914675143178567957L;

	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int CENTER = 4;

	private Component northComponent;
	private Component southComponent;
	private Component eastComponent;
	private Component westComponent;
	private Component centerComponent;
	
	public BorderPanel() {
		this(null);
	}
	
	public BorderPanel(String id) {
		this.id = id;

	}
	
	public void setComponent(Component comp, int position) {
		switch(position) {
			case NORTH:
				northComponent = comp;
				break;
			case SOUTH:
				southComponent = comp;
				break;
			case EAST:
				eastComponent = comp;
				break;
			case WEST:
				westComponent = comp;
				break;
			case CENTER:
				centerComponent = comp;
				break;
			default:
				throw new RuntimeException("Position ("+position+") not permitted in BorderPanel");
		}
	}

	public Component getComponent(int position) {
		switch(position) {
			case NORTH:
				return northComponent;
			case SOUTH:
				return southComponent;
			case EAST:
				return eastComponent;
			case WEST:
				return westComponent;
			case CENTER:
				return centerComponent;
			default:
				throw new RuntimeException("Position ("+position+") not permitted in BorderPanel");
		}
	}
	
	@Override
	public List<Component> getComponents() {
		List<Component> components = new ArrayList<Component>();
		
		if (northComponent != null) {
			components.add(northComponent);
		}
		if (southComponent != null) {
			components.add(southComponent);
		}
		if (eastComponent != null) {
			components.add(eastComponent);
		}
		if (westComponent != null) {
			components.add(westComponent);
		}
		if (centerComponent != null) {
			components.add(centerComponent);
		}
		
		return components;
	}

	@Override
	public Component getComponent(String id) {
		
		Component c = getComponentById(northComponent, id);
		if (c != null) return c;

		c = getComponentById(southComponent, id);
		if (c != null) return c;

		c = getComponentById(eastComponent, id);
		if (c != null) return c;

		c = getComponentById(westComponent, id);
		if (c != null) return c;
		
		c = getComponentById(centerComponent, id);
		if (c != null) return c;

		return null;
	}

	@Override
	public void doValidate() throws ConstraintException {
		if (northComponent != null) {
			northComponent.validate();
		}
		if (southComponent != null) {
			southComponent.validate();
		}
		if (eastComponent != null) {
			eastComponent.validate();
		}
		if (westComponent != null) {
			westComponent.validate();
		}
		if (centerComponent != null) {
			centerComponent.validate();
		}
	}
	
	@Override
	public void stamp() {
		if (northComponent != null) {
			northComponent.stamp();
		}
		if (southComponent != null) {
			southComponent.stamp();
		}
		if (eastComponent != null) {
			eastComponent.stamp();
		}
		if (westComponent != null) {
			westComponent.stamp();
		}
		if (centerComponent != null) {
			centerComponent.stamp();
		}
	}

	@Override
	void fillCacheData(Map<String, Object> userData) {
		if (northComponent != null) {
			northComponent.fillCacheData(userData);
		}
		if (southComponent != null) {
			southComponent.fillCacheData(userData);
		}
		if (eastComponent != null) {
			eastComponent.fillCacheData(userData);
		}
		if (westComponent != null) {
			westComponent.fillCacheData(userData);
		}
		if (centerComponent != null) {
			centerComponent.fillCacheData(userData);
		}
	}

	@Override
	void setCacheData(Map<String, Object> userData) {
		if (northComponent != null) {
			northComponent.setCacheData(userData);
		}
		if (southComponent != null) {
			southComponent.setCacheData(userData);
		}
		if (eastComponent != null) {
			eastComponent.setCacheData(userData);
		}
		if (westComponent != null) {
			westComponent.setCacheData(userData);
		}
		if (centerComponent != null) {
			centerComponent.setCacheData(userData);
		}
	}
}
