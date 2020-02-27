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

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.DFUtils;

import jade.content.ContentManager;
import jade.content.lang.leap.LEAPCodec;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.wrapper.gateway.DynamicJadeGateway;
import jade.wrapper.gateway.JadeGateway;

/**
 * This class represents a channel by means of which events can be submitted to the 
 * WADE Event System.
 * It can be used both within an agent and within a non-agent piece of code. In the latter
 * case it uses a JadeGateway to interact with the WADE Event System
 * 
 * @see GenericEvent
 */
public class EventChannel {
	private Connector myConnector; 
	private ContentManager myContentManager;
	
	private EventSource mySource;
	private AID eventSystemAgent;
	private String languageName = FIPANames.ContentLanguage.FIPA_SL;
	
	/**
	 * Creates an EventChannel to be used within an agent
	 * @param agent The agent that uses this EventChannel
	 */
	public EventChannel(Agent agent) {
		myConnector = new AgentBasedConnector(agent);
		initContentManager();
	}
	
	/**
	 * Creates an EventChannel to be used within a NON-agent piece of code
	 * @param gateway The JadeGateway that this EventChannel will use to interact with the 
	 * WADE Event System
	 */
	public EventChannel(DynamicJadeGateway gateway) {
		myConnector = new GatewayBasedConnector(gateway);
		initContentManager();
	}
	
	/**
	 * Creates an EventChannel to be used within a NON-agent piece of code. The created 
	 * EventChannel will use the default JadeGateway to interact with the WADE Event System
	 */
	public EventChannel() {
		this(JadeGateway.getDefaultGateway());
	}
	
	/**
	 * Set the source that will be indicated by this EventChannel when submitting events
	 * @param source The source that will be indicated by this EventChannel when submitting events
	 */
	public void setSource(EventSource source) {
		mySource = source;
	}
	
	/**
	 * Instruct this EventChannel to use a binary encoding for messages carrying submitted
	 * events. This is suggested when events may have big parameters such as images and 
	 * the like.
	 * @param b A boolean indication specifying whether or not binary encoding must be used.
	 */
	public void useBinaryEncoding(boolean b) {
		if (b) {
			// Use binary encoding (LEAP)
			languageName = LEAPCodec.NAME;
		}
		else {
			// Use normal human-readable encoding (SL)
			languageName = FIPANames.ContentLanguage.FIPA_SL;
		}
	}
	
	/**
	 * Submit an event to the WADE Event System
	 * @param ev The event to be submitted
	 * @throws Exception if an error occurs submitting the event
	 */
	public void submitEvent(GenericEvent ev) throws Exception {
		myConnector.sendEventMessage(ev);
	}
	
	private void initContentManager() {
		// Initialize java type preservation
		if (System.getProperty(SLCodec.PRESERVE_JAVA_TYPES) == null) {
			System.setProperty(SLCodec.PRESERVE_JAVA_TYPES, "true");
		}
		
		myContentManager = new ContentManager();
		myContentManager.registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
		myContentManager.registerLanguage(new LEAPCodec());
		myContentManager.registerOntology(EventOntology.getInstance());
	}
	
	private void sendEventMessage(GenericEvent ev, Agent a) throws Exception {
		if (eventSystemAgent == null) {
			eventSystemAgent = DFUtils.getAID(DFUtils.searchAnyByType(a, WadeAgent.ESA_AGENT_TYPE, null));
		}
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(eventSystemAgent);
		msg.setLanguage(languageName);
		msg.setOntology(EventOntology.getInstance().getName());
		msg.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
		
		Occurred occurred = new Occurred(System.currentTimeMillis(), ev);
		occurred.setSource(mySource);
		myContentManager.fillContent(msg, occurred);
		a.send(msg);
	}
	

	/**
	 * Inner interface Connector.
	 * This interface hides the differences in the code to execute when this EventChannel operates 
	 * inside an agent or inside a NON-agent piece of code.
	 */
	private interface Connector {
		void sendEventMessage(GenericEvent ev) throws Exception;
	} // END of inner interface Connector

	
	/**
	 * Inner class AgentBasedConnector
	 */
	private class AgentBasedConnector implements Connector {
		private Agent myAgent;
		
		AgentBasedConnector(Agent a) {
			myAgent = a;
		}

		public void sendEventMessage(GenericEvent ev) throws Exception {
			EventChannel.this.sendEventMessage(ev, myAgent);
		}
	} // END of inner class AgentBasedConnector

	
	/**
	 * Inner class AgentBasedConnector
	 */
	private class GatewayBasedConnector implements Connector {
		private DynamicJadeGateway myGateway;
		
		GatewayBasedConnector(DynamicJadeGateway gateway) {
			myGateway = gateway;
		}
		
		public void sendEventMessage(GenericEvent ev) throws Exception {
			SendEventMessageBehaviour b = new SendEventMessageBehaviour(ev);
			myGateway.execute(b);
			Exception exc = b.getException();
			if (exc != null) {
				throw exc;
			}
		}
	} // END of inner class GatewayBasedConnector
	
	
	/**
	 * Inner class SendEventMessageBehaviour
	 */
	private class SendEventMessageBehaviour extends OneShotBehaviour {
		private GenericEvent myEvent;
		private Exception exception;
		
		SendEventMessageBehaviour(GenericEvent ev) {
			super(null);
			myEvent = ev;
		}
		
		public void action() {
			try {
				EventChannel.this.sendEventMessage(myEvent, myAgent);
			}
			catch (Exception e) {
				exception = e;
			}
		}
		
		Exception getException() {
			return exception;
		}
	} // END of inner class SendEventMessageBehaviour
}
