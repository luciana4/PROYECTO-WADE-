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
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.cfa.beans.AgentPoolInfo;
import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;

/**
 * This behaviour is responsible for starting up a WADE based application according to a PlatformInfo object.
 * More in details it performs the following steps:
 * 0) Activates all main-container agents if any
 * 1) Activates all backup-mains, containers and explicitly allocated agents in each host (activations in 
 * different hosts occur in parallel)
 * 2) Activates all agent pools if any
 * 3) Wait for either all activated agents to be registered with the DF or a given timeout expires 
 */
abstract class ConfigurationStarter extends SequentialBehaviour implements AgentStartingListener, AgentStartedListener {

	protected Logger logger = Logger.getMyLogger(getClass().getName());
	protected PlatformInfo platformInfo;
	protected ConfigurationAgent cfa;
	
	private DFSubscriber dfSubscriber;
	private Set<String> startingAgents;
	private boolean timeoutExpired;
	private Behaviour checkStartedAgentsBehaviour;
	private WakerBehaviour checkStartedAgentsTimeoutBehaviour;
	private int agentsStartedTimeout;

	protected abstract Behaviour getHostManager();
	
	public ConfigurationStarter(ConfigurationAgent cfa, PlatformInfo platformInfo) {
		super(cfa);

		this.cfa = cfa;

		startingAgents = new HashSet<String>();
		timeoutExpired = false;

		agentsStartedTimeout = Integer.parseInt(cfa.getCfaProperty(ConfigurationAgent.AGENTS_STARTED_TIMEOUT_KEY, ConfigurationAgent.AGENTS_STARTED_TIMEOUT_DEFAULT));

		this.platformInfo = platformInfo;

		//////////////////////////////////////////////////////////////////////
		// Step-0: A simple behaviour to manage the main agents
		//////////////////////////////////////////////////////////////////////
		if (platformInfo.getMainAgents().size() > 0) {
			MainAgentsManager mainAgentsStarter = new MainAgentsManager(platformInfo.getMainAgents(), this);
			addSubBehaviour(mainAgentsStarter);
		}
		
		//////////////////////////////////////////////////////////////////////
		// Step-1: A parallel behaviour with one child of type HostManager for each host
		// For each host we need to compute how many backup main container must be created.
		//////////////////////////////////////////////////////////////////////
		addSubBehaviour(getHostManager());

		
		///////////////////////////////////////////////////////////////////////////
		// Step-2: A parallel behaviour with one child for each agent pool
		///////////////////////////////////////////////////////////////////////////
		if (platformInfo.getAgentPools().size() > 0) {
			AgentPoolsManager agentPoolsManager = new AgentPoolsManager(platformInfo.getAgentPools(), this);
			addSubBehaviour(agentPoolsManager);
		}

		
		/////////////////////////////////////////////////////////////////////////
		// Step-3: A parallel behaviour with two children
		// - Child 1: Completes when all agents successfully started
		// - Child 2: A waker behaviour that times-out if no agent startup notification
		// is received for more than a given timeout (at first time this timeout expanded
		// to keep initialization operations into account). This behaviour is reset at each 
		// agent startup notification. 
		/////////////////////////////////////////////////////////////////////////
		checkStartedAgentsBehaviour = new SimpleBehaviour() {
			@Override
			public void action() {
				block();
			}

			@Override
			public boolean done() {
				logger.log(Logger.FINE, "Agent "+myAgent.getName()+": there are still "+startingAgents.size()+" agents starting");
				return startingAgents.size() == 0;
			}
		};

		checkStartedAgentsTimeoutBehaviour = new WakerBehaviour(myAgent, agentsStartedTimeout*4) {
			@Override
			protected void onWake() {
				// timeout!
				timeoutExpired = true;
				logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": timeout waiting for agents to start, "+startingAgents.size()+" agents did not start");
			}
		};

		ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ANY);
		pb.addSubBehaviour(checkStartedAgentsTimeoutBehaviour);
		pb.addSubBehaviour(checkStartedAgentsBehaviour);

		addSubBehaviour(pb);
		
		// Add DF Subscriber to CFA that manage starting agents
		DFAgentDescription dfTemplate = new DFAgentDescription();
		ACLMessage subscriptionMsg = DFService.createSubscriptionMessage(cfa, cfa.getDefaultDF(), dfTemplate, null);
		dfSubscriber = new DFSubscriber(cfa, subscriptionMsg, this);
		cfa.addBehaviour(dfSubscriber);
	}	

	// from AgentStartingListener interface
	public void agentStarting(String agent) {
		logger.log(Logger.FINE, "Agent "+myAgent.getName()+": adding agent "+agent+" to starting agents");
		startingAgents.add(agent);
	}

	// from AgentStartedListener interface
	public void agentStarted(String agent) {
		if (!timeoutExpired) {
			logger.log(Logger.FINE, "Agent "+myAgent.getName()+": removing agent "+agent+" from starting agents");
			startingAgents.remove(agent);
			checkStartedAgentsBehaviour.reset();
			checkStartedAgentsTimeoutBehaviour.reset(agentsStartedTimeout);
		}
	}

	protected boolean containsWarnings(PlatformInfo pi) {
		Collection hosts = pi.getHosts();
		return pi.getMainAgents().size() > 0 || hosts.size() > 0 || pi.getAgentPools().size() > 0;
	}

	@Override
	public int onEnd() {
		// Remove from platformInfo all Main Container agents started correctly 
		Iterator mainAgentIter = platformInfo.getMainAgents().iterator();
		while (mainAgentIter.hasNext()) {
			AgentInfo ai = (AgentInfo) mainAgentIter.next();
			if (ai.getErrorCode() == null) {
				if (startingAgents.remove(ai.getName())) {
					logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": agent "+ai.getName()+" startup timeout");
					ai.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "timeout");
				} else {
					mainAgentIter.remove();
				}
			}
		}
		// Remove from platformInfo all hosts "started" correctly. 
		Iterator hostIter = platformInfo.getHosts().iterator();
		while (hostIter.hasNext()) {
			HostInfo hi = (HostInfo) hostIter.next();
			if (hi.getErrorCode() == null) {
				Iterator containerIter = hi.getContainers().iterator();
				while (containerIter.hasNext()) {
					ContainerInfo ci = (ContainerInfo)containerIter.next();
					if (ci.getErrorCode() == null) {
						Iterator agentIter = ci.getAgents().iterator();
						while (agentIter.hasNext()) {
							AgentInfo ai = (AgentInfo) agentIter.next();
							if (ai.getErrorCode() == null) {
								if (startingAgents.remove(ai.getName())) {
									logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": agent "+ai.getName()+" startup timeout");
									ai.setErrorCode(CfaMessageCode.AGENT_STARTUP_ERROR + CfaMessageCode.ARGUMENT_SEPARATOR + "timeout");
								} else {
									agentIter.remove();
								}
							}
						}
					}
					if (ci.getErrorCode() == null && ci.getAgents().size() == 0) {
						containerIter.remove();
					}
				}
				if (hi.getContainers().size() == 0) {
					hostIter.remove();
				}
			}
			else {
				// This host was not available or reachable --> Clear all its containers
				hi.getContainers().clear();
			}
		}
		// Remove from platformInfo all agent pools whose agents correctly started
		Iterator agentPoolIter = platformInfo.getAgentPools().iterator();
		AgentPoolInfo pool;
		while (agentPoolIter.hasNext()) {
			pool = (AgentPoolInfo) agentPoolIter.next();
			String agentName;
			Iterator<String> startingAgentsIter = startingAgents.iterator();
			List<String> failedAgents = new LinkedList<String>();
			while (startingAgentsIter.hasNext()) {
				agentName = startingAgentsIter.next();
				if (agentBelongsToPool(pool, agentName)) {
					logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": agent "+agentName+" startup timeout (agent belongs to pool "+pool+")");
					failedAgents.add(agentName);
				}
			}
			if (failedAgents.size() == 0 && pool.getErrorCode() == null) {
				agentPoolIter.remove();
			} else {
				// FIXME: add message code
				AgentPoolsManager.appendToPoolErrorCode(pool, failedAgents, "agent creation timeout");
			}
		}
		platformInfo.getContainerProfiles().clear();

		// Remove DF subscriber from CFA
		cfa.removeBehaviour(dfSubscriber);
		dfSubscriber = null;

		// unused return value
		return 0;
	}

	public int getExitCode() {
		// up to now this behaviour never fails but just produces warnings
		return ConfigurationOntology.OK;
	}

	public Object getResult() {
		return platformInfo;
	}	

	private static boolean agentBelongsToPool(AgentPoolInfo api, String agentName) {
		if (!agentName.startsWith(api.getName()+"_")) {
			return false;
		}
		String suffix = agentName.substring(api.getName().length()+1);
		if (suffix.length() != 4) {
			return false;
		}
		try {
			int n = Integer.parseInt(suffix);
			return n >= 1 && n <= api.getSize();
		} catch (NumberFormatException nfe) {
			return false;
		}
	}


	private class DFSubscriber extends SubscriptionInitiator {

		private AgentStartedListener agentStartedListener;

		public DFSubscriber(Agent a, ACLMessage msg, AgentStartedListener agentStartedListener) {
			super(a, msg);
			this.agentStartedListener = agentStartedListener;
		}

		@Override
		protected void handleInform(ACLMessage inform) {
			try {
				DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
				for (int i = 0; i < dfds.length; ++i) {
					Iterator services = dfds[i].getAllServices();
					AID aid = dfds[i].getName();
					if (services.hasNext()) {
						// Registration/Modification
						if (logger.isLoggable(Logger.FINE)) {
							logger.log(Logger.FINE, "Agent " + myAgent.getName() + ": DF notified registration of Agent "+aid.getLocalName());
						}
						agentStartedListener.agentStarted(aid.getLocalName());
					}
					else {
						// Deregistration
						logger.log(Logger.WARNING, "Agent " + myAgent.getName() + ": during platform startup DF notified deregistration of Agent "+aid.getLocalName());
					}
				}
			}
			catch (FIPAException fe) {
				logger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": error decoding notification from DF. Message content: \""+inform.getContent()+"\"", fe);
			}
			catch (Exception e) {
				logger.log(Logger.SEVERE, "Agent " + myAgent.getName() + ": unexpected error", e);
			}
		}
	}
	
}