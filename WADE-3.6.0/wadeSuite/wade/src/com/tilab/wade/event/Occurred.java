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

//#MIDP_EXCLUDE_BEGIN

import jade.content.Predicate;

/**
 * This predicate indicates that an event has occurred somewhere at a given time
 * @author Giovanni Caire - TILAB
 */
public class Occurred implements Predicate {
	private long time;
	private Object event;
	private EventSource source;
	
	public Occurred() {
	}
	
	public Occurred(long time, Object event) {
		this.time = time;
		this.event = event;	
	}
	
	public long getTime() {
		return time;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
	
	public Object getEvent() {
		return event;
	}
	
	public void setEvent(Object event) {
		this.event = event;
	}
	
	public EventSource getSource() {
		return source;
	}
	
	public void setSource(EventSource source) {
		this.source = source;
	}
}
