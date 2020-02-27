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

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Iterator;
import java.util.Map;

import com.tilab.wade.commons.WadeAgent;

/**
 * RAA policy allocating agents using a round-robin scheme on an explicitly defined set
 * of containers. 
 * The names of the valid containers must be specified by means of the <code>containerName</code> property 
 * as a list of pipe ('|') separated strings e.g.<br>
 * <code>Container-1|Container-2</code><br>
 * More in general the value of the <code>containerName</code> property is treated as a 
 * regular expression and all and only containers whose name matches that regular expression are considered
 * valid.
 */
public class ContainerNameBasedRRPolicy extends RoundRobinPolicy {

	private static final String CFG_CONTAINERNAME = "containerName";

	private String containerNameRegexp;

	public void init(String key, Map arguments, Agent myAgent) {
		super.init(key, arguments, myAgent);
		containerNameRegexp = (String)arguments.get(CFG_CONTAINERNAME);
	}
	
	public boolean isRelevant(ServiceDescription sd) {
		Iterator iter = sd.getAllProperties();
		while (iter.hasNext()) {
			Property p = (Property)iter.next();
			if (p.getName().equals(WadeAgent.AGENT_LOCATION)) {
				String containerName = (String) p.getValue();
				return containerName.matches(containerNameRegexp);
			}
		}
		// Location property not specified
		return false;
	}
}
