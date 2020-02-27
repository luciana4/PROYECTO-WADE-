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

import jade.content.AgentAction;
import jade.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.Property;
import jade.wrapper.ControllerException;
import jade.wrapper.gateway.DynamicJadeGateway;
import jade.wrapper.gateway.GatewayListener;
import jade.wrapper.gateway.JadeGateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.cfa.ontology.GetEventType;
import com.tilab.wade.cfa.ontology.GetEventTypes;
import com.tilab.wade.cfa.ontology.GetGlobalProperties;
import com.tilab.wade.cfa.ontology.GetRole;
import com.tilab.wade.cfa.ontology.GetRoleProperties;
import com.tilab.wade.cfa.ontology.GetRoles;
import com.tilab.wade.cfa.ontology.GetType;
import com.tilab.wade.cfa.ontology.GetTypeProperties;
import com.tilab.wade.cfa.ontology.GetTypes;
import com.tilab.wade.cfa.ontology.TypeManagementOntology;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.ActionExecutor;
import com.tilab.wade.utils.behaviours.OutcomeManager;


/**
 * Utility class to remotely manage type-wide configurations specified in the types.xml file
 */
public class TypeManagerProxy extends TypeManagerIf implements GatewayListener {

	private static final String TYPES_KEY = "TYPES";
	private static final String ROLES_KEY = "ROLES";
	private static final String TYPE_KEY = "TYPE_";
	private static final String ROLE_KEY = "ROLE_";
	private static final String TYPE_PROPERTIES_KEY = "TYPE_PROPERTIES_";
	private static final String ROLE_PROPERTIES_KEY = "ROLE_PROPERTIES_";
	private static final String PROPERTIES_KEY = "PROPERTIES";
	private static final String EVENT_TYPES_KEY = "EVENT_TYPES";
	private static final String EVENT_TYPE_KEY = "EVENT_TYPE_";
	
	private DynamicJadeGateway jadeGateway;
	private Map<String, Object> cache = new HashMap<String, Object>();

	private synchronized DynamicJadeGateway getGateway() {
		if (jadeGateway == null) {
			jadeGateway = JadeGateway.getDefaultGateway(); 
			
			init(jadeGateway);
		}
		return jadeGateway;
	}

	public synchronized void init(DynamicJadeGateway jadeGateway) {
		this.jadeGateway = jadeGateway;
		this.jadeGateway.addListener(this);
	}

	@Override
	public void handleGatewayConnected() {
	}

	@Override
	public void handleGatewayDisconnected() {
		// When the gateway shutting down clear all the cache 
		cache.clear();
	}	
	
	/**
	 * Retrieve all types defined in the system as a List of AgentType objects
	 * @return All types defined in the system as a List of AgentType objects
	 */
	@Override
	public List<AgentType> getTypes() {
		List<AgentType> types;
		if (cache.containsKey(TYPES_KEY)) {
			types = (List<AgentType>) cache.get(TYPES_KEY);
		}
		else {
			types = (new TypeManagemtExecutor<GetTypes, List<AgentType>>(new GetTypes(), getGateway())).getTypeManagemtResult();
			cache.put(TYPES_KEY, types);
		}
		return types;
	}
	
	/**
	 * Retrieve all roles defined in the system as a List of AgentRole objects
	 * @return All roles defined in the system as a List of AgentRole objects
	 * @throws ControllerException 
	 */
	@Override
	public List<AgentRole> getRoles() {
		List<AgentRole> roles;
		if (cache.containsKey(ROLES_KEY)) {
			roles = (List<AgentRole>) cache.get(ROLES_KEY);
		}
		else {
			roles = (new TypeManagemtExecutor<GetRoles, List<AgentRole>>(new GetRoles(), getGateway())).getTypeManagemtResult();
			cache.put(ROLES_KEY, roles);
		}
		return roles;
	}

	/**
	 * @param typeDescription the description identifying the type to retrieve
	 * @return  the AgentType associated with the given type description or AgentType.NONE if there is no such type in file types.xml or default types
	 * @throws ControllerException 
	 */
	@Override
	public AgentType getType(String typeDescription) {
		String key = TYPE_KEY+typeDescription;
		AgentType type;
		if (cache.containsKey(key)) {
			type = (AgentType) cache.get(key);
		}
		else {
			type = (new TypeManagemtExecutor<GetType, AgentType>(new GetType(typeDescription), getGateway())).getTypeManagemtResult();
			cache.put(key, type);
		}
		return type;
	}

	/**
	 * @param roleDescription the description identifying the role to retrieve
	 * @return the AgentRole associated with the given role description or AgentRole.NONE if there is no such role in file types.xml or default roles
	 * @throws ControllerException 
	 */
	@Override
	public AgentRole getRole(String roleDescription) {
		String key = ROLE_KEY+roleDescription;
		AgentRole role;
		if (cache.containsKey(key)) {
			role = (AgentRole) cache.get(key);
		}
		else {
			role = (new TypeManagemtExecutor<GetRole, AgentRole>(new GetRole(roleDescription), getGateway())).getTypeManagemtResult();
			cache.put(key, role);
		}
		return role;
	}

	/**
	 * @param agentType
	 * @return a Map containing all properties of the given type including those inherited from its type or null if type doesn't exist
	 * @throws ControllerException 
	 */
	@Override
	public Map<String, Object> getProperties(AgentType agentType) {
		String key = TYPE_PROPERTIES_KEY+agentType;
		Map<String, Object> properties;
		if (cache.containsKey(key)) {
			properties = (Map<String, Object>) cache.get(key);
		}
		else {
			List<Property> list = (new TypeManagemtExecutor<GetTypeProperties, List<Property>>(new GetTypeProperties(agentType), getGateway())).getTypeManagemtResult();
			properties = convertToMap(list);
			cache.put(key, properties);
		}
		return properties;
	}

	/**
	 * @param agentRole
	 * @return a Map containing all properties of the given role including those inherited from its role or null if role doesn't exist
	 * @throws ControllerException 
	 */
	@Override
	public Map<String, Object> getProperties(AgentRole agentRole) {
		String key = ROLE_PROPERTIES_KEY+agentRole;
		Map<String, Object> properties;
		if (cache.containsKey(key)) {
			properties = (Map<String, Object>) cache.get(key);
		}
		else {
			List<Property> list = (new TypeManagemtExecutor<GetRoleProperties, List<Property>>(new GetRoleProperties(agentRole), getGateway())).getTypeManagemtResult();
			properties = convertToMap(list);
			cache.put(key, properties);
		}
		return properties;
	}

	/**
	 * Retrieve the global properties of the system in form of a Map
	 * @return the global properties of the system in form of a Map
	 * @throws ControllerException 
	 */
	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties;
		if (cache.containsKey(PROPERTIES_KEY)) {
			properties = (Map<String, Object>) cache.get(PROPERTIES_KEY);
		}
		else {
			List<Property> list = (new TypeManagemtExecutor<GetGlobalProperties, List<Property>>(new GetGlobalProperties(), getGateway())).getTypeManagemtResult();
			properties = convertToMap(list);
			cache.put(PROPERTIES_KEY, properties);
		}
		return properties;
	}

	/**
	 * Retrieve all custom event types defined in the system as a List of EventType objects
	 * @return All custom event types defined in the system as a List of EventType objects
	 */
	@Override
	public List<EventType> getCustomEventTypes() {
		List<EventType> eventTypes;
		if (cache.containsKey(EVENT_TYPES_KEY)) {
			eventTypes = (List<EventType>) cache.get(EVENT_TYPES_KEY);
		}
		else {
			eventTypes = (new TypeManagemtExecutor<GetEventTypes, List<EventType>>(new GetEventTypes(), getGateway())).getTypeManagemtResult();
			cache.put(EVENT_TYPES_KEY, eventTypes);
		}
		return eventTypes;
	}

	/**
	 * Retrieve custom event type of the given type as a List of EventType objects
	 * @return Custom event type of the given type as a List of EventType objects
	 */
	@Override
	public EventType getCustomEventType(String typeDescription) {
		String key = EVENT_TYPE_KEY+typeDescription;
		EventType eventType;
		if (cache.containsKey(key)) {
			eventType = (EventType) cache.get(key);
		}
		else {
			eventType = (new TypeManagemtExecutor<GetEventType, EventType>(new GetEventType(typeDescription), getGateway())).getTypeManagemtResult();
			cache.put(key, eventType);
		}
		return eventType;
	}
	
	private Map<String, Object> convertToMap(List<Property> list) {
		Map<String, Object> map = null;
		if (list != null) {
			map = new HashMap<String, Object>();
			for (Property p : list) {
				map.put(p.getName(), p.getValue());
			}
		}
		return map;
	}


	private class TypeManagemtExecutor<ActionT extends AgentAction, ResultT> extends ActionExecutor<ActionT, ResultT> {

		private DynamicJadeGateway jadeGateway;
		
		public TypeManagemtExecutor(ActionT action, DynamicJadeGateway jadeGateway) {
			super(action, TypeManagementOntology.getInstance(), null);
			
			this.jadeGateway = jadeGateway;
		}

		@Override
		protected AID retrieveActor() throws FIPAException {
			return DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.CONFIGURATION_AGENT_TYPE, null));
		}
		
		private void execute() {
			try {
				jadeGateway.execute(this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if (getOutcome().getExitCode() == OutcomeManager.KO) {
				throw new RuntimeException(getOutcome().getErrorMsg());
			}
		}
		
		public ResultT getTypeManagemtResult() {
			execute();
			
			return getResult();
		}
	}
}
