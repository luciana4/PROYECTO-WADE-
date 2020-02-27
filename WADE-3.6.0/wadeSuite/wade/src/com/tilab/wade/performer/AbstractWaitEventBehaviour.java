package com.tilab.wade.performer;

import jade.content.ContentElement;
import jade.core.AID;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.event.EventOntology;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.GUIDGenerator;

public abstract class AbstractWaitEventBehaviour extends BaseWaitBehaviour {
	private static final long serialVersionUID = 87686874687L;

	protected long timeout;
	protected boolean timeoutExpired = false;
	private ACLMessage registrationMsg;
	private String registrationId;
	private transient EventReceiver eventReceiver;
	
	public AbstractWaitEventBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	public AbstractWaitEventBehaviour(String name, WorkflowBehaviour owner, boolean hasDedicatedMethods) {
		super(name, owner, hasDedicatedMethods);
	}
	
	public void setTimeout(long timeout){
		this.timeout = timeout; 
	}
	
	protected abstract ContentElement prepareRegistrationContent();
	
	protected abstract void handleOccurredEvent(Occurred occurred);
	
	@Override
	public void init() throws Exception {
		// Register to the EventSystemAgent to be notified about the first event matching  the specified template(s).
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
			
			ContentElement ce = prepareRegistrationContent();
			
			myAgent.getContentManager().fillContent(registrationMsg, ce);
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
					
					// Il registrationId viene utilizzato solo internamente per eliminare le registrazione di questo gruppo
					// in caso di timeout o interruzione dell'activity.
					// Il registrationId in caso di gruppo è relativo alla prima registrazione.
					// Non viene restituito l'id di gruppo per ottimizzare il comportamento dell'ESA
					// (la presenza di un gruppo di registrazioni implica scorrersi tutte le registrazioni quando 
					// arrivato un evento che matcha devo andare ad eliminare tutte le registrazioni del gruppo, nel
					// caso di registrazione singola lo spazzolamento non è necessario)
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
	public void reset() {
		super.reset();
		registrationMsg = null;
		registrationId = null;
		timeoutExpired = false;
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
				handleOccurredEvent(occurred);
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
