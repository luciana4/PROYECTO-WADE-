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

import jade.util.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public abstract class TypeManagerIf {

	public static enum TypeManagerType { LOCAL, PROXY, AUTO };
	
	protected static Logger myLogger = Logger.getMyLogger(TypeManagerIf.class.getName());
	private static TypeManagerIf theInstance;
	
	public synchronized static TypeManagerIf getInstance(TypeManagerType tmt) {
		if(theInstance == null) {
			if (tmt == null) {
				tmt = TypeManagerType.AUTO;
			}
			
			if (TypeManagerType.LOCAL.equals(tmt)) {
				try {
					theInstance = createLocalTypeManager();
				}
				catch (Exception e) {
					myLogger.log(Level.SEVERE, "Request local TypeManager but class NOT available in the classpath", e);
				}
			}
			else if (TypeManagerType.PROXY.equals(tmt)) {
				theInstance = new TypeManagerProxy();
			}
			else {
				// Automatic detection: If we are in a WADE environment (TypeManager class available 
				// in the classpath AND types file present) --> Use the real TypeManager
				// If we are in a non-WADE environment --> Use a proxy
				try {
					theInstance = createLocalTypeManager();
					
					Method isUsingDefaultMethod = theInstance.getClass().getDeclaredMethod("isUsingDefault");
					boolean isUsingDefault = (Boolean) isUsingDefaultMethod.invoke(theInstance);
					if (isUsingDefault) {
						throw new Exception();
					}
				}
				catch (Throwable t) {
					theInstance = new TypeManagerProxy();
				}
			}
		}
		
		return theInstance;
	}

	private static TypeManagerIf createLocalTypeManager() throws Exception {
		Method instanceMethod = TypeManagerIf.class.getClassLoader().loadClass("com.tilab.wade.commons.TypeManager").getMethod("getInstance");
		return (TypeManagerIf) instanceMethod.invoke(null);	
	}
	
	/**
	 * Retrieve all types defined in the system as a List of AgentType objects
	 * @return All types defined in the system as a List of AgentType objects
	 */
	public abstract List<AgentType> getTypes();

	/**
	 * Retrieve all roles defined in the system as a List of AgentRole objects
	 * @return All roles defined in the system as a List of AgentRole objects
	 */
	public abstract List<AgentRole> getRoles();

	/**
	 * @param typeDescription the description identifying the type to retrieve
	 * @return  the AgentType associated with the given type description or AgentType.NONE if there is no such type in file types.xml or default types
	 */
	public abstract AgentType getType(String typeDescription);

	/**
	 * @param roleDescription the description identifying the role to retrieve
	 * @return the AgentRole associated with the given role description or AgentRole.NONE if there is no such role in file types.xml or default roles
	 */
	public abstract AgentRole getRole(String roleDescription);

	/**
	 * @param agentType
	 * @return a Map containing all properties of the given type including those inherited from its type or null if type doesn't exist
	 */
	public abstract Map<String, Object> getProperties(AgentType agentType);

	/**
	 * @param agentRole
	 * @return a Map containing all properties of the given role including those inherited from its role or null if type doesn't exist
	 */
	public abstract Map<String, Object> getProperties(AgentRole agentRole);

	/**
	 * Retrieve the global properties of the system in form of a Map
	 * @return the global properties of the system in form of a Map
	 */
	public abstract Map<String, Object> getProperties();

	/**
	 * Retrieve all custom event types defined in the system as a List of EventType objects
	 * @return All custom event types defined in the system as a List of EventType objects
	 */
	public abstract List<EventType> getCustomEventTypes();
	
	/**
	 * Retrieve custom event type of the given type as a List of EventType objects
	 * @return Custom event type of the given type as a List of EventType objects
	 */
	public abstract EventType getCustomEventType(String typeDescription);
}
