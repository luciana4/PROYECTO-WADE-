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
package com.tilab.wade.event;

//#MIDP_EXCLUDE_FILE

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.ontology.WorkflowManagementVocabulary;

import jade.content.onto.*;
import jade.content.schema.*;
import jade.domain.FIPAAgentManagement.FIPAManagementVocabulary;
import jade.domain.FIPAAgentManagement.Property;

/**
 * Base ontology for ontologies describing events
 * @author Giovanni Caire - TILAB
 */
public class EventOntology extends Ontology implements EventVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new EventOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private EventOntology() {
		super(ONTOLOGY_NAME, new Ontology[]{BasicOntology.getInstance(), SerializableOntology.getInstance()}, new CFReflectiveIntrospector());
		
		try {
			add(new PredicateSchema(OCCURRED), Occurred.class);
			add(new PredicateSchema(MATCH), Match.class);
			add(new ConceptSchema(EVENT_TEMPLATE), EventTemplate.class);
			add(new ConceptSchema(GENERIC_EVENT), GenericEvent.class);
			add(new ConceptSchema(EVENT_SOURCE), EventSource.class);
			add(new ConceptSchema(PARAMETER), Parameter.class);
			add(new ConceptSchema(FIPAManagementVocabulary.PROPERTY), Property.class);
			
			// OCCURRED
			PredicateSchema ps = (PredicateSchema) getSchema(OCCURRED);
			ps.add(OCCURRED_TIME, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			ps.add(OCCURRED_EVENT, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.MANDATORY);	
			ps.add(OCCURRED_SOURCE, (ConceptSchema) getSchema(EVENT_SOURCE), ObjectSchema.OPTIONAL);

			// MATCH
			ps = (PredicateSchema) getSchema(MATCH);
			ps.add(MATCH_EVENT_TEMPLATE, (ConceptSchema) getSchema(EVENT_TEMPLATE), ObjectSchema.MANDATORY);
			ps.add(MATCH_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			ps.add(MATCH_EXCLUSIVE, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.MANDATORY);	
			ps.add(MATCH_FUTUREEVENTS_ONLY, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.MANDATORY);
			
			// EVENT_TEMPLATE
			ConceptSchema cs = (ConceptSchema) getSchema(EVENT_TEMPLATE);
			cs.add(EVENT_TEMPLATE_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(EVENT_TEMPLATE_EXPRESSION, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(EVENT_TEMPLATE_PARAMS, (ConceptSchema) getSchema(WorkflowManagementVocabulary.PARAMETER), 0, ObjectSchema.UNLIMITED);
			cs.add(EVENT_TEMPLATE_TAG, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			
			// EVENT_SOURCE
			cs = (ConceptSchema) getSchema(EVENT_SOURCE);
			cs.add(EVENT_SOURCE_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(EVENT_SOURCE_ID, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.MANDATORY);
			
			// PARAMETER
			cs = (ConceptSchema) getSchema(PARAMETER);
			cs.add(PARAMETER_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);        
			cs.add(PARAMETER_WRAPPED_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			
			// GENERIC_EVENT
			cs = (ConceptSchema) getSchema(GENERIC_EVENT);
			cs.add(GENERIC_EVENT_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(GENERIC_EVENT_TIME_TO_LEAVE, (PrimitiveSchema) getSchema(BasicOntology.DATE), ObjectSchema.OPTIONAL);
			cs.add(GENERIC_EVENT_PARAMS, (ConceptSchema) getSchema(WorkflowManagementVocabulary.PARAMETER), 0, ObjectSchema.UNLIMITED);
			cs.add(GENERIC_EVENT_PROPERTIES, (ConceptSchema) getSchema(FIPAManagementVocabulary.PROPERTY), 0, ObjectSchema.UNLIMITED);
			
			// PROPERTY
		  	cs = (ConceptSchema)getSchema(FIPAManagementVocabulary.PROPERTY);
		  	cs.add(FIPAManagementVocabulary.PROPERTY_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
		  	cs.add(FIPAManagementVocabulary.PROPERTY_VALUE, (TermSchema)TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
		} 
		catch (OntologyException oe) {
			oe.printStackTrace();
		} 
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
