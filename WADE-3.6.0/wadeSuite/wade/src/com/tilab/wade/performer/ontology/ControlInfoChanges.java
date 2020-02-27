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
package com.tilab.wade.performer.ontology;

//#MIDP_EXCLUDE_FILE

import jade.content.Concept;
import jade.core.AID;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

/**
   This class is used to update the information related to the type of
   control applied to a workflow under execution
 */
public class ControlInfoChanges implements Concept {
	
	private String type;
	private Boolean synch;
	private Integer verbosityLevel;
	private Boolean selfConfig;
	private List controllers;
	private List controllersToAdd;
	private List controllersToRemove;
	
	public ControlInfoChanges() {
	}

	public ControlInfoChanges(String type) {
		this.type = type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setSynch(Boolean synch) {
		this.synch = synch;
	}
	
	public Boolean getSynch() {
		return synch;
	}
	
	public void setVerbosityLevel(Integer verbosityLevel) {
		this.verbosityLevel = verbosityLevel;
	}
	
	public Integer getVerbosityLevel() {
		return verbosityLevel;
	}

	public void setSelfConfig(Boolean selfConfig) {
		this.selfConfig = selfConfig;
	}
	
	public Boolean getSelfConfig() {
		return selfConfig;
	}
	
	public void setControllers(List controllers) {
		this.controllers = controllers;
	}
	
	public List getControllers() {
		return controllers;
	}
	
	public void addController(AID controller) {
		if (controller == null) {
			controllers = new ArrayList(1);
		}
		controllers.add(controller);
	}
	
	public void setControllersToAdd(List controllersToAdd) {
		this.controllersToAdd = controllersToAdd;
	}
	
	public List getControllersToAdd() {
		return controllersToAdd;
	}
	
	public void addControllerToAdd(AID controller) {
		if (controllersToAdd == null) {
			controllersToAdd = new ArrayList(1);
		}
		controllersToAdd.add(controller);
	}
	
	public void setControllersToRemove(List controllersToRemove) {
		this.controllersToRemove = controllersToRemove;
	}
	
	public List getControllersToRemove() {
		return controllersToRemove;
	}
	
	public void addControllerToremove(AID controller) {
		if (controllersToRemove == null) {
			controllersToRemove = new ArrayList(1);
		}
		controllersToRemove.add(controller);
	}
}
