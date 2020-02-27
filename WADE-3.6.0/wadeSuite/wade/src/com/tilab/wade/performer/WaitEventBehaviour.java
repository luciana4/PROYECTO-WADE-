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

import jade.core.AID;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.event.EventOntology;
import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.Match;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.GUIDGenerator;

/**
 * The behaviour representing a workflow "activity" whose execution corresponds to suspending until a given 
 * custom event happens.
 * Such activity holds two building blocks corresponding to the Event Template specified 
 * to identify the event to wait for and the Event itself. These building blocks can be 
 * retrieved by means of the <code>getBuildingBlock(String id)</code> method passing the
 * "EVENT_TEMPLATE" and "EVENT" ids respectively.
 */
public class WaitEventBehaviour extends BaseWaitBehaviour {	
	private static final long serialVersionUID = 746786876L;

	private EventTemplateBB templateBB;
	private GenericEventBB eventBB;
	private boolean timeoutExpired = false;
	private ACLMessage registrationMsg;
	private String registrationId;
	private boolean exclusive = false;
	private boolean futureEventsOnly = false;
	private transient EventReceiver eventReceiver;
	private long timeout;
	
	
	public WaitEventBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	public WaitEventBehaviour(String name, WorkflowBehaviour owner, boolean hasDedicatedMethods) {
		super(name, owner, hasDedicatedMethods);
		
		templateBB = new EventTemplateBB(new EventTemplate(), this);
	}
	
	public void setEventType(String eventType) {
		templateBB.getEventTemplate().setEventType(eventType);
	}
	
	public void setEventIdentificationExpression(String eventIdentificationExpression) {
		templateBB.getEventTemplate().setEventIdentificationExpression(eventIdentificationExpression);
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public void setFutureEventsOnly(boolean futureEventsOnly) {
		this.futureEventsOnly = futureEventsOnly;
	}
	
	public void setTimeout(long timeout){
		this.timeout = timeout; 
	}
	
	/**
	 * Create a MethodInvocator suitable to call the beforeXXX() method with the EventTemplate as parameter
	 */
	@Override
	protected MethodInvocator createBeforeMethodInvocator(String beforeMethodName) {
		// At this time the template object has not been initialized yet
		return new MethodInvocator(owner, beforeMethodName, null, EventTemplate.class) {
			private static final long serialVersionUID = 486345624L;
			
			@Override
			protected Object[] getMethodParams() {
				return new Object[]{templateBB.getEventTemplate()};
			}
		};
	}

	/**
	 * Create a MethodInvocator suitable to call the afterXXX() method with the received GenericEvent as parameter
	 * or null if the timeout was expired
	 */
	@Override
	protected MethodInvocator createAfterMethodInvocator(String afterMethodName) {
		// At this time the occurred object (and therefore the event) has not been initialized yet
		return new MethodInvocator(owner, afterMethodName, null, GenericEvent.class) {
			private static final long serialVersionUID = 98795874L;
			
			@Override
			protected Object[] getMethodParams() {
				return new Object[]{timeoutExpired ? null : eventBB.getGenericEvent()};
			}
		};
	}
	
	@Override
	protected void manageBindings() throws Exception {
		owner.manageBindings(templateBB);
	}
	
	@Override
	protected void manageOutputBindings() throws Exception {
		owner.manageOutputBindings(templateBB);
	}
	
	@Override
	public BuildingBlock getBuildingBlock(String id) {
		if (EventTemplateBB.ID.equals(id)) {
			return templateBB;
		}
		else if (GenericEventBB.ID.equals(id)) {
			return eventBB;
		}
		
		return null;
	}
	
	public void init() throws Exception {
		// Register to the EventSystemAgent to be notified about the first event matching  the specified template.
		// If such an event has already been received we will get back an INFORM containing the event itself.
		// In that case store the event and terminate.
		// Otherwise we will get back an AGREE. In that case add a behaviour to manage the event as soon as it will happen
		// and suspend
		registrationMsg = new ACLMessage(ACLMessage.REQUEST_WHEN);
		registrationMsg.setOntology(EventOntology.getInstance().getName());
		registrationMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		String convId = GUIDGenerator.getGUID();
		registrationMsg.setConversationId(convId);
		AID esa = DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.ESA_AGENT_TYPE, null));
		if (esa != null) {
			registrationMsg.addReceiver(esa);
			
			Match match = new Match(templateBB.getEventTemplate(), owner.getExecutionId(), exclusive, futureEventsOnly);
			
			myAgent.getContentManager().fillContent(registrationMsg, match);
			myAgent.send(registrationMsg);
			ACLMessage reply = myAgent.blockingReceive(MessageTemplate.MatchConversationId(convId), 60000);
			if (reply != null) {
				switch (reply.getPerformative()) {
				case ACLMessage.INFORM:
					// Event already happened
					storeEvent(reply);
					break;
				case ACLMessage.AGREE:
					// Event not happened yet
					registrationId = reply.getContent();
					eventReceiver = new EventReceiver(reply.getConversationId(), timeout);
					myAgent.addBehaviour(eventReceiver);
					break;
				case ACLMessage.FAILURE:
					// Error
					handleFailure(reply);
					break;
				}
			}
			else {
				throw new Exception("No reply received from Event System Agent "+esa.getLocalName()+" in due time");
			}
		}
		else {
			throw new Exception("Event System Agent not found");
		}
	}
	
	@Override
	public boolean checkCompleted() {
		return eventBB != null || timeoutExpired;
	}

	@Override
	public void reset() {
		super.reset();
		templateBB.reset();
		eventBB = null;
		timeoutExpired = false;
		registrationMsg = null;
		registrationId = null;
	}


	/**
	 * Inner class EventReceiver
	 */
	private class EventReceiver extends ParallelBehaviour {
		private static final long serialVersionUID = 234274750685L;

		private MessageTemplate msgTemplate;
		
		public EventReceiver(String convId, long timeout) {
			super(null, ParallelBehaviour.WHEN_ANY);

			msgTemplate = MessageTemplate.MatchConversationId(convId);
			
			// First child: the behaviour waiting for the event notification
			addSubBehaviour(new SimpleBehaviour(null) {
				private static final long serialVersionUID = 83683451984L;
				
				private boolean finished = false;
				
				public void action() {
					ACLMessage msg = myAgent.receive(msgTemplate);
					if (msg != null) {
						if (msg.getPerformative() == ACLMessage.INFORM) {
							storeEvent(msg);
						}
						else {
							// Error
							handleFailure(msg);
						}
						finished = true;
					}
					else {
						block();
					}
				}
				
				public boolean done() {
					return finished;
				}
			});
			// Second child: watchDog (only if timeout > 0)
			if (timeout > 0) {
				addSubBehaviour(new WakerBehaviour(null, timeout) {
					private static final long serialVersionUID = 98372367L;

					public void onWake() {
						handleTimeout();
					}
				});
			}
		}
		
		public int onEnd() {
			owner.resume();
			return super.onEnd();
		}
	} // END of inner class EventReceiver
	
	private void storeEvent(ACLMessage inform) {
		if (inform.getContent() == null) {
			// Il behaviour è stato sbloccato con l'azione UnlockRegistration,
			// l'occurred non è presente e concettualmente è come se fosse scattato il timeout
			timeoutExpired = true;
		} else {
			try {
				Occurred occurred = (Occurred) myAgent.getContentManager().extractContent(inform);
				eventBB = new GenericEventBB((GenericEvent)occurred.getEvent(), this);
			}
			catch (Exception e) {
				handleException(e);
			}
		}
	}
	
	private void handleFailure(ACLMessage failure) {
		if (failure.getSender().equals(myAgent.getAMS())) {
			handleException(new Exception("Event System Agent does not exist"));
		}
		else {
			handleException(new Exception(failure.getContent()));
		}
	}
	
	private void handleTimeout() {
		timeoutExpired = true;
		
		// Cancel the registration with the Event System Agent
		cancelRegistration();
	}
	
	@Override
	public void setInterrupted() {
		super.setInterrupted();
		
		// Cancel the registration with the Event System Agent
		cancelRegistration();
		
		// We were just interrupted. In case our EventReceiver is running, abort it
		if (eventReceiver != null) {
			myAgent.removeBehaviour(eventReceiver);
		}
	}
	
	private void cancelRegistration() {
		// Cancel the registration with the Event System Agent
		registrationMsg.setPerformative(ACLMessage.CANCEL);
		registrationMsg.setContent(registrationId);
		myAgent.send(registrationMsg);
	}
}
