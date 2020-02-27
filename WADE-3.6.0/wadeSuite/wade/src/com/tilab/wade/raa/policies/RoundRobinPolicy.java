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
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;
import jade.util.leap.RoundList;

import java.util.NoSuchElementException;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.raa.DoNotCreateException;

/**
 * Base class for all RAA policies allocating agents on a set of containers using a Round-Robin scheme.
 * This base class consider all available containers.
 * Subclasses may consider only containers with given characteristics by redefining the
 * <code>isRelevant()</code> method 
 */
public class RoundRobinPolicy extends BaseAgentAllocationPolicy {

	private final static Logger logger = Logger.getMyLogger(RoundRobinPolicy.class.getName());
	
	private RoundList availableContainers = new RoundList();

	public String getContainer(AgentInfo info, Behaviour b) throws DoNotCreateException {
		try {
			return (String) availableContainers.get();
		}
		catch (NoSuchElementException nsee) {
			// No container available
			return null;
		}
	}

	/**
	 * Defines whether or not a given container is relevant for this AgentAllocationPolicy.
	 * The default implementation simply returns true so that agents will be allocated round-robin
	 * on all containers. Subclasses may redefine this method to consider only containers with given characteristics.
	 * @param sd The ServiceDescription of the ControlAgent active in the container whose relevance 
	 * is tested
	 * @return Whether or not the container is relevant for this AgentAllocationPolicy
	 */
	protected boolean isRelevant(ServiceDescription sd) {
		return true;
	}
	
	public void newContainer(String containerName, AID ca, ServiceDescription caDescription) {
		if (isRelevant(caDescription)) {
			if (!availableContainers.contains(containerName)){
				availableContainers.add(containerName);
				logger.log(Logger.CONFIG, "Agent "+myAgent.getName() + ": Policy "+myKey+" - Added container " + containerName);
				logger.log(Logger.FINER, "Agent "+myAgent.getName() + ": After newContainer() available containers are " + availableContainers.toString());
			}
		}
	}

	public void deadContainer(String containerName) {
		if (availableContainers.remove(containerName)) {
			logger.log(Logger.CONFIG, "Agent "+myAgent.getName() + ": Policy "+myKey+" - Removed container " + containerName);
			logger.log(Logger.FINER, "Agent "+myAgent.getName() + ": After deadContainer() available containers are " + availableContainers.toString());
		}
	}
}
