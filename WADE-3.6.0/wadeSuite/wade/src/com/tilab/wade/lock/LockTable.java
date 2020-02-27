package com.tilab.wade.lock;

import java.util.Map;

import jade.core.NotFoundException;
import jade.lang.acl.ACLMessage;

import com.tilab.wade.lock.ontology.AcquireLock;

public interface LockTable {

	/**
	 * Acquire a lock for the target indicated in the AcquireLock action.
	 * If such target is already locked and a timeout > 0 is specified in the AcquireLock action,
	 * a PendingRequest is stored identified by the request conversation-id 
	 * @param al The AcquireLock action describing the lock that must be acquired
	 * @return The newly generated lockId or null if the requested lock is already acquired
	 */
	String acquireLock(AcquireLock action, ACLMessage request);
	
	/**
	 * Release the indicated lock. If there are PendingRequests for this lock, 
	 * the first one is removed and acquires the lock. Such request is returned filled
	 * with the newly generated lockId 
	 * @param lockId The id of the lock to be released
	 * @return The PendingRequest (if any) that acquired the lock after this release
	 * @throws NotFoundException if the indicated lock is not found
	 */
	PendingRequest releaseLock(String lockId) throws NotFoundException;
	
	void refreshLock(String lockId) throws NotFoundException;
	PendingRequest removePendingRequest(String convId);
	
	/**
	 * Release all locks expired at time "time".
	 * For each released lock, if there were pending requests, the first one is served and acquires
	 * a new lock on the indicated target.  
	 * @return A Map associating each released lock id with the PendingRequest (if any) that was served
	 * following such release
	 */
	Map<String, PendingRequest> releaseExpiredLocks(long time);
	
	/**
	 * Return the number of currently acquired locks
	 * @return The number of currently acquired locks
	 */
	int size();
	
	String dump(String prefix);
}
