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
package com.tilab.wade.tools.management;


import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.tools.management.gui.ManagementGUI;


public class ManagementAgent extends Agent {
	private ManagementGUI mngGUI;
	
	private Logger myLogger;
	
	protected void setup() {
		myLogger = Logger.getMyLogger(getName());		
		
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(ConfigurationOntology.getInstance());
		
		AID cfa = null;
		// The Template to retrieve the CFA
		DFAgentDescription cfaTemplate  = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(WadeAgent.CONFIGURATION_AGENT_TYPE);
		cfaTemplate.addServices(sd);
		try {
			DFAgentDescription dfds[] = DFService.searchUntilFound(this, getDefaultDF(), cfaTemplate, null, 60000);
			if (dfds.length > 0) {
				cfa = dfds[0].getName();
			}
			
			// The Template to retrieve CAs
			DFAgentDescription caTemplate  = new DFAgentDescription();
			sd = new ServiceDescription();
			sd.setType(WadeAgent.CONTROL_AGENT_TYPE);
			caTemplate.addServices(sd);
			
			mngGUI = new ManagementGUI(this, cfa, caTemplate);
			mngGUI.pack();
			mngGUI.setVisible(true);
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Cannot retrieve Configuration Agent", e);
			doDelete();
		}
	}
	
	protected void takeDown() {
		if (mngGUI != null) {
			mngGUI.dispose();
		}
	}
}
