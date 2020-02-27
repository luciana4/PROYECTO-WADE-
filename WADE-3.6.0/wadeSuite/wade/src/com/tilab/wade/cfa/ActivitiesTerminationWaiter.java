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

import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.AMSService;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.util.leap.Iterator;
import jade.util.Logger;

import java.util.Date;
import java.util.Vector;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;

/**
 * The behaviour responsible for giving a chance to agents to complete their ongoing activities before
 * killing a container or the whole platform.
 * The behaviour sends a PrepareForShutdown request to a set of CAs (1 for container termination, all for platform 
 * shutdown). If no CA needs more time before shutdown can take place the behaviour completes. Otherwise it waits a bit and then 
 * repeat the request considering CAs needing more time only.
 * Each CA serves a CheckWorking request forwarding it to all local agents.
 */
public class ActivitiesTerminationWaiter extends FSMBehaviour {
	
	private final Logger logger = Logger.getMyLogger(getClass().getName());
	
	private static final String ASK_CA_STATE = "__Ask_Ca__";
	private static final String WAITING_STATE = "__Waiting__";
	private static final String DUMMY_FINAL_STATE = "__Dummy_Final__";
	
	private static final int ALL_ACTIVITIES_TERMINATED = 0;	
	private static final int NOT_ALL_ACTIVITIES_TERMINATED = 1;
	private static final int TIMEOUT_EXPIRED = -1;	
	
	private ACLMessage caRequest;	
	private Date endTime; 	
	private Vector result = new Vector();
	
	/**
	 * @param a The agent that is going to execute this behaviour
	 * @param caRequest The REQUEST message to be sent to Control Agents.
	 */
	public ActivitiesTerminationWaiter(Agent a, ACLMessage caRequest) {	
		this.caRequest = caRequest;
		endTime = caRequest.getReplyByDate();	
		
		registerFirstState(new AskAllCa(a, caRequest), ASK_CA_STATE); 
		registerState(new WakerBehaviour(a, 15000) {
			
			public void onStart() {
				logger.log(Logger.FINE, "Agent "+myAgent.getName()+": Wait a bit before trying again...");
				super.onStart();
			}
			
			protected void onWake() {
				// Just do nothing
			}
			
			public int onEnd() {
				reset(15000);
				return super.onEnd();
			}
			
		}, WAITING_STATE);
		registerLastState(new OneShotBehaviour() { public void action(){} }, DUMMY_FINAL_STATE);		
	
		registerTransition(ASK_CA_STATE, WAITING_STATE, NOT_ALL_ACTIVITIES_TERMINATED);
		registerDefaultTransition(WAITING_STATE, ASK_CA_STATE);
		// All activities terminated or timeout expired --> terminate
		registerDefaultTransition(ASK_CA_STATE, DUMMY_FINAL_STATE);		
		
	}
	
	public int onEnd() {
		// Store CAs that needs more time (if any) in the result Vector
		Iterator it = caRequest.getAllReceiver();
		while(it.hasNext()) {
			AID id = (AID) it.next();
			result.addElement(id.getLocalName());
		}
		return super.onEnd();
	}
	
	public int getExitCode() {
		return (result.size() > 0 ? ConfigurationOntology.KO : ConfigurationOntology.OK);
	}	
	
	 public Vector getResult() {
		return result;
	}
	
	 
	/**
	 * State 1: Send the Prepare-For-Shutdown request to CAs. At the end keeps in the caRequest receivers
	 * only CAs who need more time before shutdown can take place     
	 */
	private class AskAllCa extends AchieveREInitiator {		
		
		public AskAllCa(Agent a, ACLMessage request) {
			super(a, request);
		}

		public void onStart() {
			logger.log(Logger.FINE, "Agent "+myAgent.getName()+": Checking platform activities with CA...");
			super.onStart();
		}
		
		protected void handleAllResultNotifications(Vector notifications) {		
			caRequest.clearAllReceiver();		
			
			for (int i = 0; i < notifications.size(); ++i) {				
				ACLMessage notification = (ACLMessage) notifications.get(i);
				if (notification.getPerformative() == ACLMessage.INFORM) {					
					try {
						Result r = (Result) myAgent.getContentManager().extractContent(notification);
						boolean working = ((Boolean) r.getValue()).booleanValue();
						if (working) {
							// This CA is still working! Add it again to the receivers of the caRequest message for next round
							logger.log(Logger.INFO, "Agent "+myAgent.getName()+": Some agents still working in Container controlled by CA "+notification.getSender().getName());
							caRequest.addReceiver(notification.getSender());							
						}
					} catch (Exception e) {
						logger.log(Logger.SEVERE, "Agent "+myAgent.getName()+": Error decoding response from CA: "+notification.getSender().getName(), e);
					}					
				}
				else {
					if (notification.getSender().equals(myAgent.getAMS())) {
						try {
							logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": CA "+AMSService.getFailedReceiver(myAgent, notification)+" does not exist");
						}
						catch (Exception e) {
							// Should never happen 
							e.printStackTrace();
						}
					}
					else {
						logger.log(Logger.WARNING, "Agent "+myAgent.getName()+": Unexpected response received from CA "+notification.getSender().getName());
					}
				}
			}
		}
		
		public int onEnd() {	
			Iterator receivers = caRequest.getAllReceiver();			
			if (receivers.hasNext()) {
				// Some activities in the platform are still ongoing
				Date currentTime = new Date(System.currentTimeMillis());
				if (!currentTime.after(endTime)) {
					// We still have some time to wait
					reset(caRequest);
					return NOT_ALL_ACTIVITIES_TERMINATED;
				}
				else {
					// Platform activities termination timeout expired
					return TIMEOUT_EXPIRED;
				}
			}			
			// All activities terminated 
			return ALL_ACTIVITIES_TERMINATED;
		}
	}
}
