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
package com.tilab.wade.commons;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.content.onto.OntologyException;
import jade.content.AgentAction;
import jade.content.lang.Codec;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

import com.tilab.wade.commons.ontology.*;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.locale.MessageCode;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 21-giu-2006
 * Time: 18.02.48
 * To change this template use File | Settings | File Templates.
 */
public class WadeBasicResponder extends CyclicBehaviour {
	private MessageTemplate template;
	private Map<Class, ActionHandler> handlers = new HashMap<Class, ActionHandler>();
	private jade.util.Logger myLogger = jade.util.Logger.getMyLogger(getClass().getName());
	
	/**
	 * Inner interface ActionHandler
	 */
	public static interface ActionHandler extends Serializable {
		ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception;
	} // END of inner interface ActionHandler
	
	
	public WadeBasicResponder(Agent agent) {
		this(agent, 
				MessageTemplate.and(
						MessageTemplate.MatchOntology(WadeManagementOntology.getInstance().getName()), 
						MessageTemplate.MatchPerformative(ACLMessage.REQUEST))
		);
	}
	
	public WadeBasicResponder(Agent agent, MessageTemplate messageTemplate) {
		this(agent, messageTemplate, null);
	}
	
	public WadeBasicResponder(Agent agent, MessageTemplate messageTemplate, DataStore dataStore) {
		super(agent);
		template = messageTemplate;
		if (dataStore != null) {
			setDataStore(dataStore);
		}
		registerHandler(GetAgentAttributes.class, new ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				ACLMessage reply = request.createReply();
				Result r = new Result(aExpr, ((WadeAgent) myAgent).getAttributes());
				myAgent.getContentManager().fillContent(reply, r);
				reply.setPerformative(ACLMessage.INFORM);
				return reply;
			}
		} );
		registerHandler(SetAgentAttributes.class, new ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				ACLMessage reply = request.createReply();
				((WadeAgent) myAgent).setAttributes(((SetAgentAttributes) act).getValues());
				reply.setPerformative(ACLMessage.INFORM);
				return reply;
			}
		} );
		registerHandler(SetAgentAttribute.class, new ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				ACLMessage reply = request.createReply();
				List attrs = new ArrayList();
				attrs.add(((SetAgentAttribute) act).getValue());
				((WadeAgent) myAgent).setAttributes(attrs);
				reply.setPerformative(ACLMessage.INFORM);
				return reply;
			}
		} );
		registerHandler(PrepareForShutdown.class, new ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				ACLMessage reply = request.createReply();
				Result r = null;
				if (myAgent instanceof WadeAgentImpl) {
					r = new Result(aExpr, new Boolean(((WadeAgentImpl) myAgent).prepareForShutdown(System.currentTimeMillis())));
				}
				else {
					r = new Result(aExpr, new Boolean(((WadeAgent) myAgent).prepareForShutdown()));
				}
				myAgent.getContentManager().fillContent(reply, r);
				reply.setPerformative(ACLMessage.INFORM);
				return reply;
			}
		} );
		registerHandler(GetCurrentLoad.class, new ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				ACLMessage reply = request.createReply();
				Result r = new Result(aExpr, ((WadeAgent) myAgent).getCurrentLoad());
				myAgent.getContentManager().fillContent(reply, r);
				reply.setPerformative(ACLMessage.INFORM);
				return reply;
			}
		} );
	}
	
	public void setTemplate(MessageTemplate mt) {
		template = mt;
	}

	public void registerHandler(Class handlerClass, ActionHandler handler) {
		handlers.put(handlerClass, handler);
	}

	public void deregisterHandler(Class handlerClass) {
		handlers.remove(handlerClass);
	}

	public void action() {
		ACLMessage request = myAgent.receive(template);
		if (request != null) {
			boolean notificationRequired = true;
			ACLMessage response = prepareResponse(request);
			if (response != null) {
				myAgent.send(response);
				if (response.getPerformative() != ACLMessage.AGREE) {
					notificationRequired = false;
				}
			}
			if (notificationRequired) {
				ACLMessage notification  = null;
				try {
					notification = prepareResultNotification(request, response);
				}
				catch (FailureException fe) {
					notification = request.createReply();
					notification.setPerformative(ACLMessage.FAILURE);
				}
				if (notification != null) {
					myAgent.send(notification);
				}
			}
		}
		else {
			block();
		}
	}
	
	protected ACLMessage prepareResponse(ACLMessage request) {
		ACLMessage reply = null;
		AgentAction act = null;
		try {
			Action aExpr = (Action) myAgent.getContentManager().extractContent(request);
			act = (AgentAction) aExpr.getAction();
			try {
				reply = serveAction(act, aExpr, request);
			}
			catch (Exception e) {
				myLogger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Unexpected error serving "+act.getClass().getName()+" request from " + request.getSender().getName()+".", e);
				reply = request.createReply();
				reply.setContent(MessageCode.UNEXPECTED_ERROR + MessageCode.ARGUMENT_SEPARATOR + e.getMessage());
				reply.setPerformative(ACLMessage.FAILURE);
			}
		}
		catch (OntologyException oe) {
			String errorMsg = "Error decoding request from "+request.getSender().getName()+". Content was: "+request.getContent();
			myLogger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorMsg, oe);
			reply = request.createReply();
			reply.setContent(MessageCode.REQUEST_NOT_UNDERSTOOD + MessageCode.ARGUMENT_SEPARATOR + oe.getMessage());
			reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
		}
		catch (Codec.CodecException ce) {
			String errorMsg = "Error decoding request from "+request.getSender().getName()+". Content was: "+request.getContent();
			myLogger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": "+errorMsg, ce);
			reply = request.createReply();
			reply.setContent(MessageCode.REQUEST_NOT_UNDERSTOOD + MessageCode.ARGUMENT_SEPARATOR + ce.getMessage());
			reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
		}
		return reply;
	}
	
	protected ACLMessage prepareResultNotification(ACLMessage aclMessage, ACLMessage aclMessage1) throws FailureException {
		return null;
	}
	
	protected ACLMessage serveAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
		ACLMessage reply = null;
		ActionHandler handler = null;
		for (Map.Entry<Class, ActionHandler> entry : handlers.entrySet()){
			if (entry.getKey().isAssignableFrom(act.getClass())) {
				handler = entry.getValue();
				reply = handler.handleAction(act, aExpr, request);
				break;
			}
		}
		if (handler == null) {
			reply = handleUnknownAction(act, aExpr, request);
		}
		return reply;
	}
	
	protected ACLMessage handleUnknownAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
		ACLMessage reply = request.createReply();
		reply.setPerformative(ACLMessage.REFUSE);	
		reply.setContent(MessageCode.ACTION_NOT_SUPPORTED+MessageCode.ARGUMENT_SEPARATOR+act.getClass().getName()+MessageCode.ARGUMENT_SEPARATOR+((WadeAgent) myAgent).getType().getDescription());
		return reply;
	}
	
}
