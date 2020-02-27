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
package com.tilab.wade.dispatcher;

import com.tilab.wade.performer.event.*;

import jade.core.AID;

public class DefaultEventListener implements WorkflowEventListener {
	
	public void handleEvent(long time, Object ev, AID executor, String executionId) {
		// FLOW EVENTS
		if (ev instanceof BeginWorkflow) {
			handleBeginWorkflow(time, (BeginWorkflow) ev, executor, executionId);
		}
		else if (ev instanceof EndWorkflow) {
			handleEndWorkflow(time, (EndWorkflow) ev, executor, executionId);
		}
		else if (ev instanceof DelegatedSubflow) {
			handleDelegatedSubflow(time, (DelegatedSubflow) ev, executor, executionId);
		}
		else if (ev instanceof BeginActivity) {
			handleBeginActivity(time, (BeginActivity) ev, executor, executionId);
		}
		else if (ev instanceof EndActivity) {
			handleEndActivity(time, (EndActivity) ev, executor, executionId);
		}
		else if (ev instanceof BeginApplication) {
			handleBeginApplication(time, (BeginApplication) ev, executor, executionId);
		}
		else if (ev instanceof EndApplication) {
			handleEndApplication(time, (EndApplication) ev, executor, executionId);
		}
		// TRANSACTION EVENTS
		else if (ev instanceof OpenedTransaction) {
			handleOpenedTransaction(time, (OpenedTransaction) ev, executor, executionId);
		}
		else if (ev instanceof AbortedTransaction) {
			handleAbortedTransaction(time, (AbortedTransaction) ev, executor, executionId);
		}
		else if (ev instanceof CommittedTransaction) {
			handleCommittedTransaction(time, (CommittedTransaction) ev, executor, executionId);
		}
		/*else if (ev instanceof BeginCommit) {
			handleBeginCommit(time, (BeginCommit) ev, executor, executionId);
		}
		else if (ev instanceof BeginRollback) {
			handleBeginRollback(time, (BeginRollback) ev, executor, executionId);
		}*/
		// TERMINATION EVENTS
		else if (ev instanceof SuccessfulTerminationEvent) {
			handleSuccessfulTerminationEvent(time, (SuccessfulTerminationEvent) ev, executor, executionId);
		}
		else if (ev instanceof UnsuccessfulTerminationEvent) {
			handleUnsuccessfulTerminationEvent(time, (UnsuccessfulTerminationEvent) ev, executor, executionId);
		}
		// TRACING EVENTS
		else if (ev instanceof TraceEvent) {
			handleTraceEvent(time, (TraceEvent) ev, executor, executionId);
		}
		else {
			handleUnknownEvent(time, ev, executor, executionId);
		}
	}
	
	public void handleExecutionCompleted(AID executor, String executionId) {
	}		
	
	
	// Flow events
	public void handleBeginWorkflow(long time, BeginWorkflow event, AID executor, String executionId) {
	}
	
	public void handleEndWorkflow(long time, EndWorkflow event, AID executor, String executionId) {
	}
	
	public void handleDelegatedSubflow(long time, DelegatedSubflow event, AID executor, String executionId) {
	}
	
	public void handleBeginActivity(long time, BeginActivity event, AID executor, String executionId) {
	}
	
	public void handleEndActivity(long time, EndActivity event, AID executor, String executionId) {
	}
	
	public void handleBeginApplication(long time, BeginApplication event, AID executor, String executionId) {
	}
	
	public void handleEndApplication(long time, EndApplication event, AID executor, String executionId) {
	}
	

	// Transaction events
	public void handleOpenedTransaction(long time, OpenedTransaction event, AID executor, String executionId) {
	}
	
	public void handleAbortedTransaction(long time, AbortedTransaction event, AID executor, String executionId) {
	}
	
	public void handleCommittedTransaction(long time, CommittedTransaction event, AID executor, String executionId) {
	}
	
	/*public void handleBeginRollback(long time, BeginRollback event, AID executor, String executionId) {
	}
	
	public void handleBeginCommit(long time, BeginCommit event, AID executor, String executionId) {
	}*/
	

	// Termination events
	public void handleSuccessfulTerminationEvent(long time, SuccessfulTerminationEvent event, AID executor, String executionId) {
	}
	
	public void handleUnsuccessfulTerminationEvent(long time, UnsuccessfulTerminationEvent event, AID executor, String executionId) {
	}
	

	// Tracing events
	public void handleTraceEvent(long time, TraceEvent event, AID executor, String executionId) {
	}
	

	// Generic handler
	public void handleUnknownEvent(long time, Object ev, AID executor, String executionId) {
	}
}
