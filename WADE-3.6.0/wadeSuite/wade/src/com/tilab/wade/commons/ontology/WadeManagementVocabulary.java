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

public interface WadeManagementVocabulary {
	public static final String ONTOLOGY_NAME = "WADE-management-ontology";
	
	// Concepts
	public static final String ATTRIBUTE = "attribute";
	public static final String ATTRIBUTE_ID = "id";
	public static final String ATTRIBUTE_NAME = "name";
	public static final String ATTRIBUTE_VALUE = "value";
	public static final String ATTRIBUTE_READ_ONLY = "read-only";
	public static final String ATTRIBUTE_DEFAULT_VALUE = "default-value";
	public static final String ATTRIBUTE_PERMITTED_VALUES = "permitted-values";
	public static final String ATTRIBUTE_TYPE = "type";
	
	// Actions
	public static final String GET_AGENT_ATTRIBUTES = "get-agent-attributes";
	public static final String GET_AGENT_ATTRIBUTES_AGENT_NAME = "agentName";
	
	public static final String SET_AGENT_ATTRIBUTES = "set-agent-attributes";
	public static final String SET_AGENT_ATTRIBUTES_VALUES = "values";

	public static final String SET_AGENT_ATTRIBUTE = "set-agent-attribute";
	public static final String SET_AGENT_ATTRIBUTE_VALUE = "value";
	
	public static final String PREPARE_FOR_SHUTDOWN = "prepare-for-shutdown";
	
	public static final String GET_CURRENT_LOAD = "get-current-load";
}