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
package com.tilab.wade.utils.behaviours;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.utils.CFAUtils;

import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

/**
 * Ready-made behaviour that listens to WADE platform startup
 */
public class PlatformStartupListener extends SimpleBehaviour {
	private static final long serialVersionUID = 8984903141850311178L;
	
	private boolean finished = false;
	private AID platformLifeCycleTopic;
	private MessageTemplate template;
	
	private Logger myLogger = Logger.getJADELogger(getClass().getName());
	
	public PlatformStartupListener(Agent a) throws ServiceException {
		super(a);
		
		// Register to the Platform-Lifecycle-Topic
		TopicManagementHelper topicHelper = (TopicManagementHelper) a.getHelper(TopicManagementHelper.SERVICE_NAME);
		platformLifeCycleTopic = topicHelper.createTopic(ConfigurationOntology.PLATFORM_LIFE_CYCLE_TOPIC);
		topicHelper.register(platformLifeCycleTopic);
		// Create the template to receive messages
		template = MessageTemplate.MatchTopic(platformLifeCycleTopic);
	}
	
	/**
	 * Callback method that is invoked when the WADE platform startup process completes.
	 * @param platformStatus The current platform status: it may be either <code>ConfigurationOntology.ACTIVE_STATUS</code>
	 * or <code>ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS</code>
	 */
	public void handlePlatformStartup(String platformStatus) {
	}
	
	public void onStart() {
		// Be sure the Configuration Ontology is properly registered
		if (myAgent.getContentManager().lookupOntology(ConfigurationOntology.getInstance().getName()) == null) {
			myAgent.getContentManager().registerOntology(ConfigurationOntology.getInstance());
		}
	}

	public void action() {
		ACLMessage msg = myAgent.receive(template);
		if (msg != null) {
			try {
				Result r = (Result) myAgent.getContentManager().extractContent(msg);
				String platformStatus = (String) r.getValue();
				if (CFAUtils.isPlatformActive(platformStatus)) {
					// Platform is UP --> Invoke the handlePlatformStartup() callback method 
					// and then deregister from the platform lifecycle topic
					handlePlatformStartup(platformStatus);
					try {
						TopicManagementHelper topicHelper = (TopicManagementHelper) myAgent.getHelper(TopicManagementHelper.SERVICE_NAME);
						topicHelper.deregister(platformLifeCycleTopic);
					}
					catch (Exception e) {
						// Just print a warning
						myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Error deregistering from Platform-Lifecycle topic", e);
					}
					finished = true;
				}
			}
			catch (Exception e) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Error decoding Platform-Lifecycle notification", e);
			}
		}
		else {
			block();
		}
	}
	
	public boolean done() {
		return finished;
	}
}  
