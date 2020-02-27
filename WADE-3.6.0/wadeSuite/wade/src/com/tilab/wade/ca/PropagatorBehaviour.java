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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.tilab.wade.ca.ontology.DeploymentOntology;

import jade.content.AgentAction;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.AMSService;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.util.Logger;

class PropagatorBehaviour extends AchieveREInitiator {

	protected Logger logger = Logger.getMyLogger(PropagatorBehaviour.class.getName());
	
	public static final long DEFAULT_ACTION_TIMEOUT = 60000;
	
	private AgentAction agentAction;
	private long timeout;
	private Collection<AID> receiverCAs;
	private List<AID> successfulCAs = new ArrayList<AID>();
	private List<AID> failureCAs = new ArrayList<AID>();
	private List<AID> missingCAs = new ArrayList<AID>();
	private Map<AID, String> failureReasons = new HashMap<AID, String>();
	
	
	public PropagatorBehaviour(Collection<AID> receiverCAs, AgentAction agentAction) {
		super(null, null);
		this.receiverCAs = receiverCAs;
		this.agentAction = agentAction;
		this.timeout = DEFAULT_ACTION_TIMEOUT;
	}

	@Override
	protected Vector prepareRequests(ACLMessage msg) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		for (AID caAID : receiverCAs) {
			request.addReceiver(caAID);
			successfulCAs.add(caAID);
		}
		request.setOntology(DeploymentOntology.ONTOLOGY_NAME);
		request.setLanguage(LEAPCodec.NAME);
		request.setReplyByDate(new Date(System.currentTimeMillis() + timeout));

		Action action = new Action(myAgent.getAID(), agentAction);
		Vector v = new Vector(1);
		try {
			myAgent.getContentManager().fillContent(request, action);
			v.addElement(request);
		}
		catch (Exception e) {
			// Should never happen
			e.printStackTrace();
		}

		return v;
	}
	
	@Override
	protected void handleAllResultNotifications(Vector notifications) {
		Enumeration ee = notifications.elements();
		while (ee.hasMoreElements()) {
			ACLMessage msg = (ACLMessage) ee.nextElement();
			AID senderAID = msg.getSender();
			if (msg.getPerformative() == ACLMessage.INFORM) {
				successfulCAs.add(senderAID);
				failureCAs.remove(senderAID);
			}
			else {
				if (senderAID.equals(myAgent.getAMS())) {
					try {
						senderAID = AMSService.getFailedReceiver(myAgent, msg);
						logger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - CA agent "+senderAID.getName()+" does not exist.");
						missingCAs.add(senderAID);
					}
					catch (Exception e) {
						// Should never happen 
						e.printStackTrace();
					}
				}
				else {
					failureReasons.put(senderAID, msg.getContent());
				}
			}
		}    
	}

	public boolean propagationOk() {
		return failureCAs.size() == 0;
	}
	
	public List<AID> getSuccessfulCAs() {
		return successfulCAs;
	}

	public List<AID> getFailureCAs() {
		return failureCAs;
	}

	public List<AID> getMissingCAs() {
		return missingCAs;
	}
	
	public String getFailureReason(AID caAID) {
		return failureReasons.get(caAID);
	}
	
	public String getFailureMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("Propagation error of these agents:/n");
		for (AID aid : failureCAs) {
			sb.append("- ");
			sb.append(aid);
			sb.append(": ");
			sb.append(failureReasons.get(aid));
			sb.append("/n");
		}
		return sb.toString();
	}
}

