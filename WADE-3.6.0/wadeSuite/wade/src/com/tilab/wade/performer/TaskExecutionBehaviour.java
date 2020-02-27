package com.tilab.wade.performer;

import com.tilab.wade.performer.transaction.TaskEntry;
import com.tilab.wade.performer.transaction.TransactionManager;

import jade.core.Agent.Interrupted;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.WrapperBehaviour;

/**
 * The behaviour representing a workflow activity whose execution corresponds to the execution 
 * of a task implemented by a JADE behaviour.
 */
public class TaskExecutionBehaviour extends WrapperBehaviour implements HierarchyNode {
	protected WorkflowBehaviour owner;
	private OutgoingTransitions outgoingTransitions;
	protected Throwable lastException = null;
	private boolean errorActivity = false;
	private boolean executed = false;
	private boolean reinitializing = false;
	private boolean interrupted = false;
	private boolean requireSave = true;
	private BindingManager bindingManager;
	
	private boolean aborted = false;
	private Task task;
	private boolean hasDedicatedMethod;
	
	
	public TaskExecutionBehaviour(String name, Behaviour behaviour, WorkflowBehaviour owner) {
		this(name, behaviour, owner, true);
	}
	
	public TaskExecutionBehaviour(String name, Behaviour behaviour, WorkflowBehaviour owner, boolean hasDedicatedMethod) {
		super(behaviour);
		this.owner = owner;
		setBehaviourName(name.replace(' ', '_'));
		outgoingTransitions = new OutgoingTransitions();
		
		this.hasDedicatedMethod = hasDedicatedMethod;
		task = new Task(behaviour, this);
	}

	public void onStart() {
		owner.handleBeginActivity(this);
		try {
			copyInputParams();
			super.onStart();
		}
		catch (Exception e) {
			handleException(e);
			e.printStackTrace();
		}
	}

	public void action() {
		try {
			owner.enterInterruptableSection();
			super.action();
			if (Thread.interrupted()) {
				throw new Interrupted();
			}
		}
		catch (Interrupted i) {
			aborted = true;
		}
		catch (ThreadDeath td) {
			aborted = true;
		}
		catch (Throwable t) {
			aborted = true;
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
		return aborted || super.done();
	}

	public int onEnd() {
		try {
			super.onEnd();
			TransactionManager tm = owner.getTransactionManager();
			Behaviour b = getWrappedBehaviour();
			if (tm != null && isTransactional(b)) {
				String entryId = generateTaskEntryId(b.getBehaviourName());
				tm.addEntry(new TaskEntry(entryId, b, getTransactionalFields(b)));
			}
			getOwner().manageOutputBindings(task);
			restoreOutputParams();
			return EngineHelper.endActivity(this);
		}
		catch (Exception e) {
			// If an execution exception occurred, do not override it 
			if (lastException == null) {
				handleException(e);
				e.printStackTrace();
			}
		}
		return -1;
	}	

	public WorkflowBehaviour getOwner() {
		return owner;
	}
	
	public OutgoingTransitions getOutgoingTransitions() {
		return outgoingTransitions;
	}

	public boolean hasJADEDefaultTransition() {
		return owner.hasDefaultTransition(getBehaviourName());
	}

	public final void mark() {
		executed = true;
	}

	public final void reinit() {
		if (executed) {
			reinitializing = true;
			reset();
			reinitializing = false;
		}
	}

	public void reset() {
		lastException = null;
		executed = false;
		super.reset();
	}

	/**
	   This method is redefined to avoid calling Agent.notifyRestarted() 
	   (that is quite time consuming) when reset(), and therefore restart(),
	   is called due to the fact that this activity is revisited (reinit() method). In this 
	   case in facts the root behaviour is already running and notifyRestarted()
	   would have no effect but wasting time.
	 */
	public void restart() {
		if (reinitializing) {
			myEvent.init(true, NOTIFY_UP);
			handle(myEvent);
		}
		else {
			// Restart called to actually restart this behaviour. Do the normal job
			super.restart();
		}
	}

	public final WorkflowEngineAgent getAgent() {
		return (WorkflowEngineAgent) myAgent;
	}

	////////////////////////////////////////////
	// Exception handling methods
	////////////////////////////////////////////
	public final boolean isError() {
		return errorActivity;
	}

	public final void setError(boolean b) {
		errorActivity = b;
	}

	public final Throwable getLastException() {
		return lastException;
	}

	public void handleException(Throwable t) {
		lastException = t;
		EngineHelper.fireExecutionErrorEvent(this, lastException, Constants.FINE_LEVEL);
	}

	public final void propagateException(Throwable t) {
		((HierarchyNode) parent).handleException(t);
	}

	//////////////////////////////////////////////
	// Workflow interruption section
	//////////////////////////////////////////////
	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted() {
		interrupted = true;
	}

	public BindingManager getBindingManager() {
		if (bindingManager == null) {
			bindingManager = new BindingManager(this);
		}
		return bindingManager;
	}

	public BuildingBlock getBuildingBlock(String id) {
		return task;
	}
	
	private void copyInputParams() throws Exception {
		// Invoke the "before" method (if any) to fill task with input params
		if (hasDedicatedMethod) {
			String methodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.BEFORE_METHOD_TYPE);
			EngineHelper.checkMethodName(methodName, "activity", getBehaviourName()); 
			MethodInvocator invocator = new MethodInvocator((WorkflowBehaviour) parent, methodName, task, Task.class);
			invocator.invoke();
		}
		
		// Resolve bindings 
		owner.manageBindings(task);
		
		// At this point task includes input actual parameters --> pass them to the behaviour
		EngineHelper.copyInputParameters(getWrappedBehaviour(), task.getParams());
	}
	
	private void restoreOutputParams() throws Exception {
		// Extract output parameters from the behaviour and store them in task
		EngineHelper.extractOutputParameters(getWrappedBehaviour(), task.getParams());

		// Invoke "after" method (if any) 
		if (hasDedicatedMethod) {
			String methodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.AFTER_METHOD_TYPE);
			EngineHelper.checkMethodName(methodName, "activity", getBehaviourName());
			MethodInvocator invocator = new MethodInvocator((WorkflowBehaviour) parent, methodName, task, Task.class);
			invocator.invoke();
		}
	}

	public boolean requireSave() {
		return requireSave ;
	}
	
	public void setRequireSave(boolean requireSave) {
		this.requireSave = requireSave;
	}
	
	/**
	 * This method allows a behaviour used as task-activity within a workflow to retrieve the owner workflow.
	 * This is useful to access WorkflowBehaviour API such as trace(), fireEvent(), getModifier() and so on. 
	 * @param b The behaviour used as task
	 * @return The owner WorkflowBehaviour
	 */
	public static WorkflowBehaviour getOwner(Behaviour b) {
		Behaviour root = b.root();
		if (root instanceof WorkflowEngineAgent.WorkflowExecutor) {
			return ((WorkflowEngineAgent.WorkflowExecutor) root).getWorkflow();
		}
		else {
			// b is not used as a task activity of a running workflow --> no owner
			return null;
		}
	}
	
	/////////////////////////////////////////////////////////////
	// Transactions related section
	/////////////////////////////////////////////////////////////
	private static boolean isTransactional(Behaviour b) {
		// FIXME: to be implemented
		return false;
	}
	
	private static DataStore getTransactionalFields(Behaviour b) {
		// FIXME: to be implemented
		return new DataStore();
	}
	
	// Generates a unique id that identifies a task entry on the basis of the name of the task
	private static long taskEntryCnt = 0;
	private synchronized static String generateTaskEntryId(String taskName) {
		String id = "T_"+taskName+'_'+taskEntryCnt;
		taskEntryCnt++;
		return id;
	}	
}
