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

import com.tilab.wade.event.EventSource;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.utils.GUIDGenerator;

import jade.content.Concept;

public class EventDescriptor implements Concept {

	private static final long serialVersionUID = -7691851262213260285L;
	
	private String id;
	private GenericEvent event;
	private long time;
	private EventSource source;
	

	public EventDescriptor() {
	}

	public EventDescriptor(String id, GenericEvent event, long time, EventSource source) {
		this.id = id;
		this.event = event;
		this.time = time;
		this.source = source;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public GenericEvent getEvent() {
		return event;
	}
	
	public void setEvent(GenericEvent event) {
		this.event = event;
	}
	
	public long getTime() {
		return time;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
	
	public EventSource getSource() {
		return source;
	}
	
	public void setSource(EventSource source) {
		this.source = source;
	}
	
	public static EventDescriptor create(Occurred o) {
		return new EventDescriptor(GUIDGenerator.getGUID(), (GenericEvent)o.getEvent(), o.getTime(), o.getSource());
	}
	
	@Override
	public String toString() {
		return "EventDescriptor [id=" + id + ", event=" + event + ", time=" + time + ", source=" + source + "]";
	}
}
