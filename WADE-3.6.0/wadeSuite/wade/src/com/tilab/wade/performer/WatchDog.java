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

//#MIDP_EXCLUDE_FILE

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.util.Logger;

/**
   This behaviour enforces workflow execution timeouts
   @author Giovanni Caire - TILAB
 */
class WatchDog extends WakerBehaviour {
	private boolean expired = false;
	private long startTime = -1;
	private transient WorkflowEngineAgent.WorkflowExecutor executor;
	
	public WatchDog(Agent a, long timeout, WorkflowEngineAgent.WorkflowExecutor executor) {
		super(a, timeout);
		this.executor = executor;
		setBehaviourName("WatchDog-"+executor.getId());
	}
	
	public void onStart() {
		startTime = System.currentTimeMillis();
		super.onStart();
	}
	
	public void onWake() {
		if (executor != null) {
			Logger myLogger = Logger.getMyLogger(myAgent.getName());
			myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+executor.getId()+": WatchDog timer expired.");
			//((WorkflowEngineAgent) myAgent).handleKillWorkflow(executor, null, true, true);
			executor.kill(true, true);
		}
		expired = true;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public boolean isExpired() {
		return expired;
	}
}
		