package com.tilab.wade.lock.client;

import jade.core.AID;
import jade.domain.FIPAException;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.lock.ontology.LockOntology;
import com.tilab.wade.lock.ontology.RefreshLock;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.ActionExecutor;

/**
 * Ready-made behaviour allowing to refresh a previously acquired lock
 */
public class RefreshLockBehaviour extends ActionExecutor<RefreshLock, Void> {
	private static final long serialVersionUID = -756263427885L;

	public RefreshLockBehaviour(RefreshLock action) {
		super(action, LockOntology.getInstance(), null);
	}
	
	public RefreshLockBehaviour(String lockId) {
		this(new RefreshLock(lockId));
	}
	
	@Override
	protected AID retrieveActor() throws FIPAException {
		return DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.LM_AGENT_TYPE, null));
	}
}
