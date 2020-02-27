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
package com.tilab.wade.performer.event;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.io.Serializable;
import java.util.Hashtable;

import com.tilab.wade.event.EventOntology;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.ontology.ControlInfo;
import com.tilab.wade.performer.ontology.ControlInfoChanges;

/**
 * This class allows issuing events according to the WADE Event Ontology.
 * Each event is embedded into an ACLMessage and delivered to one or more listener 
 * according to the event control information (ControlInfo) associated to the <code>type</code>
 * of the event.
 */
public class EventEmitter implements Serializable {
	private transient Agent myAgent;
	private String myId;
	private Hashtable<String, ControlInfo> myControlInfo = new Hashtable<String, ControlInfo> ();
	private ACLMessage eventMsg = new ACLMessage(ACLMessage.INFORM);
	private boolean synchEnabled = true;

	/**
	 * Create an EventEmitter with a given ID and using the default ontology and language
	 * (Event-Ontology and fipa-sl) to encode the event message content 
	 * @param agent The agent this EventEmitter is bound to 
	 * @param id The emitter ID
	 */
	public EventEmitter(Agent agent, String id) {
		this(agent, id, null, null);
	}
	
	/**
	 * Create an EventEmitter with a given ID and using a given ontology and language
	 * to encode the event message content 
	 * @param agent The agent this EventEmitter is bound to 
	 * @param id The emitter ID
	 */
	public EventEmitter(Agent agent, String id, String onto, String lang) {
		myAgent = agent;
		myId = id;
		
		// Initialize the message used to notify events to controllers
		eventMsg.setConversationId(myId);
		eventMsg.setOntology(onto != null ? onto : EventOntology.getInstance().getName());
		eventMsg.setLanguage(lang != null ? lang : FIPANames.ContentLanguage.FIPA_SL);
		eventMsg.addUserDefinedParameter(Constants.EVENT_MESSAGE, "true");		
	}

	public Agent getAgent() {
		return myAgent;
	}
	
	public void setAgent(Agent a) {
		myAgent = a;
		eventMsg.setSender(a.getAID());
	}
	
	public String getId() {
		return myId;
	}
	
	public final void setControlInfo(String type, AID controller) {
		ControlInfo cInfo = new ControlInfo(type, controller);
		setControlInfo(cInfo);
	}
	
	public final void setControlInfo(String type, AID controller, int level) {
		ControlInfo cInfo = new ControlInfo(type, controller, level);
		setControlInfo(cInfo);
	}
	
	public final void setControlInfo(List cInfos) {
		myControlInfo.clear();
		if (cInfos != null) {
			Iterator it = cInfos.iterator();
			while (it.hasNext()) {
				ControlInfo cInfo = (ControlInfo) it.next();
				setControlInfo(cInfo);
			}
		}
	}
	
	public final void setControlInfo(ControlInfo info) {
		Boolean selfConfig = info.getSelfConfig();
		if (selfConfig != null && selfConfig.booleanValue()) {
			adjustControlInfo(info);
		}
		// FIXME: if the controller has changed, notify the old controller
		myControlInfo.put(info.getType(), info);
	}
	
	public final void updateControlInfo(ControlInfoChanges infoChanges) {
		String type = infoChanges.getType();
		ControlInfo oldCInfo = (ControlInfo) myControlInfo.get(type);
		if (oldCInfo == null) {
			oldCInfo = new ControlInfo();
			oldCInfo.setType(type);
		}
		// SYNCH
		if (infoChanges.getSynch() != null) {
			oldCInfo.setSynch(infoChanges.getSynch());
		}
		// VERBOSITY LEVEL
		if (infoChanges.getVerbosityLevel() != null) {
			oldCInfo.setVerbosityLevel(infoChanges.getVerbosityLevel());
		}
		// SELF CONFIG
		if (infoChanges.getSelfConfig() != null) {
			oldCInfo.setSelfConfig(infoChanges.getSelfConfig());
		}
		// CONTROLLERS
		if (infoChanges.getControllers() != null) {
			// Replace all controllers
			oldCInfo.setControllers(infoChanges.getControllers());
		}
		else {
			List oldControllers = oldCInfo.getControllers();
			
			// Remove old controllers
			List controllersToRemove = infoChanges.getControllersToRemove();
			if (oldControllers != null && controllersToRemove != null) {
				for(int i=0; i<controllersToRemove.size(); i++) {
					oldControllers.remove(controllersToRemove.get(i));
				}
			}
			
			// Add new controllers
			List controllersToAdd = infoChanges.getControllersToAdd();
			if (controllersToAdd != null) {
				if (oldControllers == null) {
					oldCInfo.setControllers(controllersToAdd);
				}
				else {
					for(int i=0; i<controllersToAdd.size(); i++) {
						AID controller = (AID)controllersToAdd.get(i);
						if (!oldControllers.contains(controller)) {
							oldControllers.add(controller);
						}
					}
				}
			}
		}
		
		// Update the control info
		setControlInfo(oldCInfo);
	}
	
	/**
	 * Subclasses firing application-specific events need to retrieve the
	 * ControlInfo to get the verbosity level and controller.
	 */
	public final Hashtable<String, ControlInfo> getControlInfo() {
		return myControlInfo;
	}	

	/**
	   Fire an event of a given type according to the 
	   execution control information of the workflow under execution
	 */
	public final void fireEvent(String type, WorkflowEvent ev, int level) {
		ControlInfo cInfo = myControlInfo.get(type);
		if (cInfo != null) {
			if (cInfo.getVerbosityLevel() >= level) {
				List controllers = cInfo.getControllers();
				if (controllers != null && controllers.size() > 0) {
					// Remote handling
					long time = System.currentTimeMillis();
					
					// Event customization
					ev = customizeEvent(myId, time, type, ev, controllers);
					
					eventMsg.clearAllReceiver();
					for (int i = 0; i < controllers.size(); ++i) {
						eventMsg.addReceiver((AID) controllers.get(i));
					}
					eventMsg.setProtocol(type);
					if (cInfo.getSynch() && synchEnabled) {
						eventMsg.setReplyWith("synch"); // Dummy value that just indicates that we wait for a reply
						eventMsg.removeUserDefinedParameter(ACLMessage.IGNORE_FAILURE);
					}
					else {
						eventMsg.setReplyWith(null);
						eventMsg.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
					}
					Occurred o = new Occurred(time, ev);
					try {
						if (myAgent != null) {
							myAgent.getContentManager().fillContent(eventMsg, o);
							myAgent.send(eventMsg);
							if (cInfo.getSynch() && synchEnabled) {
								// If the event requires synchronous handling (and synch event handling is enabled), wait for replies from controllers.
								for (int i = 0; i < controllers.size(); ++i) {
									myAgent.blockingReceive(MessageTemplate.MatchConversationId(myId));
								}
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Local handling
					handleEvent(myId, type, ev);
				}
			}
		}
	}

	public final boolean isFireable(String type, int level) {
		ControlInfo cInfo = (ControlInfo) myControlInfo.get(type);
		if (cInfo != null) {
			return cInfo.getVerbosityLevel() >= level;
		}
		return false;
	}
	
	public void disableSynchEvents() {
		synchEnabled = false;
	}
	
	/**
	   Send a CANCEL message to all control info listeners
	 */
	public void close() {
		eventMsg.setPerformative(ACLMessage.CANCEL);
		if (myAgent != null) {
			Object[] tmp = myControlInfo.values().toArray();
			for (int i = 0; i < tmp.length; ++i) {
				ControlInfo cInfo = (ControlInfo) tmp[i];
				List controllers = cInfo.getControllers();
				if (controllers != null && controllers.size() > 0) {
					
					eventMsg.clearAllReceiver();
					for (int j = 0; j < controllers.size(); ++j) {
						eventMsg.addReceiver((AID) controllers.get(j));
					}
					eventMsg.setProtocol(cInfo.getType());
					eventMsg.setReplyWith(null);
					myAgent.send(eventMsg);
				}
			}
		}
	}	
	
	/**
	 * Subclasses may redefine this method to implement application specific ControlInfo self adjustment mechanisms 
	 * @param cInfo The ControlInfo object to be adjusted
	 */
	protected void adjustControlInfo(ControlInfo cInfo) {
	}

	/**
	 * Subclasses may redefine this method to implement application specific local event handling mechanisms
	 */
	protected void handleEvent(String id, String type, Object ev) {		
	}	

	/**
	 * Subclasses may redefine this method to implement application specific remote event handling mechanisms
	 */
	protected WorkflowEvent customizeEvent(String id, long time, String type, WorkflowEvent ev, List controllers) {
		return ev;
	}	
}
