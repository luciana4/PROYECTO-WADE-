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

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FIPAManagementVocabulary;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tilab.wade.commons.AgentRole;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.WadeAgent;

/**
 * Utility Class for DF registrations and searches based on types and roles
 */
public class DFUtils {
	private static long DF_TIMEOUT = 5000;
	private static int DF_RETRY = 10;
	
	protected static Logger myLogger = Logger.getMyLogger(DFUtils.class.getName());

	public static void register(Agent agent, DFAgentDescription dfad) throws FIPAException {
		try {
			DFService.register(agent, dfad);
		}
		catch(FIPAException fe) {
			if(isAlreadyRegistered(fe)) {
				if(isRestarting(dfad)) {
					// If we get an AlreadyRegistered exception and we are restarting likely the exception is due to the fact
					// that the DF did not clear our previous registration yet. In this case we cannot simply modify the registration
					// since we may do that before the DF is notified about this agent previous death --> If that happen our new 
					// registration is deleted as if it was the old one.
					myLogger.log(Logger.WARNING, "Agent " + agent.getName() + " is restarting. Waiting for previous registration to be cleared", fe);
					int i = 0;
					while(i < DF_RETRY) {
						try { Thread.currentThread().sleep(DF_TIMEOUT); } catch (InterruptedException e) { }
						try {
							DFService.register(agent, dfad);
							return;
						} 
						catch (FIPAException e) {
							if(isAlreadyRegistered(e)) {
								myLogger.log(Logger.WARNING, "Agent " + agent.getName() + ": Attempt " + (i+1) + " to register with the DF failed. Previous registration still present");
							}
							else {
								throw e;
							}
						}
						i++;
					}
					if(i == DF_RETRY) {
						myLogger.log(Logger.SEVERE, "Agent " + agent.getName() + ": Maximum number of DF registration attempts reached");
						throw new FIPAException("already-registered");
					}
				}
				else {
					// For some reason a registration with the same name is still present in the DF. This happens
					// for instance in the case of replicated agents on Main Container that are NOT automatically 
					// deregistered by DF. In any case just MODIFY the already present registration.
					DFService.modify(agent, dfad);
				}
			}
			else {
				throw fe;
			}
		} 
	}
	
	private static boolean isAlreadyRegistered(FIPAException e) {
		return e.getMessage().contains(FIPAManagementVocabulary.ALREADYREGISTERED);
	}

	private static boolean isNotRegistered(FIPAException e) {
		return e.getMessage().contains(FIPAManagementVocabulary.NOTREGISTERED);
	}

	////////////////////
	// Registration	
	////////////////////
	public static DFAgentDescription createDFAgentDescription(Agent agent, Map<String, Object> m) {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(((WadeAgent) agent).getType().getDescription());
		sd.setOwnership(((WadeAgent) agent).getOwner());
		sd.setName(agent.getLocalName());
		
		if (m == null) {
			m = new HashMap<String, Object>();
		}
		m.put(WadeAgent.AGENT_ROLE, ((WadeAgent) agent).getRole().getDescription());
		m.put(WadeAgent.AGENT_CLASSNAME, agent.getClass().getName());
		m.put(WadeAgent.AGENT_LOCATION, agent.here().getName());
		m.remove(WadeAgent.AGENT_TYPE);
		m.remove(WadeAgent.AGENT_OWNER);
		try {
			m.put(WadeAgent.HOSTNAME, UriUtils.getLocalCanonicalHostname());
			m.put(WadeAgent.HOSTADDRESS, InetAddress.getLocalHost().getHostAddress());
		}
		catch (UnknownHostException uhe) {
			myLogger.log(Logger.WARNING, "Error retrieving local host information for DF registration of agent "+agent.getName(), uhe);
		}
		Set<Map.Entry<String, Object>> set = m.entrySet();
		for (Map.Entry<String, Object>entry : set) {
			Object value = entry.getValue();
			if (value != null) {
				sd.addProperties(new Property(entry.getKey(), value));
			}
		}
		
		dfd.setName(agent.getAID());
		dfd.addServices(sd);
		
		return dfd;
	}
	
	/**
	 * Crea l'oggetto <code>DFAgentDescription</code> da utilizzare per registrare l'agente <code>agent</code> sul DF.
	 * Sul DF vengono registrate solo le property di default.
	 *
	 * @param agent
	 * @return DFAgentDescription
	 */
	public static DFAgentDescription createDFAgentDescription(Agent agent) {
		return createDFAgentDescription(agent, (Map<String, Object>)null);
	}
	
	////////////////////
	// Search	
	////////////////////
	public static DFAgentDescription searchByName(Agent a, String localName) throws FIPAException {
		DFAgentDescription template = new DFAgentDescription();
		template.setName(new AID(localName, AID.ISLOCALNAME));
		DFAgentDescription[] result = DFService.search(a, template);
		if (result.length > 0) {
			return result[0];
		}
		else {
			return null;
		}
	}
	
	public static DFAgentDescription[] searchAllByType(Agent a, AgentType type, Property p) throws FIPAException {
		String typeDescription = null;
		if (type != null && !type.equals(AgentType.ANY) ) {
			typeDescription = type.getDescription();
		}
		return searchAllByType(a, typeDescription, p);
	}
	
	public static DFAgentDescription[] searchAllByType(Agent a, String typeDescription, Property p) throws FIPAException {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		if (typeDescription != null) {
			sd.setType(typeDescription);
		}
		if (p != null) {
			sd.addProperties(p);
		}
		template.addServices(sd);
		
		return DFService.search(a, template);
	}
	
	public static DFAgentDescription searchAnyByType(Agent a, AgentType type, Property p) throws FIPAException {
		String typeDescription = null;
		if (type != null && !type.equals(AgentType.ANY) ) {
			typeDescription = type.getDescription();
		}
		return searchAnyByType(a, typeDescription, p);
	}
	
	public static DFAgentDescription searchAnyByType(Agent a, String typeDescription, Property p) throws FIPAException {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		if (typeDescription != null) {
			sd.setType(typeDescription);
		}
		if (p != null) {
			sd.addProperties(p);
		}
		template.addServices(sd);
		
		SearchConstraints c = new SearchConstraints();
		c.setMaxResults(new Long(1));
		
		DFAgentDescription[] result = DFService.search(a, template, c);
		if (result.length > 0) {
			return result[0];
		}
		else {
			return null;
		}
	}
	
	public static DFAgentDescription[] searchAllByRole(Agent a, AgentRole role, Property p) throws FIPAException {
		String roleDescription = null;
		if (role != null && !role.equals(AgentRole.ANY)) {
			roleDescription = role.getDescription();
		}
		return searchAllByRole(a, roleDescription, p);
	}
	
	public static DFAgentDescription[] searchAllByRole(Agent a, String roleDescription, Property p) throws FIPAException {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		if (roleDescription != null) {
			sd.addProperties(new Property(WadeAgent.AGENT_ROLE, roleDescription));
		}
		if (p != null) {
			sd.addProperties(p);
		}
		template.addServices(sd);
		
		return DFService.search(a, template);
	}
	
	public static DFAgentDescription searchAnyByRole(Agent a, AgentRole role, Property p) throws FIPAException {
		String roleDescription = null;
		if (role != null && !role.equals(AgentRole.ANY)) {
			roleDescription = role.getDescription();
		}
		return searchAnyByRole(a, roleDescription, p);
	}
	
	public static DFAgentDescription searchAnyByRole(Agent a, String roleDescription, Property p) throws FIPAException {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		if (roleDescription != null) {
			sd.addProperties(new Property(WadeAgent.AGENT_ROLE, roleDescription));
		}
		if (p != null) {
			sd.addProperties(p);
		}
		template.addServices(sd);
		
		SearchConstraints c = new SearchConstraints();
		c.setMaxResults(new Long(1));
		
		DFAgentDescription[] result = DFService.search(a, template, c);
		if (result.length > 0) {
			return result[0];
		}
		else {
			return null;
		}
	}
	
	public static AID getAID(DFAgentDescription dfad) {
		if (dfad != null) {
			return dfad.getName();
		}
		else {
			return null;
		}
	}
	
	public static AID[] getAIDs(DFAgentDescription[] dfad) {
		AID[] result = new AID[dfad.length];
		for (int i = 0; i < dfad.length; i++) {
			result[i] = dfad[i].getName();
		}
		return result;
	}
	
	public static ServiceDescription getServiceDescription(DFAgentDescription dfad) {
		if (dfad != null) {
			return (ServiceDescription)dfad.getAllServices().next();
		} else {
			return null;
		}
	}
	
	/**
	 * Return the value of a given property in a given <code>ServiceDescription</code> or null if the property is not present
	 */
	public static Object getPropertyValue(ServiceDescription s, String propertyKey) {
		Object result = null;
		java.util.Iterator it = s.getAllProperties();
		while (it.hasNext()) {
			Property p = (Property) it.next();
			if (p.getName().equals(propertyKey)) {
				result = p.getValue();
				break;
			}
		}
		return result;
	}	
	
	private static boolean isRestarting(DFAgentDescription dfad) {
		boolean isRestarting = false;
		Object result = getPropertyValue(getServiceDescription(dfad), WadeAgent.RESTARTING);
		if(result != null) {
			isRestarting = Boolean.parseBoolean((String) result);
		}
		return isRestarting;
	}
}
