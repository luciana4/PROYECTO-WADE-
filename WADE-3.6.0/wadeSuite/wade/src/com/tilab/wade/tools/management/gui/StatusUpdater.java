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
package com.tilab.wade.tools.management.gui;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.GetPlatformStatus;

import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPAService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

class StatusUpdater extends CyclicBehaviour {
	private ManagementGUI gui;
	private MessageTemplate template;
	

	public StatusUpdater(ManagementGUI gui) {
		super (null);
		this.gui = gui;
	}
	
	public void onStart() {
		// Retrieve the current status
		retrieveStatus();
		
		// Register to be notified about status changes
		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) myAgent.getHelper(TopicManagementHelper.SERVICE_NAME);
			AID topic = topicHelper.createTopic(ConfigurationOntology.PLATFORM_LIFE_CYCLE_TOPIC);
			topicHelper.register(topic);
			// Initialize the MessageTemplate
			template = MessageTemplate.MatchTopic(topic);
		}
		catch (ServiceException se) {
			se.printStackTrace();
			gui.log("Error registering to platform life-cycle topic. "+se);
		}
	}

	public void action() {
		ACLMessage msg = myAgent.receive(template);
		if (msg != null) {
			if (msg.getPerformative() == ACLMessage.INFORM) {
				try {
					Result result = (Result) myAgent.getContentManager().extractContent(msg);
					gui.setStatus((String) result.getValue());
				} 
				catch (Exception e) {
					e.printStackTrace();
					gui.log("Error decoding platform life-cycle notification message. "+e);
				}
			}
		}
		else {
			block();
		}
	}
	
	private void retrieveStatus() {
		try {
			ACLMessage request = gui.createCfaRequest(new GetPlatformStatus());
			
	        ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, request, ManagementGUI.CFA_TIMEOUT);
	        
	        if (reply != null) {
				Result result = (Result) myAgent.getContentManager().extractContent(reply);
				gui.setStatus((String) result.getValue());
	        }
	        else {
				gui.log("Timeout expired while retrieving platform status from Configuration Agent");
	        }
		}
		catch (Exception e) {
			e.printStackTrace();
			gui.log("Error retrieving platform status from Configuration Agent. "+e);
		}
	}
}