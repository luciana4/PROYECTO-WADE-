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

/**
 * Vocabulary interface for the EventOntology
 * @author Giovanni Caire - TILAB
 */
public interface EventVocabulary {
	public static final String ONTOLOGY_NAME = "Event-ontology";
	
	// Predicates
	public static final String OCCURRED = "occurred";
	public static final String OCCURRED_TIME = "time";
	public static final String OCCURRED_SOURCE = "source";
	public static final String OCCURRED_EVENT = "event";
	
	public static final String MATCH = "match";
	public static final String MATCH_EVENT_TEMPLATE = "event-template";
	public static final String MATCH_EXECUTION_ID = "execution-id";
	public static final String MATCH_EXCLUSIVE = "exclusive";
	public static final String MATCH_FUTUREEVENTS_ONLY = "future-events-only";

	// Concepts
	public static final String EVENT_TEMPLATE = "event-template";
	public static final String EVENT_TEMPLATE_TYPE = "event-type";
	public static final String EVENT_TEMPLATE_EXPRESSION = "event-identification-expression";
	public static final String EVENT_TEMPLATE_PARAMS = "params";
	public static final String EVENT_TEMPLATE_TAG = "tag";
	
	public static final String GENERIC_EVENT = "generic-event";
	public static final String GENERIC_EVENT_TYPE = "type";
	public static final String GENERIC_EVENT_TIME_TO_LEAVE = "time-to-leave";
	public static final String GENERIC_EVENT_PARAMS = "params";
	public static final String GENERIC_EVENT_PROPERTIES = "properties";
	
	public static final String EVENT_SOURCE = "event-source";
	public static final String EVENT_SOURCE_TYPE = "type";
	public static final String EVENT_SOURCE_ID = "id";
	
	public static final String PARAMETER = "parameter";
	public static final String PARAMETER_NAME = "name";
	public static final String PARAMETER_WRAPPED_VALUE = "wrapped-value";
}