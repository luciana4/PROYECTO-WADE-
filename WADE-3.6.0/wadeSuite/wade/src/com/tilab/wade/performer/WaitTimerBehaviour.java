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

import jade.core.behaviours.WakerBehaviour;
import jade.util.Logger;

/**
 * The behaviour representing a workflow "activity" whose execution corresponds to suspending until a given 
 * timer expires.
 */
public class WaitTimerBehaviour extends BaseWaitBehaviour {

	private long timeout;
	private long deadline;
	private boolean deadlineExpired = false;
	
	private transient WakerBehaviour timerWatchDog;
	
	private Logger myLogger = Logger.getJADELogger(getClass().getName());

	
	public WaitTimerBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	public WaitTimerBehaviour(String name, WorkflowBehaviour owner, boolean callMethods) {
		super(name, owner, callMethods);
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		deadline = System.currentTimeMillis() + timeout;
	}

	public void init() {
		long currentTime = System.currentTimeMillis();	
		if (currentTime < deadline) {
			// Deadline not expired yet --> activate a watchDog and suspend 
			timerWatchDog = new WakerBehaviour(myAgent, deadline - currentTime) {
				private static final long serialVersionUID = 111111111111L;

				@Override
				protected void onWake() {
					deadlineExpired = true;
					try {
						owner.resume();
					}
					catch (Exception e) {
						// The workflow may have been killed or frozen while it was suspended
						myLogger.log(Logger.INFO, "Cannot resume workflow "+owner.getExecutionId());
					}
				}
			};
			
			myAgent.addBehaviour(timerWatchDog);
		}
		else {
			deadlineExpired = true;
		}
	}
	
	public boolean checkCompleted() {
		return deadlineExpired;
	}
	
	public int onEnd() {
		// Clean the timeWatchDog if still present
		if (timerWatchDog != null) {
			myAgent.removeBehaviour(timerWatchDog);
		}
		
		return super.onEnd();
	}
}
