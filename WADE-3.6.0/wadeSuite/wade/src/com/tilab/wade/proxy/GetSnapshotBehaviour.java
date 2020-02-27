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

import com.tilab.wade.performer.interactivity.InteractivitySnapshot;
import com.tilab.wade.performer.interactivity.ontology.InteractivityCompleted;
import com.tilab.wade.performer.interactivity.ontology.InteractivityOntology;
import com.tilab.wade.performer.interactivity.ontology.GetSnapshot;

import jade.content.Predicate;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.IMTPException;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class GetSnapshotBehaviour extends SimpleBehaviour {

	protected static Logger logger = Logger.getMyLogger(GetSnapshotBehaviour.class.getName());
	
	public static final int UNDEFINED_STATUS  = -1;
	public static final int SUCCESS_STATUS  = 0;
	public static final int FAILURE_STATUS  = 1;
	private static final String GET_SNAPSHOT_REPLY_WITH = "GET_SNAPSHOT";
	
	private AID executor;
	private String sessionId;
	private InteractivitySnapshot snapshot;
	private int status = UNDEFINED_STATUS;
	private String error;
	private Exception nestedException;
	private MessageTemplate snapshotTemplate;
	private ACLMessage request;
	private Ontology interactivityOnto = InteractivityOntology.getInstance();
	private boolean finished = false;
	
	
	public GetSnapshotBehaviour(AID executor, String sessionId) {
		super(null);
		
		this.executor = executor;
		this.sessionId = sessionId;
		
		// Prepare template
		snapshotTemplate = MessageTemplate.and(
								MessageTemplate.and(
								MessageTemplate.MatchOntology(InteractivityOntology.getInstance().getName()), 
								MessageTemplate.MatchConversationId(sessionId)),
								MessageTemplate.MatchInReplyTo(GET_SNAPSHOT_REPLY_WITH));
	}

	// Mutual exclusion with abort()
	@Override
	public void onStart() {
		super.onStart();

		if (!finished) {
			try {
				// Delete all spurious messages that match the snapshot template 
				while (myAgent.receive(snapshotTemplate) != null);
	
				// Prepare request
				request = new ACLMessage(ACLMessage.REQUEST);
				request.setLanguage(LEAPCodec.NAME);
				request.setOntology(interactivityOnto.getName());
				request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				request.setConversationId(sessionId);
				request.addReceiver(executor);
				request.setReplyWith(GET_SNAPSHOT_REPLY_WITH);
				
				Action act = new Action(myAgent.getAID(), new GetSnapshot());
				myAgent.getContentManager().fillContent(request, act);
				
				// Send request
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE, "Send REQUEST(GET-SNAPSHOT) for sessionId="+sessionId);
				}
				myAgent.send(request);
				
			} catch (Exception e) {
				status = FAILURE_STATUS;
				error = e.getMessage();
				finished = true;
			}
		}
	}
	
	@Override
	public int onEnd() {
		return super.onEnd();
	}
	
	@Override
	public void action() {
		ACLMessage msg = myAgent.receive(snapshotTemplate);
		if (msg != null) {
			synchronized (this) {
				// finished may be true if we have been aborted in the meanwhile
				if (!finished) {					
					if (msg.getSender().equals(myAgent.getAMS())) {
						error = "Agent "+executor.getLocalName()+" UNREACHABLE";
						status = FAILURE_STATUS;
						try {
							nestedException = new IMTPException(AMSService.getFailureReason(myAgent, msg));
						} catch (FIPAException e) {	
							logger.log(Logger.WARNING, "Wrong AMS FAILURE reason, msg="+msg);
						}
					} else {
						if (msg.getPerformative() == ACLMessage.INFORM) {
							try {
								// Check if the received message is actually the reply we are waiting for. 
								// Discard other messages.
								// Could receive a spurious INFORM of a previous GO request.
								Predicate p = (Predicate)myAgent.getContentManager().extractContent(msg);
								if (p instanceof Result) {
									// The reply is a correct InteractivitySnapshot -> exit with success
									snapshot = (InteractivitySnapshot)((Result)p).getValue();
									status = SUCCESS_STATUS;
								}
								else if (p instanceof InteractivityCompleted) {
									// The reply is a InteractivityCompleted -> exit with error
									throw new Exception("Workflow interactivity already terminated, snapshot not available");
								}
		
							} catch (Exception e) {
								status = FAILURE_STATUS;
								error = "Extracting result error: "+e.getMessage();
							}
						} else {
							status = FAILURE_STATUS;
							error = msg.getContent();
						}
					}
					finished = true;
				} // END !finished
			} // END synchronize
		}
		else {
			block();
		}
	}

	@Override
	public boolean done() {
		return finished;
	}
	
	public InteractivitySnapshot getSnapshot() {
		return snapshot;
	}

	public String getError() {
		return error;
	}

	public Exception getNestedException() {
		return nestedException;
	}

	public int getStatus() {
		return status;
	}
	
	public void abort() {
		boolean aborted = false;
		// Mutual exclusion with result processing in action()
		synchronized (this) {
			if (status == UNDEFINED_STATUS) {
				status = FAILURE_STATUS;
				error = "Aborted";
				finished = true;
				aborted = true;
			}
		}
		// Call restart() outside the synchronized block to avoid dangerous synchronization issues 
		if (aborted) {
			restart();
		}
	}
}
