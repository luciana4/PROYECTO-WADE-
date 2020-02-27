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

import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CompositeBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Collection;
import jade.util.leap.List;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.Hashtable;

import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.EventEmitter;
import com.tilab.wade.performer.event.ExecutionErrorEvent;
import com.tilab.wade.performer.transaction.TransactionManager;

/**
 * This class is only used to replace a WorkflowExecutor when it is serialized.
 * @see WorkflowSerializationManager
 */
class WorkflowExecutorReplacer extends CompositeBehaviour {
	
	private String executionId;
	private int status;
	private int abortCondition;
	private String failureReason;
	private Action requestedAction;
	private List modifiers;
	private Hashtable context = new Hashtable();
	private ExecutionErrorEvent lastErrorEvent = null;
	private ACLMessage reply;
	private WorkflowDescriptor descriptor;
	private WorkflowEngineAgent enclosingAgent;
	private boolean transactionScope;
	private TransactionManager transactionManager;
	private EventEmitter eventEmitter;
	private WorkflowBehaviour workflow;

	
	WorkflowExecutorReplacer(Agent agent,
			                 String executionId, 
			                 int status, 
			                 ACLMessage reply, 
			                 Action requestedAction, 
			                 WorkflowDescriptor descriptor,
			                 List modifiers,
			                 EventEmitter eventEmitter,
			                 WorkflowBehaviour workflow,
			                 Hashtable context,
			                 int abortCondition,
			                 ExecutionErrorEvent lastErrorEvent,
			                 String failureReason,
			                 boolean transactionScope,
			                 TransactionManager transactionManager) {
		enclosingAgent = (WorkflowEngineAgent) agent;
		this.executionId = executionId;
		this.status = status;
		this.reply = reply;
		this.requestedAction = requestedAction;
		this.descriptor = descriptor;
		this.modifiers = modifiers;
		this.eventEmitter = eventEmitter;
		this.workflow = workflow;
		this.context = context;
		this.abortCondition = abortCondition;
		this.lastErrorEvent = lastErrorEvent;
		this.failureReason = failureReason;
		this.transactionManager = transactionManager;
		
		this.workflow.setAgent(null);
	}
	
	WorkflowEngineAgent getEnclosingAgent() {
		return enclosingAgent;
	}

	String getExecutionId() {
		return executionId;
	}

	int getStatus() {
		return status;
	}

	Action getRequestedAction() {
		return requestedAction;
	}

	WorkflowDescriptor getDescriptor() {
		return descriptor;
	}

	EventEmitter getEventEmitter() {
		return eventEmitter;
	}

	List getModifiers() {
		return modifiers;
	}

	WorkflowBehaviour getWorkflow() {
		return workflow;
	}

	Hashtable getContext() {
		return context;
	}

	ACLMessage getReply() {
		return reply;
	}

	int getAbortCondition() {
		return abortCondition;
	}

	ExecutionErrorEvent getLastErrorEvent() {
		return lastErrorEvent;
	}

	String getFailureReason() {
		return failureReason;
	}

	public boolean getTransactionScope() {
		return transactionScope;
	}

	TransactionManager getTransactionManager() {
		return transactionManager;
	}

	private Object readResolve() throws ObjectStreamException {
		if (enclosingAgent == null) {
			// If the workflow we are replacing was serialized alone (i.e. not within the scope of an agent serialization)
			// the enclosingAgent is null (see WorkflowSerializationManager.serializeWorkflow())
			// --> Get it from the WorkflowSerializationManager (see WorkflowSerializationManager.deserializeWorkflow())
			enclosingAgent = WorkflowSerializationManager.getEnclosingAgent();
		}
		if (enclosingAgent != null) {
			return enclosingAgent.createWorkflowExecutor(this);
		}
		else {
			throw new InvalidObjectException("No enclosing agent available to deserialize workflow "+executionId);
		}
	}

	
	// Extend CompositeBehaviour only for serialization problem
	// CompositeBehaviour override method
	
	@Override
	protected boolean checkTermination(boolean currentDone, int currentResult) {
		return false;
	}

	@Override
	public Collection getChildren() {
		return null;
	}

	@Override
	protected Behaviour getCurrent() {
		return null;
	}

	@Override
	protected void scheduleFirst() {
	}

	@Override
	protected void scheduleNext(boolean currentDone, int currentResult) {
	}
}
