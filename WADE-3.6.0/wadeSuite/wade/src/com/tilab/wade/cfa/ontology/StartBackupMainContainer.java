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
package com.tilab.wade.cfa.ontology;

import java.util.Collection;
import java.util.HashSet;

import com.tilab.wade.cfa.beans.AgentInfo;

import jade.content.AgentAction;

public class StartBackupMainContainer implements AgentAction {

	private String containerName;
	private String javaProfile;
	private String jadeProfile;
	private String jadeAdditionalArgs;
	private String hostName;
	private Collection<AgentInfo> agents = new HashSet<AgentInfo>();
	
	
	public Collection<AgentInfo> getAgents() {
		return agents;
	}
	
	public void setAgents(Collection<AgentInfo> agents) {
		this.agents = agents;
	}

	public String getHostName() {
		return hostName;
	}
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public String getJadeAdditionalArgs() {
		return jadeAdditionalArgs;
	}
	
	public void setJadeAdditionalArgs(String jadeAdditionalArgs) {
		this.jadeAdditionalArgs = jadeAdditionalArgs;
	}
	
	public String getJadeProfile() {
		return jadeProfile;
	}
	
	public void setJadeProfile(String jadeProfile) {
		this.jadeProfile = jadeProfile;
	}
	
	public String getJavaProfile() {
		return javaProfile;
	}
	
	public void setJavaProfile(String javaProfile) {
		this.javaProfile = javaProfile;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}
	

}
