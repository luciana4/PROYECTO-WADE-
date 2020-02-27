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

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;

public class GetEvents implements AgentAction {

	private static final long serialVersionUID = -2026258856121050328L;
	
	private String eventType;
	private boolean convertToAbs;

	
	public GetEvents() {
		this(null, false);
	}

	public GetEvents(String eventType) {
		this(eventType, false);
	}

	public GetEvents(boolean convertToAbs) {
		this(null, convertToAbs);
	}
	
	public GetEvents(String eventType, boolean convertToAbs) {
		this.eventType = eventType;
		this.convertToAbs = convertToAbs;
	}
	
	@Slot(mandatory=false)
	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	@Slot(mandatory=false)
	public boolean isConvertToAbs() {
		return convertToAbs;
	}

	public void setConvertToAbs(boolean convertToAbs) {
		this.convertToAbs = convertToAbs;
	}

	@Override
	public String toString() {
		return "GetEvents [eventType=" + eventType + ", convertToAbs=" + convertToAbs + "]";
	}
}
