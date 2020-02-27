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
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.AgentPoolInfo;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.CAUtils;

class AgentPoolsManager extends ParallelBehaviour {
	
	// Searching timeout of RAA agent on DF
	private static final long RAA_SEARCH_TIMEOUT = 5000;

	private Logger logger = Logger.getMyLogger(getClass().getName());
	private AID raaAid;
	private Collection<AgentPoolInfo> agentPools;
	
	public AgentPoolsManager(Collection<AgentPoolInfo> agentPools, AgentStartingListener agentStartingListener) {
		super(ParallelBehaviour.WHEN_ALL);
		
		this.agentPools = agentPools;
		
		AgentPoolInfo api;
		Iterator pools = agentPools.iterator();
		AgentPoolStarter aps;
		while (pools.hasNext()) {
			api = (AgentPoolInfo)pools.next();
			aps = new AgentPoolStarter(api, agentStartingListener);
			addSubBehaviour(aps);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		
		try {
			// The Template to retrieve the RAA
			DFAgentDescription raaTemplate  = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(WadeAgent.RAA_AGENT_TYPE);
			raaTemplate.addServices(sd);
			
			DFAgentDescription dfds[] = DFService.searchUntilFound(myAgent, myAgent.getDefaultDF(), raaTemplate, null, RAA_SEARCH_TIMEOUT);
			if (dfds != null && dfds.length > 0) {
				raaAid = dfds[0].getName();
			} else {
				throw new Exception("RAA agent not found on DF");
			}
		} catch (Exception e) {
			logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Unable to find agent raa", e);
			
			AgentPoolInfo api;
			Iterator pools = agentPools.iterator();
			while (pools.hasNext()) {
				api = (AgentPoolInfo)pools.next();
				// FIXME create a proper error code
				api.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + " error searching for raa " +e.getMessage());
			}
		}
	}
	
	private static String buildAgentName(AgentPoolInfo pool, int index) {
		String prefix = pool.getName();
		String result = "000"+Integer.toString(index);
		result = result.substring(result.length()-4, result.length());
		result = prefix+"_"+result;
		return result;
	}
	
	static void appendToPoolErrorCode(AgentPoolInfo api, List agentsNotStarted, String error) {
		// StringBuffer used only for java 1.4 compatibility
		StringBuffer sb = new StringBuffer();
		if (api.getErrorCode() != null) {
			sb.append(api.getErrorCode());
			sb.append(" -- ");
		}
		Iterator iter = agentsNotStarted.iterator();
		while (iter.hasNext()) {
			sb.append((String)iter.next());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		// FIXME create a proper errorCode
		api.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + error + ": " + sb.toString());
	}
	
	public static List<String> getAgentNames(AgentPoolInfo pool) {
		List<String> names = new ArrayList<String>();
		for (int i = 1; i <= pool.getSize(); i++) {
			names.add(buildAgentName(pool, i));
		}
		return names;
	}

	
	/*
	 * Inner class AgentPoolStarter
	 */
	private class AgentPoolStarter extends Behaviour {
	
		private Logger logger = Logger.getMyLogger(getClass().getName());
		
		private AgentPoolInfo pool;
		private int currentAgent;
		private List<String> agentsNotStarted = new ArrayList<String>();
		private AgentStartingListener agentStartingListener;
	
		public AgentPoolStarter(AgentPoolInfo pool, AgentStartingListener agentStartingListener) {
			this.pool = pool;
			this.agentStartingListener = agentStartingListener;
			currentAgent = 1;
		}
	
		private void createAgent(String name, boolean master) {
			String agentName = buildAgentName(pool, currentAgent);
			AgentInfo ai = new AgentInfo();
			ai.setName(agentName);
			ai.setClassName(pool.getClassName());
			ai.setOwner(pool.getOwner());
			ai.setType(pool.getType());
			ai.setGroup(pool.getGroup());
			ai.getParameters().addAll(pool.getParameters());
			ai.addParameter(new AgentArgumentInfo(WadeAgent.AGENT_POOL, pool.getName()));
			if (master) {
				ai.addParameter(new AgentArgumentInfo(WadeAgent.AGENT_MASTER, true));
			}
			
			try {
				logger.log(Logger.FINER, "Agent "+myAgent.getName()+": Starting agent " + agentName + " via raa");
	            CAUtils.createAgent(myAgent, ai, raaAid);
				if (agentStartingListener != null) {
					agentStartingListener.agentStarting(ai.getName());
				}
			} catch (Exception e) {
				logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Unable to start agent " + agentName, e);
				agentsNotStarted.add(agentName);
			}
		}
	
		public void action() {
			String name = buildAgentName(pool, currentAgent);
			createAgent(name, currentAgent==1);
			currentAgent++;
		}
	
		public boolean done() {
			return currentAgent > pool.getSize();
		}
	
		public int onEnd() {
			if (agentsNotStarted.size() > 0) {
				appendToPoolErrorCode(pool, agentsNotStarted, "agent creation error");
			}
			return super.onEnd();
		}
		
	} // End of inner class AgentPoolStarter 
}
