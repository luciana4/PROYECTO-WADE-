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
package com.tilab.wade.proxy;

import jade.core.AID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.ontology.ControlInfo;

/**
 * Workflow events generation configuration
 */
public class EventGenerationConfiguration {

	private jade.util.leap.List controlInfos = new jade.util.leap.ArrayList();
	private Map<String, WorkflowEventListener> listeners = new HashMap<String, WorkflowEventListener>();
	
	public EventGenerationConfiguration() {
	}

	public void setGenerateEvents(String type, int verbosityLevel, WorkflowEventListener notificationListener) throws EngineProxyException {
		setGenerateEvents(type, verbosityLevel, notificationListener, null);
	}

	public void setGenerateEvents(String type, int verbosityLevel, WorkflowEventListener notificationListener, List<AID> controllers) throws EngineProxyException {
		if (listeners.containsKey(type)) {
			throw new EngineProxyException("Event of type "+type+" already managed");
		}
		
		ControlInfo ci = new ControlInfo();
		ci.setType(type);
		ci.setVerbosityLevel(verbosityLevel);
		if (controllers != null) {
			ci.setControllers(new jade.util.leap.ArrayList((ArrayList)controllers));
		}
		
		controlInfos.add(ci);
		if (notificationListener != null) {
			listeners.put(type, notificationListener);
		}
	}
	
	jade.util.leap.List getControlInfos() {
		return controlInfos;
	}
	
	Map<String, WorkflowEventListener> getListeners() {
		return listeners;
	}
	
	static EventGenerationConfiguration getDefault(WorkflowEventListener eventListener) {
		EventGenerationConfiguration eventCfg = new EventGenerationConfiguration();
		
		try {
			eventCfg.setGenerateEvents(Constants.FLOW_TYPE, Constants.ACTIVITY_LEVEL, eventListener);
			eventCfg.setGenerateEvents(Constants.TRACING_TYPE, Constants.INFO_LEVEL, eventListener);
		} catch (EngineProxyException e) {
			e.printStackTrace();
		}
		
		return eventCfg;
	}
}
