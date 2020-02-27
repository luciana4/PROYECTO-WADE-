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
package com.tilab.wade.ca.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.SerializableOntology;

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.ontology.ErrorsOntology;

public class DeploymentOntology extends BeanOntology {

	public static final String ONTOLOGY_NAME = "Deployment-ontology";
	public static final String DEPLOY_TOPIC = "Deploy";
	
	// The singleton instance of this ontology
	private final static Ontology INSTANCE = new DeploymentOntology();

	public final static Ontology getInstance() {
		return INSTANCE;
	}

	private DeploymentOntology() {
		super(ONTOLOGY_NAME, new Ontology[] { ErrorsOntology.getInstance(), SerializableOntology.getInstance() });
		
		try {
			add(Parameter.class);
			add(WorkflowDetails.class);
			add(ModuleInfo.class);
			
			add(Deploy.class);
			add(Undeploy.class);
			add(Deployed.class);
			add(Undeployed.class);
			add(Revert.class);
			add(Reverted.class);
			add(GetWorkflowList.class);
			add(GetWorkflowParameters.class);
			add(GetModules.class);
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
