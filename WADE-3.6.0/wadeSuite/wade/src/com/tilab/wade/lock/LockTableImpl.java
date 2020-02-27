package com.tilab.wade.lock;

import jade.core.AID;
import jade.core.NotFoundException;
import jade.lang.acl.ACLMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.lock.ontology.AcquireLock;
import com.tilab.wade.utils.GUIDGenerator;

public class LockTableImpl implements LockTable {

	private long lockExpirationTimeout;
	
	private Map<String, LockInfo> locks = new HashMap<String, LockInfo>();
	private Map<String, String> idsByTarget = new HashMap<String, String>();
	private Map<String, String> idsByPendingRequestConvId = new HashMap<String, String>();
	
	public SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
	
	
	public LockTableImpl(long lockExpirationTimeout) {
		this.lockExpirationTimeout = lockExpirationTimeout;
	}
	
	/**
	 * Acquire a lock for the target indicated in the AcquireLock action.
	 * If such target is already locked and a timeout > 0 is specified in the AcquireLock action,
	 * a PendingRequest is stored identified by the request conversation-id 
	 * @param al The AcquireLock action describing the lock that must be acquired
	 * @return The newly generated lockId or null if the requested lock is already acquired
	 */
	public String acquireLock(AcquireLock action, ACLMessage request) {
		String target = action.getTarget();
		String currentLockId = idsByTarget.get(target);
		if (currentLockId == null) {
			// Target is free --> Lock it
			String lockId = target+"_"+GUIDGenerator.getGUID(); // Prefixing the lockId with the target name is just to facilitate log reading
			locks.put(lockId, new LockInfo(action, lockId, request.getSender()));
			idsByTarget.put(target, lockId);
			return lockId;
		}
		else {
			// Target already locked 
			if (action.getTimeout() > 0) {
				// Add a PendingRequest to the current lock
				LockInfo info = locks.get(currentLockId);
				info.addPendingRequest(action, request);
				// Be sure a conversationId is set
				String convId = request.getConversationId();
				if (convId == null) {
					convId = GUIDGenerator.getGUID();
					request.setConversationId(convId);
				}
				idsByPendingRequestConvId.put(convId, currentLockId);
			}
			return null;
		}
	}
	
	/**
	 * Release the indicated lock. If there are PendingRequests for this lock, 
	 * the first one is removed and acquires the lock. Such request is returned filled
	 * with the newly generated lockId 
	 * @param lockId The id of the lock to be released
	 * @return The PendingRequest (if any) that acquired the lock after this release
	 * @throws NotFoundException if the indicated lock is not found
	 */
	public PendingRequest releaseLock(String lockId) throws NotFoundException {
		LockInfo info = locks.remove(lockId);
		if (info != null) {
			idsByTarget.remove(info.getAction().getTarget());
			// Lock released. If there are PendingRequests, get the first one and let it acquire the lock
			PendingRequest pr = info.extractFirstPendingRequest();
			if (pr != null) {
				idsByPendingRequestConvId.remove(pr.getId());
				String newLockId = acquireLock(pr.getAction(), pr.getRequest());
				pr.setAcquiredLockId(newLockId);
				
				// Make remaining PendingRequests (if any) point to the new lockId
				for (PendingRequest remainigPr : info.getAllPendingRequests()) {
					idsByPendingRequestConvId.put(pr.getId(), newLockId);
				}
			}
			
			return pr;
		}
		else {
			// Lock not found
			throw new NotFoundException("Lock "+lockId+" not found");
		}
	}
	
	public void refreshLock(String lockId) throws NotFoundException {
		LockInfo info = locks.get(lockId);
		if (info != null) {
			info.refresh();
		}
		else {
			// Lock not found
			throw new NotFoundException("Lock "+lockId+" not found");
		}
	}
	
	public PendingRequest removePendingRequest(String convId) {
		String lockId = idsByPendingRequestConvId.remove(convId);
		LockInfo info = locks.get(lockId);
		if (info != null) {
			return info.removePendingRequest(convId);
		}
		else {
			return null;
		}
	}
	
	public Map<String, PendingRequest> releaseExpiredLocks(long time) {
		Map<String, PendingRequest> result = new HashMap<String, PendingRequest>();
		
		String[] allLockIds = locks.keySet().toArray(new String[0]);
		for (String lockId : allLockIds) {
			LockInfo info = locks.get(lockId);
			if (info.isExpired(time)) {
				try {
					PendingRequest pr = releaseLock(lockId);
					result.put(lockId, pr);
				}
				catch (NotFoundException nfe) {
					// Should never happen
					nfe.printStackTrace();
				}
			}
		}
		return result;
	}
	
	public int size() {
		return locks.size();
	}
	
	public String dump(String prefix) {
		if (prefix == null) {
			prefix = "";
		}
		StringBuffer sb = new StringBuffer(prefix+"ACTIVE LOCKS:\n");
		if (!locks.isEmpty()) {
			for (String id : locks.keySet()) {
				LockInfo info = locks.get(id);
				sb.append(info.dump(prefix));
			}
		}
		else {
			sb.append(prefix+"  NONE\n");
		}
		return sb.toString();
	}
	
	
	/**
	 * Inner class LockInfo
	 */
	private class LockInfo {
		private AcquireLock action;
		private String lockId;
		private AID ownerAgent;
		private long creationTime;
		private long expiryTime;
		private int refreshCnt;
		private List<PendingRequest> pendingRequests = new ArrayList<PendingRequest>();
		
		public LockInfo(AcquireLock action, String lockId, AID ownerAgent) {
			this.action = action;
			this.lockId = lockId;
			this.ownerAgent = ownerAgent;
			creationTime = System.currentTimeMillis();
			if (lockExpirationTimeout > 0) {
				expiryTime = creationTime + lockExpirationTimeout;
			}
			else {
				expiryTime = -1;
			}
			refreshCnt = 0;
		}
		
		public AcquireLock getAction() {
			return action;
		}
		
		public void addPendingRequest(AcquireLock action, ACLMessage request) {
			pendingRequests.add(new PendingRequest(action, request));
		}
		
		public PendingRequest extractFirstPendingRequest() {
			if (!pendingRequests.isEmpty()) {
				return pendingRequests.remove(0);
			}
			else {
				return null;
			}
		}
		
		public PendingRequest removePendingRequest(String convId) {
			Iterator<PendingRequest> it = pendingRequests.iterator();
			while (it.hasNext()) {
				PendingRequest pr = it.next();
				if (convId.equals(pr.getId())) {
					it.remove();
					return pr;
				}
			}
			return null;
		}
		
		public List<PendingRequest> getAllPendingRequests() {
			return pendingRequests;
		}
		
		public void refresh() {
			long now = System.currentTimeMillis();
			if (lockExpirationTimeout > 0) {
				expiryTime = now + lockExpirationTimeout;
			}
			else {
				expiryTime = -1;
			}
			refreshCnt++;
		}
		
		public boolean isExpired(long time) {
			return expiryTime > 0 && time > expiryTime;
		}
		
		public String dump(String prefix) {
			StringBuffer sb = new StringBuffer();
			sb.append(prefix).append(action.getTarget()+": ID="+lockId+"\n");
			if (action.getOwner() != null) {
				sb.append(prefix+"- owner="+action.getOwner()+"\n");
			}
			if (action.getDetails() != null) {
				sb.append(prefix+"- details="+action.getDetails()+"\n");
			}
			sb.append(prefix+"- creation-time="+timeFormatter.format(new Date(creationTime))+"\n");
			if (expiryTime > 0) {
				sb.append(prefix+"- expiry-time="+timeFormatter.format(new Date(expiryTime))+"\n");
				sb.append(prefix+"- refresh-cnt="+refreshCnt+"\n");
			}
			
			sb.append(prefix+"- Pending-requests:\n");
			if (!pendingRequests.isEmpty()) {
				for (PendingRequest pr : pendingRequests) {
					sb.append(prefix+"  - "+pr.getId()+": owner="+pr.getAction().getOwner());
					sb.append(", enqueuingTime="+timeFormatter.format(new Date(pr.getEnqueuingTime())));
					sb.append(", deadline="+timeFormatter.format(new Date(pr.getDeadline()))+"\n");
				}
			}
			else {
				sb.append(prefix+"  NONE\n");
			}
			return sb.toString();
		}
	}  // END of inner class LockInfo
	
	
	public static void main(String[] args) {
		LockTable lt = new LockTableImpl(-1);
		
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setConversationId("conv1");
		AcquireLock al = new AcquireLock();
		al.setOwner("Pippo");
		al.setTarget("Target1");
		String lockId = lt.acquireLock(al, request);
		if (lockId != null) {
			System.out.println("Target1 acquired by Pippo with lock "+lockId);
		}
		
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setConversationId("conv2");
		al = new AcquireLock();
		al.setOwner("Pluto");
		al.setTarget("Target2");
		lockId = lt.acquireLock(al, request);
		if (lockId != null) {
			System.out.println("Target2 acquired by Pluto with lock "+lockId);
		}
		
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setConversationId("conv3");
		al = new AcquireLock();
		al.setOwner("Minni");
		al.setTarget("Target2");
		al.setTimeout(10000);
		lockId = lt.acquireLock(al, request);
		if (lockId != null) {
			System.out.println("Target2 acquired by Minni with lock "+lockId);
		}
		
		request = new ACLMessage(ACLMessage.REQUEST);
		request.setConversationId("conv4");
		al = new AcquireLock();
		al.setOwner("Paperino");
		al.setTarget("Target2");
		al.setTimeout(10000);
		lockId = lt.acquireLock(al, request);
		if (lockId != null) {
			System.out.println("Target2 acquired by Paperino with lock "+lockId);
		}
		
		System.out.println(lt.dump(null));
	}
}
