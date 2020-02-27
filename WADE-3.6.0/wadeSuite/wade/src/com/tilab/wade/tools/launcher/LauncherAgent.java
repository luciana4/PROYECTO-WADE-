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
package com.tilab.wade.tools.launcher;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;

import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.tools.launcher.gui.LauncherGUI;

public class LauncherAgent extends Agent {
	
	private LauncherGUI launcherGUI;
	private Logger myLogger;
	private List<AID> performerAgents = new ArrayList<AID>();
	
	protected void setup() {
		myLogger = Logger.getMyLogger(getName());		
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(ConfigurationOntology.getInstance());
		getContentManager().registerOntology(DeploymentOntology.getInstance());

		// Create GUI
		launcherGUI = new LauncherGUI(this);
		launcherGUI.pack();
		launcherGUI.setVisible(true);

	}
	
	public List<AID> getPerformerAgents() {
		return performerAgents;
	}
	
	protected void takeDown() {
		if (launcherGUI != null) {
			launcherGUI.dispose();
		}
	}
}
