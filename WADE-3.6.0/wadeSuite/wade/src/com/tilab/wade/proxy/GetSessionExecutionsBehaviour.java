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

import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.domain.FIPAException;

import java.util.List;

import com.tilab.wade.wsma.ontology.GetSessionExecutions;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowStatusOntology;

public class GetSessionExecutionsBehaviour extends EngineProxyBehaviour<GetSessionExecutions, List<WorkflowExecutionInfo>> {

	public GetSessionExecutionsBehaviour(String sessionId) {
		super(new GetSessionExecutions(sessionId), WorkflowStatusOntology.getInstance(), null);
		
		setLanguage(LEAPCodec.NAME);
	}

	@Override
	protected AID retrieveActor() throws FIPAException {
		return getWSMAActor();
	}
	
	public List<WorkflowExecutionInfo> getExecutions() throws EngineProxyException {
		return (List<WorkflowExecutionInfo>) getEngineProxyResult();
	}

	@Override
	protected List<WorkflowExecutionInfo> extractResult(Result r) {
		jade.util.leap.List leapList = (jade.util.leap.List) r.getValue();
		return convertJadeList(leapList);
	}
}
