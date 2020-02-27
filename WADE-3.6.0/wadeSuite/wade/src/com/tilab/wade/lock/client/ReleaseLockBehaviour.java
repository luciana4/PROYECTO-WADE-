package com.tilab.wade.lock.client;

import jade.core.AID;
import jade.domain.FIPAException;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.lock.ontology.LockOntology;
import com.tilab.wade.lock.ontology.ReleaseLock;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.ActionExecutor;

/**
 * Ready-made behaviour allowing to release a previously acquired lock
 */
public class ReleaseLockBehaviour extends ActionExecutor<ReleaseLock, Void> {
	private static final long serialVersionUID = 463134567234L;

	public ReleaseLockBehaviour(ReleaseLock action) {
		super(action, LockOntology.getInstance(), null);
	}
	
	public ReleaseLockBehaviour(String lockId) {
		this(new ReleaseLock(lockId));
	}
	
	@Override
	protected AID retrieveActor() throws FIPAException {
		return DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.LM_AGENT_TYPE, null));
	}
}
