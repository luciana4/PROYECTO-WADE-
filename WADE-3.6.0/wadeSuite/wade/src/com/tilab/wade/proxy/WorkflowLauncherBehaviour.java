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
package com.tilab.wade.proxy;

import jade.content.AgentAction;
import jade.util.Logger;
import jade.util.leap.List;

import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.ExecuteWorkflow;
import com.tilab.wade.utils.GUIDGenerator;


class WorkflowLauncherBehaviour extends WorkflowManagementBehaviour {
	
	private static final long serialVersionUID = -503193840117703276L;

	private WorkflowDescriptor wd;
	
	
	public WorkflowLauncherBehaviour(EngineProxy ep, WorkflowDescriptor wd, WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) {
		super(ep, resultListener, eventCfg, context, interactiveMode);
		
		this.wd = wd;
		
		// Check if workflow sessionId is null -> generate it
		sessionId = wd.getSessionId();
		if (sessionId == null) {
			sessionId = GUIDGenerator.getGUID();
			wd.setSessionId(sessionId);
		}
	}
	
	@Override
	public void onStart() {
		// Get an executor
		executor = engineProxy.nextExecutor(myAgent);
		if (executor == null) {
			logger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+": "+getClass().getSimpleName()+": No workflow executors available, sessionId="+sessionId);
			abort("No workflow executors available");
		}
		
		super.onStart();
	}

	@Override
	protected AgentAction prepareAgentAction() {
		// Set the execution in synch mode (so the event of workflow termination is always received)
		wd.setExecution(Constants.SYNCH);

		// Prepare control infos
		List cInfos = prepareControlInfos();
		
		// Prepare modifiers
		List modifiers = prepareModifiers();

		// Prepare action
		return new ExecuteWorkflow(wd, cInfos, modifiers);
	}
}
