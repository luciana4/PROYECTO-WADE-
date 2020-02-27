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
import jade.util.leap.List;

import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.ontology.RecoverWorkflow;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;

public class WorkflowRecoverBehaviour extends WorkflowManagementBehaviour {
	
	private static final long serialVersionUID = -8847978303760802357L;

	private String recoverExecutionId;
	
	public WorkflowRecoverBehaviour(EngineProxy ep, WorkflowExecutionInfo wei, WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) {
		super(ep, resultListener, eventCfg, context, interactiveMode);
		
		this.executor = wei.getExecutor();
		this.sessionId = wei.getSessionId();
		this.recoverExecutionId = wei.getExecutionId();
		this.recovered = true;
	}

	@Override
	protected AgentAction prepareAgentAction() {
		// Prepare control infos
		List cInfos = prepareControlInfos();
		
		// Prepare modifiers
		List modifiers = prepareModifiers();

		// Prepare action
		return new RecoverWorkflow(recoverExecutionId, cInfos, modifiers);
	}
}
