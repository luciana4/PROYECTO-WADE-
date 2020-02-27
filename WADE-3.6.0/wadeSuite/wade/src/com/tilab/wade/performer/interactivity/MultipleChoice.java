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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class MultipleChoice extends InformationElement {

	private static final long serialVersionUID = -8325314586411410437L;

	public static final int CHECKBOX_TYPE = 0;
	public static final int LISTBOX_TYPE = 1;
	
	public static final int VERTICAL_ORIENTATION = 0;
	public static final int HORIZONTAL_ORIENTATION = 1;
	
	private int type = CHECKBOX_TYPE;
	private int orient = VERTICAL_ORIENTATION;
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "items")
	@XmlElement(name = "item")
	//#ANDROID_EXCLUDE_END
	private List<ChoiceItem> items = new ArrayList<ChoiceItem>();
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "selectedItems")
	@XmlElement(name = "item")
	//#ANDROID_EXCLUDE_END
	private List<ChoiceItem> selectedItems = new ArrayList<ChoiceItem>();
	
	protected MultipleChoice() {
		// Do not remove, used by JAXB
	}

	public MultipleChoice(String id) {
		super(id);
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
	}

	public void addItem(String itemValue) {
		addItem(itemValue, false);
	}

	public void addItem(String itemValue, boolean selected) {
		addItem(new ChoiceItem(itemValue), selected);
	}

	public void addItem(ChoiceItem item) {
		addItem(item, false);
	}

	public void addItem(ChoiceItem item, boolean selected) {
		items.add(item);
		
		if (selected) {
			selectedItems.add(item);
		}
	}
	
	public List<ChoiceItem> getItems() {
		return items;
	}

	public List<ChoiceItem> getSelectedItems() {
		return selectedItems;
	}

	public List<String> getSelectedItemValues() {
		List<String> values = new ArrayList<String>();
		for (ChoiceItem ci : selectedItems) {
			if (ci.getValue() != null) {
				values.add(ci.getValue().toString());
			}
		}
		return values;
	}
	
	public void setSelected(String itemValue, boolean selected) {
		setSelected(new ChoiceItem(itemValue), selected);
	}
	
	public void setSelected(ChoiceItem item, boolean selected) {
		if (selected && !selectedItems.contains(item)) {
			selectedItems.add(item);
		} else if (!selected) {
			selectedItems.remove(item);			
		}
	}
	
	@Override
	public void doValidate() throws ConstraintException {

		Object objToValidate;
		if (selectedItems.size() <= 0) {
			// To check mandatory
			objToValidate = null;
		} else {
			objToValidate = selectedItems;
		}
		
		validateConstraints(objToValidate);
	}
	
	@Override
	public void stamp() {
	}

	@Override
	protected Object getCacheValue() {
		return getSelectedItems();
	}

	@Override
	protected void setCacheValue(Object value) {
		selectedItems = (List<ChoiceItem>)value;
	}	
}	

