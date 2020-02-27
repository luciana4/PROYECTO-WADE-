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
package com.tilab.wade.raa;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Map;

import com.tilab.wade.cfa.beans.AgentInfo;

/**
 * The interface to be implemented by classes implementing policies used by the 
 * Runtime Allocator Agent (RAA) to allocate agents at runtime i.e. to decide in which 
 * container an agent must be created.<br>
 * The main method in this interface is <code>getContainer()</code> that is responsible 
 * for the identification of the target container<br> 
 * In certain cases such identification may require the execution of a behaviour
 * (e.g. to query the current CPU and memory usage in a set of hosts). The 
 * <code>getAllocationBehaviour()<code> method can be used for that purpose.<br> 
 * When this policy is selected to allocate an agent, first the <code>getAllocationBehaviour()<code>
 * method is invoked. If this returns a valid behaviour, that behaviour is executed 
 * and, on completion, the getContainer() method is invoked passing the allocation behaviour
 * as argument. In this way the getContainer() method implementation may access information
 * prepared by the allocation behaviour in a policy-specific way.<br>
 * If getAllocationBehaviour() returns null, instead, getContainer() is immediately 
 * invoked with te allocationBehaviour argument set to null. 
 */
public interface AgentAllocationPolicy {

	/**
	 * Initialize this AgentAllocationPolicy. This method is invoked only once at RAA startup.
	 * @param key The key identifying this policy within the RAA.
	 * @param properties The properties specified for this policy in the RAA configuration file
	 * @param agent The RAA agent using this policy
	 */
	public void init(String key, Map properties, Agent agent);

	/**
	 * @return The key identifying this policy within the RAA
	 */
	public String getKey();

	/**
	 * Retrieve the Behaviour, if any, that must be executed for the policy to be able to
	 * identify the target container.  
	 * @param info The AgentInfo object describing the agent to be created.
	 * @return The Behaviour that must be executed for the policy to be able to
	 * identify the target container or <code>null</code> if no behaviour must be executed
	 */
	public Behaviour getAllocationBehaviour(AgentInfo info);

	/**
	 * Retrieve the name of the target container
	 * @param info The <code>AgentInfo</code> object describing the agent to be created
	 * @param b The allocation behaviour as returned by the <code>getAllocationBehaviour()</code>
	 * method. 
	 * @throws DoNotCreateException If the agent must not be created. This can be useful 
	 * when redistributing agents after the crash of a container/host.
	 */
	public String getContainer(AgentInfo info, Behaviour b) throws DoNotCreateException;

	/**
	 * This method is invoked by the RAA when a new container is added to the platform.
	 * @param containerName The name of the newly started container
	 * @param ca The <code>AID</code> of the COntrolAgent running in the newly started container 
	 * @param caDescription The DF <code>ServiceDescription</code> of the ControlAgent running in
	 * the newly started container. 
	 */
	public void newContainer(String containerName, AID ca, ServiceDescription caDescription);

	/**
	 * This method is invoked by the RAA when a container terminates. 
	 * @param containerName The name of the terminated container
	 */
	public void deadContainer(String containerName);
}
