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
package com.tilab.wade.commons.ontology;

import jade.content.onto.*;
import jade.content.schema.*;

public class WadeManagementOntology extends Ontology implements WadeManagementVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new WadeManagementOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private WadeManagementOntology() {
		super(ONTOLOGY_NAME, new Ontology[]{ BasicOntology.getInstance(), SerializableOntology.getInstance()}, new ReflectiveIntrospector());
		
		try {
			add(new ConceptSchema(ATTRIBUTE), Attribute.class);
			add(new AgentActionSchema(GET_AGENT_ATTRIBUTES), GetAgentAttributes.class);
			add(new AgentActionSchema(SET_AGENT_ATTRIBUTES), SetAgentAttributes.class);
			add(new AgentActionSchema(SET_AGENT_ATTRIBUTE), SetAgentAttribute.class);
			add(new AgentActionSchema(PREPARE_FOR_SHUTDOWN), PrepareForShutdown.class);
			add(new AgentActionSchema(GET_CURRENT_LOAD), GetCurrentLoad.class);
			
			// ATTRIBUTE
			ConceptSchema cs = (ConceptSchema) getSchema(ATTRIBUTE);
			cs.add(ATTRIBUTE_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(ATTRIBUTE_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(ATTRIBUTE_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			cs.add(ATTRIBUTE_READ_ONLY, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			cs.add(ATTRIBUTE_DEFAULT_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			cs.add(ATTRIBUTE_PERMITTED_VALUES, (TermSchema) TermSchema.getBaseSchema(), 0, ObjectSchema.UNLIMITED);
			cs.add(ATTRIBUTE_TYPE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);			
			
			//GET_AGENT_ATTRIBUTES
			AgentActionSchema as = (AgentActionSchema) getSchema(GET_AGENT_ATTRIBUTES);
			as.add(GET_AGENT_ATTRIBUTES_AGENT_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			
			//SET_AGENT_ATTRIBUTES
			as = (AgentActionSchema) getSchema(SET_AGENT_ATTRIBUTES);
			as.add(SET_AGENT_ATTRIBUTES_VALUES, (ConceptSchema) getSchema(ATTRIBUTE), 0, ObjectSchema.UNLIMITED);
			
			//SET_AGENT_ATTRIBUTE
			as = (AgentActionSchema) getSchema(SET_AGENT_ATTRIBUTE);
			as.add(SET_AGENT_ATTRIBUTE_VALUE, (ConceptSchema) getSchema(ATTRIBUTE), ObjectSchema.MANDATORY);
		}
		catch (OntologyException oe) {
			oe.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
