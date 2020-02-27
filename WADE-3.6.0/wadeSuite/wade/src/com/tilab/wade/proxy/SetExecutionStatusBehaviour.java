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

import jade.content.Predicate;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.logging.Level;

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.wsma.ontology.StatusChanged;
import com.tilab.wade.wsma.ontology.Terminated;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;
import com.tilab.wade.wsma.ontology.WorkflowStatusOntology;

public class SetExecutionStatusBehaviour extends OneShotBehaviour {

	protected static Logger logger = Logger.getMyLogger(SetExecutionStatusBehaviour.class.getName());;
	
	private String executionId;
	private WorkflowExecutionInfo.WorkflowStatus status;
	private String terminationMessage;
	private String errorMessage;
	
	public SetExecutionStatusBehaviour(String executionId, WorkflowStatus status, String terminationMessage) {
		super();
		
		this.executionId = executionId;
		this.status = status;
		this.terminationMessage = terminationMessage;
		this.errorMessage = null;
	}
	
	public void action() {
		AID wsmaAid;
		try {
			wsmaAid = EngineProxyBehaviour.getWSMAActor(myAgent);
			if (wsmaAid == null) {
				throw new Exception("Agent WSMA not available");
			}
		} catch (Exception e) {
			errorMessage = e.getMessage();
			logger.log(Level.SEVERE, "Error searching actor", e);
			return;
		}
		
		try {
			Predicate predicate;
			if (status == WorkflowStatus.TERMINATED) {
				predicate = new Terminated(executionId, new ArrayList<Parameter>(), terminationMessage);
			} else {
				predicate = new StatusChanged(executionId, status);
			}
			
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			inform.setOntology(WorkflowStatusOntology.getInstance().getName());
			inform.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			inform.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
			inform.addReceiver(wsmaAid);
		
			myAgent.getContentManager().fillContent(inform, predicate);
			myAgent.send(inform);
			
			logger.log(Logger.FINE, "Status notification "+predicate+" sended to agent "+wsmaAid);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			logger.log(Level.SEVERE, "Error sending status notification request", e);
		}
	}
	
	public void checkError() throws EngineProxyException {
		if (errorMessage != null) {
			throw new EngineProxyException(errorMessage);
		}
	}
}
