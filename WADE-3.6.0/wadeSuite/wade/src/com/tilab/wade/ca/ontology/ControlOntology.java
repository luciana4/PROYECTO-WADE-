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

import jade.content.onto.*;
import jade.content.schema.*;

import com.tilab.wade.commons.ontology.WadeManagementOntology;

/**
 @author Giovanni Caire - TILAB
 */
public class ControlOntology extends Ontology implements ControlVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new ControlOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private ControlOntology() {
		super(ONTOLOGY_NAME, WadeManagementOntology.getInstance(), new ReflectiveIntrospector());
		
		try {
			// CREATE_AGENT 
			add(new AgentActionSchema(CREATE_AGENT), CreateAgent.class);
			AgentActionSchema as = (AgentActionSchema) getSchema(CREATE_AGENT);
			as.add(CREATEAGENT_AGENT_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING)); 
			as.add(CREATEAGENT_CLASS_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING)); 
			as.add(CREATEAGENT_AGENT_ARGUMENTS, (TermSchema) TermSchema.getBaseSchema(), 0, ObjectSchema.UNLIMITED); 

			// KILL_AGENT 
			add(new AgentActionSchema(KILL_AGENT), KillAgent.class);
			as = (AgentActionSchema) getSchema(KILL_AGENT);
			as.add(KILL_AGENT_AGENT, (ConceptSchema) getSchema(BasicOntology.AID));			
		
			// ASK_FOR_EXECUTOR 
			add(new AgentActionSchema(ASK_FOR_EXECUTOR), AskForExecutor.class);
			as = (AgentActionSchema) getSchema(ASK_FOR_EXECUTOR);
			as.add(ASK_FOR_EXECUTOR_POOL_SIZE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));			
			
			// SET_AUTO_RESTART 
			add(new AgentActionSchema(SET_AUTO_RESTART), SetAutoRestart.class);
			as = (AgentActionSchema) getSchema(SET_AUTO_RESTART);
			as.add(SET_AUTO_RESTART_AUTORESTART, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN));			
		}
		catch (OntologyException oe) {
			oe.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
