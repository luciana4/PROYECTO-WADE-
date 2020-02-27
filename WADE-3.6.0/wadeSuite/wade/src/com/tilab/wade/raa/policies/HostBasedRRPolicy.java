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
import jade.core.Profile;
import jade.core.Specifier;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.commons.WadeAgent;

/**
 * RAA policy allocating agents using a round-robin scheme on containers in a given set of hosts.
 * The names of the valid hosts must be specified by means of the <code>hostname</code> property 
 * as a list of pipe ('|') separated strings e.g.<br>
 * <code>foo.bar.host1|localhost</code>     
 */
public class HostBasedRRPolicy extends RoundRobinPolicy {
	private static final String CFG_HOSTNAME = "hostname";

	private List<String> validHosts;

	public void init(String key, Map arguments, Agent agent) {
		super.init(key, arguments, agent);
		String tmp = (String)arguments.get(CFG_HOSTNAME);
		validHosts = Specifier.parseList(tmp, '|');
	}
	
	public boolean isRelevant(ServiceDescription caServiceDescription) {
		Iterator iter = caServiceDescription.getAllProperties();
		while (iter.hasNext()) {
			Property p = (Property)iter.next();
			if (p.getName().equals(WadeAgent.HOSTNAME)) {
				String host = (String) p.getValue();
				return isValid(host);
			}
		}
		// Hostname property not specified
		return false;
	}
	
	private boolean isValid(String host) {
		for (String validHost : validHosts) {
			if (Profile.compareHostNames(validHost, host)) {
				return true;
			}
		}
		return false;
	}
}
