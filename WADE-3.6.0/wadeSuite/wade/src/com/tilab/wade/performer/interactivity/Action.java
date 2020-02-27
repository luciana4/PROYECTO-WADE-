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
import javax.xml.bind.annotation.XmlTransient;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class Action extends VisualElement {

	private static final long serialVersionUID = 2169877448335206976L;

	public static enum ActionType { DEFAULT, ABORT, FREEZE, CUSTOM };
	
	private boolean selected;
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "validateComponents")
	@XmlElement(name = "id")
	//#ANDROID_EXCLUDE_END
	private List<String> toValidateComponentIds = new ArrayList<String>();
	private boolean validateAllComponents;
	private ActionType type;
	//#ANDROID_EXCLUDE_BEGIN
	@XmlTransient
	//#ANDROID_EXCLUDE_END
	protected Panel mainPanel;
	
	
	protected Action() {
		// Do not remove, used by JAXB
	}
	
	public Action(String label) {
		this(null, label);
	}
	
	public Action(String id, String label) {
		this.id = id;
		this.label = label;
		this.selected = false;
		this.validateAllComponents = false;
		this.type = ActionType.DEFAULT;
	}

	public ActionType getType() {
		return type;
	}

	public void setType(ActionType type) {
		this.type = type;
	}

	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	/**
	 * Enable validation check to all main-panel components
	 */
	public void requireAllComponentsValidation() {
		validateAllComponents = true;
		toValidateComponentIds.clear();
	}
	
	/**
	 * Add a single component into the list of components to validate
	 */
	public void addRequireValidationComponent(Component comp) {
		if (comp.getId() != null) {
			validateAllComponents = false;
			toValidateComponentIds.add(comp.getId());
		}
	}
	
	/**
	 * Get the list of components to validate
	 * return null if full-require flag is enabled or main-panel is not present 
	 */
	public List<Component> getRequireValidationComponents() {
		if (validateAllComponents || mainPanel == null) {
			return null;
		}
		
		List<Component> mandatoryComponents = new ArrayList<Component>();
		for (String id : toValidateComponentIds) {
			mandatoryComponents.add(mainPanel.getComponent(id));
		}
		return mandatoryComponents;
	}
	
	/**
	 * Check if all components in main-panel are valid
	 * Throw ConstraintException if at least one component is not valid
	 */
	public void validate() throws ConstraintException {
		if (mainPanel == null) {
			return;
		}

		if (validateAllComponents) {
			mainPanel.validate();
		} else {
			for (String id : toValidateComponentIds) {
				Component comp = mainPanel.getComponent(id);
				if (comp != null) {
					comp.validate();
				}
			}
		}
	}

	@Override
	void fillCacheData(Map<String, Object> userData) {
	}

	@Override
	void setCacheData(Map<String, Object> userData) {
	}
}
