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

import com.tilab.wade.commons.ontology.WadeManagementVocabulary;

public interface ControlVocabulary extends WadeManagementVocabulary {
	public static final String ONTOLOGY_NAME = "Control-ontology";
	
	// The constant returned by the RAA in reply to a CreateAgent request when
	// the selected allocation policy states that the agent must not be recreated
	public static final String NO_CONTAINER = "NO-CONTAINER";
	
	// Actions
	public static final String CREATE_AGENT = "create-agent";
	public static final String CREATEAGENT_AGENT_NAME = "name";
	public static final String CREATEAGENT_CLASS_NAME = "class-name";
	public static final String CREATEAGENT_AGENT_ARGUMENTS = "agent-arguments";
	
	public static final String KILL_AGENT = "kill-agent";
	public static final String KILL_AGENT_AGENT = "agent";

	public static final String ASK_FOR_EXECUTOR = "ask-for-executor";
	public static final String ASK_FOR_EXECUTOR_POOL_SIZE = "pool-size";
	
	public static final String SET_AUTO_RESTART = "set-auto-restart";
	public static final String SET_AUTO_RESTART_AUTORESTART = "autorestart";
}