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
package com.tilab.wade.proxy;

import jade.core.AID;
import jade.domain.FIPAException;

import java.util.List;

import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.GetModules;
import com.tilab.wade.ca.ontology.ModuleInfo;

public class GetModulesBehaviour extends EngineProxyBehaviour<GetModules, List<ModuleInfo>> {
	
	public GetModulesBehaviour() {
		super(new GetModules(), DeploymentOntology.getInstance(), null);
	}

	public List<ModuleInfo> getModulesInfo() throws EngineProxyException {
		return (List<ModuleInfo>) getEngineProxyResult();
	}

	@Override
	protected AID retrieveActor() throws FIPAException {
		return getCAActor();
	}	
}
