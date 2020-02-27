package com.tilab.wade.performer;

import jade.core.Agent.Interrupted;
import jade.util.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The behaviour implementing activities of type SUBFLOW-JOIN in a workflow. 
 */
public class SubflowJoinBehaviour extends ActivityBehaviour {
	public static final String BBINDEX_SEPARTOR = "#";
	
	public static final int OR = 0;
	public static final int AND = 1;
	
	public static final int ANY = 0;
	public static final int ALL = 1;
	
	private int mode = AND;

	private int expectedCnt, completedCnt;
	private boolean finished = false;
	private List<AsynchronousFlow> asynchFlows = new ArrayList<AsynchronousFlow>();
	
	private MethodInvocator invocator;
	private SubflowList asyncSubflowMap;
	
	protected Logger myLogger = Logger.getMyLogger(TerminationNotificationReceiver.class.getName());
	
	public SubflowJoinBehaviour(String name, WorkflowBehaviour owner) {
		super(name, owner);
		requireSave = true;
		
		asyncSubflowMap = new SubflowList();
		
		String methodName = EngineHelper.activityName2Method(getBehaviourName());
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, asyncSubflowMap, SubflowList.class);
	}
	
	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public void setTimeout(long timeout) {
	}
	
	public void addSubflowDelegationActivity(String activityName, int policy) {
		// We do not get the corresponding SubflowDelegationBehaviour here because it may not have been registered yet 
		AsynchronousFlow af = new AsynchronousFlow(activityName, policy, asynchFlows.size());
		asynchFlows.add(af);
	}
	
	public void onStart(){
		super.onStart();

		int validAsyncFlowsCnt = 0;
		for (AsynchronousFlow af : asynchFlows) {
			if (af.bind()){
				asyncSubflowMap.put(af.getName(),af.getPolicy());
				if (af.getPendingReceivers() > 0)
					validAsyncFlowsCnt++;
			}
		}
		expectedCnt = (mode == OR && validAsyncFlowsCnt != 0) ? 1 : validAsyncFlowsCnt;
		completedCnt = 0;
		
		// Once all asynchronous-flows have been bound, initialize them (from this point on we start receiving termination notifications)
		for (AsynchronousFlow af : asynchFlows) {
			af.init();
		}
	}
	
	public synchronized void action() {
		try {
			owner.enterInterruptableSection();
			if (completedCnt == expectedCnt) {
				// Manage output bindings
				for (Subflow subflow : asyncSubflowMap.getAll()) {
					owner.manageOutputBindings(subflow);
				}
				// All expected asynchronous flow have completed --> Invoke workflow callback method to store output parameters. Then terminate
				myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": invoking activity callback method");
				invocator.invoke();
				finished = true;
			}
			else if (lastException != null) {
				// One of the subflow failed. Just terminate
				finished = true;
			}
			else {
				// Pause the root executor
				myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": suspending workflow");
				owner.suspend();
			}
		}			
		catch (InterruptedException ie) {
		}
		catch (Interrupted i) {
		}
		catch (ThreadDeath td) {
		}
		catch (Throwable t) {
			handleException(t);
			if (!EngineHelper.logIfUncaughtOnly(this, t)) {
				t.printStackTrace();
			}
		}
		finally {
			owner.exitInterruptableSection(this);
		}
	}
	
	public boolean done() {
		return finished || isInterrupted();
	}
	
	public int onEnd() {
		// Clean all asynchronous flows. Note that some of them have already been cleaned. Cleaning them two time is not a problem 
		for (AsynchronousFlow af : asynchFlows) {
			af.clean();
		}
		myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": terminated");
		
		return super.onEnd();
	}
	
	public void reset() {
		finished = false;
		asyncSubflowMap.reset();
		super.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		// SubflowJoinActivities handle a map of lists of BuildingBlocks -->
		// The BuildingBlock ID has the form <Subflow-delegation-activity-name>#<index>
		BuildingBlock bb = null;
		int sepPos = id.indexOf(BBINDEX_SEPARTOR);
		if (sepPos > 0 && sepPos < (id.length()-1)) {
			String actName = id.substring(0, sepPos);
			String indexStr = id.substring(sepPos+1);
			
			try {
				int index = Integer.parseInt(indexStr);
				Object obj = asyncSubflowMap.get(actName);
				if (obj != null) {
					// policy == SubflowJoinBehaviour.ANY
					if (obj instanceof Subflow) {
						bb = (Subflow)obj;
					} 
					// policy == SubflowJoinBehaviour.ALL
					else {
						List<Subflow> subflows = (List <Subflow>)obj;
						try {
							bb = subflows.get(index);
						} catch(IndexOutOfBoundsException e) {
							// Wrong index
						}
					}
				}
			} catch(NumberFormatException e) {
				// Wrong index
			}
		}
		return bb;
	}
	
	// This is executed by the Agent Thread --> Synchronized to ensure mutual exclusion with action() 
	private synchronized boolean handleAsynchronousFlowSuccess(AsynchronousFlow af, List<Subflow> flowResults) {
		if (completedCnt < expectedCnt && lastException == null) {
			completedCnt++;
			
			// Add at subflow the link to specific activity 
			for (Subflow subflow : flowResults) {
				subflow.setActivity(this);
			}
			
			asyncSubflowMap.put(af.getName(), af.getPolicy(), flowResults);
			if (completedCnt == expectedCnt) {
				myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": all expected AsynchFlow-s completed successfully. Resuming workflow");
				owner.resume();
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	// This is executed by the Agent Thread --> Synchronized to ensure mutual exclusion with action() 
	private synchronized boolean handleAsynchronousFlowFailure(AsynchronousFlow af, Exception exception) {
		if (completedCnt < expectedCnt && lastException == null) {
			handleException(exception);
			myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": AsynchFlow-s "+af.getName()+" failed. Resuming workflow");
			owner.resume();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Inner class AsynchronousFlow
	 */
	private class AsynchronousFlow implements Serializable, TerminationNotificationReceiver.TerminationListener {
		private String delegationActivityName;
		private int policy;
		private int index;
		
		private SubflowDelegationBehaviour delegationBehaviour;
		private List<TerminationNotificationReceiver> pendingReceivers = new ArrayList<TerminationNotificationReceiver>();
		private List<TerminationNotificationReceiver> completedReceivers = new ArrayList<TerminationNotificationReceiver>();

		private List<Subflow> flowResults;
		private int joinedCnt = 0;
		private boolean bound = true;
		
		AsynchronousFlow(String delegationActivityName, int policy, int index) {
			super();
			this.delegationActivityName = delegationActivityName;
			this.policy = policy;
			this.index = index;
		}
		
		/**
		 * Bind this AsynchronousFlow to the related SubflowDelegationActivity.
		 */		
		boolean  bind() {
			delegationBehaviour = (SubflowDelegationBehaviour) owner.getState(delegationActivityName);
			if (delegationBehaviour != null){
				flowResults = new ArrayList<Subflow>();
				Iterator<TerminationNotificationReceiver> it = delegationBehaviour.getAllAsynchronousDelegations();
				while (it.hasNext()) {
					TerminationNotificationReceiver ter = it.next();
					// Note that there is no need for any synchronization here as we haven't registered for termination notifications yet
					pendingReceivers.add(ter);
				}
			}else{
				bound = false;
			}
			return bound;
		}

		/**
		 * Register itself as listener to the TerminationEventReceivers of all subflows belonging to this AsynchronousFlow
		 */
		void init() {
			TerminationNotificationReceiver[] tt = (TerminationNotificationReceiver[]) pendingReceivers.toArray(new TerminationNotificationReceiver[0]);
			for (int i = 0;i < tt.length; ++i) {
				tt[i].registerListener(this);
			}
		}
		
		String getName() {
			return delegationActivityName;
		}
		
		int getPolicy() {
			return policy;
		}
		
		int getIndex() {
			return index;
		}
		
		int getJoinedCnt() {
			return index;
		}
		
		int getPendingReceivers(){
			return pendingReceivers.size();
		}
		
		void clean() {
			TerminationNotificationReceiver[] pp = null;
			TerminationNotificationReceiver[] cc = null;
			synchronized (this) {
				pp = (TerminationNotificationReceiver[]) pendingReceivers.toArray(new TerminationNotificationReceiver[0]);
				pendingReceivers.clear();
				cc = (TerminationNotificationReceiver[]) completedReceivers.toArray(new TerminationNotificationReceiver[0]);
				completedReceivers.clear();
			}
			for (int i = 0; i < pp.length; ++i) {
				pp[i].deregisterListener();
			}
			for (int i = 0; i < cc.length; ++i) {
				cc[i].deregisterListener();
			}
			
			delegationBehaviour = null;
		}
		
		// This is executed by the Agent Thread --> Can never be executed two times in parallel
		public void handleTermination(TerminationNotificationReceiver ter) {
			// Mutual exclusion with clean()
			synchronized (this) { 
				if (!pendingReceivers.remove(ter)) {
					return;
				}
				completedReceivers.add(ter);
				myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": received termination notification for delegation "+ter.getDelegationId()+" of AsynchFlow "+getName());
			}
			
			try {
				flowResults.add(ter.getResult());
				if (policy == ANY || pendingReceivers.isEmpty()) {
					// Asynchronous flow successfully completed 
					myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": AsynchFlow "+getName()+" successfully completed");
					if (handleAsynchronousFlowSuccess(this, flowResults)) {
						// All completed subflows have been joined
						synchronized (this) {
							for (TerminationNotificationReceiver tt : completedReceivers) {
								tt.setJoined();
								joinedCnt++;
							}
						}
					}
					clean();
				}
			}
			catch (Exception e) {
				// Asynchronous flow completed due to a failure of one of its subflows
				myLogger.log(Logger.INFO, ">>>>>>>> SJB "+getBehaviourName()+": AsynchFlow "+getName()+" failed due to failure of subflow "+ter.getDelegationId(), e);
				if (handleAsynchronousFlowFailure(this, e)) {
					// Only the failed subflow has been joined
					ter.setJoined();
					joinedCnt++;
				}
				clean();
			}
		}
	}
}
