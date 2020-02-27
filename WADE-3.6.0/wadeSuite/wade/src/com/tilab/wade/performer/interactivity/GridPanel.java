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
 * Panel with a fixed grid layout:
 *  +--+--+--+
 *  |  |  |  |
 *  +--+--+--+
 *  |  |  |  |
 *  +--+--+--+
 *  |  |  |  |
 *  +--+--+--+
 */
//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class GridPanel extends Panel {

	private static final long serialVersionUID = -3516378151658862252L;

	private int rows;
	private int columns;
	private ArrayList<ArrayList<Component>> gridComponents;

	protected GridPanel() {
		// Do not remove, used by JAXB
	}
		
	public GridPanel(int rows, int columns) {
		this(null, rows, columns);
	}
	
	public GridPanel(String id, int rows, int columns) {
		this.id = id;
		this.rows = rows;
		this.columns = columns;
		

		gridComponents = new ArrayList<ArrayList<Component>>(rows);
		for (int r=0; r<rows; r++) {
			ArrayList<Component> cols = new ArrayList<Component>(columns);
			for (int c=0; c<columns; c++) {
				cols.add(null);
			}
			gridComponents.add(cols);
		}
	}
	
	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	public void setComponent(Component comp, int row, int col) {
		gridComponents.get(row).set(col, comp);
	}

	public Component getComponent(int row, int col) {
		return gridComponents.get(row).get(col);
	}
	
	@Override
	public List<Component> getComponents() {
		List<Component> components = new ArrayList<Component>();
		
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponent(r,c);
				if (comp != null) {
					components.add(comp);
				}
			}
		}

		return components;		
	}
	
	@Override
	public Component getComponent(String id) {
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponentById(getComponent(r,c), id);
				if (comp != null) {
					return comp;
				}
			}
		}
		return null;
	}

	@Override
	public void doValidate() throws ConstraintException {
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponent(r,c);
				if (comp != null) {
					comp.validate();
				}
			}
		}
	}
	
	@Override
	public void stamp() {
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponent(r,c);
				if (comp != null) {
					comp.stamp();
				}
			}
		}
	}

	@Override
	void fillCacheData(Map<String, Object> userData) {
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponent(r,c);
				if (comp != null) {
					comp.fillCacheData(userData);
				}
			}
		}
	}

	@Override
	void setCacheData(Map<String, Object> userData) {
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				Component comp = getComponent(r,c);
				if (comp != null) {
					comp.setCacheData(userData);
				}
			}
		}
	}
}
