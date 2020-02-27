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
package com.tilab.wade.performer.transaction;

import com.tilab.wade.performer.EngineHelper;
import com.tilab.wade.performer.TerminationNotificationReceiver;
import com.tilab.wade.performer.WorkflowEngineAgent;
import com.tilab.wade.performer.WorkflowException;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.KillWorkflow;

import jade.core.Agent;
import jade.core.AID;
import jade.core.NotFoundException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.content.onto.basic.Action;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.RefuseException;


import jade.util.Logger;

/**
 * Transaction entry class embedding the information and logic required to 
 * commit or rollback a subflow
 * @author Giovanni Caire - TILAB
 */
public class SubflowEntry extends TransactionEntry {
	public static final long TRANSACTION_CLOSURE_TIMEOUT = 600000; // 10 min
	
	// Subflow states
	public static final int REQUESTED = 0;
	public static final int ACCEPTED = 1;
	public static final int COMPLETED = 2;
	public static final int FAILED = 3;
	public static final int COMMITTING = 4;
	public static final int ROLLBACKING = 5;
	public static final int DONE = 6;
	public static final int ERROR = 7;
	
	private WorkflowEngineAgent myAgent;
	private AID delegatedPerformer;
	private WorkflowDescriptor myDescriptor;
	private int status;
	private TerminationNotificationReceiver terminationNotificationReceiver;
	
	private String executionId;
	private ACLMessage agree;
	private ACLMessage notification;
	
	private Logger myLogger;
	
	public SubflowEntry(String id, String activity, Agent agent, AID performer, WorkflowDescriptor wd) {
		setId(id);
		setActivity(activity);
		myAgent = (WorkflowEngineAgent) agent;
		delegatedPerformer = performer;
		myDescriptor = wd;
		status = REQUESTED;
		
		myLogger = Logger.getMyLogger(myAgent.getName());
	}

	public void setTerminationNotificationReceiver(TerminationNotificationReceiver terminationNotificationReceiver) {
		this.terminationNotificationReceiver = terminationNotificationReceiver;
	}

	public final AID getDelegatedPerformer() {
		return delegatedPerformer;
	}
	
	public final WorkflowDescriptor getDescriptor() {
		return myDescriptor;
	}
	
	public final void setAgree(ACLMessage agree) {
		this.agree = agree;
		executionId = agree.getContent();
		status = ACCEPTED;
	}
	
	public final String getExecutionId() {
		return executionId;
	}

	public final int getStatus() {
		return status;
	}
	
	public final void setNotification(ACLMessage notification) {
		this.notification = notification;
		if (notification.getPerformative() == ACLMessage.PROPOSE) {
			status = COMPLETED;
		}
		else {
			status = FAILED;
		}
	}
	
	public final ACLMessage getNotification() {
		return notification;
	}
	
	public boolean isSuccessful() {
		return status != FAILED;
	}
	
	public void commit() throws Throwable {
		try {
			if (status == COMPLETED) {
				status = COMMITTING;
				myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - Committing subflow "+getId());
				close(ACLMessage.ACCEPT_PROPOSAL);
				myLogger.log(Logger.FINER, "Agent "+myAgent.getName()+" - Subflow "+getId()+" successfully committed");
				status = DONE;
			}
			else {
				// Should never happen
				throw new IllegalStateException("Cannot commit a subflow in state "+status);
			}
		}
		catch (Throwable t) {
			status = ERROR;
			throw t;
		}
	}
	
	public void rollback() throws Throwable {
		try {
			if (status == COMPLETED) {
				status = ROLLBACKING;
				myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - Rolling back subflow "+getId());
				close(ACLMessage.REJECT_PROPOSAL);
				myLogger.log(Logger.FINER, "Agent "+myAgent.getName()+" - Subflow "+getId()+" successfully rolled back");
				status = DONE;
			}
			else if (status == ACCEPTED || status == REQUESTED) {
				status = ROLLBACKING;
				myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Killing pending subflow "+getId());
				killPendingSubflow();
				myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Pending subflow "+getId()+" successfully killed");
				status = DONE;
			}
			else {
				throw new IllegalStateException("Cannot rollback a subflow in state "+status);
			}
		}
		catch (Throwable t) {
			status = ERROR;
			throw t;
		}
	}
	
	private void close(int performative) throws NotFoundException, WorkflowException {
		ACLMessage msg = notification.createReply();
		msg.setPerformative(performative);
		myAgent.send(msg);
		MessageTemplate template = MessageTemplate.MatchConversationId(msg.getConversationId());
		template = EngineHelper.adjustReplyTemplate(template, msg);
		ACLMessage reply = myAgent.blockingReceive(template, TRANSACTION_CLOSURE_TIMEOUT);
		myAgent.removeConversation(notification);
		if (reply != null) {
			if (reply.getPerformative() == ACLMessage.FAILURE) {
				if (reply.getSender().equals(myAgent.getAMS())) {
					// The delegated performer does not exist
					throw new NotFoundException("Agent "+delegatedPerformer.getName()+" not found.");
				}
				else {
					// The delegated performer explicitly replied with a FAILURE message.
					// This may happen if the commit/rollback process performed by the 
					// delegated performer failed.
					throw new WorkflowException("Agent "+delegatedPerformer.getName()+" failed to close the transaction");					
				}
			}
		}
		else {
			// The delegated performer did not reply
			throw new WorkflowException("Agent "+delegatedPerformer.getName()+" did not reply in due time");
		}
	}
	
	private void killPendingSubflow() throws WorkflowException {
		AID performer = agree.getSender();
		KillWorkflow ki = new KillWorkflow(executionId);
		Action aExpr = new Action(performer, ki);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(performer);
		msg.setLanguage(myAgent.getLanguage().getName());
		msg.setOntology(myAgent.getOntology().getName());
		try {
			
			if (terminationNotificationReceiver != null) {
				terminationNotificationReceiver.abort();
			}
			
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
			
			// Wait for the termination notification from the subflow 
			MessageTemplate template = MessageTemplate.MatchConversationId(agree.getConversationId());
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Waiting for subflow "+getId()+" termination notification...");
			notification = myAgent.blockingReceive(template, TRANSACTION_CLOSURE_TIMEOUT);
			if (notification != null) {
				if (notification.getPerformative() == ACLMessage.PROPOSE) {
					// The subflow successfully completed while we were killing it --> Note that in this case the subflow will not send back any further transaction completion notification (see comments in the WAIT_COMMIT state of the WorkflowExecutor)
					myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Successful termination notification received from subflow "+getId()+" while we were killing it. Don't wait for any further notification");
				}
			}
			else {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Missing termination notification from subflow "+getId());
			}
		}
		catch (RefuseException re) {
			// The subflow is no longer there. This may happen if the subflow failed just before we tried 
			// to kill it --> Just do nothing
		}
		catch (Exception e) {
			throw new WorkflowException("Error killing pending subflow. ", e);
		}
		finally {
			if (agree != null) {
				myAgent.removeConversation(agree);
			}
		}
	}
}
