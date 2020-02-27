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
package com.tilab.wade.cfa.beans;

import java.util.*;

public class ContainerInfo extends PlatformElement{

	private static final long serialVersionUID = -8863153516957235502L;

	private String name;
	private String projectName;
	private String javaProfile;
	private String jadeProfile;
	private String jadeAdditionalArgs;
	private boolean split = false;
	private Collection<AgentInfo> agents = new HashSet<AgentInfo>();
	
	public ContainerInfo() {
	}
	
	public ContainerInfo(String name) {
		this.name = name;
	}
	
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean getSplit() {
		return split;
	}
	public void setSplit(boolean split) {
		this.split = split;
	}
	public void setAgents(Collection<AgentInfo> agents) {
		this.agents = agents;
	}
	public Collection<AgentInfo> getAgents() {
		return agents;
	}
		
	//required by console in order to show detailInfos
    public List getAgentsAsList() {
        return new ArrayList(agents);
    }
	
	// Required by Digester
	public void addAgent(AgentInfo agentInfo) {
		// NOTE that for some reason add() called on an object that is already in the set has no effect 
		if (agents.contains(agentInfo)) {
			agents.remove(agentInfo);
		}
		agents.add(agentInfo);
	}
	
	// Required by CA
	public AgentInfo getAgent(String agentName) {
		AgentInfo result = null;
		Iterator<AgentInfo> it = agents.iterator();
		while(it.hasNext()) {
			AgentInfo ai = it.next();
			if(ai.getName().equals(agentName)) {
				result = ai;
				break;
			}
		}
		return result;
	}

	public boolean removeAgent(AgentInfo ai) {
		return agents.remove(ai);
	}

	public String toString() {
		return toString("", false);
	}

	public String toString(String prefix, boolean onlyError) {
		StringBuffer sb = new StringBuffer(prefix+"CONTAINER ");
		sb.append(name);
		sb.append('\n');
		if (getErrorCode() != null) {
			sb.append(prefix+"- error-code: ");
			sb.append(getErrorCode());
			sb.append('\n');
		}
		
		if (agents.size() > 0) {
			sb.append(prefix+"- agents:\n");
			Iterator it = agents.iterator();
			while (it.hasNext()) {
				AgentInfo agent = (AgentInfo) it.next();
				sb.append(agent.toString(prefix+"  ", onlyError));
			}
		}
		
		return sb.toString();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ContainerInfo) {
			return name.equals(((ContainerInfo) obj).getName());
		}
		return false;
	}
}
