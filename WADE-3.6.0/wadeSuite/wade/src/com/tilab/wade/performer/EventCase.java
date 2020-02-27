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
package com.tilab.wade.performer;

import jade.core.Agent;

import java.io.Serializable;

import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.Match;

public class EventCase implements Serializable {
	private static final long serialVersionUID = 9879834792834L;
	
	private String id;
	private EventTemplateBB templateBB;
	private GenericEventBB eventBB;
	private boolean exclusive = false;
	private boolean futureEventsOnly = false;
	
	private HierarchyNode activity;
	
	void setId(String id) {
		this.id = id;
	}
	
	void setActivity(HierarchyNode activity) {
		this.activity = activity;
		templateBB = new EventTemplateBB(new EventTemplate(), activity);
		templateBB.getEventTemplate().setTag(id);
	}
	
	public void setEventType(String eventType) {
		templateBB.getEventTemplate().setEventType(eventType);
	}
	
	public void setEventIdentificationExpression(String eventIdentificationExpression) {
		templateBB.getEventTemplate().setEventIdentificationExpression(eventIdentificationExpression);
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public void setFutureEventsOnly(boolean futureEventsOnly) {
		this.futureEventsOnly = futureEventsOnly;
	}
	
	// This is a placeholder for subclasses to insert the code necessary to initialize the
	// event type starting.
	void init(Agent a) throws Exception {
		// Just do nothing
	}
	
	/**
	 * Return the Match predicate corresponding to this case
	 */
	Match getMatch() {
		return new Match(templateBB.getEventTemplate(), activity.getOwner().getExecutionId(), exclusive, futureEventsOnly);
	}
	
	EventTemplate getEventTemplate() {
		return templateBB.getEventTemplate();
	}
	
	GenericEvent getEvent() {
		return eventBB != null ? eventBB.getGenericEvent() : null;
	}
	
	void setEvent(GenericEvent event) {
		eventBB = new GenericEventBB(event, activity);
	}
	
	BuildingBlock getBuildingBlock(String type) {
		if (EventTemplateBB.ID.equals(type)) {
			return templateBB;
		}
		else if (GenericEventBB.ID.equals(type)) {
			return eventBB;
		}		
		return null;
	}
	
	void reset() {
		templateBB.reset();
		eventBB = null;
	}
}
