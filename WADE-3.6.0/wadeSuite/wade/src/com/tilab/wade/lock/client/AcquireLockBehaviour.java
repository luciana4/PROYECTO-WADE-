package com.tilab.wade.lock.client;

import jade.core.AID;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.lock.ontology.AcquireLock;
import com.tilab.wade.lock.ontology.LockOntology;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.ActionExecutor;

/**
 * Ready-made behaviour allowing to acquire the lock on a given target
 */
public class AcquireLockBehaviour extends ActionExecutor<AcquireLock, String> {
	private static final long serialVersionUID = 9866742345688L;

	public AcquireLockBehaviour(AcquireLock action) {
		super(action, LockOntology.getInstance(), null);
	}
	
	public AcquireLockBehaviour(String target, String owner, long timeout) {
		this(buildAcquireLock(target, owner, null, timeout));
	}
	
	public AcquireLockBehaviour(String target, String owner, String details, long timeout) {
		this(buildAcquireLock(target, owner, details, timeout));
	}
	
	
	@Override
	protected ACLMessage createInitiation() {
		// If the protocol timeout is shorter than the action timeout, enlarge it
		if (action.getTimeout() > timeout) {
			timeout = action.getTimeout() + 5000; // 5 sec tolerance
		}
		return super.createInitiation();
	}
	
	@Override
	protected AID retrieveActor() throws FIPAException {
		return DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.LM_AGENT_TYPE, null));
	}
	
	@Override
	public void handleRefuse(ACLMessage refuse) {
		// Just do nothing
		// When the requested lock cannot be acquired within the specified timeout the 
		// LockManagerAgent replies with REUSE. This is not an error condition. The client 
		// will distinguish this situation since it gets a null lockId
	}
	
	public static AcquireLock buildAcquireLock(String target, String owner, String details, long timeout) {
		AcquireLock al = new AcquireLock();
		al.setTarget(target);
		al.setOwner(owner);
		al.setDetails(details);
		al.setTimeout(timeout);
		return al;
	}
}
