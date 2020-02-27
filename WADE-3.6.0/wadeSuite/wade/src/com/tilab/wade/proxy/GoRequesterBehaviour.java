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
import jade.lang.acl.MessageTemplate.MatchExpression;
import jade.util.Logger;

import com.tilab.wade.performer.interactivity.Interaction;
import com.tilab.wade.performer.interactivity.ontology.Go;
import com.tilab.wade.performer.interactivity.ontology.InteractivityCompleted;
import com.tilab.wade.performer.interactivity.ontology.InteractivityOntology;


public class GoRequesterBehaviour extends SimpleBehaviour {

	protected static Logger logger = Logger.getMyLogger(GoRequesterBehaviour.class.getName());
	
	public static final int UNDEFINED_STATUS  = -1;
	public static final int SUCCESS_STATUS  = 0;
	public static final int FAILURE_STATUS  = 1;
	public static final int SUSPENDED_STATUS  = 2;
	public static final int FROZEN_STATUS  = 3;
	
	private AID executor;
	private String sessionId;
	private Interaction prevInteraction;
	private Interaction nextInteraction;
	private Ontology interactivityOnto = InteractivityOntology.getInstance();
	private MessageTemplate interactionTemplate;
	private ACLMessage request;
	private int status = UNDEFINED_STATUS;
	private String error;
	private Exception nestedException;
	private boolean finished = false;
	
	public GoRequesterBehaviour(AID executor, String sessionId, Interaction interaction) {
		super(null);
		
		this.executor = executor;
		this.sessionId = sessionId;
		this.prevInteraction = interaction;

		// Prepare template
		interactionTemplate = MessageTemplate.and(
								MessageTemplate.and(
								MessageTemplate.MatchOntology(InteractivityOntology.getInstance().getName()), 
								MessageTemplate.MatchConversationId(sessionId)),
								new MessageTemplate(new MatchExpression() {
									public boolean match(ACLMessage msg) {
										return msg.getInReplyTo()==null;
									}
								}));
	}

	@Override
	public void onStart() {
		super.onStart();

		if (!finished) {
			try {
				// Delete all spurious messages that match the interaction template 
				while (myAgent.receive(interactionTemplate) != null);
				
				// Prepare request
				request = new ACLMessage(ACLMessage.REQUEST);
				request.setLanguage(LEAPCodec.NAME);
				request.setOntology(interactivityOnto.getName());
				request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				request.setConversationId(sessionId);
				request.addReceiver(executor);
				
				Action act = new Action(myAgent.getAID(), getAgentAction());
				myAgent.getContentManager().fillContent(request, act);

				// Send prev interaction
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE, "Send REQUEST(GO/BACK) for sessionId="+sessionId);
				}
				myAgent.send(request);
				 
			} catch (Exception e) {
				status = FAILURE_STATUS;
				error = e.getMessage();
				finished = true;
			}
		}
	}

	public int onEnd() {
		return super.onEnd();
	}

	public void action() {
		// Receive next interaction
		ACLMessage msg = myAgent.receive(interactionTemplate);
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
								Predicate p = (Predicate)myAgent.getContentManager().extractContent(msg);
								if (p instanceof Result) {
									// Result of GO action -> extract interaction
									if (logger.isLoggable(Logger.FINE)) {
										logger.log(Logger.FINE, "Receive INFORM(RESULT(GO)) for sessionId="+sessionId);
									}
									nextInteraction = (Interaction)((Result)p).getValue();
									status = SUCCESS_STATUS;
									error = null;
								}
								else if (p instanceof InteractivityCompleted) {
									nextInteraction = null;
									error = null;
									
									InteractivityCompleted ic = (InteractivityCompleted)p;
									if (ic.getReason() == InteractivityCompleted.Reason.PROGRAMMATING) {
										// Workflow interaction marked as completed -> return with null
										if (logger.isLoggable(Logger.FINE)) {
											logger.log(Logger.FINE, "Receive INFORM(INTERACTIVITY-COMPLETED-PROGRAMMATING) for sessionId="+sessionId);
										}
										status = SUCCESS_STATUS;
									}
									else if (ic.getReason() == InteractivityCompleted.Reason.SUSPENDED) {
										// Workflow is suspended -> throw exception
										if (logger.isLoggable(Logger.FINE)) {
											logger.log(Logger.FINE, "Receive INFORM(INTERACTIVITY-COMPLETED-SUSPENDED) for sessionId="+sessionId);
										}
										status = SUSPENDED_STATUS;
										error = ic.getSuspendMessage();
									}
									else if (ic.getReason() == InteractivityCompleted.Reason.FROZEN) {
										// Workflow is frozen -> throw exception
										if (logger.isLoggable(Logger.FINE)) {
											logger.log(Logger.FINE, "Receive INFORM(INTERACTIVITY-COMPLETED-FROZEN) for sessionId="+sessionId);
										}
										status = FROZEN_STATUS;
									}
								}
								else {
									throw new Exception("Predicate "+p+" not supported");
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
				}  // END !finished
			} // END synchronize
		}
		else {
			block();
		}
	}

	public boolean done() {
		return finished;
	}

	protected AgentAction getAgentAction() {
		return new Go(prevInteraction);
	}
	
	public Interaction getNextInteraction() {
		return nextInteraction;
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
