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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.tilab.wade.commons.ontology.*;
import com.tilab.wade.performer.WorkflowEngineAgent;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.PlatformStartupListener;

import jade.content.lang.Codec;
import jade.content.lang.leap.LEAPCodec;
import jade.content.lang.sl.SLCodec;
import jade.core.*;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.replication.AgentReplicationHelper;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.List;
import jade.util.leap.ArrayList;

/**
 * Base class for all WADE agents.
 * Developers creating agents for a WADE-based application should not extend <code>jade.core.Agent</code>
 * directly. Rather they should extend this class and redefine the <code>agentSpecificSetup()</code> method
 */
public class WadeAgentImpl extends Agent implements WadeAgent {
	private static final long serialVersionUID = 868876861131123L;
	
	protected transient Codec codec = new SLCodec();
	private Date startupTime;
	private boolean restarted = false;
	private boolean shuttingDown = false;
	private Date shutdownInitiationTime;

	private AgentType myType;
	private AgentRole myRole;
	private Map<String, Object> typeProperties;
	private Map<String, Object> globalProperties;
	protected Map<String, Object> arguments;
	
	protected final Logger myLogger = Logger.getMyLogger(getClass().getName());;

	/**
	 * Implement the setup() method to perform all initializations common to WADE agents and register with the DF.
	 * Specific initializations should be performed in the <code>agentSpecificSetup()</code> method
	 * #see agentSpecificSetup()
	 */
	protected final void setup() {

		myLogger.log(Logger.INFO,"Agent "+getName()+" - Starting...........");

		// Wade common initializations
		wadeSetup();

		try {
			// Manage the case that this is the master replica of a virtual agent
			String virtualName = (String) arguments.remove(WadeAgent.VIRTUAL_NAME);
			if (virtualName != null) {
				// Virtual agent
				manageVirtualNature(virtualName, (String) arguments.get(WadeAgent.REPLICATION_TYPE), (String) arguments.get(WadeAgent.NUMBER_OF_REPLICAS));
				arguments.put(WadeAgent.VIRTUAL, "true");
			}
			
			// Agent specific initialization
			agentSpecificSetup();

			// Register with the DF
			DFUtils.register(this, prepareDFDescription());

			// Clear the RESTARTING argument if present
			arguments.remove(WadeAgent.RESTARTING);
		} 
		catch (FIPAException fe) {
			myLogger.log(Logger.SEVERE,"Agent "+getName()+" - Error registering with DF: terminate", fe);
			doDelete();
			return;
		}
		catch (AgentInitializationException aie) {
			myLogger.log(Logger.SEVERE,"Agent "+getName()+" - Error in agent initialization ["+(aie.getMessage() != null ? aie.getMessage() : aie.toString())+"]: terminate", aie);
			doDelete();
			return;
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE,"Agent "+getName()+" - Unexpected error in agent initialization["+(e.getMessage() != null ? e.getMessage() : e.toString())+"]: terminate", e);
			doDelete();
			return;
		}

		myLogger.log(Logger.INFO,"Agent "+getName()+" - STARTUP COMPLETED SUCCESSFULLY!");
	}

	private void wadeSetup() {		
		// Get agent up-time
		startupTime = new Date();

		// Read instance configuration arguments
		Object[] args = getArguments();
		if (args != null && args.length == 1 && args[0] instanceof Map) {
			arguments = (Map<String, Object>) args[0];
		}
		else {
			arguments = parseArguments(args);
		}

		// For debugging purpose
		if ("true".equalsIgnoreCase((String) arguments.get(WadeAgent.DUMP_ARGUMENTS))) {
			myLogger.log(Logger.INFO, "Agent "+getName()+" - startup-arguments = "+arguments);
		}

		// An agent may be restarted by the MainReplicationService (isRestarting() returns true) or by 
		// the WADE CA (RESTARTING arguments set to true).
		if (isRestarting() || "true".equalsIgnoreCase((String) arguments.get(WadeAgent.RESTARTING))) {
			restarted = true;
		}
		
		// Register the WADE Management ontology and default codecs
		getContentManager().registerOntology(WadeManagementOntology.getInstance());
		getContentManager().registerLanguage(codec);
		getContentManager().registerLanguage(new LEAPCodec());

		// Add the responder of the WADE Management ontology
		addBehaviour(getManagementResponder());
	}

	private void manageVirtualNature(final String virtualName, String replicationType, final String numberOfReplicas) throws AgentInitializationException {
		// Make virtual and create replicas
		try {
			final AgentReplicationHelper rplHelper = (AgentReplicationHelper) getHelper(AgentReplicationHelper.SERVICE_NAME);
			int replType = AgentReplicationHelper.HOT_REPLICATION;
			if ("COLD".equalsIgnoreCase(replicationType)) {
				replType = AgentReplicationHelper.COLD_REPLICATION;
			}
			System.out.println(getLocalName()+": Making VIRTUAL - "+replType);
			rplHelper.makeVirtual(virtualName, replType);
			
			SequentialBehaviour sb = new SequentialBehaviour();
			// Wait for complete platform startup
			sb.addSubBehaviour(new PlatformStartupListener(this));
			// Activate replicas
			sb.addSubBehaviour(new OneShotBehaviour() {
				public void action() {
					// The numberOfReplicas parameter has the form N(container1,container2...)
					// N indicates the number of replicas excluding the master --> N=1 --> master + 1 replica
					// The list of containers is optional and, when present, may contain less than N containers
					// If some replicas don't have an explicitly indicated container, the raa should be interrogated 
					// to select one.
					// FIXME: The raa involvement is not yet supported --> the local container is used
					// The format is similar to that of specifiers where N replaces the className and containers replaces arguments
					try {
						Specifier spec = Specifier.parseSpecifier(numberOfReplicas, ',');
						String nStr = spec.getClassName();
						int n = Integer.parseInt(nStr);
						String[] containers = (String[]) spec.getArgs();
						if (containers == null) {
							containers = new String[0];
						}
						for (int i = 0; i < n; ++i) {
							Location location = (i < containers.length) ? new ContainerID(containers[i], null) : here();
							try {			
								myLogger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+" - Creating replica "+i+" in container "+location.getName());
								rplHelper.createReplica(virtualName+"_R"+(i+2), location);
							}
							catch (ServiceException se) {
								myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Error creating replica of virtual agent "+virtualName, se);
							}
						}
					}
					catch (Exception e) {
						myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Cannot create replicas of virtual agent "+virtualName+": numberOfReplicas parameter \""+numberOfReplicas+"\" does not have the expected form N(c1,c2...)", e);
					}
				}
			} );
			addBehaviour(sb);
		}
		catch (ServiceException se) {
			throw new AgentInitializationException("Error accessing AgentReplicationHelper", se);
		}
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		// Recreate transient fields and reinitialize the ContentManager
		codec = new SLCodec();
		// Register the WADE Management ontology and default codec
		getContentManager().registerOntology(WadeManagementOntology.getInstance());
		getContentManager().registerLanguage(codec);
		getContentManager().registerLanguage(new LEAPCodec());
	}
	
	protected void afterMove() {
		try {
			// Register with the DF
			DFService.modify(this, prepareDFDescription());
			myLogger.log(Logger.INFO, "Agent "+getName()+" - Correctly moved");
		}
		catch (FIPAException fe) {
			myLogger.log(Logger.SEVERE,"Agent "+getName()+" - Error modifying DF registration after migration!!!!!!", fe);
		}
	}

	
	/**
	 * Parse agent arguments of type key=value passed in command line
	 * @param args Agent arguments
	 * @return Wade agent arguments map
	 */
	private Map<String, Object> parseArguments(Object[] args) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		
		// Loop all arguments
		if (args != null) {
			for (Object arg : args) {
				
				// Manage only string arguments
				if (arg instanceof String) {
					String strArg = (String)arg;
					int equalsPos = strArg.indexOf('=');
					
					// Manage only string with format key=value
					if (equalsPos > 0) {
						String key = strArg.substring(0,equalsPos);
						String value = strArg.substring(equalsPos+1);

						// Add argument to map
						arguments.put(key, value);
					}
				}
			}
		}
		return arguments;
	}

	/**
	 * Retrieve an agent argument in form of a <code>String</code> managing format conversions 
	 * @param key the key identifying the argument to be retrieved
	 * @param defaultValue the value to be used in case the argument is not specified 
	 * @return The retrieved argument in form of a <code>String</code> 
	 */
	public String getArgument(String key, String defaultValue) {
		return TypeManager.getString(arguments, key, defaultValue);
	}
	
	/**
	 * Retrieve an agent argument in form of a <code>boolean</code> managing format conversions 
	 * @param key the key identifying the argument to be retrieved
	 * @param defaultValue the value to be used in case the argument is not specified or cannot be 
	 * converted to a <code>boolean</code> value
	 * @return The retrieved argument in form of a <code>boolean</code> 
	 */
	public boolean getBooleanArgument(String key, boolean defaultValue) {
		return TypeManager.getBoolean(arguments, key, defaultValue);
	}
	
	/**
	 * Retrieve an agent argument in form of an <code>int</code> managing format conversions 
	 * @param key the key identifying the argument to be retrieved
	 * @param defaultValue the value to be used in case the argument is not specified or cannot be 
	 * converted to an <code>int</code> value
	 * @return The retrieved argument in form of an <code>int</code> 
	 */
	public int getIntArgument(String key, int defaultValue) {
		return TypeManager.getInt(arguments, key, defaultValue);
	}
	
	/**
	 * Retrieve an agent argument in form of a <code>long</code> managing format conversions 
	 * @param key the key identifying the argument to be retrieved
	 * @param defaultValue the value to be used in case the argument is not specified or cannot be 
	 * converted to a <code>long</code> value
	 * @return The retrieved argument in form of a <code>long</code> 
	 */
	public long getLongArgument(String key, long defaultValue) {
		return TypeManager.getLong(arguments, key, defaultValue);
	}
	
	/**
	 * Retrieve a type property in form of a <code>String</code> managing format conversions 
	 * @param key the key identifying the property to be retrieved
	 * @param defaultValue the value to be used in case the property is not specified 
	 * @return The retrieved property in form of a <code>String</code> 
	 */
	public String getTypeProperty(String key, String defaultValue) {
		if (typeProperties == null) {
			typeProperties = TypeManager.getInstance().getProperties(getType());
		}
		return TypeManager.getString(typeProperties, key, defaultValue);
	}
	
	/**
	 * Retrieve a type property in form of a <code>boolean</code> managing format conversions 
	 * @param key the key identifying the property to be retrieved
	 * @param defaultValue the value to be used in case the property is not specified or cannot be 
	 * converted to a <code>boolean</code> value
	 * @return The retrieved property in form of a <code>boolean</code> 
	 */
	public boolean getBooleanTypeProperty(String key, boolean defaultValue) {
		if (typeProperties == null) {
			typeProperties = TypeManager.getInstance().getProperties(getType());
		}
		return TypeManager.getBoolean(typeProperties, key, defaultValue);
	}
	
	/**
	 * Retrieve a type property in form of an <code>int</code> managing format conversions 
	 * @param key the key identifying the property to be retrieved
	 * @param defaultValue the value to be used in case the property is not specified or cannot be 
	 * converted to an <code>int</code> value
	 * @return The retrieved property in form of an <code>int</code> 
	 */
	public int getIntTypeProperty(String key, int defaultValue) {
		if (typeProperties == null) {
			typeProperties = TypeManager.getInstance().getProperties(getType());
		}
		return TypeManager.getInt(typeProperties, key, defaultValue);
	}
	
	/**
	 * Retrieve a type property in form of a <code>long</code> managing format conversions 
	 * @param key the key identifying the property to be retrieved
	 * @param defaultValue the value to be used in case the property is not specified or cannot be 
	 * converted to a <code>long</code> value
	 * @return The retrieved property in form of a <code>long</code> 
	 */
	public long getLongTypeProperty(String key, long defaultValue) {
		if (typeProperties == null) {
			typeProperties = TypeManager.getInstance().getProperties(getType());
		}
		return TypeManager.getLong(typeProperties, key, defaultValue);
	}
	
	public String getGlobalProperty(String key, String defaultValue) {
		return TypeManager.getString(getGlobalProperties(), key, defaultValue);
	}
	
	private Map<String, Object> getGlobalProperties() {
		if (globalProperties == null) {
			globalProperties = TypeManager.getInstance().getProperties();
		}
		return globalProperties;
	}
	
	/**
	 * Retrieve an agent generic configuration in form of a <code>String</code> managing format conversions.
	 * A configuration is searched in the agent arguments first, then is the type properties and finally 
	 * in the global properties. If not found the default value is returned.
	 * @param key the key identifying the configuration to be retrieved
	 * @param defaultValue the value to be used in case the configuration is not specified 
	 * @return The retrieved configuration value in form of a <code>String</code> 
	 */
	public String getConfig(String key, String defaultValue) {
		return getArgument(key, getTypeProperty(key, getGlobalProperty(key, defaultValue)));
	}
	
	/**
	 * Retrieve an agent generic configuration in form of a <code>boolean</code> managing format conversions.
	 * A configuration is searched in the agent arguments first, then is the type properties and finally 
	 * in the global properties. If not found the default value is returned.
	 * @param key the key identifying the configuration to be retrieved
	 * @param defaultValue the value to be used in case the configuration is not specified 
	 * @return The retrieved configuration value in form of a <code>boolean</code> 
	 */
	public boolean getBooleanConfig(String key, boolean defaultValue) {
		return getBooleanArgument(key, getBooleanTypeProperty(key, TypeManager.getBoolean(getGlobalProperties(), key, defaultValue)));
	}
	
	/**
	 * Retrieve an agent generic configuration in form of an <code>int</code> managing format conversions.
	 * A configuration is searched in the agent arguments first, then is the type properties and finally 
	 * in the global properties. If not found the default value is returned.
	 * @param key the key identifying the configuration to be retrieved
	 * @param defaultValue the value to be used in case the configuration is not specified 
	 * @return The retrieved configuration value in form of an <code>int</code> 
	 */
	public int getIntConfig(String key, int defaultValue) {
		return getIntArgument(key, getIntTypeProperty(key, TypeManager.getInt(getGlobalProperties(), key, defaultValue)));
	}
	
	/**
	 * Retrieve an agent generic configuration in form of a <code>long</code> managing format conversions.
	 * A configuration is searched in the agent arguments first, then is the type properties and finally 
	 * in the global properties. If not found the default value is returned.
	 * @param key the key identifying the configuration to be retrieved
	 * @param defaultValue the value to be used in case the configuration is not specified 
	 * @return The retrieved configuration value in form of a <code>long</code> 
	 */
	public long getLongConfig(String key, long defaultValue) {
		return getLongArgument(key, getLongTypeProperty(key, TypeManager.getLong(getGlobalProperties(), key, defaultValue)));
	}
	
	
	
	/**
	 * Placeholder method for all agent specific initializations
	 */
	protected void agentSpecificSetup() throws AgentInitializationException {
	}

	/**
	 * Create and retrieve the behaviour responsible for serving requests to perform actions 
	 * of the Wade-Management ontology.
	 * @return The behaviour responsible for serving requests to perform actions 
	 * of the Wade-Management ontology
	 */
	protected WadeBasicResponder getManagementResponder() {
		return new WadeBasicResponder(this);
	}



	//////////////////////////////////////////////
	// WadeAgent interface implementation
	//////////////////////////////////////////////
	/**
	 * Returns the type of this agent
	 * @return the type of this agent
	 */
	public AgentType getType() {
		if (myType == null) {
			// Lazy initialization
			String typeDescription = (String) arguments.get(AGENT_TYPE);
			if (typeDescription != null) {
				myType = TypeManager.getInstance().getType(typeDescription);
			}
			else {
				myType = TypeManager.getInstance().getTypeForClass(getClass().getName());
			}
		}
		return myType;
	}

	/**
	 * Returns the role of this agent
	 * @return the role of this agent
	 */
	public AgentRole getRole() {
		if (myRole == null) {
			// Lazy initialization
			AgentType type = getType();
			if (type != null) {
				myRole = TypeManager.getInstance().getRole(type.getRole());
			}
			if (myRole == null || myRole.equals(AgentRole.NONE)) {
				if (this instanceof WorkflowEngineAgent) {
					myRole = TypeManager.getInstance().getRole(WadeAgent.WORKFLOW_EXECUTOR_ROLE);
				}
				else {
					myRole = AgentRole.NONE;
				}
			}
		}
		return myRole;
	}

	/**
	 * Returns the name of the owner (if any) of this agent
	 * @return The name of the owner (if any) of this agent
	 */
	public String getOwner() {
		String owner = (String) arguments.get(AGENT_OWNER);
		return (owner != null ? owner : NONE_OWNER);
	}

	/**
	 * Returns the list of attributes (each one represented by an Attribute object) of this agent 
	 * @return the list of attributes of this agent
	 */
	public List getAttributes() {
		List methods = getAttributeGetterMethods();
		List attributes = new ArrayList(methods.size());
		try {
			Iterator it = methods.iterator();
			while (it.hasNext()) {
				Method method = (Method) it.next();
				String id = method.getName().substring(3);
				Object value = method.invoke(this, new Object[0]);
				Attribute attr = new Attribute(id, value);
				// Check if a name is defined
				AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
				if (getter != null) {
					String name = getter.name();
					if (!WadeAgent.NULL.equals(name)) {
						attr.setName(name);
					}
				}
				// Prepare attribute information required to set the attribute value (if relevant)
				handleSettingInfo(attr, method.getReturnType());
				attributes.add(attr);
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error retrieving attributes.", e);
		}
		return attributes;
	}


	/**
	 * Set the attributes specified in a given list to this agent
	 * @param attributes The list of Attribute objects to set
	 */
	public void setAttributes(List attributes) {
		Iterator it = attributes.iterator();
		try {
			while (it.hasNext()) {
				Attribute attr = (Attribute) it.next();
				setAttribute(attr);
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error setting attributes.", e);
		}
	}

	/**
	 * Returns the DFAgentDescription to be registered with the DF.
	 * @return the DFAgentDescription to be registered with the DF
	 */
	public DFAgentDescription getDFDescription() {
		return DFUtils.createDFAgentDescription(this, arguments);
	}

	private DFAgentDescription prepareDFDescription() {
		DFAgentDescription dfd = getDFDescription();
		dfd.setName(getExposedAID());
		return dfd;
	}
	
	/**
	 * Utility method that returns the virtual AID in case this agent is a replica of 
	 * a virtual agent, and the normal AID if it is not. 
	 * @return
	 */
	public AID getExposedAID() {
		try {
			AgentReplicationHelper rplHelper = (AgentReplicationHelper) getHelper(AgentReplicationHelper.SERVICE_NAME);
			AID virtualAid = rplHelper.getVirtualAid();
			if (virtualAid != null) {
				return virtualAid;
			}
		}
		catch (ServiceNotActiveException snae) {
			// AgentReplicationService not active --> Just do nothing 
		}
		catch (ServiceException se) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error accessing AgentReplicationHelper", se);
		}
		return getAID();
	}

	/**
	 * Check if this agent is performing some activity. The default implementation returns <code>false</code>.
	 * Subclasses are expected to override it to apply agent-specific criteria.
	 * @return true if this agent is performing some activity (false otherwise)
	 */
	public boolean isWorking() {
		return false;
	}

	/**
	 * This method is automatically invoked when a WADE-based application is shutting down in a smooth way.
	 * Its default implementation just does nothing.
	 * The return value indicates that this agent is still working and needs more time before the shut-down 
	 * can actually take place. The default implementation gets that indication from the <code>isWorking</code>
	 * method.<br>
	 * Subclasses can override this method in case they need to implement application specific shutdown preparation
	 * operations.<br>
	 * It should be noticed that if some agents is still working the whole prepare-for-shutdown procedure is 
	 * repeated. As a consequence this method may be invoked more than one time. 
	 * @return a boolean value indicating that this agent is still working and needs more time before the shut-down 
	 * can actually take place.
	 */
	public boolean prepareForShutdown() {
		return isWorking();
	}
	
	boolean prepareForShutdown(long time) {
		if (!shuttingDown) {
			shutdownInitiationTime = new Date(time);
			shuttingDown = true;
		}
		return prepareForShutdown();
	}
	
	/**
	 * Return <code>true</code> if this agent has already received a prepare-for-shutdown request.
	 * @return <code>true</code> if this agent has already received a prepare-for-shutdown request
	 */
	public final boolean isShuttingDown() {
		return shuttingDown;
	}
	
	/**
	 * Return the time (in form of a Date object) of the first call to the prepareForShutdown() method
	 * @return the time (in form of a Date object) of the first call to the prepareForShutdown() method
	 */
	public final Date getShutdownInitiationTime() {
		return shutdownInitiationTime;
	}

	public boolean belongToChildProject() {
		// The -project-name configuration option is only specified in containers belonging to child projects
		return getProperty(WadeAgent.PROJECT_NAME, null) != null;
	}
	
	
	/////////////////////////////////////////////
	// Attributes retrieval methods
	/////////////////////////////////////////////
	@AttributeGetter(name="Startup time")
	public Date getStartupTime() {
		return startupTime;
	}

	@AttributeGetter(name="Number of pending messages in agent queue")
	public int getMessageQueueSize() {
		return this.getCurQueueSize();
	}

	@AttributeGetter(name="Restarted after an unexpected down")
	public boolean getRestarted() {
		return restarted;
	}

	/////////////////////////////////////////////
	// Utility methods
	/////////////////////////////////////////////
	private List getAttributeGetterMethods() {
		List methods = new ArrayList();
		Method[] mm = getClass().getMethods();
		for (Method method : mm) {
			if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
				AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
				if (getter != null) {
					methods.add(method);
				}
			}
		}
		return methods;
	}

	private Method getAttributeSetterMethod(String attrId, Class paramType) throws Exception {
		String capitalizedName = Character.toUpperCase(attrId.charAt(0))+attrId.substring(1);
		String setMethodName = "set"+capitalizedName;
		if (paramType == null) {
			// Retrieve the param type from the return value of the corresponding getter method
			String getMethodName = "get"+capitalizedName;
			Method getter = getClass().getMethod(getMethodName, new Class[0]);
			paramType = getter.getReturnType();
		}
		return getClass().getMethod(setMethodName, new Class[]{paramType});
	}

	private void handleSettingInfo(Attribute attr, Class c) throws Exception {
		try {
			int type = getType(c);
			attr.setType(type);

			String id = attr.getId();
			Method setMethod = getAttributeSetterMethod(id, c);
			AttributeSetter setter = setMethod.getAnnotation(AttributeSetter.class);
			if (setter != null) {
				attr.setReadOnly(false);
				// Default value
				String defaultStr = setter.defaultValue();
				Object defaultValue = null;
				if (defaultStr != null && !defaultStr.equals(WadeAgent.NULL)) {
					// Default value specified --> Use it
					defaultValue = Attribute.decode(defaultStr, type);
				}
				else {				
					String defaultGetterMethodName = setter.defaultValueMethod();
					if (defaultGetterMethodName != null && !defaultGetterMethodName.equals(WadeAgent.NULL)) {
						// Method for default value retrieval specified --> Invoke it
						defaultValue = invokeGetterMethod(defaultGetterMethodName);
					}
				}
				if (defaultValue != null) {
					setDefaultValue(attr, defaultValue);
				}

				// Permitted values
				String[] permittedValuesStr = setter.permittedValues();
				List permittedValues = null;
				if (permittedValuesStr != null && permittedValuesStr.length > 0) {
					// Permitted values specified --> Use them
					permittedValues = decodeList(permittedValuesStr, type);
				}
				else {				
					String permittedValuesMethodName = setter.permittedValuesMethod();
					if (permittedValuesMethodName != null && !permittedValuesMethodName.equals(WadeAgent.NULL)) {
						// Method for permitted values retrieval specified --> Invoke it
						permittedValues = (List) invokeGetterMethod(permittedValuesMethodName);
					}
				}
				if (permittedValues != null) {
					setPermittedValues(attr, permittedValues);
				}
			}
		}
		catch (NoSuchMethodException nsme) {
			// Setter method does not exists --> Attribute is read-only: nothing to do
		}
	}

	private void setDefaultValue(Attribute attr, Object defaultValue) {
		Object value = attr.getValue();
		if (value instanceof Integer) {
			// Convert default value into an Integer in case it is a Long
			if (defaultValue instanceof Long) {
				defaultValue = Integer.valueOf(defaultValue.toString());
			}
		}
		else if (value instanceof Float) {
			// Convert default value into a Float in case it is a Double
			if (defaultValue instanceof Double) {
				defaultValue = Float.valueOf(defaultValue.toString());
			}
		}

		attr.setDefaultValue(defaultValue);
	}

	private void setPermittedValues(Attribute attr, List permittedValues) {
		Object value = attr.getValue();
		if (value instanceof Integer) {
			// Convert permitted values into Integer in case they are Long
			List intPermittedValues = new ArrayList(permittedValues.size());
			for (int i = 0; i < permittedValues.size(); ++i) {
				Object pv = permittedValues.get(i);
				if (pv instanceof Long) {
					intPermittedValues.add(Integer.valueOf(pv.toString()));
				}
			}
			permittedValues = intPermittedValues;
		}
		else if (value instanceof Float) {
			// Convert permitted values into Float in case they are Double
			List floatPermittedValues = new ArrayList(permittedValues.size());
			for (int i = 0; i < permittedValues.size(); ++i) {
				Object pv = permittedValues.get(i);
				if (pv instanceof Double) {
					floatPermittedValues.add(Float.valueOf(pv.toString()));
				}
			}
			permittedValues = floatPermittedValues;
		}

		// If the current value is not among the permitted values, add it
		if (!permittedValues.contains(value)) {
			permittedValues.add(value);
		}

		attr.setPermittedValues(permittedValues);
	}

	private Object invokeGetterMethod(String methodName) {
		Object value = null;
		try {
			if (methodName != null) {
				Method m = getClass().getMethod(methodName, new Class[0]);
				value = m.invoke(this, new Object[0]);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	private List decodeList(String[] permittedValuesStr, int type) throws Attribute.FormatException {
		List l = new ArrayList(permittedValuesStr.length);
		for (String valueStr : permittedValuesStr) {
			l.add(Attribute.decode(valueStr, type));
		}
		return l;
	}

	private int getType(Class c) {
		if (c.equals(String.class)) {
			return Attribute.STRING_TYPE;
		}
		else if (c.equals(Integer.TYPE) || c.equals(Long.TYPE)) {
			return Attribute.INTEGER_TYPE;
		}
		else if (c.equals(Boolean.TYPE)) {
			return Attribute.BOOLEAN_TYPE;
		}
		else if (c.equals(Date.class)) {
			return Attribute.DATE_TYPE;
		}
		else if (c.equals(Float.TYPE) || c.equals(Double.TYPE)) {
			return Attribute.FLOAT_TYPE;
		}
		else if (Serializable.class.isAssignableFrom(c)) {
			return Attribute.SERIALIZABLE_TYPE;
		}
		else {
			return Attribute.NO_TYPE;
		}
	}

	private void setAttribute(Attribute attr) throws Exception {
		Method m = getAttributeSetterMethod(attr.getId(), null);
		Object val = attr.getValue();
		try {
			m.invoke(this, new Object[]{val});
		}
		catch (IllegalArgumentException iae) {
			// Maybe this is due to a Long/Integer or Double/Float mismatch
			try {
				if (val instanceof Long) {
					// Retry as an Integer
					val = new Integer(((Long) val).intValue());
					m.invoke(this, new Object[]{val});
				}
				else if (val instanceof Double) {
					// Retry as a Float
					val = new Float(((Double) val).floatValue());
					m.invoke(this, new Object[]{val});
				}
				else {
					throw iae;
				}
			}
			catch (Exception e) {
				// Rethrow the original exception 
				throw iae;
			}
		}
	}

	public int getCurrentLoad() {
		return WadeAgent.CURRENT_LOAD_UNKNOWN;
	}
}
