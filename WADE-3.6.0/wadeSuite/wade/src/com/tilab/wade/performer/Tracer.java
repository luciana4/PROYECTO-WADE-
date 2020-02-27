package com.tilab.wade.performer;

import jade.core.Agent;

import java.io.Serializable;

import com.tilab.wade.performer.event.EventEmitter;
import com.tilab.wade.performer.event.TraceEvent;

public class Tracer implements Serializable {
	private String sessionId;
	private EventEmitter eventEmitter;
	private String defaultAspect = null;
	
	public Tracer(String id, String sessionId, Agent a) {
		this.sessionId = sessionId;
		eventEmitter = new EventEmitter(a, id);
		// FIXME: How do we get the AuditManager?
		eventEmitter.setControlInfo(Constants.TRACING_TYPE, null);
	}
	
	
	/**
	 * This constructor allows NON-JADE components to trace events 
	 * @param id
	 * @param sessionId
	 * @param host
	 * @param port
	 */
	public Tracer(String id, String sessionId, String host, int port) {
		// FIXME: To be implemented
	}
	
	Tracer(String sessionId, EventEmitter eventEmitter) {
		this.sessionId = sessionId;
		this.eventEmitter = eventEmitter;
	}

	public void setDefaultAspect(String aspect) {
		defaultAspect = aspect;
	}
	
	public void trace(String msg) {
		trace(Constants.INFO_LEVEL, msg, defaultAspect);
	}
	
	public void trace(String msg, String aspect) {
		trace(Constants.INFO_LEVEL, msg, aspect);
	}
	
	public void trace(int level, String msg) {
		trace(level, msg, defaultAspect);
	}
	
	public void trace(int level, String msg, String aspect) {
		TraceEvent ev = new TraceEvent(sessionId, msg, level, aspect);
		if (eventEmitter != null) {
			eventEmitter.fireEvent(Constants.TRACING_TYPE, ev, level);		
		}
		else {
			// FIXME: To be implemented
		}
	}
}
