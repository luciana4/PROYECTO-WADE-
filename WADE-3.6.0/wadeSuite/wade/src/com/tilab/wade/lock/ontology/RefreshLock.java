package com.tilab.wade.lock.ontology;

import jade.content.AgentAction;

public class RefreshLock implements AgentAction {
	private static final long serialVersionUID = 542413389876L;
	
	private String lockId;

	public RefreshLock() {
	}
	
	public RefreshLock(String lockId) {
		this.lockId = lockId;
	}
	
	public String getLockId() {
		return lockId;
	}

	public void setLockId(String lockId) {
		this.lockId = lockId;
	}
}
