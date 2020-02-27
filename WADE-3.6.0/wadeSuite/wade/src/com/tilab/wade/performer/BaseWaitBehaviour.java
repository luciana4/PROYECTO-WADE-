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

import jade.core.Agent.Interrupted;

/**
 * Base class for activities that have to wait for something (an event, a timeout...).
 */
public abstract class BaseWaitBehaviour extends ActivityBehaviour {

	private int state = 0;
	private boolean finished;
	private MethodInvocator beforeInvocator;
	private MethodInvocator afterInvocator;

	public abstract boolean checkCompleted()throws Exception;
	public abstract void init() throws Exception;

	public BaseWaitBehaviour(String name, WorkflowBehaviour owner, boolean hasDedicatedMethods) {
		super(name, owner);
		requireSave = true;

		if (hasDedicatedMethods) {
			String beforeMethodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.BEFORE_METHOD_TYPE);
			EngineHelper.checkMethodName(beforeMethodName, "activity", name);
			beforeInvocator = createBeforeMethodInvocator(beforeMethodName);

			String afterMethodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.AFTER_METHOD_TYPE);
			EngineHelper.checkMethodName(afterMethodName, "activity", name);
			afterInvocator = createAfterMethodInvocator(afterMethodName);
		} 
	}


	protected MethodInvocator createBeforeMethodInvocator(String beforeMethodName) {
		return new MethodInvocator(owner, beforeMethodName);
	}

	protected MethodInvocator createAfterMethodInvocator(String afterMethodName) {
		return new MethodInvocator(owner, afterMethodName);
	}

	@Override
	public void onStart() {
		super.onStart();

		try {
			owner.enterInterruptableSection();
			if (beforeInvocator != null) {
				beforeInvocator.invoke();
			}

			manageBindings();
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
	
	protected void manageBindings() throws Exception {
		// Implementation delegated to subclasses
	}

	protected void manageOutputBindings() throws Exception	{
		// Implementation delegated to subclasses
	}
	
	@Override
	public void action() {
		try {
			owner.enterInterruptableSection();

			switch (state) {
			case 0:
				state = 1;
				if (owner.isLongRunning()) {
					WorkflowSerializationManager.save(owner);
				}
				break;

			case 1:
				init();
				state = 2;
				break;

			case 2:
				if (checkCompleted()) {
					finished = true;
					manageOutputBindings();
					if (afterInvocator != null) {
						afterInvocator.invoke();
					}
				}
				else {
					if (lastException == null) { 
						owner.suspend();
					}
				}
				break;
			}
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

	@Override
	public boolean done() {
		return finished || isInterrupted() || (lastException != null);
	}

	@Override
	public void reset() {
		super.reset();
		finished = false;
		state = 0;
	}
}
