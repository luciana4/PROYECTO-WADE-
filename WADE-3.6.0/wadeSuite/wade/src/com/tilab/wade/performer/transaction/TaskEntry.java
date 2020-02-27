package com.tilab.wade.performer.transaction;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;

public class TaskEntry extends TransactionEntry {
	private Behaviour myBehaviour;
	private DataStore transactionalFieldDataStore;
	
	public TaskEntry(String id, Behaviour b, DataStore ds) {
		setId(id);
		myBehaviour = b;
		transactionalFieldDataStore = ds;
	}
	
	public boolean isSuccessful() {
		return true;
	}
	
	public void commit() throws Throwable {
		// FIXME: To be implemented
	}
	
	public void rollback() throws Throwable {
		// FIXME: To be implemented
	}
}
