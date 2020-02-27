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

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class Interaction implements Serializable {

	private static final long serialVersionUID = 3655342160816769261L;

	private String id;
	private String title;
	private boolean last;
	private boolean backEnabled;
	
	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "actions")
	@XmlElement(name = "action")
	//#ANDROID_EXCLUDE_END
	private List<Action> actions = new ArrayList<Action>();
	private Panel mainPanel;

	public Interaction() {
		this(null, null);
	}

	public Interaction(String title) {
		this(null, title);
	}

	public Interaction(String id, String title) {
		this.id = id;
		this.title = title;
		this.last = false;
		this.setBackEnabled(false);
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public boolean isLast() {
		return last;
	}

	/**
	 * Used to mark the last workflow interaction
	 * The default value is false
	 */
	public void setLast(boolean last) {
		this.last = last;
	}

	public void setBackEnabled(boolean backEnabled) {
		this.backEnabled = backEnabled;
	}

	public boolean isBackEnabled() {
		return backEnabled;
	}

	public void addAction(Action action) {
		action.mainPanel = mainPanel;
		actions.add(action);
	}

	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	public List<Action> getActions() {
		return actions;
	}

	public Action getAction(String id) {
		for (Action action : actions) {
			String actId = action.getId();
			if (actId != null && actId.equals(id)) {
				return action;
			}
		}
		return null;
	}
	
	public Action getSelectedAction() {
		for (Action action : actions) {
			if (action.isSelected()) {
				return action;
			}
		}
		return null;
	}

	public Panel getMainPanel() {
		return mainPanel;
	}

	public void setMainPanel(Panel mainPanel) {
		this.mainPanel = mainPanel;
		for (Action action : actions) {
			action.mainPanel = mainPanel;
		}
	}
	
	public Component getComponent(String id) {
		if (mainPanel != null) {
			return mainPanel.getComponent(id);
		}
		return null;
	}
	
	/**
	 * Finalize the component.
	 * Is the last method called before send back the interaction
	 * Eg. remove optional node not filled in DataElement   
	 */
	public void stamp() {
		if (mainPanel != null) {
			mainPanel.stamp();
		}
	}

	public Map<String, Object> getCacheData() {
		Map<String, Object> cacheDataMap = new HashMap<String, Object>();
		if (mainPanel != null) {
			mainPanel.fillCacheData(cacheDataMap);
		}
		return cacheDataMap;
	}

	public void setCacheData(Map<String, Object> userDataMap) {
		if (userDataMap != null && mainPanel != null) {
			mainPanel.setCacheData(userDataMap);
		}
	}
}
