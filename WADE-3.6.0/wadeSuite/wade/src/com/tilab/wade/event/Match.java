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
package com.tilab.wade.event;

import jade.content.Predicate;

public class Match implements Predicate {

	private static final long serialVersionUID = 7004570629231241555L;
	
	private EventTemplate eventTemplate;
	private String executionId;
	private boolean exclusive;
	private boolean futureEventsOnly;
	

	public Match() {
	}
	
	public Match(EventTemplate eventTemplate, String executionId, boolean exclusive, boolean futureEventsOnly) {
		this.eventTemplate = eventTemplate;
		this.executionId = executionId;
		this.exclusive = exclusive;
		this.futureEventsOnly = futureEventsOnly;
	}

	public EventTemplate getEventTemplate() {
		return eventTemplate;
	}

	public void setEventTemplate(EventTemplate eventTemplate) {
		this.eventTemplate = eventTemplate;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public boolean getExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public boolean getFutureEventsOnly() {
		return futureEventsOnly;
	}

	public void setFutureEventsOnly(boolean futureEventsOnly) {
		this.futureEventsOnly = futureEventsOnly;
	}

	@Override
	public String toString() {
		return "Match [eventTemplate=" + eventTemplate + ", executionId=" + executionId + ", exclusive=" + exclusive + ", futureEventsOnly="
				+ futureEventsOnly + "]";
	}
}
