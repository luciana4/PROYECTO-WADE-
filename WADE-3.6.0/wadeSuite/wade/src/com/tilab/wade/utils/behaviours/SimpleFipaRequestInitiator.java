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
package com.tilab.wade.utils.behaviours;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.Vector;

public class SimpleFipaRequestInitiator extends AchieveREInitiator {
	public SimpleFipaRequestInitiator() {
		super(null, null);
	}
	
	public SimpleFipaRequestInitiator(Agent a, ACLMessage msg) {
		super(a, msg);
	}
	
	protected ACLMessage prepareRequest(ACLMessage request) {
		return request;
	}
	
	protected Vector prepareRequests(ACLMessage request) {
		Vector v = new Vector(1);
		ACLMessage actualRequest = prepareRequest(request);
		if (actualRequest != null) {
			v.addElement(actualRequest);
		}
		return v;
	}
	
	protected void handleFailure(ACLMessage failure) {
		handleError(failure);
	}
	protected void handleRefuse(ACLMessage refuse) {
		handleError(refuse);
	}
	protected void handleNotUnderstood(ACLMessage notUnderstood) {
		handleError(notUnderstood);
	}
	protected void handleAllResultNotifications(Vector notifications) {
		if (notifications.size() == 0) {
			ACLMessage reply = (ACLMessage) getDataStore().get(REPLY_KEY);
			if (reply == null || reply.getPerformative() == ACLMessage.AGREE) {
				handleTimeout();
			}
		}
	}
	
	protected void handleError(ACLMessage msg) {			
	}
	
	protected void handleTimeout() {
	}
}
