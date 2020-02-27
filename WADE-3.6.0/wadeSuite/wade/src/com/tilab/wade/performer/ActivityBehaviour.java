/*****************************************************************
 WADE - Workflow and Agent Development Environment is a framework to develop 
 multi-agent systems able to execute tasks defined according to the workflow
 metaphor.
 Copyright (C) 2008 Telecom Italia S.p.A. 

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation, 
 version 2.1 of the License. 

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/
package com.tilab.wade.performer;

import jade.core.behaviours.Behaviour;

/**
   Base class for behaviours implementing "simple" XPDL activities
   i.e. activities of type TOOL and SUBFLOW.
   @author Giovanni Caire - TILAB
 */
public class ActivityBehaviour extends Behaviour implements HierarchyNode {
	protected WorkflowBehaviour owner;
	private OutgoingTransitions outgoingTransitions;
	protected Throwable lastException = null;
	private boolean errorActivity = false;
	private boolean executed = false;
	private boolean reinitializing = false;
	private boolean interrupted = false;
	protected boolean requireSave = false;
	private BindingManager bindingManager;

	public ActivityBehaviour(String name, WorkflowBehaviour owner) {
		super();
		this.owner = owner;
		setBehaviourName(name.replace(' ', '_'));
		outgoingTransitions = new OutgoingTransitions();
	}

	public void onStart() {
		owner.handleBeginActivity(this);
	}

	public void action() {
	}

	public boolean done() {
		return true;
	}

	public int onEnd() {
		// This also calls getAgent().handleEndActivity()
		return EngineHelper.endActivity(this);
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
		return null;
	}

	public WorkflowBehaviour getOwner() {
		return owner;
	}

	public boolean requireSave() {
		return requireSave;
	}
	
	public void setRequireSave(boolean requireSave) {
		this.requireSave = requireSave;
	}
}
