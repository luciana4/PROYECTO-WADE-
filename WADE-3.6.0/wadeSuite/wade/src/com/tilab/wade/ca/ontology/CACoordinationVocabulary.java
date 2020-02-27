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


public interface CACoordinationVocabulary {
	public static final String ONTOLOGY_NAME = "CA-Coordination-ontology";
	
	// Predicates
	public static final String RESTARTING_CONTAINER = "restarting-container";
	public static final String RESTARTING_CONTAINER_CONTAINER_NAME = "container-name";

	public static final String RESTARTING_AGENT = "restarting-agent";
	public static final String RESTARTING_AGENT_AGENT_NAME = "agent-name";
	
	public static final String IS_GLOBAL_PROPERTY = "is-global-property";
	public static final String IS_GLOBAL_PROPERTY_PROPERTY = "property";
	
	public static final String CA_STATUS = "ca-status";
	public static final String CA_STATUS_AUTORESTART = "autorestart";
	public static final String CA_STATUS_GLOBAL_PROPERTIES = "global-properties";
}