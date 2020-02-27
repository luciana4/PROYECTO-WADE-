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

import jade.core.Specifier;
import jade.core.behaviours.Behaviour;
import jade.util.Logger;
import jade.wrapper.AgentController;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.CAUtils;

/**
 * This behaviour is responsible for starting agents on main-container specified in configuration
 */
class MainAgentsManager extends Behaviour {		

	private Logger logger = Logger.getMyLogger(getClass().getName());
	
	private Iterator agentsIt;
	private AgentStartingListener agentStartingListener;
	
	public MainAgentsManager(Collection<AgentInfo> agents, AgentStartingListener agentStartingListener) {
		this.agentsIt = agents.iterator();
		this.agentStartingListener = agentStartingListener;
	}
	
	public void action() {
		if (agentsIt.hasNext()) {
			AgentInfo agentInfo = (AgentInfo) agentsIt.next();
			if (agentInfo.getErrorCode() == null) {
				try {
					logger.log(Logger.INFO, "Agent "+myAgent.getName()+": Starting agent " + agentInfo.getName() + " in MAIN-Container");

					Specifier s = CAUtils.agentInfoToSpecifier(agentInfo);
					// Add a suitable property to be able to identify Main Container agents to be killed at platform shutdown
					Object[] args = s.getArgs();
					Map m = (Map) args[0];
					m.put(WadeAgent.MAIN_AGENT, "true");
					
					// Create agent
					AgentController ac = myAgent.getContainerController().createNewAgent(s.getName(), s.getClassName(), args);
					ac.start();
					if (agentStartingListener != null) {
						agentStartingListener.agentStarting(s.getName());
					}
					
				} catch (Exception e) {
					logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Unable to start agent "+agentInfo.getName(), e);
					agentInfo.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + e.getMessage());						
				}
			}
		}
	}

	public boolean done() {
		return !agentsIt.hasNext();
	}
}
