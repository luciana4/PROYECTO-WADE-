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

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsTerm;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.util.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bsh.Interpreter;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.ca.WadeClassLoader;
import com.tilab.wade.commons.EventType;
import com.tilab.wade.esa.ontology.EventDescriptor;
import com.tilab.wade.esa.ontology.EventRegistrationDescriptor;
import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.EventTypeManager;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.performer.descriptors.Parameter;


class EventManager {

	private static final Logger myLogger = Logger.getMyLogger(EventManager.class.getName());;
	
	private Map<String, List<EventDescriptor>> eventsByType = new HashMap<String, List<EventDescriptor>>();
	private Map<String, List<EventRegistrationDescriptor>> eventRegistrationsByType = new HashMap<String, List<EventRegistrationDescriptor>>();
	private Map<String, EventType> eventTypes = new HashMap<String, EventType>();

	private EventSystemAgent esa;
	
	EventManager(EventSystemAgent esa) {
		this.esa = esa;
	}
	
	int getEventsCount() {
		int eventsCount = 0;
		Collection<List<EventDescriptor>> typedEvents = eventsByType.values();
		for (List<EventDescriptor> events : typedEvents) {
			eventsCount += events.size();
		}
		return eventsCount;
	}

	int getEventRegistrationsCount() {
		int eventRegistrationsCount = 0;
		Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
		for (List<EventRegistrationDescriptor> eventRegistrations : typedEventRegistrations) {
			eventRegistrationsCount += eventRegistrations.size();
		}
		return eventRegistrationsCount;
	}

	/**
	 * Add all event registrations to map  
	 */
	void addEventRegistrations(List<EventRegistrationDescriptor> erds) {
		synchronized (eventRegistrationsByType) {
			for (EventRegistrationDescriptor erd : erds) {
				addEventRegistration(erd);
			}
		}
	}

	
	/**
	 * Add a new event registration to map  
	 */
	void addEventRegistration(EventRegistrationDescriptor erd) {
		synchronized (eventRegistrationsByType) {
			String type = erd.getEventTemplate().getEventType(); 
			List<EventRegistrationDescriptor> eventRegistrations = eventRegistrationsByType.get(type);
			if (eventRegistrations == null) {
				eventRegistrations = new ArrayList<EventRegistrationDescriptor>();
				eventRegistrationsByType.put(type, eventRegistrations);
			}
	
			eventRegistrations.add(erd);
			
			myLogger.log(Logger.FINE, "Added event registration "+erd);
		}
	}
	
	/**
	 * Add a new event 
	 */
	void addEvent(EventDescriptor ed) {
		synchronized (eventsByType) {
			String type = ed.getEvent().getType();
			List<EventDescriptor> events = eventsByType.get(type);
			if (events == null) {
				events = new ArrayList<EventDescriptor>();
				eventsByType.put(type, events);
			}
	
			events.add(ed);
			myLogger.log(Logger.FINE, "Added event "+ed);
		}
	}

	/**
	 * Match event with registrations
	 * All matching registrations are returned and removed from map
	 * Break search at first exclusive registration
	 */
	List<EventRegistrationDescriptor> extractMatchingEventRegistrations(GenericEvent event) {
		synchronized (eventRegistrationsByType) {
			List<EventRegistrationDescriptor> matchings = new ArrayList<EventRegistrationDescriptor>();

			String type = event.getType();
			List<EventRegistrationDescriptor> eventRegistrations = eventRegistrationsByType.get(type);
			if (eventRegistrations != null) {
				List<String> groupIds = new ArrayList<String>();
				
				Iterator<EventRegistrationDescriptor> eventRegistrationsIterator = eventRegistrations.iterator();
				while (eventRegistrationsIterator.hasNext()) {
					EventRegistrationDescriptor eventRegistration = eventRegistrationsIterator.next();
					if (matchEvent(event, eventRegistration.getEventTemplate())) {
						String groupId = eventRegistration.getGroupId();

						// Add to the list of matching registration only the first registration of a group
						// (Send to wf only one event notification)
						if (groupId == null || !groupIds.contains(groupId)) {
							matchings.add(eventRegistration);
						}

						// If the registration belongs to a group mark it as already managed
						if (groupId != null) {
							groupIds.add(groupId);
						}

						// Remove the registration 
						eventRegistrationsIterator.remove();
						myLogger.log(Logger.FINE, "Removed event registration "+eventRegistration);
						
						// Break search at first exclusive registration
						if (eventRegistration.isExclusive()) {
							break;
						}
					}
				}
				
				// If some registrations belongs to groups remove all group registrations
				if (!groupIds.isEmpty()) {
					deleteEventGroupsRegistrations(groupIds);
				}
			}
			
			return matchings;
		}
	}
	
	/**
	 * Return the first event that match with template 
	 */
	EventDescriptor getMatchingEvent(EventTemplate eventTemplate) {
		synchronized (eventsByType) {
			String type = eventTemplate.getEventType();
			List<EventDescriptor> events = eventsByType.get(type);
			if (events != null) {
				for (EventDescriptor eventDescriptor : events) {
					GenericEvent event = eventDescriptor.getEvent();
					if (matchEvent(event, eventTemplate)) { 
						return eventDescriptor;
					}
				}
			}
		
			return null;
		}
	}

	/**
	 * Check the matching of event with template
	 */
	private boolean matchEvent(GenericEvent event, EventTemplate eventTemplate) {
		boolean match;
		
		// If no expression is specified the matching is true
		String eventIdentificationExpression = eventTemplate.getEventIdentificationExpression();
		if (eventIdentificationExpression == null || "".equals(eventIdentificationExpression)) {
			match = true;
		} 
		
		// If expression is specified interpreter it
		else {
			try {
				Interpreter bshInterpreter = new Interpreter();
				
				// Set the wade-classloader
				WadeClassLoader wcl = (WadeClassLoader)CAServices.getInstance(esa).getDefaultClassLoader();
				bshInterpreter.setClassLoader(wcl);
				
				// Get EventType
				EventType eventType = eventTypes.get(event.getType());
				if (eventType == null) {
					throw new Exception("Event type associated to "+event.getType()+" unknown");
				}
				
				// Add event parameters class into bsh namespace
				Ontology eventTypeOnto = eventType.getOntology();
				Iterator it = eventTypeOnto.getConceptNames().iterator();
				while(it.hasNext()) {
					String conceptName = (String)it.next();
					String className = eventTypeOnto.getClassForElement(conceptName).getName();
					bshInterpreter.getNameSpace().importClass(className);
				}
				
				// Set template parameter values
				List<Parameter> templateParameters = eventTemplate.getParams();
				for (Parameter templateParameter : templateParameters) {
					bshInterpreter.set(templateParameter.getName(), templateParameter.getValue());
				}
				
				// Adjust event parameter primitive value
				List<Parameter> eventParameters = event.getParams();
				if (eventParameters != null) {
					Map<String, Parameter> eventFormalParametersMap = eventType.getParametersMap();
					
					for (Parameter parameter : eventParameters) {
						Parameter formalParameter = eventFormalParametersMap.get(parameter.getName());
						if (formalParameter != null) {
							parameter.setValue(BasicOntology.adjustPrimitiveValue(parameter.getValue(), formalParameter.getTypeClass()));	
						}
					}
				}
				
				// Set event object
				bshInterpreter.set("event", event);
	
				// Evaluate expression
				match = (Boolean)bshInterpreter.eval(eventIdentificationExpression);
	
			} catch(Exception e) {
				myLogger.log(Logger.WARNING, "Error matching expression "+eventTemplate, e);
				match = false;
			}
		}
		
		if (match) {
			myLogger.log(Logger.FINE, "Matching found: event "+event+" with template "+eventTemplate);
		}
		return match;
	}

	/**
	 * Cleanup expired events 
	 */
	void cleanupEvents(long defaultEventTimeToLive) {
		synchronized (eventsByType) {
			Collection<List<EventDescriptor>> typedEvents = eventsByType.values();
			for (List<EventDescriptor> events : typedEvents) {
				Iterator<EventDescriptor> eventsIterator = events.iterator();
				while(eventsIterator.hasNext()) {
					EventDescriptor ed = eventsIterator.next();

					long eventTime = ed.getTime();
					long eventTimeToLeave = defaultEventTimeToLive;
					Date eventTimeToLeaveDate = ed.getEvent().getTimeToLeave();
					if (eventTimeToLeaveDate != null) {
						eventTimeToLeave = eventTimeToLeaveDate.getTime();
					}
					
					if (eventTimeToLeave > 0 && System.currentTimeMillis() >= (eventTime+eventTimeToLeave)) {
						myLogger.log(Logger.FINE, "Remove expired event "+ed);
						eventsIterator.remove();
					}
				}
			}
		}
	}
	
	StringBuilder dumpEvents() {
		StringBuilder sb = new StringBuilder();
		sb.append("-- Events list --");
		sb.append("\n");
		synchronized (eventsByType) {
			Collection<List<EventDescriptor>> typedEvents = eventsByType.values();
			for (List<EventDescriptor> events : typedEvents) {
				for (EventDescriptor ed : events) {
					sb.append("\t- Source: "+(ed.getSource()!=null?ed.getSource():"")+"\n");
					sb.append("\t- Date: "+new Date(ed.getTime())+"\n");
					GenericEvent event = (GenericEvent)ed.getEvent();
					sb.append("\t- Type: "+event.getType()+"\n");
					Date timeToLeave = event.getTimeToLeave();
					sb.append("\t- TimeToLeave: "+(timeToLeave!=null?timeToLeave:"")+"\n");
					sb.append("\t- Parameters\n");
					for (Parameter param : event.getParams()) {
						sb.append("\t\t- "+param.getName()+"="+param.getValue()+"\n");
					}
					sb.append("\n");
				}
			}
		}
		return sb;
	}

	StringBuilder dumpEventRegistrations() {
		StringBuilder sb = new StringBuilder();
		sb.append("-- Event registrations list --");
		sb.append("\n");
		synchronized (eventRegistrationsByType) {
			Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
			for (List<EventRegistrationDescriptor> eventRegistrations : typedEventRegistrations) {
				for (EventRegistrationDescriptor eventRegistration : eventRegistrations) {
					sb.append("\t- Agent: "+eventRegistration.getRegistrationMessage().getSender().getName()+"\n");
					sb.append("\t- ConversationId: "+eventRegistration.getRegistrationMessage().getConversationId()+"\n");
					sb.append("\t- RegistrationId: "+eventRegistration.getId()+"\n");
					sb.append("\t- Type: "+eventRegistration.getEventTemplate().getEventType()+"\n");
					String expression = eventRegistration.getEventTemplate().getEventIdentificationExpression();
					sb.append("\t- Expression: "+(expression!=null?expression:"")+"\n");
					sb.append("\t- Parameters\n");
					for (Parameter param : eventRegistration.getEventTemplate().getParams()) {
						sb.append("\t\t- "+param.getName()+"="+param.getValue()+"\n");
					}
					sb.append("\n");
				}
			}
		}
		return sb;
	}

	EventDescriptor deleteEvent(String eventId) {
		synchronized (eventsByType) {
			Collection<List<EventDescriptor>> typedEvents = eventsByType.values();
			for (List<EventDescriptor> events : typedEvents) {
				Iterator<EventDescriptor> eventsIterator = events.iterator();
				while(eventsIterator.hasNext()) {
					EventDescriptor event = eventsIterator.next();
					if (event.getId().equals(eventId)) {
						myLogger.log(Logger.FINE, "Removed event "+event);
						eventsIterator.remove();
						return event;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Remove a event registration 
	 */
	EventRegistrationDescriptor deleteEventRegistration(String registrationId) {
		EventRegistrationDescriptor matchedERD = null;
		
		synchronized (eventRegistrationsByType) {
			Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
			for (List<EventRegistrationDescriptor> eventRegistrations : typedEventRegistrations) {
				Iterator<EventRegistrationDescriptor> eventRegistrationsIterator = eventRegistrations.iterator();
				while(eventRegistrationsIterator.hasNext()) {
					EventRegistrationDescriptor eventRegistration = eventRegistrationsIterator.next();
					if (eventRegistration.getId().equals(registrationId)) {
						myLogger.log(Logger.FINE, "Removed event registration "+eventRegistration);
						matchedERD = eventRegistration;
						eventRegistrationsIterator.remove();
						break;
					}
				}
				
				if (matchedERD != null) {
					break;
				}
			}
		}
		
		// Remove all registrations for this group
		if (matchedERD != null && matchedERD.getGroupId() != null) {
			deleteEventGroupRegistrations(matchedERD.getGroupId());
		}
		
		return matchedERD;
	}

	/**
	 * Remove all registrations belongs to a group list 
	 */
	void deleteEventGroupsRegistrations(List<String> groupIds) {
		synchronized (eventRegistrationsByType) {
			Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
			for (List<EventRegistrationDescriptor> eventRegistrations : typedEventRegistrations) {
				Iterator<EventRegistrationDescriptor> eventRegistrationsIterator = eventRegistrations.iterator();
				while(eventRegistrationsIterator.hasNext()) {
					EventRegistrationDescriptor eventRegistration = eventRegistrationsIterator.next();
					if (groupIds.contains(eventRegistration.getGroupId())) {
						myLogger.log(Logger.FINE, "Removed event registration "+eventRegistration);
						eventRegistrationsIterator.remove();
					}
				}
			}
		}
	}

	/**
	 * Remove all registrations belongs to a group 
	 */
	void deleteEventGroupRegistrations(String groupId) {
		synchronized (eventRegistrationsByType) {
			Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
			for (List<EventRegistrationDescriptor> eventRegistrations : typedEventRegistrations) {
				Iterator<EventRegistrationDescriptor> eventRegistrationsIterator = eventRegistrations.iterator();
				while(eventRegistrationsIterator.hasNext()) {
					EventRegistrationDescriptor eventRegistration = eventRegistrationsIterator.next();
					if (groupId.equals(eventRegistration.getGroupId())) {
						myLogger.log(Logger.FINE, "Removed event registration "+eventRegistration);
						eventRegistrationsIterator.remove();
					}
				}
			}
		}
	}
	
	List<EventDescriptor> getEvents(String eventType, boolean convertToAbs) throws Exception {
		List<EventDescriptor> events = new ArrayList<EventDescriptor>();
		synchronized (eventsByType) {
			if (eventType != null) {
				List<EventDescriptor> typedEvents = eventsByType.get(eventType);
				if (typedEvents != null) {
					events.addAll(typedEvents);
				}
			} else {
				Collection<List<EventDescriptor>> typedEvents = eventsByType.values();
				for (List<EventDescriptor> eventsList : typedEvents) {
					events.addAll(eventsList);
				}
			}
		}
		
		// Adjust event parameters value (convert obj into abs)
		if (convertToAbs) {
			for (int i=0; i<events.size(); i++) {
				events.set(i, cloneEventDescriptor(events.get(i)));
			}
		}
		
		return events;
	}

	List<EventRegistrationDescriptor> getEventRegistrations(String eventType, boolean convertToAbs) throws Exception {
		List<EventRegistrationDescriptor> eventRegistrations = new ArrayList<EventRegistrationDescriptor>();
		synchronized (eventRegistrationsByType) {
			if (eventType != null) {
				List<EventRegistrationDescriptor> typedEventRegistrations = eventRegistrationsByType.get(eventType);
				if (typedEventRegistrations != null) {
					eventRegistrations.addAll(typedEventRegistrations);
				}
			} else {
				Collection<List<EventRegistrationDescriptor>> typedEventRegistrations = eventRegistrationsByType.values();
				for (List<EventRegistrationDescriptor> eventregistrationsList : typedEventRegistrations) {
					eventRegistrations.addAll(eventregistrationsList);
				}
			}
		}

		// Adjust event-template parameters value (convert obj into abs)
		if (convertToAbs) {
			for (int i=0; i<eventRegistrations.size(); i++) {
				eventRegistrations.set(i, cloneEventRegistrationDescriptor(eventRegistrations.get(i)));
			}
		}
		
		return eventRegistrations;
	}
	
	private EventRegistrationDescriptor cloneEventRegistrationDescriptor(EventRegistrationDescriptor erd) throws Exception {
		EventRegistrationDescriptor clonedEventRegistrationDescriptor = new EventRegistrationDescriptor();
		clonedEventRegistrationDescriptor.setEventTemplate(cloneEventTemplate(erd.getEventTemplate()));
		clonedEventRegistrationDescriptor.setExclusive(erd.isExclusive());
		clonedEventRegistrationDescriptor.setExecutionId(erd.getExecutionId());
		clonedEventRegistrationDescriptor.setId(erd.getId());
		clonedEventRegistrationDescriptor.setGroupId(erd.getGroupId());
		clonedEventRegistrationDescriptor.setRegistrationMessage(erd.getRegistrationMessage());
		return clonedEventRegistrationDescriptor;
	}

	private EventTemplate cloneEventTemplate(EventTemplate et) throws Exception {
		EventTemplate clonedEventTemplate = new EventTemplate();
		clonedEventTemplate.setEventIdentificationExpression(et.getEventIdentificationExpression());
		clonedEventTemplate.setEventType(et.getEventType());

		Ontology onto = et.getOntology();
		if (onto == null) {
			throw new Exception("Ontology not found for event event-template " + et);
		}
		
		List<Parameter> clonedParams = new ArrayList<Parameter>();
		for (Parameter p : et.getParams()) {
			clonedParams.add(cloneParameter(p, onto));
		}
		clonedEventTemplate.setParams(clonedParams);
		
		return clonedEventTemplate;
	}

	private EventDescriptor cloneEventDescriptor(EventDescriptor ed) throws Exception {
		EventDescriptor clonedEventDescriptor = new EventDescriptor();
		clonedEventDescriptor.setId(ed.getId());
		clonedEventDescriptor.setSource(ed.getSource());
		clonedEventDescriptor.setTime(ed.getTime());
		clonedEventDescriptor.setEvent(cloneGenericEvent(ed.getEvent()));
		return clonedEventDescriptor;	
	}

	private GenericEvent cloneGenericEvent(GenericEvent ge) throws Exception {
		GenericEvent clonedGenericEvent = new GenericEvent();
		clonedGenericEvent.setType(ge.getType());
		clonedGenericEvent.setTimeToLeave(ge.getTimeToLeave());
		
		EventType eventType = getEventType(ge);
		Ontology onto = eventType.getOntology();
		if (onto == null) {
			throw new Exception("Ontology not found for event type " + eventType);
		}
		
		List<Parameter> clonedParams = new ArrayList<Parameter>();
		for (Parameter p : ge.getParams()) {
			clonedParams.add(cloneParameter(p, onto));
		}
		clonedGenericEvent.setParams(clonedParams);

		return clonedGenericEvent;
	}
	
	private Parameter cloneParameter(Parameter p, Ontology onto) throws OntologyException {
		Parameter clonedParameter = new Parameter();
		clonedParameter.setName(p.getName());
		clonedParameter.setType(p.getType());
		clonedParameter.setSchema(p.getSchema());
		
		// Convert parameter value in abs-object
		AbsObject absValue;
		Object value = p.getValue(); 
		if (value != null) {
			Class valueClass = value.getClass();
			if (valueClass.isArray() && valueClass != byte[].class) {
				absValue = new AbsAggregate(BasicOntology.SEQUENCE);
				for (int i = 0; i < Array.getLength(value); i++) {
					Object elementValue = Array.get(value, i);
					AbsObject elementAbsValue = onto.fromObject(elementValue);
					((AbsAggregate)absValue).add((AbsTerm)elementAbsValue);
				}
			} else {
				absValue = onto.fromObject(value);
			}
			clonedParameter.setValue(absValue);
		}
		
		return clonedParameter;
	}
	
	EventType getEventType(GenericEvent ge) throws Exception {
		myLogger.log(Logger.FINER, "Searching for event type " + ge.getType());
		EventType eventType = eventTypes.get(ge.getType());
		if (eventType == null) {
			throw new Exception("Unknown EventType " + ge.getType());
		}
		return eventType;
	}

	List<EventType> getEventTypes() {
		List<EventType> eventTypesList = new ArrayList<EventType>();
		Collection<EventType> values = eventTypes.values();
		for (EventType eventType : values) {
			eventTypesList.add(eventType);
		}
		return eventTypesList;
	}

	void buildEventTypeMap() {
		eventTypes.clear();

		try {
			EventTypeManager etm = new EventTypeManager(esa);
			List<EventType> eventTypeList = etm.getEventTypes();
			for (EventType eventType : eventTypeList) {
				eventTypes.put(eventType.getDescription(), eventType);
			}
		} catch(Exception e) {
			myLogger.log(Logger.WARNING, "Error building event-type map", e);
		}
	}
}
