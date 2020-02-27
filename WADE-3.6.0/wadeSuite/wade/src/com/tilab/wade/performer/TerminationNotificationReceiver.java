package com.tilab.wade.performer;

import jade.content.ContentException;
import jade.content.onto.basic.Result;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.WorkflowEngineAgent.WorkflowExecutor;
import com.tilab.wade.performer.transaction.SubflowEntry;

/**
 * This behaviour is responsible for asynchronously intercepting the termination notification issued by a subflow delegated asynchronously.
 * This behaviour is used internally by the framework.
 */
public class TerminationNotificationReceiver extends SimpleBehaviour {
	static final int RUNNING = 0;
	static final int SUCCESS = 1;
	static final int FAILURE = -1;
	static final int JOINED = 2;
	static final int ABORTED = 3;
	
	private WorkflowExecutor we;
	//private String executionId;
	private String delegationId;
	private Subflow subflow;
	private SubflowEntry sbflEntry;		
	private MessageTemplate template;
	private SubflowDelegationBehaviour delegationBehaviour;
	private String delegatedExecutionId;
	
	private volatile int status = RUNNING;
	private Exception subflowException;
	private TerminationListener listener;

	protected Logger myLogger = Logger.getMyLogger(TerminationNotificationReceiver.class.getName());
	
	TerminationNotificationReceiver(WorkflowExecutor we, String delegatedExecutionId, String delegationId, String delegatedWorkflowId, MessageTemplate template, SubflowEntry sbflEntry) {
		super();
		
		this.we = we;
		this.setBehaviourName("TNR-"+we.getId() +"#"+delegationId);
		
		this.delegatedExecutionId = delegatedExecutionId;
		this.delegationId = delegationId;
		
		// At this point the activity is unknown, this will be setted in handleAsynchronousFlowSuccess of SubflowJoinBehaviour
		subflow = new Subflow(delegatedWorkflowId, null);
		this.sbflEntry = sbflEntry;
		if (sbflEntry != null) {
			this.sbflEntry.setTerminationNotificationReceiver(this);
		}
		this.template = template;
	}
	
	void setDelegationBehaviour(SubflowDelegationBehaviour delegationBehaviour) {
		this.delegationBehaviour = delegationBehaviour;
	}
	
	public void action() {
		if (status == RUNNING) { 
			// Check if we received the message carrying the termination notification. Block otherwise 
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				// Termination notification  received
				handleTerminationNotification(msg);
			}
			else {
				block();
			}
		}
		else if (status == SUCCESS || status == FAILURE){
			// The termination notification has been received --> If we have a listener notify it. Block otherwise 
			// Mutual exclusion with register/deregisterListener()
			synchronized (this) {
				if (listener != null) {
					listener.handleTermination(this);
					listener = null;
				}
				else {
					block();
				}
			}
		}
	}
	
	public boolean done() {
		return status == JOINED || status == ABORTED;
	}
	
	private void handleTerminationNotification(ACLMessage msg) {
		if (sbflEntry != null) {
			// If we are in a transaction scope, update the subflow-entry
			sbflEntry.setNotification(msg);
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+ we.getId()+": Set "+ACLMessage.getPerformative(msg.getPerformative())+" notification to asynchronous Subflow Entry "+delegationId);
		}
		CAServices.getInstance(myAgent).expectedReplyReceived(msg);
		try {
			switch (msg.getPerformative()) {
			case ACLMessage.INFORM:
			case ACLMessage.PROPOSE:
				Result r = (Result) myAgent.getContentManager().extractContent(msg);
				subflow.setParams(r.getItems());
				status = SUCCESS;
				break;
			case ACLMessage.FAILURE:
				SubflowDelegationBehaviour.handleSubflowFailure(myAgent, msg);
			default:
				// Note that REFUSE and NOT_UNDERSTOOD cannot happen here
				throw new FailedSubflow("Unexpected performative "+ACLMessage.getPerformative(msg.getPerformative()));
			}
		}
		catch (Exception e) {
			status = FAILURE;
			if (e instanceof ContentException) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+we.getId()+": Error parsing termination notification of asynchronous Subflow "+delegationId, e);
			}
			subflowException = e;
		}
	}

	
	////////////////////////////////////////////
	// Methods called by the executor's Thread
	////////////////////////////////////////////
	/**
	 * This method can only be called when the subflow already completed and returns   
	 * the a Subflow object holding output parameters if the subflow succeeded while 
	 * throws an exception if the subflow failed. The actual exception is the
	 * same that would be thrown if the failed subflow was executed synchronously. 
	 */
	Subflow getResult() throws Exception {
		switch(status) {
		case SUCCESS:
			return subflow;
		case FAILURE:
			throw subflowException;
		default:
			// Should never happen
			throw new IllegalStateException("Wrong TerminationNotificationReceiver status "+status);
		}
	}
	
	synchronized void registerListener(TerminationListener listener) {
		this.listener = listener;
		restart();
	}
	
	synchronized void deregisterListener() {
		listener = null;
	}
	
	// This does not require synchronization as it is only called after the listener has been notified 
	void setJoined() {
		if (status != JOINED){
			int sbfexitValue = (status == SUCCESS) ? Constants.SUCCESS:Constants.FAILURE;
			((WorkflowEngineAgent)myAgent).handleCompletedSubflow(we, delegatedExecutionId, sbfexitValue);
		}
		status = JOINED;
		delegationBehaviour.removeAsynchronousDelegation(this);
		// Restart the behaviour to make it terminate
		restart();
	}
	
	public void abort() {
		status = ABORTED;
		delegationBehaviour.removeAsynchronousDelegation(this);
		// Restart the behaviour to make it terminate
		restart();
	}
	
	String getDelegationId() {
		return delegationId;
	}
	
	Subflow getSubflow() {
		return subflow;
	}

	/**
	 * Inner interface TerminationListener
	 */
	static interface TerminationListener {
		void handleTermination(TerminationNotificationReceiver ter);
	}
}
