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
package com.tilab.wade.esa.ontology;

import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.Match;
import com.tilab.wade.utils.GUIDGenerator;

import jade.content.Concept;
import jade.content.onto.annotations.SuppressSlot;
import jade.lang.acl.ACLMessage;

public class EventRegistrationDescriptor implements Concept {

	private static final long serialVersionUID = 8168131281771252204L;
	
	private String id;
	private String groupId;
	private String executionId;
	private EventTemplate eventTemplate;
	private boolean exclusive;
	private ACLMessage registrationMessage;
	

	public EventRegistrationDescriptor() {
	}

	public EventRegistrationDescriptor(String id, String groupId, String executionId, EventTemplate eventTemplate, boolean exclusive, ACLMessage registrationMessage) {
		this.id = id;
		this.groupId = groupId;
		this.executionId = executionId;
		this.eventTemplate = eventTemplate;
		this.exclusive = exclusive;
		this.registrationMessage = registrationMessage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public EventTemplate getEventTemplate() {
		return eventTemplate;
	}

	public void setEventTemplate(EventTemplate eventTemplate) {
		this.eventTemplate = eventTemplate;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	@SuppressSlot
	public ACLMessage getRegistrationMessage() {
		return registrationMessage;
	}

	public void setRegistrationMessage(ACLMessage registrationMessage) {
		this.registrationMessage = registrationMessage;
	}

	public static EventRegistrationDescriptor create(String groupId, Match match, ACLMessage registrationMessage) {
		return new EventRegistrationDescriptor(GUIDGenerator.getGUID(), groupId, match.getExecutionId(), match.getEventTemplate(), match.getExclusive(), registrationMessage);
	}

	@Override
	public String toString() {
		return "EventRegistrationDescriptor [id=" + id + ", executionId=" + executionId + ", eventTemplate=" + eventTemplate + ", exclusive="
				+ exclusive + "]";
	}
}
