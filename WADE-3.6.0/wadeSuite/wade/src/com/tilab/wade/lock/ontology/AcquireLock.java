package com.tilab.wade.lock.ontology;

import jade.content.AgentAction;
import jade.content.onto.annotations.Result;

@Result(type=String.class)
public class AcquireLock implements AgentAction {
	private static final long serialVersionUID = 1535926496927L;
	
	private String target;
	private String owner;
	private String details;
	private long timeout;
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public long getTimeout() {
		return timeout;
	}
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
