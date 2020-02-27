package com.tilab.wade.performer;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class AsynchSubflowCollector extends SimpleBehaviour implements TerminationNotificationReceiver.TerminationListener {
	private static final int WAITING = 2;
	
	private WorkflowBehaviour owner;
	private List<TerminationNotificationReceiver> pendingReceivers;
	private int status;
	
	protected Logger myLogger = Logger.getMyLogger(AsynchSubflowCollector.class.getName());
	
	AsynchSubflowCollector(WorkflowBehaviour owner) {
		super();
		this.owner = owner;
	}
	
	public void onStart() {
		status = Constants.SUCCESS;
		pendingReceivers = new ArrayList<TerminationNotificationReceiver>();
		jade.util.leap.Collection activities = owner.getChildren();
		Iterator it = activities.iterator();
		while (it.hasNext()) {
			Behaviour activity = (Behaviour) it.next();
			if (activity instanceof SubflowDelegationBehaviour) {
				Iterator<TerminationNotificationReceiver> rr = ((SubflowDelegationBehaviour) activity).getAllAsynchronousDelegations();
				while (rr.hasNext()) {
					pendingReceivers.add(rr.next());
				}
			}
		}
		
		if (pendingReceivers.size() > 0) {
			status  = WAITING;
			myLogger.log(Logger.INFO, ">>>>>>>> "+owner.getClass().getName()+"-ASC: "+pendingReceivers.size()+" asynchronous subflows still running...");
			TerminationNotificationReceiver[] tt = (TerminationNotificationReceiver[]) pendingReceivers.toArray(new TerminationNotificationReceiver[0]);
			for (int i = 0;i < tt.length; ++i) {
				tt[i].registerListener(this);
			}
		}
	}
	
	public synchronized void action() {
		if (!pendingReceivers.isEmpty()) {
			myLogger.log(Logger.INFO, ">>>>>>>> "+owner.getClass().getName()+"-ASC: suspending workflow");
			owner.suspend();
		}
	}

	public boolean done() {
		return status != WAITING;
	}
	
	public int onEnd() {
		return status;
	}
	
	// This is executed by the Agent Thread --> Can never be executed two times in parallel
	public void handleTermination(TerminationNotificationReceiver ter) {
		synchronized (this) {
			if (pendingReceivers.remove(ter)) {
				
				try {
					ter.getResult();
					if (pendingReceivers.isEmpty()) {
						status = Constants.SUCCESS;
						myLogger.log(Logger.INFO, ">>>>>>>> "+owner.getClass().getName()+"-ASC: all pending subflow-s completed successfully. Resuming workflow");
						owner.resume();
					}
				}
				catch (Exception e) {
					status = Constants.FAILURE;
					owner.handleException(e);
					myLogger.log(Logger.INFO, ">>>>>>>> "+owner.getClass().getName()+"-ASC: subflow "+ter.getDelegationId()+" failed. Resuming workflow");
					owner.resume();
				}
				finally{
					ter.setJoined();
				}
			}
		}
		
		if (status != WAITING) {
			clean();
		}
	}
	
	private void clean() {
		TerminationNotificationReceiver[] pp = null;
		pp = (TerminationNotificationReceiver[]) pendingReceivers.toArray(new TerminationNotificationReceiver[0]);
		pendingReceivers.clear();
		for (int i = 0; i < pp.length; ++i) {
			pp[i].deregisterListener();
			pp[i].setJoined();
		}
	}

}
