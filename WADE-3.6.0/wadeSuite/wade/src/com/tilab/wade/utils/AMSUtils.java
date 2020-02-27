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

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.Location;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAService;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.domain.JADEAgentManagement.JADEManagementVocabulary;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.lang.acl.ACLMessage;


/**
 * Utility class providing static methods to interact with the AMS in a synchronous way
 */
public class AMSUtils {
	
	private final static long DEFAULT_AMS_REQUEST_TIMEOUT = 30000;
	
	/**
	 *  Request the AMS to create an agent and collect the reply
	 *  @return the AID of the newly created agent
	 */
	public static AID createAgent(Agent agent, String name, String className, Object args[], String containerName) throws Exception {
		CreateAgent createagent;
		createagent = new CreateAgent();
		createagent.setAgentName(name);
		createagent.setClassName(className);
		if (args != null) {
			for(int i = 0; i < args.length; i++) {
				createagent.addArguments(args[i]);
			}
		}
		if(containerName != null) {
			createagent.setContainer(new ContainerID(containerName, null));
		}
		else {
			createagent.setContainer((ContainerID)agent.here());
		}
		
		requestAMSAction(agent, createagent, DEFAULT_AMS_REQUEST_TIMEOUT);
		
		return new AID(name, AID.ISLOCALNAME);
	}
	
	/**
	 *  Request the AMS the list of currently active containers
	 *  @return the list of containers as an array of <code>Location</code> objects
	 */
	public static Location[] getLocations(Agent a) throws Exception {
		java.util.List locationList;
		Location [] result;
		locationList = ((jade.util.leap.ArrayList) requestAMSAction(a, new QueryPlatformLocationsAction(), DEFAULT_AMS_REQUEST_TIMEOUT)).toList();
		result = new Location[locationList.size()];
		return (Location[])locationList.toArray(result);
	}
		
	/**
	 *  Request the AMS to perform a given action of the JADE Management Ontology
	 *  @return the AID of the newly created agent
	 */
	public static Object requestAMSAction(Agent a, AgentAction action, long timeout) throws Exception {
		ACLMessage request = createRequestMessage(a, a.getAMS(), JADEManagementVocabulary.NAME);
		Action act = new Action();
		act.setActor(a.getAMS());
		act.setAction(action);
		a.getContentManager().fillContent(request, act);
		ACLMessage reply = FIPAService.doFipaRequestClient(a, request, timeout);
		if (reply != null) {
			ContentElement ce = a.getContentManager().extractContent(reply);
			if (ce instanceof Result) {
				return ((Result) ce).getValue();
			}
			else {
				return null;
			}
		}
		else {
			throw new FIPAException("Timeout expired");
		}
	}
	
	public static ACLMessage createRequestMessage(Agent sender, AID receiver, String ontology) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setSender(sender.getAID());
		request.addReceiver(receiver);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		request.setOntology(ontology);
		return request;
	}
}
