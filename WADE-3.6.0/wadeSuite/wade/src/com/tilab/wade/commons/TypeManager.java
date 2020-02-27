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

import jade.domain.FIPAAgentManagement.Property;
import jade.util.Logger;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.tilab.wade.bca.BackupControllerAgent;
import com.tilab.wade.ca.ControllerAgent;
import com.tilab.wade.cfa.ConfigurationAgent;
import com.tilab.wade.cfa.beans.AgentBaseInfo;
import com.tilab.wade.esa.EventSystemAgent;
import com.tilab.wade.lock.LockManagerAgent;
import com.tilab.wade.performer.WorkflowEngineAgent;
import com.tilab.wade.raa.RuntimeAllocatorAgent;
import com.tilab.wade.utils.OntologyUtils;
import com.tilab.wade.utils.XMLManager;
import com.tilab.wade.wsma.WorkflowStatusManagerAgent;


/**
 * Utility class to manage type-wide configurations specified in the types.xml file
 */
public class TypeManager extends TypeManagerIf {
	private static TypeManager theInstance;

	private static final String TYPES_FILE = "types-file";
	private static final String TYPES_FILE_DEFAULT ="types.xml";

	protected static Logger myLogger = Logger.getMyLogger(TypeManager.class.getName());
	private PlatformTypes platformTypes;
	private boolean usingDefault = false;

	private static Map<String, AgentType> defaultTypes = new HashMap<String, AgentType>();
	private static Map<String, AgentRole> defaultRoles = new HashMap<String, AgentRole>();

	static {
		defaultTypes.put(WadeAgent.CONFIGURATION_AGENT_TYPE, new AgentType(WadeAgent.CONFIGURATION_AGENT_TYPE, ConfigurationAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.CONTROL_AGENT_TYPE, new AgentType(WadeAgent.CONTROL_AGENT_TYPE, ControllerAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.BCA_AGENT_TYPE, new AgentType(WadeAgent.BCA_AGENT_TYPE, BackupControllerAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.RAA_AGENT_TYPE, new AgentType(WadeAgent.RAA_AGENT_TYPE, RuntimeAllocatorAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.WFENGINE_AGENT_TYPE, new AgentType(WadeAgent.WFENGINE_AGENT_TYPE, WorkflowEngineAgent.class.getName(), WadeAgent.WORKFLOW_EXECUTOR_ROLE));
		defaultTypes.put(WadeAgent.WSMA_AGENT_TYPE, new AgentType(WadeAgent.WSMA_AGENT_TYPE, WorkflowStatusManagerAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.ESA_AGENT_TYPE, new AgentType(WadeAgent.ESA_AGENT_TYPE, EventSystemAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));
		defaultTypes.put(WadeAgent.LM_AGENT_TYPE, new AgentType(WadeAgent.LM_AGENT_TYPE, LockManagerAgent.class.getName(), WadeAgent.ADMINISTRATOR_ROLE));

		defaultRoles.put(WadeAgent.ADMINISTRATOR_ROLE, new AgentRole(WadeAgent.ADMINISTRATOR_ROLE));
		defaultRoles.put(WadeAgent.WORKFLOW_EXECUTOR_ROLE, new AgentRole(WadeAgent.WORKFLOW_EXECUTOR_ROLE));
	}

	private TypeManager(boolean initialize) {
		try {
			if (initialize) {
				String typesPath = System.getProperty(TYPES_FILE, TYPES_FILE_DEFAULT);
				List<URL> typeURLs = new ArrayList<URL>();
				
				// Check in system classloader
				Enumeration<URL> systemResources = ClassLoader.getSystemResources(typesPath);
				while(systemResources.hasMoreElements()) {
					typeURLs.add(systemResources.nextElement());
				}
				
				if (typeURLs.size() == 0) {
					// If not found search from the package of the local class. In this way
					// the types file is found both in the case it is specified as a/b.xml and in 
					// the case it is specified as /a/b.xml
					Enumeration<URL> classResources = getClass().getClassLoader().getResources(typesPath);
					while(classResources.hasMoreElements()) {
						typeURLs.add(classResources.nextElement());
					}
				}
				if (typeURLs.size() == 0) {
					// If not found in the classpath, try in the file system
					File f = new File(typesPath);
					if (f.exists()) {
						typeURLs.add(f.toURI().toURL());
					}
				}
				if (typeURLs.size() != 0) {
					init(typeURLs);
				} else {
					myLogger.log(Logger.WARNING, "Could not retrieve types file, use default values");
					usingDefault = true;
				}
			}
		} catch(Exception e) {
			myLogger.log(Logger.SEVERE, "Exception reading types file, use default values", e);
			usingDefault = true;
		}
	}

	/**
	 * Retrieve the singleton instance of the TypeManager
	 * @return the singleton instance of the TypeManager
	 */
	public synchronized static TypeManager getInstance() {
		return getInstance(true);
	}

	public synchronized static TypeManager getInstance(boolean initialize) {
		if(theInstance == null) {
			theInstance = new TypeManager(initialize);
		}
		return theInstance;
	}

	boolean isUsingDefault() {
		return usingDefault;
	}
	
	public void init(List<URL> urls) throws Exception {
		if (urls == null || urls.size() == 0) {
			throw new Exception("Could not retrieve types file");
		}

		XMLManager manager = new XMLManager();
		manager.add(PlatformTypes.class);
		manager.add(AgentType.class);
		manager.add(AgentRole.class);
		manager.add(Property.class);
		manager.add(EventType.class);
		
		// Load all types.xml and merge it
		// The sequence is: project-type -> wade-type -> addons-type
		for (URL url : urls) {
			PlatformTypes pt = loadPlatformTypes(manager, url);
			if (platformTypes == null) {
				platformTypes = pt;
			} else {
				mergePlatformTypes(platformTypes, pt);
			}
		}
		
		usingDefault = false;
	}

	// Copy all entity from sourcePlatformTypes to targetPlatformTypes.
	// If entity is already present skip it
	private static void mergePlatformTypes(PlatformTypes targetPlatformTypes, PlatformTypes sourcePlatformTypes) {
			
		// Merge properties
		List<Property> targetProperties = targetPlatformTypes.getProperties();
		List<Property> sourceProperties = sourcePlatformTypes.getProperties();
		for (Property sourceProperty : sourceProperties) {
			if (!containsProperty(targetProperties, sourceProperty)) {
				targetProperties.add(sourceProperty);
			}
		}

		// Merge agent-roles
		List<AgentRole> targetRoles = targetPlatformTypes.getRoles();
		List<AgentRole> sourceRoles = sourcePlatformTypes.getRoles();
		for (AgentRole sourceRole : sourceRoles) {
			if (!targetRoles.contains(sourceRole)) {
				targetRoles.add(sourceRole);
			}
		}
		
		// Merge agent-types
		List<AgentType> targetTypes = targetPlatformTypes.getTypes();
		List<AgentType> sourceTypes = sourcePlatformTypes.getTypes();
		for (AgentType sourceType : sourceTypes) {
			if (!targetTypes.contains(sourceType)) {
				targetTypes.add(sourceType);
			}
		}
		
		// Merge custom-event-types
		List<EventType> targetEventTypes = targetPlatformTypes.getCustomEventTypes();
		List<EventType> sourceEventTypes = sourcePlatformTypes.getCustomEventTypes();
		for (EventType sourceEventType : sourceEventTypes) {
			if (!targetEventTypes.contains(sourceEventType)) {
				targetEventTypes.add(sourceEventType);
			}
		}
	}

	private static boolean containsProperty(List<Property> properties, Property property) {
		for (Property p : properties) {
			if (p.getName().equalsIgnoreCase(property.getName())) {
				return true;
			}
		}
		return false;
	}
	
	private static PlatformTypes loadPlatformTypes(XMLManager manager, URL url) throws Exception {
		myLogger.log(Logger.INFO, "Loading types from "+url);
		PlatformTypes platformTypes = (PlatformTypes) manager.decode(url.openStream());
		substituteVariables(platformTypes);
		return platformTypes;
	}
	
	/**
	 * Retrieve all types defined in the system as a List of AgentType objects
	 * @return All types defined in the system as a List of AgentType objects
	 */
	@Override
	public List<AgentType> getTypes() {
		if (platformTypes != null) {
			return platformTypes.getTypes();
		} else {
			return (List)defaultTypes.values();
		}
	}

	/**
	 * Retrieve all roles defined in the system as a List of AgentRole objects
	 * @return All roles defined in the system as a List of AgentRole objects
	 */
	@Override
	public List<AgentRole> getRoles() {
		if (platformTypes != null) {
			return platformTypes.getRoles();
		} else {
			return (List)defaultRoles.values();
		}
	}

	/**
	 * @param typeDescription the description identifying the type to retrieve
	 * @return  the AgentType associated with the given type description or AgentType.NONE if there is no such type in file types.xml or default types
	 */
	@Override
	public AgentType getType(String typeDescription) {
		if (typeDescription == null) {
			return AgentType.NONE;
		}
		AgentType res = null;
		if (platformTypes != null) {
			// Try in types.xml
			for (AgentType agentType : platformTypes.getTypes()) {
				if (typeDescription.equals(agentType.getDescription())) {
					res = agentType;
					break;
				}
			}
		}
		if (res == null) {
			// Try in default types
			res = defaultTypes.get(typeDescription);
		}
		if (res == null) {
			// Default value
			res = AgentType.NONE;
		}
		return  res;
	}

	/**
	 * @param roleDescription the description identifying the role to retrieve
	 * @return the AgentRole associated with the given role description or AgentRole.NONE if there is no such role in file types.xml or default roles
	 */
	@Override
	public AgentRole getRole(String roleDescription) {
		if (roleDescription == null) {
			return AgentRole.NONE;
		}
		AgentRole res = null;
		if (platformTypes != null) {
			// Try in types.xml
			for (AgentRole agentRole : platformTypes.getRoles()) {
				if (roleDescription.equals(agentRole.getDescription())) {
					res = agentRole;
					break;
				}
			}
		}
		if (res == null) {
			// Try in default roles
			res = defaultRoles.get(roleDescription);
		}
		if (res == null) {
			// Default value
			res = AgentRole.NONE;
		}
		return  res;
	}

	/**
	 *
	 * @param className the className of an Agent
	 * @return the AgentType associated with the given className or AgentType.NONE if there is no such type in file types.xml or default types
	 */
	public AgentType getTypeForClass(String className) {

		// Get class of name className
		Class searchedClass;
		try {
			searchedClass = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			myLogger.log(Logger.WARNING, "Class "+className+" not found in classpath");
			return AgentType.NONE;
		}

		// Try in types.xml
		AgentType res = null;
		if (platformTypes != null) {
			List<AgentType> agentTypes = platformTypes.getTypes();
			for(AgentType agentType : agentTypes) {
				try {
					Class agentTypeClass = Class.forName(agentType.getClassName());
					if (agentTypeClass.isAssignableFrom(searchedClass)) {
						res = agentType;
					}
				} catch (ClassNotFoundException e) {
					// AgentType present in types.xml associated to class non present in current classpath 
				}
			}
		}
		if (res == null) {
			// Try in default types
			for(AgentType agentType : defaultTypes.values()) {
				try {
					Class agentTypeClass = Class.forName(agentType.getClassName());
					if (agentTypeClass.isAssignableFrom(searchedClass)) {
						res = agentType;
					}
				} catch (ClassNotFoundException e) {
					// AgentType present in defaultTypes associated to class non present in current classpath
				}
			}
		}
		if (res == null) {
			// Default value
			res = AgentType.NONE;
		}
		return  res;
	}

	/**
	 * @param agentType
	 * @return a Map containing all properties of the given type including those inherited from its role or null if type doesn't exist
	 */
	@Override
	public Map<String, Object> getProperties(AgentType agentType) {
		Map res  = null;
		if(agentType != null) {
			AgentRole agentRole = getRole(agentType.getRole());
			res = new HashMap();
			fillMap(res, agentRole.getProperties());
			fillMap(res, agentType.getProperties());
		}
		return res;
	}

	/**
	 * @param agentRole
	 * @return a Map containing all properties of the given role including those inherited from its role or null if type doesn't exist
	 */
	@Override
	public Map<String, Object> getProperties(AgentRole agentRole) {
		Map res  = null;
		if(agentRole != null) {
			res = new HashMap();
			fillMap(res, agentRole.getProperties());
		}
		return res;
	}

	/**
	 * Retrieve the global properties of the system in form of a Map
	 * @return the global properties of the system in form of a Map
	 */
	@Override
	public Map<String, Object> getProperties() {
		Map res = new HashMap();
		fillMap(res, platformTypes.getProperties());
		return res;
	}

	private void fillMap(Map map, List pp) {
		if (pp != null) {
			Iterator it = pp.iterator();
			while(it.hasNext()) {
				Property p = (Property) it.next();
				map.put(p.getName(),p.getValue());
			}
		}
	}

	/**
	 * Retrieve all custom event types defined in the system as a List of EventType objects
	 * @return All custom event types defined in the system as a List of EventType objects
	 */
	@Override
	public List<EventType> getCustomEventTypes() {
		return platformTypes.getCustomEventTypes();
	}
	
	/**
	 * Retrieve custom event type of the given type as a List of EventType objects
	 * @return Custom event type of the given type as a List of EventType objects
	 */
	@Override
	public EventType getCustomEventType(String typeDescription) {
		if (platformTypes != null) {
			for (EventType eventType : platformTypes.getCustomEventTypes()) {
				if (typeDescription.equals(eventType.getDescription())) {
					return eventType;
				}
			}
		}
		return  null;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////
	// This methods are in ConfigurationAgent to serve the TypeManagerProxy requests
	//////////////////////////////////////////////////////////////////////////////////
	public List<Property> getPropertiesList() {
		return platformTypes.getProperties();
	}

	public List<Property> getPropertiesList(AgentRole agentRole) {
		if(agentRole != null) {
			return agentRole.getProperties();
		}
		return null;
	}

	public List<Property> getPropertiesList(AgentType agentType) {
		List<Property> properties = null;
		if(agentType != null) {
			properties = new ArrayList<Property>(); 
			AgentRole agentRole = getRole(agentType.getRole());
			properties.addAll(agentRole.getProperties());
			properties.addAll(agentType.getProperties());
		}
		return properties;
	}
	
	
	//////////////////////////////////////////
	// Utility static methods
	//////////////////////////////////////////
	public static long getLong(Map props, String key, long defaultVal) {
		return OntologyUtils.getLong(props, key, defaultVal);
	}

	public static int getInt(Map props, String key, int defaultVal) {
		return OntologyUtils.getInt(props, key, defaultVal);
	}	

	public static boolean getBoolean(Map props, String key, boolean defaultVal) {
		return OntologyUtils.getBoolean(props, key, defaultVal);
	}	

	public static String getString(Map props, String key, String defaultVal) {
		return OntologyUtils.getString(props, key, defaultVal);
	}	

	/*
	 * If the class-name is null retrieve it from TypeManager
	 * This is the case of xml configuration file without class-name attribute specified that 
	 * is correct if the type attribute is referenced in types.xml file  
	 */
	public static String getSafeClassName(AgentBaseInfo abi) {
		String safeClassName = abi.getClassName(); 
		if (safeClassName == null) {
			try {
				safeClassName = TypeManager.getInstance().getType(abi.getType()).getClassName();
			}
			catch (Exception e) {
				myLogger.log(Logger.WARNING, "Error retrieving class for agent type "+abi.getType(), e);
			}
		}
		return safeClassName;
	}

	////////////////////////////////////////////////////////////////////
	// Environment and System variables substitution section
	////////////////////////////////////////////////////////////////////
	private static void substituteVariables(PlatformTypes platformTypes) {
		// Substitute variables in global properties
		substituteVariables(platformTypes.getProperties().iterator());
		// Substitute variables in type-related properties
		Iterator iter = platformTypes.getTypes().iterator();
		while (iter.hasNext()) {
			AgentType at = (AgentType)iter.next();
			substituteVariables(at.getProperties().iterator());
		}
		// Substitute variables in role-related properties
		iter = platformTypes.getRoles().iterator();
		while (iter.hasNext()) {
			AgentRole ar = (AgentRole)iter.next();
			substituteVariables(ar.getProperties().iterator());
		}
	}

	private static void substituteVariables(Iterator iter) {
		while (iter.hasNext()) {
			Property p = (Property)iter.next();
			Object value = p.getValue();
			if (value instanceof String) {
				value = substVars((String)value, null);
				p.setValue(value);
			}
		}
	}

	static String DELIM_START = "${";
	static char   DELIM_STOP  = '}';
	static int DELIM_START_LEN = 2;
	static int DELIM_STOP_LEN  = 1;


	private static String getEnvOrSystemProperty(String key, String def) {
		try {
			String value = System.getenv(key);
			if (value == null) {
				value = System.getProperty(key, def);
			}
			return value;
		} catch (Throwable e) { // MS-Java throws
			// com.ms.security.SecurityExceptionEx
			return def;
		}
	}

	/**
    Perform variable substitution in string <code>val</code> from the
    values of keys found in the system propeties.

    <p>The variable substitution delimeters are <b>${</b> and <b>}</b>.

    <p>For example, if the System properties contains "key=value", then
    the call
    <pre>
    String s = OptionConverter.substituteVars("Value of key is ${key}.");
    </pre>

    will set the variable <code>s</code> to "Value of key is value.".

    <p>If no value could be found for the specified key, then the
    <code>props</code> parameter is searched, if the value could not
    be found there, then substitution defaults to the empty string.

    <p>For example, if system propeties contains no value for the key
    "inexistentKey", then the call

    <pre>
    String s = OptionConverter.subsVars("Value of inexistentKey is [${inexistentKey}]");
    </pre>
    will set <code>s</code> to "Value of inexistentKey is []"

    <p>An {@link java.lang.IllegalArgumentException} is thrown if
    <code>val</code> contains a start delimeter "${" which is not
    balanced by a stop delimeter "}". </p>

    <p><b>Author</b> Avy Sharell</a></p>

    @param val The string on which variable substitution is performed.
    @throws IllegalArgumentException if <code>val</code> is malformed.

	 */
	private static String substVars(String val, Properties props) throws IllegalArgumentException {
		StringBuffer sbuf = new StringBuffer();
		int i = 0;
		int j, k;

		while (true) {
			j = val.indexOf(DELIM_START, i);
			if (j == -1) {
				// no more variables
				if (i == 0) { // this is a simple string
					return val;
				} else { // add the tail string which contains no variables and return the result.
					sbuf.append(val.substring(i, val.length()));
					return sbuf.toString();
				}
			} else {
				sbuf.append(val.substring(i, j));
				k = val.indexOf(DELIM_STOP, j);
				if (k == -1) {
					throw new IllegalArgumentException('"' + val + "\" has no closing brace. Opening brace at position " + j + '.');
				} else {
					j += DELIM_START_LEN;
					String key = val.substring(j, k);
					// first try in System properties
					String replacement = getEnvOrSystemProperty(key, null);
					// then try props parameter
					if (replacement == null && props != null) {
						replacement = props.getProperty(key);
					}

					if (replacement != null) {
						// Do variable substitution on the replacement string
						// such that we can solve "Hello ${x2}" as "Hello p1" also where 
						// x2=${x1}
						// x1=p1
						String recursiveReplacement = substVars(replacement, props);
						sbuf.append(recursiveReplacement);
					}
					i = k + DELIM_STOP_LEN;
				}
			}
		}
	}
}
