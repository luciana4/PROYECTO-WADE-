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
 * RAA policy allocating agents using a round-robin scheme on containers that have a given
 * JAVA Profile.<br>
 * The name of the JAVA Profile must be specified by means of the <code>javaProfile</code> property 
 */
public class JavaProfileBasedRRPolicy extends RoundRobinPolicy {

	private static final String CFG_JAVA_PROFILE = "javaProfile";
	//private final static String DF_JAVA_PROFILE = "JAVA-PROFILE";

	private String javaProfile;

	public void init(String key, Map arguments, Agent myAgent) {
		super.init(key, arguments, myAgent);
		javaProfile = (String)arguments.get(CFG_JAVA_PROFILE);
	}

	public boolean isRelevant(ServiceDescription sd) {
		Iterator iter = sd.getAllProperties();
		while (iter.hasNext()) {
			Property p = (Property)iter.next();
			if (p.getName().equals(WadeAgent.JAVA_PROFILE)) {
				String profile = (String) p.getValue();
				return profile.equals(javaProfile);
			}
		}
		// Java-profile property not specified
		return false;
	}
	
}
