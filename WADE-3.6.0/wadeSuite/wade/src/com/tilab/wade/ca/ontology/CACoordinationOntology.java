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

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.ReflectiveIntrospector;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;
import jade.content.schema.TermSchema;
import jade.domain.FIPAAgentManagement.FIPAManagementOntology;
import jade.domain.FIPAAgentManagement.Property;


public class CACoordinationOntology extends Ontology implements CACoordinationVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new CACoordinationOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private CACoordinationOntology() {
		super(ONTOLOGY_NAME, BasicOntology.getInstance(), new ReflectiveIntrospector());
		
		try {
			add(new PredicateSchema(RESTARTING_CONTAINER), RestartingContainer.class);
			add(new PredicateSchema(RESTARTING_AGENT), RestartingAgent.class);
			add(new PredicateSchema(IS_GLOBAL_PROPERTY), IsGlobalProperty.class);
			add(new PredicateSchema(CA_STATUS), CAStatus.class);
			
			add(FIPAManagementOntology.getInstance().getSchema(FIPAManagementOntology.PROPERTY), Property.class);
			
			PredicateSchema ps = (PredicateSchema) getSchema(RESTARTING_CONTAINER);
			ps.add(RESTARTING_CONTAINER_CONTAINER_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING)); 
			
			ps = (PredicateSchema) getSchema(RESTARTING_AGENT);
			ps.add(RESTARTING_AGENT_AGENT_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING)); 
			
			ps = (PredicateSchema) getSchema(IS_GLOBAL_PROPERTY);
			ps.add(IS_GLOBAL_PROPERTY_PROPERTY, (TermSchema) getSchema(FIPAManagementOntology.PROPERTY)); 
			
			ps = (PredicateSchema) getSchema(CA_STATUS);
			ps.add(CA_STATUS_AUTORESTART, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN)); 
			ps.add(CA_STATUS_GLOBAL_PROPERTIES, (TermSchema) getSchema(FIPAManagementOntology.PROPERTY), 0, ObjectSchema.UNLIMITED); 
		}
		catch (OntologyException oe) {
			oe.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
