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
package com.tilab.wade.raa.policies;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Map;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.raa.AgentAllocationPolicy;

/**
 * Base class for agent allocation policies used by the RuntimeAllocatorAgent (RAA)
 * @see com.tilab.wade.raa.RuntimeAllocatorAgent
 */
public abstract class BaseAgentAllocationPolicy implements AgentAllocationPolicy {

	protected String myKey;
	protected Map properties;
	protected Agent myAgent;
	
	public void init(String key, Map properties, Agent agent) {
		myKey = key;
		this.properties = properties;
		myAgent = agent;
	}

	
	public Behaviour getAllocationBehaviour(AgentInfo info) {
		return null;
	}

	public String getKey() {
		return myKey;
	}

	public void newContainer(String containerName, AID ca, ServiceDescription caDescription) {
	}

	public void deadContainer(String containerName) {
	}
}
