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
 * Panel with a custom table layout:
 * +--------------+
 * |              |
 * +---+----------+
 * |   |          |
 * +---+------+---+
 * |   |      |   |
 * +---+------+---+ 
 */
//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class TabelPanel extends Panel {

	private static final long serialVersionUID = 2296784780368694737L;

	private int columns;
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "tableElements")
	@XmlElement(name = "tableElement")
	//#ANDROID_EXCLUDE_END
	private List<TableElement> tableElements = new ArrayList<TableElement>();
	
	protected TabelPanel() {
		// Do not remove, used by JAXB
	}
	
	public TabelPanel(int columns) {
		this(null, columns);
	}
	
	public TabelPanel(String id, int columns) {
		this.id = id;
		this.columns = columns;
		
	}

	public int getColumns() {
		return columns;
	}
	
	public void addTableElement(Component comp, String title, int colSpan, int rowSpan) {
		TableElement te = new TableElement(comp, title, colSpan, rowSpan);
		tableElements.add(te);
	}
	
	public List<TableElement> getTableElements() {
		return tableElements;
	}
	
	@Override
	public List<Component> getComponents() {
		List<Component> components = new ArrayList<Component>();
		
		for (TableElement te : tableElements) {
			components.add(te.getComponent());
		}
		
		return components;		
	}

	@Override
	public Component getComponent(String id) {
		for (TableElement te : tableElements) {
			Component c = getComponentById(te.getComponent(), id);
			if (c != null) {
				return c;
			}
		}
		return null;
	}

	@Override
	public void doValidate() throws ConstraintException {
		for (TableElement te : tableElements) {
			te.getComponent().validate();
		}
	}
	
	@Override
	public void stamp() {
		for (TableElement te : tableElements) {
			te.getComponent().stamp();
		}
	}

	@Override
	void fillCacheData(Map<String, Object> userData) {
		for (TableElement te : tableElements) {
			te.getComponent().fillCacheData(userData);
		}
	}

	@Override
	void setCacheData(Map<String, Object> userData) {
		for (TableElement te : tableElements) {
			te.getComponent().setCacheData(userData);
		}
	}
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	//#ANDROID_EXCLUDE_END
	public class TableElement {
		
		private int colSpan;
		private int rowSpan;
		private String title;
		private Component comp;

		public TableElement(Component comp, String title, int colSpan, int rowSpan) {
			this.comp = comp;
			this.title = title;
			this.colSpan = colSpan;
			this.rowSpan = rowSpan;
		}
		
		public int getColSpan() {
			return colSpan;
		}
		
		public int getRowSpan() {
			return rowSpan;
		}
		
		public String getTitle() {
			return title;
		}
		
		public Component getComponent() {
			return comp;
		}
	}
}
