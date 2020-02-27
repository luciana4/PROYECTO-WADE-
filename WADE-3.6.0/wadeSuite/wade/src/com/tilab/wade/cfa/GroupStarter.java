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

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.util.Logger;

import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.GroupStatus;

class GroupStarter extends ConfigurationStarter {

	private String group;
	
	public GroupStarter(ConfigurationAgent cfa, PlatformInfo platformInfo, String group) {
		super(cfa, platformInfo);
		
		this.group = group;
	}

	@Override
	protected Behaviour getHostManager() {
		ParallelBehaviour hostsManager = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);

		for (HostInfo hi : platformInfo.getHosts()) {
			SequentialBehaviour hb = new SequentialBehaviour();
			
			for (ContainerInfo ci : hi.getContainers()) {
				AgentsStarter agentsStarter = new AgentsStarter(ci, this);
				hb.addSubBehaviour(agentsStarter);
			}
			hostsManager.addSubBehaviour(hb);
		}
		
		return hostsManager;
	}
	
	@Override
	public int onEnd() {
		int code = super.onEnd();
		
		Object groupResult = getResult();
		if (getExitCode() == ConfigurationOntology.OK) {
			PlatformInfo groupDetail = (PlatformInfo) groupResult;
			PlatformInfo globalDetail = getGlobalDetail(groupDetail);
			if (containsWarnings(groupDetail)) {
				logger.log(Logger.WARNING, "Group <" + group + "> startup completed with warnings:\n" + groupDetail);

				cfa.getGroupManager().setGroupStatus(group, ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS);
				cfa.setGlobalStatus(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS, globalDetail);
				
			} else {
				cfa.getGroupManager().setGroupStatus(group, ConfigurationOntology.ACTIVE_STATUS);
				
				if (containsWarnings(globalDetail)) {
					cfa.setGlobalStatus(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS, globalDetail);
				} else {
					boolean otherGroupsDown = false;
					for (GroupStatus gs : cfa.getGroupManager().getGroupsStatus()) {
						if (!gs.getName().equals(group) && gs.getStatus().equals(ConfigurationOntology.DOWN_STATUS)) {
							otherGroupsDown = true;
							break;
						}
					}
					if (otherGroupsDown) {
						cfa.setGlobalStatus(ConfigurationOntology.ACTIVE_INCOMPLETE_STATUS, ConfigurationAgent.NO_DETAIL);
					} else {
						cfa.setGlobalStatus(ConfigurationOntology.ACTIVE_STATUS, ConfigurationAgent.NO_DETAIL);
					}
				}
			}
		} else {
			cfa.getGroupManager().setGroupStatus(group, ConfigurationOntology.ERROR_STATUS);
			cfa.setGlobalStatus(ConfigurationOntology.ERROR_STATUS, groupResult);
		}

		return code;
	}

	protected PlatformInfo getGlobalDetail(PlatformInfo groupDetail) {
		return groupDetail;
	}
}