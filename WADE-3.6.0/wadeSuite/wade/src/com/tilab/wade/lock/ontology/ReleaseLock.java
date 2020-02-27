package com.tilab.wade.lock.ontology;

import jade.content.AgentAction;

public class ReleaseLock implements AgentAction {
	private static final long serialVersionUID = 875543123445678L;
	
	private String lockId;

	public ReleaseLock() {
	}
	
	public ReleaseLock(String lockId) {
		this.lockId = lockId;
	}
	
	public String getLockId() {
		return lockId;
	}

	public void setLockId(String lockId) {
		this.lockId = lockId;
	}
}
