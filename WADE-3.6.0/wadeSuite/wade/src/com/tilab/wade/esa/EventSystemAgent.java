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
package com.tilab.wade.esa;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.ContentElementList;
import jade.content.Predicate;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OntologyServer;
import jade.core.behaviours.TickerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;

import com.tilab.wade.ca.ontology.Deployed;
import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.Reverted;
import com.tilab.wade.ca.ontology.Undeployed;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.EventType;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.esa.ontology.DeleteEvent;
import com.tilab.wade.esa.ontology.DeleteRegistration;
import com.tilab.wade.esa.ontology.EventDescriptor;
import com.tilab.wade.esa.ontology.EventManagementOntology;
import com.tilab.wade.esa.ontology.EventRegistrationDescriptor;
import com.tilab.wade.esa.ontology.GetEventTypes;
import com.tilab.wade.esa.ontology.GetEvents;
import com.tilab.wade.esa.ontology.GetRegistrations;
import com.tilab.wade.esa.ontology.UnlockRegistration;
import com.tilab.wade.event.EventOntology;
import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.Match;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.GUIDGenerator;

public class EventSystemAgent extends WadeAgentImpl {

	private static final long serialVersionUID = -4329423177345161593L;

	private static final long EVENTS_CLEANUP_PERIOD_DEFAULT = 5 * 60 * 1000; // 5 minutes (0=disable)
	private static final String EVENTS_CLEANUP_PERIOD_KEY = "eventsCleanupPeriod";

	private static final long DEFAULT_EVENT_TIME_TO_LEAVE_DEFAULT = 1 * 60 * 60 * 1000; // 1 hour (0=don't remove)
	private static final String DEFAULT_EVENT_TIME_TO_LEAVE_KEY = "defaultEventTimeToLeave";

	private EventManager eventManager;
	private long eventsCleanupPeriod;
	private long defaultEventTimeToLive;
		

	///////////////////////////////////////////////////
	// Agent attributes
	///////////////////////////////////////////////////
	@AttributeGetter(name="Events count")
	public int getEventsCount() {
		return getEventManager().getEventsCount();
	}

	@AttributeGetter(name="Event registrations count")
	public int getEventRegistrationsCount() {
		return getEventManager().getEventRegistrationsCount();
	}

	@AttributeGetter(name="Events cleanup period")
	public long getEventsCleanpPeriod() {
		return eventsCleanupPeriod;
	}

	@AttributeGetter(name="Default event time-to-leave")
	public long getDefaultEventTimeToLive() {
		return defaultEventTimeToLive;
	}
	
	@Override
	protected void agentSpecificSetup() throws AgentInitializationException {
		
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(EventManagementOntology.getInstance());
		getContentManager().registerOntology(DeploymentOntology.getInstance());
		
		// Add the behaviour listening to events (inform) from EventChannel and 
		// registrations (request_when) from EventSubcriber
		OntologyServer eventManagementOntologyServer = new OntologyServer(this, EventManagementOntology.getInstance(), ACLMessage.REQUEST, this);
		addBehaviour(eventManagementOntologyServer);

		// Add the behaviour listening all EventManagementOntology actions
		OntologyServer eventOntologyServer = new OntologyServer(this, EventOntology.getInstance(), new int[] {ACLMessage.REQUEST_WHEN, ACLMessage.INFORM, ACLMessage.CANCEL}, this) {
			protected void handleMessage(ACLMessage msg) {
				if (msg.getPerformative() == ACLMessage.CANCEL) {
					// Tramite l'EventOntology con la performativa CANCEL è possibile cancellare una registrazione 
					// e tutto l'eventuale gruppo associato.
					// Questa modalità è utilizzata in caso di timeout o interruzione  della activity 
					String registrationId = msg.getContent();
					getEventManager().deleteEventRegistration(registrationId);
				} else {
					super.handleMessage(msg);
				}
			}
		};
		addBehaviour(eventOntologyServer);
		
		
		// Subscribe to the DEPLOYER topic to be notified about new jar deployed
		AID deployTopic = registerToTopic(DeploymentOntology.DEPLOY_TOPIC);
		addBehaviour(new DeployListener(this, deployTopic));
	}	
	
	private EventManager getEventManager() {
		if (eventManager == null) {
			// Lazy initialization to properly deal with situations where the ESA is started 
			// together with the ControlAgent
			myLogger.log(Logger.INFO, "Agent "+getName()+" - Initializing EventManager");
			eventManager = new EventManager(this);
			// Build event-type map
			eventManager.buildEventTypeMap();
			// Add the behaviour to cleanup expired events
			eventsCleanupPeriod = getLongArgument(EVENTS_CLEANUP_PERIOD_KEY, EVENTS_CLEANUP_PERIOD_DEFAULT);
			if (eventsCleanupPeriod > 0) {
				defaultEventTimeToLive = getLongArgument(DEFAULT_EVENT_TIME_TO_LEAVE_KEY, DEFAULT_EVENT_TIME_TO_LEAVE_DEFAULT);
				TickerBehaviour eventsCleanupBehaviour = new TickerBehaviour(this, eventsCleanupPeriod) {
					
					@Override
					protected void onTick() {
						eventManager.cleanupEvents(defaultEventTimeToLive);
					}
				};
				addBehaviour(eventsCleanupBehaviour);
			} else {
				myLogger.log(Logger.INFO, "Agent "+getName()+" - Expired events cleanup mechanism disabled");
			}
		}
		return eventManager;
	}
	
	public void serveContentElementListRequestWhen(ContentElementList cel, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving multiple events registration from workflow");
		
		String groupId = GUIDGenerator.getGUID();
		List<EventRegistrationDescriptor> erds = new ArrayList<EventRegistrationDescriptor>();
		boolean matched = false;
		Iterator it = cel.iterator();
		while (it.hasNext()) {
			Match match = (Match)it.next();
			
			EventRegistrationDescriptor erd = manageMatch(groupId, match, msg);
			if (erd != null) {
				erds.add(erd);
			} else {
				matched = true;
				break;
			}
		}
		
		if (!matched) {
			// Register new templates
			getEventManager().addEventRegistrations(erds);
			
			// Send AGREE 
			notifyRegistration(msg, erds.get(0).getId());
		}
	}
	
	public void serveMatchRequestWhen(Match match, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving event registration from workflow " + match.getExecutionId());
		
		EventRegistrationDescriptor erd = manageMatch(null, match, msg);
		if (erd != null) {
			// Register new template and send back the registrationId
			getEventManager().addEventRegistration(erd);
			
			// Send AGREE 
			notifyRegistration(msg, erd.getId());
		}
	}
	
	private EventRegistrationDescriptor manageMatch(String groupId, Match match, ACLMessage msg) {
		EventDescriptor eventDescriptor = null;
		if (!match.getFutureEventsOnly()) {
			// Check if a matching event is already present
			eventDescriptor = getEventManager().getMatchingEvent(match.getEventTemplate());
		}
		if (eventDescriptor != null) {
			// Send INFORM to unlock workflow
			notifyEvent(msg, eventDescriptor.getTime(), eventDescriptor.getEvent());
			
			// Check if remove the event
			if (match.getExclusive()) {
				getEventManager().deleteEvent(eventDescriptor.getId());
			}
			
			// No registration request
			return null;
		} else {
			// Prepare new template for registration
			return EventRegistrationDescriptor.create(groupId, match, msg);
		}
	}
	
	public void serveOccurredInform(Occurred o, ACLMessage msg) throws Exception {
		// Get event-type 
		GenericEvent event = (GenericEvent)o.getEvent();
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Occurred event " + event);
		EventType eventType = getEventManager().getEventType(event);
		
		// Manage tag property
		List<Property> eventProperties = event.getProperties();
		if (eventProperties == null) {
			eventProperties = new ArrayList<Property>();
			event.setProperties(eventProperties);
		}
		Property eventTagProperty = new Property(EventTemplate.TAG_PROPERTY, null);
		eventProperties.add(eventTagProperty);
		
		// Adjust event parameters value (convert abs into obj)
		List<Parameter> eventParams = event.getParams();
		for (Parameter eventParam : eventParams) {
			Object paramValue = eventParam.getValue();
			if (paramValue != null && paramValue instanceof AbsObject) {
				AbsObject absValue = AbsHelper.nullifyVariables((AbsObject)paramValue, false);
				paramValue = eventType.getOntology().toObject(absValue);
				eventParam.setValue(paramValue);
			}
		}

		// Match event to all template and send event to matching subscriber
		boolean exclusive = false;
		List<EventRegistrationDescriptor> matchingEventRegistrations = getEventManager().extractMatchingEventRegistrations(event);
		for (EventRegistrationDescriptor eventRegistration : matchingEventRegistrations) {
			myLogger.log(Logger.INFO, "Agent "+getName()+" - Notifying event "+event.getType()+" to workflow "+eventRegistration.getExecutionId());
			
			// Manage event tag		
			String tag = eventRegistration.getEventTemplate().getTag();
			eventTagProperty.setValue(tag);
			
			notifyEvent(eventRegistration.getRegistrationMessage(), o);
			
			if (eventRegistration.isExclusive()) {
				exclusive = true;
			}
		}
		
		if (!exclusive) {
			// Store new event
			EventDescriptor ed = EventDescriptor.create(o) ;
			getEventManager().addEvent(ed);
		}
	}
	
	public void serveDeleteEventRequest(DeleteEvent deleteEvent, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving DeleteEvent action " + deleteEvent);
		
		try {
			getEventManager().deleteEvent(deleteEvent.getEventId());
			reply(msg, ACLMessage.INFORM, deleteEvent, null);
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving DeleteEvent action " + deleteEvent, e);
			throw e;
		}
	}
	
	public void serveDeleteRegistrationRequest(DeleteRegistration deleteRegistration, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving DeleteRegistration action " + deleteRegistration);
		
		try {
			getEventManager().deleteEventRegistration(deleteRegistration.getRegistrationId());
			reply(msg, ACLMessage.INFORM, deleteRegistration, null);
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving DeleteRegistration action " + deleteRegistration, e);
			throw e;
		}
	}
	
	public void serveGetEventTypesRequest(GetEventTypes getEventTypes, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetEventTypes action " + getEventTypes);	

		try {
			List<EventType> eventTypes = getEventManager().getEventTypes();
		
			reply(msg, ACLMessage.INFORM, getEventTypes, eventTypes);
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetEventTypes action " + getEventTypes, e);
			throw e;
		}
	}
	
	public void serveGetEventsRequest(GetEvents getEvents, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetEvents action " + getEvents);
		
		try {
			List<EventDescriptor> events = getEventManager().getEvents(getEvents.getEventType(), getEvents.isConvertToAbs());
			
			reply(msg, ACLMessage.INFORM, getEvents, events);
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetEvents action " + getEvents, e);
			throw e;
		}
	}

	public void serveGetRegistrationsRequest(GetRegistrations getRegistrations, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetRegistrations action " + getRegistrations);

		try {
			List<EventRegistrationDescriptor> events = getEventManager().getEventRegistrations(getRegistrations.getEventType(), getRegistrations.isConvertToAbs());
			
			reply(msg, ACLMessage.INFORM, getRegistrations, events);
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetRegistrations action " + getRegistrations, e);
			throw e;
		}
	}

	public void serveUnlockRegistrationRequest(UnlockRegistration unlockRegistration, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving UnlockRegistration action " + unlockRegistration);

		try {
			EventRegistrationDescriptor registration = getEventManager().deleteEventRegistration(unlockRegistration.getRegistrationId());
			if (registration != null) {
				// Notify a dummy event to unlock the workflow
				notifyEvent(registration.getRegistrationMessage(), null);

				reply(msg, ACLMessage.INFORM, unlockRegistration, null);
			} else {
				// The registration is not present -> FAILURE
				reply(msg, ACLMessage.FAILURE, unlockRegistration, "Registration not present");
			}
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving UnlockRegistration action " + unlockRegistration, e);
			throw e;
		}
	}
	
	private void notifyEvent(ACLMessage registrationMessage, long time, GenericEvent event) {
		Occurred o = new Occurred(time, event);
		notifyEvent(registrationMessage, o);
	}

	private void notifyEvent(ACLMessage registrationMessage, Occurred occurred) {
		try {
			ACLMessage message = registrationMessage.createReply();
			message.setPerformative(ACLMessage.INFORM);
			
			if (occurred != null) {
				getContentManager().fillContent(message, occurred);
			}
			
			send(message);
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Notify event, message="+message);
		} catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error notifying event "+occurred, e);
		}
	}

	private void notifyRegistration(ACLMessage registrationMessage, String registrationId) {
		try {
			ACLMessage message = registrationMessage.createReply();
			message.setPerformative(ACLMessage.AGREE);
			message.setContent(registrationId);
			send(message);
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Notify registration, message="+message);
		} catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error notifying registration for registrationId="+registrationId, e);
		}
	}
	
	private void reply(ACLMessage request, int performative, AgentAction agentAction, Object result) {
		ACLMessage reply = request.createReply();
		reply.setPerformative(performative);

		if (performative == ACLMessage.INFORM) {
			if (result != null) {
				Action action = new Action(getAID(), agentAction);
				ContentElement ce = new Result(action, result);

				try {
					getContentManager().fillContent(reply, ce);
				} catch (Exception e) {
					// Should never happen
					myLogger.log(Level.SEVERE, "Agent "+getName()+" - Error encoding reply", e);
					performative = ACLMessage.FAILURE;
					reply.setContent("Unexpected error: "+e.getMessage());
				}
			}
		} else {
			// FAILURE response
			if (result != null && result instanceof String) {
				reply.setContent((String)result);
			}
		}
		
		send(reply);
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append(getEventManager().dumpEvents());
		sb.append(getEventManager().dumpEventRegistrations());
		return sb.toString();
	}
	
	private AID registerToTopic(String topicName) throws AgentInitializationException {
		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
			AID topic = topicHelper.createTopic(topicName);
			topicHelper.register(topic);
			return topic;
		} catch (ServiceException se) {
			throw new AgentInitializationException("Agent " + getName() + ": Error registering to topic "+topicName, se);
		}
	}

	
	
	/**
	 * Inner class DeployerListener
	 * This behaviour is responsible for listening to jarDeployer
	 */
	private class DeployListener extends CyclicBehaviour {
		private MessageTemplate template;
		
		private DeployListener(Agent myAgent, AID deployTopic) {
			template = MessageTemplate.MatchTopic(deployTopic);
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				try {
					Predicate p = (Predicate)myAgent.getContentManager().extractContent(msg);
					
					boolean rebuildClassLoader = false;
					if (p instanceof Deployed) {
						rebuildClassLoader = ((Deployed)p).isRebuildedClassLoader();
					}
					if (p instanceof Undeployed) {
						rebuildClassLoader = ((Undeployed)p).isRebuildedClassLoader();
					}
					if (p instanceof Reverted) {
						rebuildClassLoader = ((Reverted)p).isRebuildedClassLoader();
					}
					
					if (rebuildClassLoader) {
						EventSystemAgent esa = (EventSystemAgent)myAgent;
						esa.getEventManager().buildEventTypeMap();
					}
				}
				catch (Exception e) {
					myLogger.log(Level.WARNING, "Agent "+getName()+" - Error decoding deploy notification", e);
				}
			}
			else {
				block();
			}
		}
	}  // END of inner class DeployListener
}
