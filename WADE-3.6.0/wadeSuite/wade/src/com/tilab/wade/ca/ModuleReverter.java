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
package com.tilab.wade.ca;

import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.Date;
import java.util.Vector;

import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.Revert;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.DFUtils;


public class ModuleReverter extends AchieveREInitiator {
	
	public static final int UNKNOWN_STATUS  = 0;
	public static final int SUCCESS_STATUS  = 1;
	public static final int FAILURE_STATUS  = 2;

	private int status = UNKNOWN_STATUS;
	private String error;
	private Revert revert;
	private AID caAID;

	public ModuleReverter(String moduleName, boolean rebuildClassLoader) {
		super(null, null);
		
		revert = new Revert();
		revert.setModuleName(moduleName);
		revert.setRebuildClassLoader(rebuildClassLoader);
	}

	@Override
	public void onStart() {
		super.onStart();

		myAgent.getContentManager().registerOntology(DeploymentOntology.getInstance());
		myAgent.getContentManager().registerLanguage(new LEAPCodec());
	}

	@Override
	protected Vector prepareRequests(ACLMessage msg) {
		Vector v = new Vector(1);

		try {
			DFAgentDescription dfad = DFUtils.searchAnyByType(myAgent, WadeAgent.CONTROL_AGENT_TYPE, null);
			caAID = DFUtils.getAID(dfad);
			if (caAID == null) {
				throw new Exception("No one CA available");
			}

			ACLMessage request = new ACLMessage(ACLMessage.PROPAGATE);
			request.addReceiver(caAID);
			request.setOntology(DeploymentOntology.ONTOLOGY_NAME);
			request.setLanguage(LEAPCodec.NAME);
			request.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

			myAgent.getContentManager().fillContent(request, new Action(caAID, revert));

			v.addElement(request);
		}
		catch(Exception e) {
			status = FAILURE_STATUS;
			error = e.getMessage();			
		}

		return v;
	}

	@Override
	protected void handleInform(ACLMessage message)	{
		status = SUCCESS_STATUS;
	}

	@Override
	protected void handleFailure(ACLMessage failure)	{
		status = FAILURE_STATUS;
		if (failure.getSender().equals(myAgent.getAMS())) {
			// CA agent unreachable
			error = "Agent "+caAID.getLocalName()+" UNREACHABLE";
		} else {
			// Deploy failure
			error = failure.getContent();
		}
	}

	@Override
	protected void handleRefuse(ACLMessage refuse) {
		status = FAILURE_STATUS;
		error = "Agent "+refuse.getSender().getLocalName()+" REFUSE request";
	}

	@Override
	protected void handleNotUnderstood(ACLMessage notUnderstood) {
		status = FAILURE_STATUS;
		error = "Agent "+notUnderstood.getSender().getLocalName()+" NOT_UNDERSTOOD request";
	}

	public boolean isSuccessfullyUndeployed() {
		return status == SUCCESS_STATUS;
	}

	public String getFailureReason() {
		return error;
	}
}
