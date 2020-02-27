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

import jade.core.Agent;

import java.lang.reflect.Constructor;
import java.util.Map;

class PolicyConfiguration {

	private String key;
	private String className;
	private Map configurationProperties;
	private AgentAllocationPolicy policy;
	private Agent myAgent;

	public PolicyConfiguration(String key, String className, Map configurationProperties, Agent myAgent) throws ConfigurationException {
		if (className == null) {
			throw new ConfigurationException("className cannot be null");
		}
		this.key = key;
		this.className = className;
		this.configurationProperties = configurationProperties;
		this.myAgent = myAgent;
		loadPolicy();
	}

	private void loadPolicy() throws ConfigurationException {
		try {
			Class policyClass = Class.forName(className);
			Constructor policyClassDefaultConstructor = policyClass.getConstructor();
			policy = (AgentAllocationPolicy)(policyClassDefaultConstructor.newInstance());
			policy.init(key, configurationProperties, myAgent);
		} catch (ClassNotFoundException cnfe) {
			throw new ConfigurationException("cannot load class "+className, cnfe);
		} catch (ClassCastException cce) {
			throw new ConfigurationException("class "+className+" does not implement AgentAllocationPolicy", cce);
		} catch (Exception e) {
			throw new ConfigurationException("error instantiating class "+className, e);
		}
	}

	public String getClassName() {
		return className;
	}

	public Map getConfigurationProperties() {
		return configurationProperties;
	}

	public AgentAllocationPolicy getPolicy() {
		return policy;
	}

	public String toString() {
		return "PolicyConfiguration {className=\""+className+"\"; configurationProperties="+configurationProperties+"}";
	}
}
