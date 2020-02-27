package com.tilab.wade.lock;

import com.tilab.wade.lock.ontology.AcquireLock;

import jade.lang.acl.ACLMessage;

public class PendingRequest {
	private AcquireLock action;
	private ACLMessage request;
	private String acquiredLockId;
	private long enqueuingTime;
	private long deadline;
	
	public PendingRequest(AcquireLock action, ACLMessage request) {
		this.action = action;
		this.request = request;
		enqueuingTime = System.currentTimeMillis();
		deadline = enqueuingTime + action.getTimeout();
	}
	
	public String getId() {
		return request.getConversationId();
	}
	
	public AcquireLock getAction() {
		return action;
	}
	
	public ACLMessage getRequest() {
		return request;
	}
	
	public long getEnqueuingTime() {
		return enqueuingTime;
	}
	
	public long getDeadline() {
		return deadline;
	}
	
	public String getAcquiredLockId() {
		return acquiredLockId;
	}
	
	public void setAcquiredLockId(String lockId) {
		acquiredLockId = lockId;
	}
}
