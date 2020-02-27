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
package com.tilab.wade.raa;

import jade.content.AgentAction;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.tilab.wade.ca.ontology.ControlOntology;
import com.tilab.wade.ca.ontology.CreateAgent;
import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.AttributeSetter;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.commons.WadeBasicResponder;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.condition.MatcherException;

/**
 * This is the agent responsible for allocating agents at runtime.
 * More in details whenever an agent has to be started and there is no explicit indication 
 * about the container where to start it, the agent creation is delegated to the RAA. 
 * The RAA selects a container according to the policies defined in its configuration file
 * and then creates the new agent through the ControlAgent running on the selected container.<br>
 * <br>
 * The RAA configuration file can be set either by means of the <code>cfgFilename</code> argument
 * in the application configuration file or by means of the <code>cfgFilename</code> property of the
 * <code>Runtime Allocator Agent</code> type in the types file.<br>
 * If none is specified, the default <code>/raa/raa.xml</code> file is used.<br>
 * In all cases the configuration file is searched in the classpath first and then, if not found,
 * in the file system.<br>
 * <br>
 * The RAA configuration file is an XML file containing a set of allocation-rules each one composed of
 * <ul>
 * <li>A <b>condition</b> defining which agents will be considered by this rule</li>
 * <li>An allocation <b>policy</b> defining how to actually allocate the considered agents across available containers</li>
 * </ul>
 * The condition specifies a list of semicolon (';') separated boolean formulas that apply to the name, className,
 * type, role and arguments of the agent to be created. The condition is met if all 
 * formulas evaluate to <code>true</code>. A few examples are provided below
 * <ul>
 * <li><code>type==Workflow Engine Agent</code> --> Matches all agents of type <code>Workflow Engine Agent</code></li>
 * <li><code>role!=Workflow Executor;arguments.foo==*</code> --> Matches all agents that do not have the 
 * <code>Workflow Executor</code> role and that have the <code>foo</code> argument specified to whatever value</li>
 * </ul>
 * The policy specifies a class implementing the <code>com.tilab.wade.raa.AgentAllocationPolicy</code> 
 * interface and possibly a set of policy specific properties. The policy class is responsible for 
 * identifying the container where to create an agent by means of the <code>getContainer()</code> method.<br>
 * WADE provides a number of ready-made <code>AgentAllocationPolicy</code> classes. Programmers can create
 * new application specific policies by simply developing new classes implementing the 
 * <code>com.tilab.wade.raa.AgentAllocationPolicy</code> interface.  
 * 
 * @see AgentAllocationPolicy 
 * @see com.tilab.wade.raa.policies.RoundRobinPolicy
 * @see com.tilab.wade.raa.policies.HostBasedRRPolicy
 * @see com.tilab.wade.raa.policies.ContainerNameBasedRRPolicy
 * @see com.tilab.wade.raa.policies.JavaProfileBasedRRPolicy
 * @see com.tilab.wade.raa.policies.JadeProfileBasedRRPolicy
 */
public class RuntimeAllocatorAgent extends WadeAgentImpl {

	// RAA type configuration properties
	public static final String CFG_FILENAME_KEY = "cfgFilename";
	private static final String CFG_FILENAME_DEFAULT = "/raa/raa.xml";

	// Attribute IDs
	public static final String CFG_FILENAME_ATTRIBUTE = "CfgFilename";

	//private static final String AGENT_TYPE = "AGENT-TYPE";

	private TypeManager tm = TypeManager.getInstance();

	private List<AllocationRule> allocationRules = null;
	
	private Map<String, DFAgentDescription> controlAgentDescriptions = new HashMap<String, DFAgentDescription>();
	private Map<AID, String> containers = new HashMap<AID, String>();

	///////////////////////////////////////////////////
	// Agent attributes
	///////////////////////////////////////////////////

	private String cfgFilename;

	@AttributeGetter(name="Configuration file name")
	public String getCfgFilename() {
		return cfgFilename;
	}

	@AttributeSetter(defaultValue=CFG_FILENAME_DEFAULT)
	public void setCfgFilename(String newCfgFilename) {

		String oldCfgFilename = cfgFilename;
		List<AllocationRule> oldAllocationRules = null;

		try {
			cfgFilename = newCfgFilename;
			readConfiguration();
			notifyExistingContainers();
			if (!cfgFilename.equals(oldCfgFilename)) {
				arguments.put(CFG_FILENAME_KEY, cfgFilename);
				// Refresh the DF registration
				DFService.modify(this, getDFDescription());
			}
		} catch (ConfigurationException ce) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + " - Cannot load new configuration (restore the previous one). ", ce);
			cfgFilename = oldCfgFilename;
			allocationRules = oldAllocationRules;
		}
		catch (FIPAException fe) {
			myLogger.log(Logger.WARNING, "Agent " + getName() + " - Error modifying DF-registration after loding of new configuration file");
		}
	}


	///////////////////////////////////////////////////
	// WadeAgent methods
	///////////////////////////////////////////////////
	protected void agentSpecificSetup() throws AgentInitializationException {
		try {
			// Register ontology
			getContentManager().registerOntology(ControlOntology.getInstance());

			// Read configuration
			readConfiguration();

			// Subscribe to the DF to notify policies about existing containers
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE).getDescription());
			template.addServices(sd);
			ACLMessage subscriptionMsg = DFService.createSubscriptionMessage(this, getDefaultDF(), template, null);
			addBehaviour(new SubscriptionInitiator(this, subscriptionMsg) {

				protected void handleInform(ACLMessage inform) {
					try {
						DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
						for (int i = 0; i < dfds.length; ++i) {
							AID caAid = dfds[i].getName();
							Iterator services = dfds[i].getAllServices();
							if (services.hasNext()) {
								// CA Registration --> Container started
								ServiceDescription sd = (ServiceDescription) services.next();
								String containerName = (String) DFUtils.getPropertyValue(sd, WadeAgent.AGENT_LOCATION);
								myLogger.log(Level.INFO, "Agent " + myAgent.getName() + ": Container "+containerName+" detected - CA is "+caAid.getName());
								controlAgentDescriptions.put(containerName, dfds[i]);
								containers.put(caAid, containerName);
								notifyNewContainer(containerName, caAid, sd);

							} else {
								// CA De-registration --> Container terminated
								String containerName = containers.remove(caAid);
								if (containerName != null) {
									controlAgentDescriptions.remove(containerName);
									myLogger.log(Level.INFO, "Agent " + myAgent.getName() + ": Container "+containerName+" removed");
									notifyDeadContainer(containerName);
								}
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			throw new AgentInitializationException("Agent " + getName() + ": Error during startup, could not start", e);
		}
	}

	/**
	 * Redefine this method to manage the CreateAgent action of the ControlOntology
	 */
	public WadeBasicResponder getManagementResponder() {
		WadeBasicResponder responder = new WadeBasicResponder(this);
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.or(
						MessageTemplate.MatchOntology(ControlOntology.getInstance().getName()),
						MessageTemplate.MatchOntology(WadeManagementOntology.getInstance().getName())
				),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		responder.setTemplate(template);
		responder.registerHandler(CreateAgent.class, new WadeBasicResponder.ActionHandler() {
			public ACLMessage handleAction(AgentAction act, Action aExpr, ACLMessage request) throws Exception {
				return serveCreateAgent((CreateAgent) act, aExpr, request);
			}
		});
		return responder;
	}
	
	
	///////////////////////////////////////////////////
	// Utility methods
	///////////////////////////////////////////////////
	private ACLMessage serveCreateAgent(CreateAgent createAgent, final Action action, final ACLMessage msg) {
		final AgentInfo info = prepareAgentInfo(createAgent);
		myLogger.log(Level.INFO, "Agent " + getName() + ": CreateAgent request received from " + msg.getSender().getName() + ". Name = " + info.getName() + " class = " + info.getClassName());
		addBehaviour(new AgentStarter(this,msg,action,info));
		// The AgentStarter behaviour will reply asynchronously
		return null;
	}

	/**
	 * Retrieve the AID of the ControlAgent running in a given container
	 */
	AID getCA(String containerName) {
		DFAgentDescription dfd = controlAgentDescriptions.get(containerName);
		if (dfd != null) {
			return dfd.getName();
		}
		else {
			return null;
		}
	}
	
	private AgentInfo prepareAgentInfo(CreateAgent ca) {
		Collection<AgentArgumentInfo> properties = null;
		Object [] args = ca.getArguments();
		String agentType = null;
		String agentGroup = null;
		// Convert arguments passed as values in a Map (if any) to a list of AgentArgumentInfo objects.
		if (args != null && args.length == 1 && args[0] instanceof Map) {
			Map<String, Object> m = (Map<String, Object>) args[0];
			properties = new HashSet<AgentArgumentInfo>();
			Iterator<Entry<String, Object>> iter = m.entrySet().iterator();
			Entry<String, Object> e;
			AgentArgumentInfo aai;
			while (iter.hasNext()) {
				e = iter.next();
				String key = e.getKey();
				Object value = e.getValue();
				// Handle the agent-type in case it is passed as an argument
				if (key.equals(WadeAgent.AGENT_TYPE)) {
					agentType = (String) value;
				}
				// Handle the agent-group in case it is passed as an argument
				if (key.equals(WadeAgent.AGENT_GROUP)) {
					agentGroup = (String) value;
				}
				aai = new AgentArgumentInfo(key, value);
				properties.add(aai);
			}
		}
		String name = ca.getName();
		String className = ca.getClassName();
		AgentType type = null;
		if (agentType == null) {
			type = tm.getTypeForClass(className);
		} else {
			type = tm.getType(agentType);
		}
		if (className == null) {
			className = type.getClassName();
		}
		if (className == null) {
			myLogger.log(Logger.SEVERE, "Agent " + getName() + ": unknown class-name for agent " + name);
		}
		String owner = WadeAgent.NONE_OWNER;
		return new AgentInfo(name, type.getDescription(), className, owner, agentGroup, properties);
	}

	/**
	 * Retrieve all allocation policies whose associated condition matches a given AgentInfo
	 * @param info the AgentInfo that has to be matched
	 * @return all allocation policies whose associated condition matches a the info AgentInfo
	 */
	Iterator<AgentAllocationPolicy> getPolicies(AgentInfo info) {
		List<AgentAllocationPolicy> policies = new LinkedList<AgentAllocationPolicy>();
		if (allocationRules != null) {
			for (AllocationRule allocationRule: allocationRules) {
				try {
					if (allocationRule.getCondition().matches(info)) {
						policies.add(allocationRule.getConfiguration().getPolicy());
					}
				} catch (MatcherException me) {
					myLogger.log(Logger.SEVERE, "Agent " + getName() + ": error trying to match condition of " + allocationRule + " with AgentInfo " + info + "; skipping rule", me);
				}
			}
		}
		myLogger.log(Logger.FINE, "Agent " + getName() + ": "+policies.size()+" policies applying to creation of agent "+info.getName());
		return policies.iterator();
	}

	/**
	 * The configuration file is searched in the classpath. Its name is read
	 * 1) as value of the "cfgFilename" argument
	 * 2) as value of the "cfgFilename" type property
	 * 3) If none of the above is specified the default "raa.xml" filename is used.
	 */
	private void readConfiguration() throws ConfigurationException {
		cfgFilename = null;
		if (arguments != null) {
			cfgFilename = (String) arguments.get(CFG_FILENAME_KEY);
		}
		if (cfgFilename != null) {
			myLogger.log(Logger.CONFIG, "Agent " + getName() + " - Argument " + CFG_FILENAME_KEY + " found. Value = " + cfgFilename);
		} 
		else {
			cfgFilename = (String) tm.getProperties(getType()).get(CFG_FILENAME_KEY);
			if (cfgFilename != null) {
				myLogger.log(Logger.CONFIG, "Agent " + getName() + " - Type-property " + CFG_FILENAME_KEY + " found. Value = " + cfgFilename);
			} 
		}
		if (cfgFilename == null) {
			myLogger.log(Logger.CONFIG, "Agent " + getName() + " - Using default configuration file " + CFG_FILENAME_DEFAULT);
			cfgFilename = CFG_FILENAME_DEFAULT;
		}
		
		myLogger.log(Logger.INFO, "Agent " + getName() + " - Reading agent allocation rules from file " + cfgFilename);
		ConfigurationReader cr = new ConfigurationReader(cfgFilename, this);
		allocationRules = cr.getAllocationRules();
		if (allocationRules == null) {
			myLogger.log(Logger.WARNING, "Agent " + getName() + " - No allocation rule available");
		}
		else{
			myLogger.log(Logger.INFO, "Agent " + getName() + " - Initialized " + allocationRules.size()+" allocation rules");
			myLogger.log(Logger.CONFIG, "Agent " + getName() + " - Allocation rules are: " + allocationRules.toString());
		}

	}

	private void notifyExistingContainers() {
		for (String containerName : controlAgentDescriptions.keySet()) {
			DFAgentDescription dfd = controlAgentDescriptions.get(containerName);
			AID caAid = dfd.getName();
			Iterator services = dfd.getAllServices();
			if (services.hasNext()) {
				ServiceDescription sd = (ServiceDescription) services.next();
				notifyNewContainer(containerName, caAid, sd);
			}
		}
	}
	
	private void notifyNewContainer(String containerName, AID caAid, ServiceDescription sd){
		for (int j = 0; j < allocationRules.size(); j++) {
			allocationRules.get(j).getConfiguration().getPolicy().newContainer(containerName, caAid, sd);
		}
	}

	private void notifyDeadContainer(String containerName){
		for (int j = 0; j < allocationRules.size(); j++) {
			allocationRules.get(j).getConfiguration().getPolicy().deadContainer(containerName);
		}
	}
}
