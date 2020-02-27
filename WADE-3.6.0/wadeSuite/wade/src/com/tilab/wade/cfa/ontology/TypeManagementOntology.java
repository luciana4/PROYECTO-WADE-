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
package com.tilab.wade.cfa.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.JavaCollectionOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.domain.FIPAAgentManagement.Property;

import com.tilab.wade.commons.AgentRole;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.EventType;

public class TypeManagementOntology extends BeanOntology {

	public static final String ONTOLOGY_NAME = "Type-management-ontology";
	
	// The singleton instance of this ontology
	private final static Ontology INSTANCE = new TypeManagementOntology();

	public final static Ontology getInstance() {
		return INSTANCE;
	}

	private TypeManagementOntology() {
		super(ONTOLOGY_NAME, new Ontology[] { JavaCollectionOntology.getInstance() });
		
		try {
			add(AgentType.class);
			add(AgentRole.class);
			add(Property.class);
			add(EventType.class);
			
			add(GetType.class);
			add(GetTypes.class);
			add(GetTypeProperties.class);
			add(GetRole.class);
			add(GetRoles.class);
			add(GetRoleProperties.class);
			add(GetGlobalProperties.class);
			add(GetEventTypes.class);
			add(GetEventType.class);
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
