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
package com.tilab.wade.utils;

import java.util.List;

import com.tilab.wade.commons.AgentInitializationException;

import jade.content.AgentAction;
import jade.content.Predicate;
import jade.content.onto.JavaCollectionOntology;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

/**
 * This class provides static utility methods to perform common agent-oriented 
 * operations.<br>  
 */
public class AgentUtils {

	private static Logger myLogger = Logger.getJADELogger(AgentUtils.class.getName());
	
	public static void sendResult(Agent a, AgentAction action, ACLMessage request, Object result) {
		if (result instanceof List) {
			Ontology onto = a.getContentManager().lookupOntology(request.getOntology());
			if (!Ontology.isBaseOntology(new Ontology[]{onto}, JavaCollectionOntology.getInstance().getName())) {
				// If the result is a java List and our ontology is not able to deal with it, convert it into a leap List
				jade.util.leap.ArrayList l = new jade.util.leap.ArrayList(); 
				l.fromList((List) result);
				result = l;
			}
		}
		ACLMessage reply = request.createReply();
		Action actExpr = new Action(a.getAID(), action);
		Result resExpr = new Result(actExpr, result);
		try {
			a.getContentManager().fillContent(reply, resExpr);
			reply.setPerformative(ACLMessage.INFORM);
			a.send(reply);
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+a.getLocalName()+" - Unexpected error encoding reply to "+action.getClass().getSimpleName()+" request", e);
			sendFailure(a, request, "Error encoding response");
		}
	}
	
	public static void sendDone(Agent a, AgentAction action, ACLMessage request) {
		sendDone(a, action, null, request);
	}
	
	public static void sendDone(Agent a, AgentAction action, Predicate condition, ACLMessage request) {
		ACLMessage reply = request.createReply();
		Action actExpr = new Action(a.getAID(), action);
		Done doneExpr = new Done(actExpr);
		doneExpr.setCondition(condition);
		try {
			a.getContentManager().fillContent(reply, doneExpr);
			reply.setPerformative(ACLMessage.INFORM);
			a.send(reply);
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+a.getLocalName()+" - Unexpected error encoding reply to "+action.getClass().getSimpleName()+" request", e);
			sendFailure(a, request, "Error encoding response");
		}
	}
	
	public static void sendFailure(Agent a, ACLMessage request, String message) {
		sendNegativeResponse(a, request, ACLMessage.FAILURE, message);
	}
	
	public static void sendNegativeResponse(Agent a, ACLMessage request, int performative, String message) {
		ACLMessage reply = request.createReply();
		reply.setPerformative(performative);
		reply.setContent(message);
		a.send(reply);
	}

	public static AID registerToTopic(Agent agent, String topicName) throws AgentInitializationException {
		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) agent.getHelper(TopicManagementHelper.SERVICE_NAME);
			AID topic = topicHelper.createTopic(topicName);
			topicHelper.register(topic);
			return topic;
		} catch (ServiceException se) {
			throw new AgentInitializationException("Error registering to topic "+topicName, se);
		}
	}

	public static void deregisterToTopic(Agent agent, AID topic) {
		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) agent.getHelper(TopicManagementHelper.SERVICE_NAME);
			topicHelper.deregister(topic);
		} catch (ServiceException se) {
			myLogger.log(Logger.WARNING, "Agent "+agent.getLocalName()+" - Unexpected error deregistering from topic "+topic.getLocalName(), se);
		}
	}
}
