package com.tilab.wade.lock;

import jade.core.NotFoundException;
import jade.core.behaviours.OntologyServer;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.HashMap;
import java.util.Map;

import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.lock.ontology.AcquireLock;
import com.tilab.wade.lock.ontology.LockOntology;
import com.tilab.wade.lock.ontology.RefreshLock;
import com.tilab.wade.lock.ontology.ReleaseLock;
import com.tilab.wade.utils.AgentUtils;

public class LockManagerAgent extends WadeAgentImpl {
	private static final long serialVersionUID = 352443242436799L;

	private LockTable locks;
	private Map<String, WakerBehaviour> watchDogs = new HashMap<String, WakerBehaviour>();
	
	
	/**
	 * This method is responsible to create and initialize the LockTable used by this agent to 
	 * keep locks. Subclasses could redefine it to provide an ad hoc implementation of the LockTable
	 * interface.
	 * @param restart A boolean indication stating whether the initialization is done within
	 * the scope of a normal startup (restart = false) or a restart (restart = true).
	 * @return A concrete implementation of the LockTable interface
	 */
	protected LockTable initLockTable(boolean restart) {
		return new LockTableImpl(-1);
	}
	
	
	@Override
	public void agentSpecificSetup() throws AgentInitializationException {
		locks = initLockTable(this.getRestarted());
		
		addBehaviour(new OntologyServer(this, LockOntology.getInstance(), ACLMessage.REQUEST, this));
		addBehaviour(new LockExpirationController(getLongArgument("lockExpirationCheckPeriod", 60000)));
	}
	
	@AttributeGetter
	public int getLocksCnt() {
		return locks.size();
	}
	
	@AttributeGetter
	public int getPendingRequestsCnt() {
		return watchDogs.size();
	}
	
	
	//////////////////////////////////////////////////////
	// LockOntology actions serving methods
	//////////////////////////////////////////////////////
	public void serveAcquireLockRequest(AcquireLock al, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Serving AcquireLock REQUEST from agent "+request.getSender().getLocalName()+": target="+al.getTarget()+", owner="+al.getOwner());
		
		String lockId = locks.acquireLock(al, request);
		if (lockId != null) {
			// Lock acquired 
			myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Lock for target "+al.getTarget()+" acquired: lock-id="+lockId);
			AgentUtils.sendResult(this, al, request, lockId);
		}
		else {
			// Target already locked
			if (al.getTimeout() > 0) {
				// AcquireLock request enqueued --> Start a watch-dog
				myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - AcquireLock REQUEST for target "+al.getTarget()+" enqueued.");
				startWatchDog(request.getConversationId(), al.getTimeout());
			}
			else {
				// Lock not acquired --> REFUSE
				myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Lock for target "+al.getTarget()+" NOT acquired");
				AgentUtils.sendNegativeResponse(this, request, ACLMessage.REFUSE, null);
			}
		}
	}
	
	public void serveReleaseLockRequest(ReleaseLock rl, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Serving ReleaseLock REQUEST from agent "+request.getSender().getLocalName()+": lock-id="+rl.getLockId());

		try {
			PendingRequest pr = locks.releaseLock(rl.getLockId());
			myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Lock "+rl.getLockId()+" released");
			AgentUtils.sendDone(this, rl, request);
			if (pr != null) {
				// There were pending requests for this target --> A new lock was acquired
				handlePendingRequestServed(pr);
			}
		}
		catch (NotFoundException nfe) {
			// Lock not found. Just print a WARNING, but reply OK to the requester as the desired effect is "achieved"
			myLogger.log(Logger.WARNING, "Agent "+getLocalName()+" - Lock "+rl.getLockId()+" not found");
			AgentUtils.sendDone(this, rl, request);
		}
	}
	
	public void serveRefreshLockRequest(RefreshLock rl, ACLMessage request) {
		myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Serving RefreshLock REQUEST from agent "+request.getSender().getLocalName()+": lock-id="+rl.getLockId());

		try {
			locks.refreshLock(rl.getLockId());
			myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Lock "+rl.getLockId()+" refreshed");
			AgentUtils.sendDone(this, rl, request);
		}
		catch (NotFoundException nfe) {
			// Lock not found. Send back a FAILURE
			myLogger.log(Logger.WARNING, "Agent "+getLocalName()+" - Lock "+rl.getLockId()+" not found");
			AgentUtils.sendFailure(this, request, "Lock not found");
		}
	}
	
	
	//////////////////////////////////////////////////////
	// Utility methods
	//////////////////////////////////////////////////////
	private void handlePendingRequestServed(PendingRequest pr) {
		myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Lock for target "+pr.getAction().getTarget()+" now acquired by owner "+pr.getAction().getOwner()+": new lock-id="+pr.getAcquiredLockId());
		stopWatchDog(pr.getId());
		AgentUtils.sendResult(this, pr.getAction(), pr.getRequest(), pr.getAcquiredLockId());
	}
	
	private void startWatchDog(final String pendingRequestId, long timeout) {
		WakerBehaviour watchDog = new WakerBehaviour(this, timeout) {
			@Override
			public void onWake() {
				watchDogs.remove(pendingRequestId);
				PendingRequest pr = locks.removePendingRequest(pendingRequestId);
				if (pr != null) {
					myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Enqueued AcquireLock REQUEST "+pendingRequestId+" expired: target="+pr.getAction().getTarget()+", owner="+pr.getAction().getOwner());
					// Send back a REFUSE reply to the requester
					AgentUtils.sendNegativeResponse(myAgent, pr.getRequest(), ACLMessage.REFUSE, null);
				}
				else {
					// A watch-dog expired for an unknown request (should never happen)
					myLogger.log(Logger.WARNING, "Agent "+getLocalName()+" - Unknown pending AcquireLock REQUEST "+pendingRequestId+" expired.");
				}
			}
		};
		watchDog.setBehaviourName(pendingRequestId+"_WatchDog");
		addBehaviour(watchDog);
		watchDogs.put(pendingRequestId, watchDog);
	}
	
	private void stopWatchDog(String pendingRequestId) {
		WakerBehaviour watchDog = watchDogs.remove(pendingRequestId);
		if (watchDog != null) {
			removeBehaviour(watchDog);
		}
	}
	
	
	/**
	 * Inner class LockExpirationController
	 * This is a TickerBehaviour periodically checking expired locks
	 */
	private class LockExpirationController extends TickerBehaviour {

		public LockExpirationController(long period) {
			super(LockManagerAgent.this, period);
		}
		
		@Override
		public void onTick() {
			myLogger.log(Logger.FINE, "Agent "+myAgent.getLocalName()+" - Checking for expired locks...");
			Map<String, PendingRequest> releasedLocks = locks.releaseExpiredLocks(System.currentTimeMillis());
			for (String lockId : releasedLocks.keySet()) {
				myLogger.log(Logger.INFO, "Agent "+myAgent.getLocalName()+" - Lock "+lockId+" expired");
				PendingRequest pr = releasedLocks.get(lockId);
				if (pr != null) {
					handlePendingRequestServed(pr);
				}
			}
		}
	} // END of inner class LockExpirationController
	
	
	public String dump() {
		return locks.dump(null);
	}
}
