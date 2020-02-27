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
package com.tilab.wade.cfa;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.util.Iterator;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.VirtualAgentInfo;
import com.tilab.wade.utils.CAUtils;

class AgentsStarter extends Behaviour {		
	private static final long serialVersionUID = 11111124L;
	
	private Logger logger = Logger.getMyLogger(getClass().getName());
	
	private AID aidCA = null;
	private Iterator agents;
	private ContainerInfo container;
	private AgentStartingListener agentStartingListener;
	
	public AgentsStarter(AgentStartingListener agentStartingListener) {
		this(null, agentStartingListener);
	}
	
	public AgentsStarter(ContainerInfo container, AgentStartingListener agentStartingListener) {
		super();
		
		this.container = container;
		this.agentStartingListener = agentStartingListener;
	}

	public void setContainer(ContainerInfo container) {
		this.container = container;
		
	}
	
	public void onStart() {
		try {
			aidCA = CAUtils.getCAOnLocation(myAgent, container.getName());
		} catch (FIPAException e) {
			// should never happen, since we already checked for CA existence through CAChecker
			e.printStackTrace();
		}
		agents = container.getAgents().iterator();
	}

	public void action() {
		if (agents.hasNext()) {
			AgentInfo originalCurrentAgent = (AgentInfo) agents.next();
			if (originalCurrentAgent.getErrorCode() == null) {
				try {
					// In case of virtual agents the AgentInfo object will be replaced --> 
					// We need to store the original one 
					AgentInfo currentAgent = originalCurrentAgent;
					if (currentAgent instanceof VirtualAgentInfo) {
						// Virtual Agent: Note that in this case the currentAgent name changes in that of the master replica
						logger.log(Logger.INFO, "Agent "+myAgent.getName()+": Starting virtual agent " + originalCurrentAgent.getName() + " in container " + container.getName());
						currentAgent = ((VirtualAgentInfo) currentAgent).asAgentInfo();
					}
					else {
						// Normal Agent
						logger.log(Logger.INFO, "Agent "+myAgent.getName()+": Starting agent " + originalCurrentAgent.getName() + " in container " + container.getName());
					}
                    CAUtils.createAgent(myAgent, currentAgent, aidCA);
					if (agentStartingListener != null) {
						agentStartingListener.agentStarting(originalCurrentAgent.getName());
					}
				} catch (Exception e) {
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Unable to start agent "+originalCurrentAgent.getName(), e);
					originalCurrentAgent.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());						
				}
			}
		}
	}

	public boolean done() {
		return !agents.hasNext();
	}
}	
