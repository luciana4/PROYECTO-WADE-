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

import com.tilab.wade.performer.layout.WorkflowSkipped;

//#MIDP_EXCLUDE_FILE

/**
   This behaviour implements an XPDL Activity of type BlockActivity.
   Note that this extends WorkflowBehaviour since it shares all the
   activity management mechanism
   @author Giovanni Caire - TILAB
 */
@WorkflowSkipped
public abstract class BlockExecutionBehaviour extends WorkflowBehaviour {
	
	// REFACTOR
	public BlockExecutionBehaviour(String activityName) {
		super(activityName);
	}

	public void onStart() {
		rootExecutor = (WorkflowEngineAgent.WorkflowExecutor) root();
		getAgent().handleBeginActivity(rootExecutor, getBehaviourName());
	}
	
	public int onEnd() {
		// This also calls getAgent().handleEndActivity()
		return EngineHelper.endActivity(this);
	}
}
		