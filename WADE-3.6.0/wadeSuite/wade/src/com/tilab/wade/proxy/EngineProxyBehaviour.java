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
package com.tilab.wade.proxy;

import java.util.List;

import jade.content.AgentAction;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.util.Logger;

import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.utils.behaviours.ActionExecutor;

abstract class EngineProxyBehaviour<ActionT extends AgentAction, ResultT> extends ActionExecutor<ActionT, ResultT> {

	protected static Logger logger = Logger.getMyLogger(EngineProxyBehaviour.class.getName());;

	public EngineProxyBehaviour(ActionT action, Ontology ontology, AID actor) {
		super(action, ontology, actor);
	}

	protected ResultT getEngineProxyResult() throws EngineProxyException {
		if (outcome.isSuccessful()) {
			return getResult();
		}
		else {
			throw new EngineProxyException(outcome.getErrorMsg());
		}
	}
	
	protected AID getWSMAActor() throws FIPAException {
		return getWSMAActor(myAgent);
	}
	
	static AID getWSMAActor(Agent agent) throws FIPAException {
		AID aid = null;
		DFAgentDescription[] dfds = DFUtils.searchAllByType(agent, WadeAgent.WSMA_AGENT_TYPE, null);
		if (dfds.length > 0) {
			int randomPos = (int)(Math.random() * dfds.length);
			aid = DFUtils.getAID(dfds[randomPos]);
		} 
		return aid;
	}
	
	protected AID getCAActor() throws FIPAException {
		return DFUtils.getAID(DFUtils.searchAnyByType(myAgent, WadeAgent.CONTROL_AGENT_TYPE, null));
	}
	
	protected List convertJadeList(jade.util.leap.List leapList) {
		if (leapList != null) {
			return ((jade.util.leap.ArrayList) leapList).toList();
		}
		else {
			return null;
		}
	}
}
