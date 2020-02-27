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
package com.tilab.wade.event;

import jade.content.onto.Ontology;
import jade.content.schema.ObjectSchema;
import jade.core.Agent;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.ca.WadeClassLoader;
import com.tilab.wade.commons.EventType;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.webservice.OperationDescriptor;
import com.tilab.wade.performer.descriptors.webservice.PortDescriptor;
import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor;
import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor.ServiceUsage;
import com.tilab.wade.utils.OntologyUtils;

public class EventTypeManager {

	private static final Logger myLogger = Logger.getMyLogger(EventTypeManager.class.getName());

	/**
	 * Used to identify the purpose of ServiceDescriptor 
	 */
	public static final String EVENT_SENDER_TYPE = "EVENT_SENDER";

	private Agent agent;
	
	
	public EventTypeManager(Agent agent) {
		this.agent = agent;
	}
	
	/**
	 * Get the list of EventTypes including the custom event types (type.xml) and the 
	 * webservices events (ServiceDescriptor with purpose EVENT_SENDER)
	 * @return List of EventTypes
	 */
	public List<EventType> getEventTypes() throws Exception {
		List<EventType> eventTypes = new ArrayList<EventType>();
		
		try {
			// Get web-service events type
			ClassLoader cl = CAServices.getInstance(agent).getDefaultClassLoader();
			if (cl instanceof WadeClassLoader) {
				// We are in a container with a local CA 
				myLogger.log(Logger.INFO, "Loading Incoming WS event types");
				WadeClassLoader wcl = (WadeClassLoader) cl;
				jade.util.leap.List serviceDescriptorClassNames = wcl.getWebServiceDescriptorList();
				Map<String, ServiceDescriptor> eventServiceDescriptors = getEventServiceDescriptors(((jade.util.leap.ArrayList)serviceDescriptorClassNames).toList(), wcl);
				
				Iterator<ServiceDescriptor> eventServiceDescriptorIt = eventServiceDescriptors.values().iterator();
				while (eventServiceDescriptorIt.hasNext()) {
					ServiceDescriptor eventServiceDescriptor = eventServiceDescriptorIt.next();
					String serviceName = eventServiceDescriptor.getServiceName();
					myLogger.log(Logger.INFO, "Handling WS "+serviceName);
					Set<String> portNames = eventServiceDescriptor.getPortNames();
					for (String portName : portNames) {
						myLogger.log(Logger.INFO, "Handling Port "+portName);
						PortDescriptor portDescriptor = eventServiceDescriptor.getPortDescriptor(portName);
						
						Set<String> operationNames = portDescriptor.getOperationNames();
						for (String operationName : operationNames) {
							myLogger.log(Logger.INFO, "Handling Operation "+operationName);
							EventType webServiceEventType = getEventType(eventServiceDescriptor, portName, operationName);
							webServiceEventType.setOntology(eventServiceDescriptor.getOntology());
							buildEventTypeParametersSchema(webServiceEventType, wcl);
							
							eventTypes.add(webServiceEventType);
							myLogger.log(Logger.INFO, "Found event type "+webServiceEventType.getDescription());
						}
					}
				}
			}
			
			// Get custom events type
			myLogger.log(Logger.INFO, "Loading Custom event types");
			List customEventTypes = TypeManager.getInstance().getCustomEventTypes();
			for (Object objCustomEventType : customEventTypes) {
				EventType customEventType = (EventType)objCustomEventType;
				buildEventTypeParametersSchema(customEventType, null);
				
				eventTypes.add(customEventType);
				myLogger.log(Logger.INFO, "Found event type "+customEventType.getDescription());
			}
		} catch(Exception e) {
			myLogger.log(Logger.SEVERE, "Error building event-type list", e);
			throw e;
		}
		
		return eventTypes;
	}
	
	private static void buildEventTypeParametersSchema(EventType eventType, ClassLoader cl) throws Exception {
		List params = eventType.getParameters();
		if (params.size() > 0) {
			Ontology onto = eventType.getOntology();
	
			Iterator it = params.iterator();
			while (it.hasNext()) {
				Parameter param = (Parameter)it.next();
				ObjectSchema paramSchema = OntologyUtils.getParameterSchema(param, onto, cl);
				param.setSchema(paramSchema);
			}				
		}				
	}

	/**
	 * Get the EventType associated to specific service-port-operation WebServiceEvent
	 * @param serviceDescriptor service-descriptor instance 
	 * @param portName service port (can be null)
	 * @param operationName service operation
	 * @return the event-type
	 */
	public static EventType getEventType(ServiceDescriptor serviceDescriptor, String portName, String operationName) throws Exception {

		if (serviceDescriptor == null) {
			throw new Exception("Service descriptor not specified");
		}

		OperationDescriptor operationDescriptor;
		if (portName != null) {
			PortDescriptor portDescriptor = serviceDescriptor.getPortDescriptor(portName);
			if (portDescriptor == null) {
				throw new Exception("Port "+portName+" not present in service "+serviceDescriptor.getServiceName());
			}
			operationDescriptor = portDescriptor.getOperationDescriptor(operationName);
		} else {
			operationDescriptor = serviceDescriptor.getOperationDescriptor(operationName);
		}
		
		if (operationDescriptor == null) {
			throw new Exception("Operation "+operationName+" not present in port "+portName+" of service "+serviceDescriptor.getServiceName());
		}

		EventType serviceEventType = new EventType();
		String description = serviceDescriptor.getServiceName()+"."+portName+"."+operationName; 
		serviceEventType.setDescription(description);
		
		jade.util.leap.List operationFormalParams = operationDescriptor.getFormalParams();
		if (operationFormalParams != null) {
			List<Parameter> eventParams = new ArrayList<Parameter>(); 
			jade.util.leap.Iterator operationFormalParamIt = operationFormalParams.iterator();
			while(operationFormalParamIt.hasNext()) {
				Parameter operationFormalParam = (Parameter)operationFormalParamIt.next();
				if (operationFormalParam.getMode() == Constants.IN_MODE ||
					operationFormalParam.getMode() == Constants.INOUT_MODE) {
					Parameter eventParam = new Parameter();
					eventParam.setName(operationFormalParam.getName());
					eventParam.setType(operationFormalParam.getType());
					eventParams.add(eventParam);
				}
			}
			serviceEventType.setParameters(eventParams);
		}
		
		return serviceEventType;
	}
	
	/**
	 * Return a map serviceName/serviceDescriptor with only the serviceDescriptors of
	 * usage SERVER or CLIENT-SERVER and purpose EVENT_SENDER_TYPE
	 * @param serviceDescriptorClassNames class-name list of service descriptor
	 * @param cl ClassLoader (can be null)
	 * @return map serviceName-serviceDescriptor
	 * @throws Exception
	 */
	public static Map<String, ServiceDescriptor> getEventServiceDescriptors(List<String> serviceDescriptorClassNames, ClassLoader cl) {
		
		Map<String, ServiceDescriptor> eventServiceDescriptors = new HashMap<String, ServiceDescriptor>();
		Iterator<String> serviceDescriptorClassNameIt = serviceDescriptorClassNames.iterator();
		while (serviceDescriptorClassNameIt.hasNext()) {

			// Load descriptor class
			String serviceDescriptorClassName = serviceDescriptorClassNameIt.next();
			try {
				Class serviceDescriptorClass = Class.forName(serviceDescriptorClassName, true, cl);
				
				if (isEventSenderService(serviceDescriptorClass)) {
					ServiceDescriptor serviceDescriptor = (ServiceDescriptor) serviceDescriptorClass.newInstance();
					String serviceName = serviceDescriptor.getServiceName();
	
					eventServiceDescriptors.put(serviceName, serviceDescriptor);
				}
			} catch(Exception e) {
				myLogger.log(Logger.WARNING, "Problem loading service-descriptor "+serviceDescriptorClassName, e);
			}
		}
		return eventServiceDescriptors;
	}
	
	/**
	 * Return true if the service-descriptor represent a EVENT_SENDER_TYPE
	 */
	public static boolean isEventSenderService(Class serviceDescriptorClass) {
		return ServiceDescriptor.check(serviceDescriptorClass, ServiceUsage.SERVER, EVENT_SENDER_TYPE) ||
		ServiceDescriptor.check(serviceDescriptorClass, ServiceUsage.CLIENT_SERVER, EVENT_SENDER_TYPE);
	}
}
