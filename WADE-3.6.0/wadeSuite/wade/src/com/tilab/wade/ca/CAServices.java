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

import jade.core.AID;
import jade.core.Agent;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.tilab.wade.ca.ontology.IsGlobalProperty;
import com.tilab.wade.utils.CAUtils;

/**
 * This class provides a number of features that are managed by the local Control Agent and made available 
 * to all agents living in the same container.
 */
public class CAServices{

	private static Map<String, CAServices> containerCAServices = new Hashtable<String, CAServices>();
	
	private ControllerAgent localCA;
	
	// This is static as it is shared by all CAServices in the same JVM in case there is more than one
	private static WadeClassLoaderManager classLoaderManager;
	private Map<AID, Map> receiversMap = new HashMap<AID, Map>();
	private Map<String, String> globalProperties = new Hashtable<String, String>();
	private Map<String, Object> extensions = new Hashtable<String, Object>();
	
	private Logger myLogger = Logger.getMyLogger(CAServices.class.getName());
	
	/**
	 * Retrieve the singleton instance of the CAServices associated to the container agent <code>a</code> lives in
	 * @param a The agent requesting access to the CAServices object
	 * @return The singleton instance of the CAServices associated to the container agent <code>a</code> lives in
	 */
	public static CAServices getInstance(Agent a) {
		if (a == null) {
			throw new NullPointerException("Agent cannot be null when retrieving CAServices");
		}
		CAServices result = null;
		String container = a.here().getName();
		synchronized (containerCAServices){
			result = containerCAServices.get(container);
			// Lazy initialization
			if (result == null) {
				result = new CAServices();
				containerCAServices.put(container, result);
			}
		}
		return result;
	}
	
	
	////////////////////////////////////////////
	// Local CA retrieval section
	////////////////////////////////////////////
	/**
	 * Retrieve the AID of the local Control Agent
	 * @return The AID of the local Control Agent or <code>null</code> if no Control Agent lives in the local container
	 */
	public AID getLocalCA(){
		if (localCA != null) {
			return localCA.getAID();
		}
		else {
			return null;
		}
	}
	
	void setLocalCA(ControllerAgent ca){
		localCA = ca;
	}

	
	////////////////////////////////////////////
	// WADE Class Loader section
	////////////////////////////////////////////
	/**
	 * Retrieve the WADE class loader corresponding to a given identifier. Since deployment 
	 * of new classes can occur at any time asynchronously with respect to workflow execution,
	 * delegated subflows must be loaded using the same classloader that loaded the parent workflow even if 
	 * newer classloaders exist. If a WADE class loader doesn't exist, retrieve system class loader.
	 * @param id The identifier of the WADE class loader to be retrieved
	 * @return The WADE class loader corresponding to a given identifier
	 */
	public ClassLoader getClassLoader(String id){
		if (classLoaderManager != null){
			return classLoaderManager.getClassLoader(id);
		}
		return getClass().getClassLoader();
	}
	
	/**
	 * Retrieve the newest WADE class loader. Calling this method has the same effect of calling
	 * <code>getClassLoader()</code> with a null classloader identifier
	 * @return the newest WADE class loader.
	 */
	public ClassLoader getDefaultClassLoader(){
		return getClassLoader(null);
	}

	// This must be called by the local CA prior to any call to getClassLoader() or getDefaultClassLoader().
	// It is synchronized in case there is more than one CA/CAServices in the same JVM
	synchronized WadeClassLoaderManager getClassLoaderManager(){
		if (classLoaderManager == null){
			classLoaderManager = new WadeClassLoaderManager();
		}
		return classLoaderManager;
	}

	
	////////////////////////////////////////////
	// Expected replies registration section
	////////////////////////////////////////////
	/**
	 * Register interest to be notified in case an agent that is expected to send a reply
	 * to a given message <code>msg</code> suddenly dies. The notification is sent in form of 
	 * a FAILURE message configured as a reply to <code>msg</code>. The sender of the FAILURE
	 * message will be the local Control Agent. The content of the message includes the agent 
	 * that suddenly died.  
	 * @param msg The message to which one or more replies (depending on how many receivers are
	 * specified) are expected.
	 * @see #expectedReplyReceived(ACLMessage)
	 * @see CAUtils#getDeadAgent(ACLMessage)
	 */
	public void registerExpectedReply (ACLMessage msg){
		if (localCA != null) {
			synchronized (receiversMap){
				jade.util.leap.Iterator allReceiver = msg.getAllReceiver();
				AID receiver;
				while(allReceiver.hasNext()){
					receiver = (AID)allReceiver.next();
					if (receiver.equals(msg.getSender())){
						continue;
					}
					Map messages = (Map)receiversMap.get(receiver);
					if (messages == null){
						messages = new HashMap();
						receiversMap.put(receiver, messages);
					}
					String key = msg.getSender().getName()+"/"+msg.getConversationId()+"/"+msg.getReplyWith();
					messages.put(key, msg);
					if (myLogger.isLoggable(Logger.FINEST)) {
						myLogger.log(Logger.FINEST, "Registered the message with key " + key + " and receiver " + receiver.getName()+ ".\n Now receiver map is " + receiversMap);
					}
					
				}
			}
		}
	}
	
	/**
	 * Notify the CAServices object that a reply that was expected has been received (it is not expected 
	 * anymore)
	 * @param reply The received reply
	 * @see #registerExpectedReply(ACLMessage)
	 */
	public void expectedReplyReceived (ACLMessage reply){
		if (localCA != null) {
			AID sender = reply.getSender();
			
			// If sender is the local CA, extract the original sender from content
			if (sender.equals(getLocalCA())){
				try {
					sender = CAUtils.getDeadAgent(reply);
					if (myLogger.isLoggable(Logger.FINEST)) {
						myLogger.log(Logger.FINEST,"Received failure from CA, the original sender is " + sender.getName());
					}
				} catch (FIPAException e) {
					myLogger.log(Logger.WARNING, "Message content invalid ("+reply.getContent()+")");
					return;
				}
			}
			// If sender is the ams, extract the original sender from content
			else if (sender.equals(localCA.getAMS())){ 
				try {
					sender = AMSService.getFailedReceiver(localCA, reply); 
					if (myLogger.isLoggable(Logger.FINEST)) {
						myLogger.log(Logger.FINEST, "Received failure from AMS, the original sender is " + sender.getName());
					}
				} catch (FIPAException e) {
					myLogger.log(Logger.WARNING, "Message content invalid ("+reply.getContent()+")");
					return;
				}
			}
	
			AID receiver = (AID)reply.getAllReceiver().next();
			if (!receiver.equals(sender)) {
				synchronized (receiversMap){
					Map messages = (Map)receiversMap.get(sender);
					if (messages != null){
						String key = receiver.getName()+"/"+reply.getConversationId()+"/"+reply.getInReplyTo();
						messages.remove(key);
						if (messages.isEmpty()){
							receiversMap.remove(sender);
						}
						if (myLogger.isLoggable(Logger.FINEST)) {
							myLogger.log(Logger.FINEST, "Deregistered the message with key " + key + " and sender " + sender.getName()+ ".\n Now receiver map is " + receiversMap);
						}
					}
				}
			}
		}
	}
	
	void handleDeadAgent(AID agent){
		synchronized (receiversMap) {
			Iterator it = receiversMap.keySet().iterator();
			while (it.hasNext()){
				AID a = (AID)it.next();
				Map msgs = (Map)receiversMap.get(a);
				if (msgs != null){
					Iterator msgIter = msgs.keySet().iterator();
					while (msgIter.hasNext()){
						String key = (String)msgIter.next();
						ACLMessage m = (ACLMessage)msgs.get(key);
						if (a.equals(agent)){
							//CA replies with failure message because the agent is dead
							ACLMessage reply = m.createReply();
							reply.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true"); //in this way some possible failures will be ignored.
							reply.setSender(localCA.getAID());
							reply.setPerformative(ACLMessage.FAILURE);
							reply.setContent("((Agent-dead (agent-identifier :name " + agent.getName() + ")(internal-error \" Agent not found: getContainerID() failed to find agent " + agent.getName()+"\")))");
							localCA.send(reply);
						}else{
							//remove dandling reply
							if (agent.equals(m.getSender())){
								if (myLogger.isLoggable(Logger.FINEST)) {
									myLogger.log(Logger.FINEST, "Removed message (with key = "+ key + ") that had as sender the dead agent " + agent + ".");
								}
								msgIter.remove();
							}
						}
					}
					if (msgs.isEmpty()){
						it.remove();
					}
				}
			}
		}
	}
	
	void handleMovedAgent(AID agent){
		//if an agent is moved in another container, its info must be removed
		synchronized (receiversMap) {
			Iterator it = receiversMap.keySet().iterator();
			while (it.hasNext()){
				AID a = (AID)it.next();
				Map msgs = (Map)receiversMap.get(a);
				if (msgs != null){
					Iterator msgIter = msgs.keySet().iterator();
					while (msgIter.hasNext()){
						String key = (String)msgIter.next();
						ACLMessage m = (ACLMessage)msgs.get(key);
						if (agent.equals(m.getSender())){
							if (myLogger.isLoggable(Logger.FINEST)) {
								myLogger.log(Logger.FINEST, agent.getName() + " is moved in another container, then its message (with key = "+ key + ") is deleted.");
							}
							msgIter.remove();
						}
					}
					if (msgs.isEmpty()){
						it.remove();
					}
				}
			}
		}
	}

	
	////////////////////////////////////////////
	// Global properties section
	////////////////////////////////////////////
	public String getGlobalProperty(String name) {
		return globalProperties.get(name);
	}
	
	public void setGlobalProperty(String name, String value) {
		if (localCA != null) {
			globalProperties.put(name, value);
			IsGlobalProperty igp = new IsGlobalProperty(new Property(name, value));
			localCA.notifyControlAgents(ACLMessage.INFORM, igp);
		}
	}
	
	public void removeGlobalProperty(String name) {
		if (localCA != null) {
			String value = globalProperties.remove(name);
			IsGlobalProperty igp = new IsGlobalProperty(new Property(name, value));
			localCA.notifyControlAgents(ACLMessage.DISCONFIRM, igp);
		}
	}
	
	// Method only accessible to the local CA
	Map<String, String> getGlobalProperties() {
		return globalProperties;
	}
	
	////////////////////////////////////////////
	// CAServices extension section
	////////////////////////////////////////////
	/**
	 * Retrieve an application specific extension of the CAServices functionality
	 * @param extensionName The name of the CAServices extension to retrieve
	 * @return The CAServices extension corresponding to the given name or null if no such extension is registered. 
	 * @see ControllerAgent#registerCAServicesExtension(String, Object)
	 */
	public Object getExtension(String extensionName) {
		return extensions.get(extensionName);
	}
	
	void registerExtension(String extensionName, Object extension) {
		extensions.put(extensionName, extension);
	}
	
	/////////////////////////////////////////////
	// Self termination
	/////////////////////////////////////////////
	/**
	 * Inform the local CA that this agent is spontaneously terminating --> The local CA will not restart it 
	 * when receiving the DeadAgent event.
	 * This method is intended to be called just before self invoking doDelete() 
	 * @param a The agent that is about to terminate
	 */
	public void setTerminating(Agent a) {
		if (localCA != null) {
			localCA.setTerminating(a);
		}
	}
}
