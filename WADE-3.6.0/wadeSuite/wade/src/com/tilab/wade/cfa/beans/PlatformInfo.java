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

public class PlatformInfo extends PlatformElement {

	private static final long serialVersionUID = -639390821466559901L;

	private String name;
	private String description;
	private int backupsNumber;
	private Collection<AgentInfo> mainAgents = new HashSet<AgentInfo>();
	private Collection<HostInfo> hosts = new HashSet<HostInfo>();
	private Collection<ContainerProfileInfo> containerProfiles = new HashSet<ContainerProfileInfo>();
	private Collection<AgentPoolInfo> agentPools = new HashSet<AgentPoolInfo>();

	public PlatformInfo() {
		this.backupsNumber = 0;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	public void setMainAgents(Collection<AgentInfo> mainAgents) {
		this.mainAgents = mainAgents;
	}
	public Collection<AgentInfo> getMainAgents() {
		return mainAgents;
	}
	
	public void addMainAgents(AgentInfo mainAgentsInfo) {
		this.mainAgents.add(mainAgentsInfo);
	}
	
	public void removeMainAgents() {
		this.mainAgents.removeAll(mainAgents);
	}

	public Collection<ContainerProfileInfo> getContainerProfiles() {
		return containerProfiles;
	}

	
	// Required by Digester
	public void addContainerProfile(ContainerProfileInfo containerProfileInfo) {
		this.containerProfiles.add(containerProfileInfo);
	}

	
	public Collection<HostInfo> getHosts() {
		return hosts;
	}

	public HostInfo getHost(String name) {
		Iterator it = hosts.iterator();
		while (it.hasNext()) {
			HostInfo hi = (HostInfo) it.next();
			if (hi.getName().equals(name)) {
				return hi;
			}
		}
		return null;
	}
	
	//needed by the console in order to show detailInfos
	public List  getHostsAsList() {
		return new ArrayList(hosts);
	}

	// Required by Digester
	public void addHost(HostInfo hostInfo) {
		this.hosts.add(hostInfo);
	}


	public void setContainerProfiles(Collection<ContainerProfileInfo> containerProfiles) {
		this.containerProfiles = containerProfiles;
	}

	public void setHosts(Collection<HostInfo> hosts) {
		this.hosts = hosts;
	}

	public boolean equals(Object obj) {
		return  (obj instanceof PlatformInfo && super.equals(obj));
	}

	public int getBackupsNumber() {
		return backupsNumber;
	}

	public void setBackupsNumber(int backupsNumber) {
		this.backupsNumber = backupsNumber;
	}

	//maybe needed by the console in order to show detailInfos
	public List  getAgentPoolsAsList() {
		return new ArrayList(agentPools);
	}

	public Collection<AgentPoolInfo> getAgentPools() {
		return agentPools;
	}

	// Required by Digester
	public void addAgentPool(AgentPoolInfo agentPoolInfo) {
		this.agentPools.add(agentPoolInfo);
	}

	public void setAgentPools(Collection<AgentPoolInfo> agentPools) {
		this.agentPools = agentPools;
	}


	public String toString() {
		return toString(false);
	}


	public String toString(boolean onlyError) {
		StringBuffer sb = new StringBuffer("PLATFORM ");
		sb.append(name);
		sb.append('\n');
		if (getErrorCode() != null) {
			sb.append("- error-code: ");
			sb.append(getErrorCode());
			sb.append('\n');
		}
		
		if (mainAgents != null && mainAgents.size() > 0) {
			sb.append("- mainAgents:\n");
			Iterator it = mainAgents.iterator();
			while (it.hasNext()) {
				AgentInfo agent = (AgentInfo) it.next();
				sb.append(agent.toString("  ", onlyError));
			}
			sb.append('\n');
		}

		if (hosts != null && hosts.size() > 0) {
			sb.append("- hosts:\n");
			Iterator it = hosts.iterator();
			while (it.hasNext()) {
				HostInfo host = (HostInfo) it.next();
				sb.append(host.toString("  ", onlyError));
			}
			sb.append('\n');
		}

		if (agentPools != null && agentPools.size() > 0) {
			sb.append("- agentPools:\n");
			Iterator it = agentPools.iterator();
			while (it.hasNext()) {
				AgentPoolInfo agentPool = (AgentPoolInfo) it.next();
				sb.append(agentPool.toString("  ", onlyError));
			}
			sb.append('\n');
		}

		if (!onlyError) sb.append("- backup mains: " +backupsNumber);		
		return sb.toString();
	}


}
