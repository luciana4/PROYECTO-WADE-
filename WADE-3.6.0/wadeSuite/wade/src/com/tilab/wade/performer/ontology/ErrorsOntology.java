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
package com.tilab.wade.performer.ontology;

//#MIDP_EXCLUDE_FILE

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.ReflectiveIntrospector;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

/**
 Define schemas for domain independent errors.
 
 @author Giovanni Caire - TILAB
 */
public class ErrorsOntology extends Ontology implements ErrorsVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new ErrorsOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private ErrorsOntology() {
		super(ONTOLOGY_NAME, new Ontology[]{BasicOntology.getInstance()}, new ReflectiveIntrospector());
		
		try {
			add(new PredicateSchema(GENERIC_ERROR), GenericError.class);
			add(new PredicateSchema(NOTIFICATION_ERROR), NotificationError.class);
			
			// GENERIC_ERROR
			PredicateSchema ps = (PredicateSchema) getSchema(GENERIC_ERROR);
			ps.add(GENERIC_ERROR_REASON, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
		} 
		catch (OntologyException oe) {
			oe.printStackTrace();
		} 
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
