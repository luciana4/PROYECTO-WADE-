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

import com.tilab.wade.performer.Constants;

/**
   This class embeds the information related to the type of
   control applied to a workflow under execution
 */
public class ControlInfo implements Concept {
	private String type;
	private boolean synch = false;
	private int verbosityLevel = Constants.DEFAULT_LEVEL;
	private List controllers;
	private Boolean selfConfig;
	
	public ControlInfo() {
	}
	
	public ControlInfo(String type, AID controller) {
		setType(type);
		setController(controller);
	}
	
	public ControlInfo(String type, AID controller, int level) {
		this(type, controller);
		setVerbosityLevel(level);
	}
	
	public ControlInfo(String type, AID controller, int level, Boolean b) {
		this(type, controller);
		setVerbosityLevel(level);
		setSelfConfig(b);
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setSynch(boolean synch) {
		this.synch = synch;
	}
	
	public boolean getSynch() {
		return synch;
	}
	
	/**
	 * @deprecated Use setSynch() instead
	 */
	public void setDebug(boolean debug) {
		setSynch(debug);
	}
	
	/**
	 * @deprecated Use getSynch() instead
	 */
	public boolean getDebug() {
		return getSynch();
	}
	
	public void setVerbosityLevel(int verbosityLevel) {
		this.verbosityLevel = verbosityLevel;
	}
	
	public int getVerbosityLevel() {
		return verbosityLevel;
	}
	
	/**
	 * @deprecated Use setVerbosityLevel() instead
	 */
	public void setLogLevel(int logLevel) {
		setVerbosityLevel(logLevel);
	}
	
	/**
	 * @deprecated Use getVerbosityLevel() instead
	 */
	public int getLogLevel() {
		return getVerbosityLevel();
	}
	
	public void setControllers(List controllers) {
		this.controllers = controllers;
	}
	
	public List getControllers() {
		return controllers;
	}
	
	public void setController(AID controller) {
		if (controller != null) {
			controllers = new ArrayList(1);
			controllers.add(controller);
		}
		else {
			controllers = null;
		}
	}
	
	public AID getController() {
		if (controllers != null && controllers.size() >= 1) {
			return (AID) controllers.get(0);
		}
		else {
			return null;
		}
	}
	
	public void setSelfConfig(Boolean selfConfig) {
		this.selfConfig = selfConfig;
	}
	
	public Boolean getSelfConfig() {
		return selfConfig;
	}
}
