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

import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SL0Vocabulary;
import jade.content.lang.sl.SimpleSLTokenizer;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Specifier;
import jade.domain.FIPAException;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.ca.ontology.CreateAgent;
import com.tilab.wade.ca.ontology.KillAgent;
import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;

/**
 * Utility class providing static methods to interact with Control Agents in a synchronous way
 */
public class CAUtils {
	protected static Logger myLogger = Logger.getMyLogger(CAUtils.class.getName());
	
	private final static long DEFAULT_CA_REQUEST_TIMEOUT = 30000;
	
	
	/**
	 * Retrieve the Control Agent on a given location.
	 * @param a The agent performing the operation
	 * @param location the name of the location (container) whose Control Agent is needed
	 * @return The AID of the Control Agent running on the specified location
	 */
	public static AID getCAOnLocation(Agent a, String location) throws FIPAException {
		return DFUtils.getAID(DFUtils.searchAnyByType(a, WadeAgent.CONTROL_AGENT_TYPE, new Property(WadeAgent.AGENT_LOCATION, location)));
	}
	
	/**
	 * Request the creation of a new Agent to an agent able to serve the CreateAgent action of the ControlOntology.
	 * Such agents typically are the CAs and the RAA. 
	 */
	public static ACLMessage createAgent(Agent agent, AgentInfo agentInfo, AID creator) throws CodecException, OntologyException, FIPAException {
		ACLMessage messageCreateAgent = prepareCreateAgentRequest(agent, agentInfo, creator);
		return FIPAService.doFipaRequestClient(agent, messageCreateAgent, DEFAULT_CA_REQUEST_TIMEOUT);
	}
	
	public static ACLMessage createAgent(Agent agent, AgentInfo agentInfo, String containerName) throws CodecException, OntologyException, FIPAException {
		AID aidCA = getCAOnLocation(agent, containerName);
		if(aidCA == null) {
			throw new FIPAException("Cannot find control agent on container " + containerName);
		}
		return createAgent(agent, agentInfo, aidCA);
	}
	
	/**
	 * Prepare the message to create a new agent
	 * @param agent The agent that requests the operation.
	 * @param agentInfo The AgentInfo object describing the agent to be to started.
	 * @param aidCA The AID of the Control Agent the request will be sent to.
	 * @return The REQUEST message to be sent to the Control Agent
	 */
	public static ACLMessage prepareCreateAgentRequest(Agent agent, AgentInfo agentInfo, AID aidCA) throws CodecException, OntologyException {
		Specifier s = agentInfoToSpecifier(agentInfo);
		
		// Initialize the CreateAgent action to be requested to the CA
		CreateAgent createAgent = new CreateAgent();
		createAgent.setName(s.getName());
		createAgent.setClassName(s.getClassName());
		createAgent.setArguments(s.getArgs());
		ACLMessage message = AMSUtils.createRequestMessage(agent, aidCA, ControlOntology.getInstance().getName());
		Action createAgentAction = new Action(aidCA, createAgent);
		agent.getContentManager().fillContent(message, createAgentAction);
		return message;
	}
	
	public static Specifier agentInfoToSpecifier(AgentInfo agentInfo) {
		// Create the map of properties to be passed to the agent to be created and insert
		// arguments specified in the AgentInfo bean
		Collection<AgentArgumentInfo> arguments = agentInfo.getParameters();
		Map<String, Object> properties = new HashMap<String, Object>();
		
		if (arguments != null) {
			for (AgentArgumentInfo currentArgument : arguments) {
				properties.put(currentArgument.getKey(), currentArgument.getValue());
			}
		}
		
		// Add the agent type and owner to the properties
		if (agentInfo.getType() != null) {
			properties.put(WadeAgent.AGENT_TYPE, agentInfo.getType());
		}
		if (agentInfo.getOwner() != null) {
			properties.put(WadeAgent.AGENT_OWNER, agentInfo.getOwner());
		}
		if (agentInfo.getGroup() != null) {
			properties.put(WadeAgent.AGENT_GROUP, agentInfo.getGroup());
		}
		
		// Create the Object[] arguments
		Object[] args = new Object[1];
		args[0] = properties;
		
		// Initialize the Specifier to be returned
		Specifier s = new Specifier();
		s.setName(agentInfo.getName());
		s.setClassName(TypeManager.getSafeClassName(agentInfo));
		s.setArgs(args);
		return s;
	}
	
	public static ACLMessage killAgent(Agent agent, AID target) throws CodecException, OntologyException, FIPAException {
		DFAgentDescription dfd = DFUtils.searchByName(agent, target.getLocalName());
		if (dfd != null) {
			ServiceDescription sd = DFUtils.getServiceDescription(dfd);
			String location = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
			if (location != null) {
				AID ca = getCAOnLocation(agent, location);
				return killAgent(agent, target, ca);
			}
			else {
				throw new FIPAException("Missing location for target agent "+target.getName());
			}
		}
		else {
			throw new FIPAException("Target agent "+target.getName()+" DF description not found");
		}
	}
	
	public static ACLMessage killAgent(Agent agent, AID target, AID ca) throws CodecException, OntologyException, FIPAException {
		ACLMessage request = preparekillAgentRequest(agent, target, ca);
		return FIPAService.doFipaRequestClient(agent, request, DEFAULT_CA_REQUEST_TIMEOUT);
	}
	
	public static ACLMessage preparekillAgentRequest(Agent agent, AID target, AID actor) throws CodecException, OntologyException {
		ACLMessage message = AMSUtils.createRequestMessage(agent, actor, ControlOntology.getInstance().getName());
		KillAgent ka = new KillAgent();
		ka.setAgent(target);
		Action killAgentAction = new Action(actor, ka);
		agent.getContentManager().fillContent(message, killAgentAction);
		return message;
	}
	
	
	/**
	 * Check if a container is an auxiliary container i.e. a Back-End or a Gateway container
	 */
	public static boolean isAuxiliary(String containerName) {
		return containerName.startsWith("BE-") ||
		       containerName.startsWith("AUX-") ||
		       containerName.startsWith("GW-") || 
		       containerName.startsWith("WSEG") || 
		       containerName.startsWith("WSIG");
	}
	
	/**
	 * This method can be used together with CAServices.registerExpectedReply() to be notified if an 
	 * agent from which a reply is expected suddenly dies. In particular this method extract the name of the
	 * dead agent from the FAILURE notification received by the local Control Agent. 
	 * @param failure The FAILURE message received by the local Control Agent.
	 * @return the AID of the dead agent
	 */
	public static AID getDeadAgent(ACLMessage failure) throws FIPAException {
		if (failure.getPerformative() != ACLMessage.FAILURE) {
			throw new FIPAException("Invalid FAILURE message");
		}
		try {
			String content = failure.getContent();
			int start = content.indexOf("Agent-dead");
			start = content.indexOf(SL0Vocabulary.AID, start);
			SimpleSLTokenizer parser = new SimpleSLTokenizer(content.substring(start));
			return FIPAService.parseAID(parser);
		}
		catch (Exception e) {
			throw new FIPAException("Invalid content. "+e);
		}
	}
	
}
