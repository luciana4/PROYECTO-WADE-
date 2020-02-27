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
package com.tilab.wade.dispatcher;


import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.FIPAService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.tilab.wade.event.Occurred;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.WorkflowException;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.ControlInfo;
import com.tilab.wade.performer.ontology.ExecuteWorkflow;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.performer.ontology.GenericError;
import com.tilab.wade.performer.ontology.GetPoolSize;
import com.tilab.wade.performer.ontology.GetWRD;
import com.tilab.wade.performer.ontology.KillWorkflow;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.ontology.NotificationError;
import com.tilab.wade.performer.ontology.SetControlInfo;
import com.tilab.wade.performer.ontology.SetPoolSize;
import com.tilab.wade.performer.ontology.SetWRD;
import com.tilab.wade.performer.ontology.ControlInfoChanges;
import com.tilab.wade.performer.ontology.UpdateControlInfo;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;

/**
 * This class provides utility methods corresponding to the actions 
 * of the WorkflowManagementOntology. By embedding a <code>DispatchingCapabilities</code>
 * instance, an agent acquires the ability of launching workflows, setting and retrieving,
 * workflow relevant data and listening to events generated by the execution of workflows.
 * 
 * @author Giovanni Caire - TILAB
 */
public class DispatchingCapabilities {
	private Agent myAgent;
	private String requestReplyWith;
	
	private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	
	public Codec codec = new SLCodec();
	public Ontology onto = WorkflowManagementOntology.getInstance();

	private int controllersCnt = 0;
	private Hashtable listeners = new Hashtable();
	private Hashtable controllers = new Hashtable();
	
	private Logger myLogger;
	
	/**
	   Initialize this <code>DispatchingCapabilities</code> object
	   connecting it to the Agent that will use it.
	 */
	public void init(Agent a) {
		init(a, null);
	}
	
	/**
	   Initialize this <code>DispatchingCapabilities</code> object
	   connecting it to the Agent that will use it and specifying the value of the 
	   reply-with slot that will be inserted in all workflow execution REQUEST messages.
	 */
	public void init(Agent a, String replyWith) {
		myAgent = a;
		myAgent.getContentManager().registerLanguage(codec);
		myAgent.getContentManager().registerOntology(onto);	
		
		requestReplyWith = replyWith;
		
		myLogger = Logger.getMyLogger(myAgent.getName());
	}
	
	public void reset() {
		tbf.interrupt();
		listeners.clear();
		synchronized (controllers) {
			Enumeration ee = controllers.elements();
			while (ee.hasMoreElements()) {
				Behaviour b = (Behaviour) ee.nextElement();
				myAgent.removeBehaviour(b);
			}
		}
	}
	
	public void setRequestReplyWith(String replyWith) {
		requestReplyWith = replyWith;
	}
	
	
	///////////////////////////////////////////
	// Dispatching methods
	///////////////////////////////////////////
	/** 
	   Launch the execution of a workflow on a given WorkflowEngineAgent
	   specifying the WorkflowDescriptor, the execution control information
	   and a listener for the results.
	   @return The conversation-id associated by the protocol used to launch the workflow and get back the result.
	 */
	public String launchWorkflow(AID execAid, WorkflowDescriptor wd, WorkflowResultListener resultListener, List cInfos) throws WorkflowException {
		ACLMessage msg = prepareRequest(execAid, wd, cInfos);
		return launchWorkflow(msg, resultListener, wd.getExecution());
	}
	
	/** 
	   Launch the execution of a workflow on a given WorkflowEngineAgent
	   through a given WorkflowDispatcherAgent specifying the WorkflowDescriptor, the execution control information
	   and a listener for the results.
	   @return The conversation-id associated by the protocol used to launch the workflow and get back the result.
	 */
	public String launchWorkflow(AID execAid, AID dispatcher, WorkflowDescriptor wd, WorkflowResultListener resultListener, List cInfos) throws WorkflowException {
		ACLMessage msg = prepareRequest(execAid, dispatcher, wd, cInfos);
		return launchWorkflow(msg, resultListener, wd.getExecution());
	}
	
	/** 
	   Launch the execution of a workflow on a given WorkflowEngineAgent
	   through a given WorkflowDispatcherAgent specifying the ExecuteWorkflow action and a listener for the results.
	   @return The conversation-id associated by the protocol used to launch the workflow and get back the result.
	 */
	public String launchWorkflow(AID execAid, AID dispatcher, ExecuteWorkflow ew, WorkflowResultListener resultListener) throws WorkflowException {
		ACLMessage msg = prepareRequest(execAid, dispatcher, ew);
		return launchWorkflow(msg, resultListener, ew.getWhat().getExecution());
	}
		
	/** 
	   Launch the execution of a workflow by means of a given (already prepared) REQUEST message 
	   specifying a given WorkflowResultListener
	   @return The conversation-id associated to the protocol used to launch the workflow and get back the result.
	 */
	public String launchWorkflow(ACLMessage request, WorkflowResultListener resultListener, int mode) {
		// Be sure a conversationID is set
		if (request.getConversationId() == null) {
			request.setConversationId(buildConversationId());
		}
		if (resultListener != null) {
			ExecutionControllerBehaviour ecb = new ExecutionControllerBehaviour(request, resultListener, mode);
			myAgent.addBehaviour(ecb);
		}
		else {
			myAgent.send(request);
		}
		return request.getConversationId();
	}
	
	/** 
	   Create the message to launch the execution of a workflow on a given WorkflowEngineAgent
	 */
	public ACLMessage prepareRequest(AID execAid, WorkflowDescriptor wd, List cInfos) throws WorkflowException {
		return prepareRequest(execAid, execAid, wd, cInfos);
	}
		
	/** 
	   Create the message to launch the execution of a workflow on a given WorkflowEngineAgent through a given 
	   WorkflowDispatcherAgent, specifying the execution control information.
	 */
	public ACLMessage prepareRequest(AID execAid, AID dispatcher, WorkflowDescriptor wd, List cInfos) throws WorkflowException {
		ExecuteWorkflow ew = new ExecuteWorkflow(wd, cInfos);
		return prepareRequest(execAid, dispatcher, ew);
	}
	
	/** 
	   Create the message to launch the execution of a workflow on a given WorkflowEngineAgent through a given 
	   WorkflowDispatcherAgent, specifying the ExecuteWorkflow action.
	 */
	public ACLMessage prepareRequest(AID execAid, AID dispatcher, ExecuteWorkflow ew) throws WorkflowException {
		if (execAid == null) {
			// No executor specified: let the dispatcher choose one
			execAid = dispatcher;
		}
		Action aExpr = new Action(execAid, ew);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(dispatcher);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		long timeout = getMaxResponseTime(execAid, ew.getWhat());
		if (timeout > 0) {
			msg.setReplyByDate(new Date(System.currentTimeMillis() + timeout));
		}
		msg.setConversationId(buildConversationId());
		controllersCnt++;
		msg.setReplyWith(requestReplyWith);
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
		}
		catch (Exception e) {
			throw new WorkflowException("Error encoding execution REQUEST.", e); 
		}
		
		return msg;
	}
	
	/**
	 * Retrieve the REQUEST message of the protocol used to launch a workflow currently in execution.
	 * @param convId The conversation-id of the protocol
	 * @return the REQUEST message of the protocol used to launch a workflow currently in execution.
	 */
	public ACLMessage getLaunchWorkflowRequest(String convId) {
		ExecutionControllerBehaviour b = (ExecutionControllerBehaviour) controllers.get(convId);
		if (b != null) {
			Vector requests = (Vector) b.getDataStore().get(b.ALL_REQUESTS_KEY);
			if (requests != null && requests.size() > 0) {
				return (ACLMessage) requests.elementAt(0);
			}
		}
		return null;
	}
	
	/**
	 * Create a new ACLMessage holding the same ExecuteWorkflow request as a given REQUEST message,
	 * but with the VERIFY modifier turned on.
	 * @param request The original ExecuteWorkflow request
	 */
	public ACLMessage setVerifyModifier(ACLMessage request) throws WorkflowException {
		try {
			Action aExpr = (Action) myAgent.getContentManager().extractContent(request);
			ExecuteWorkflow ew = (ExecuteWorkflow) aExpr.getAction();
			List modifiers = ew.getModifiers();
			if (modifiers == null) {
				modifiers = new ArrayList();
				ew.setModifiers(modifiers);
			}
			Modifier verifyMod = Modifier.getModifier(Constants.VERIFY_MODIFIER, modifiers);
			if (verifyMod == null) {
				verifyMod = new Modifier(Constants.VERIFY_MODIFIER);
				modifiers.add(verifyMod);
			}
			
			ACLMessage verifyRequest = (ACLMessage) request.clone();
			myAgent.getContentManager().fillContent(verifyRequest, aExpr);
			
			// Also update the reply-by slot
			long timeout = getMaxResponseTime(aExpr.getActor(), ew.getWhat());
			if (timeout > 0) {
				verifyRequest.setReplyByDate(new Date(System.currentTimeMillis() + timeout));
			}
			return verifyRequest;
		}
		catch (Exception e) {
			throw new WorkflowException("Error setting VERIFY modifier.", e); 
		}
	}

	public void killWorkflow(AID execAid, String executionId) throws WorkflowException {
		killWorkflow(execAid, executionId, false);
	}
	
	/**
	   Kill a workflow currently in execution.
	 */
	public void killWorkflow(AID execAid, String executionId, boolean freeze) throws WorkflowException {
		KillWorkflow ki = new KillWorkflow(executionId);
		ki.setFreeze(freeze);
		Action aExpr = new Action(execAid, ki);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
		catch (Exception e) {
			throw new WorkflowException("Error killing workflow. ", e);
		}
	}
						
	/**
	   Set an event listener for events of a given type generated 
	   during the execution of a given workflow.
 	 */
	public void setEventListener(WorkflowEventListener listener, String type, AID execAid, String executionId) {
		setEventListener(new WorkflowEventListener[]{listener}, new String[]{type}, execAid, executionId);
	}
	
	/**
	   Set the event listeners for events of given types generated 
	   during the execution of a given workflow.
 	 */
	public void setEventListener(WorkflowEventListener[] ll, String[] types, AID execAid, String executionId) {
		String id = buildListenerId(execAid, executionId);
		
		WorkflowEventListenerBehaviour b = (WorkflowEventListenerBehaviour) listeners.get(id);
		if (b != null) {
			b.setListeners(types, ll);
		}
		else {
			// Start a new listener-behaviour.
			// We use a threaded behaviour so that the Agent thread
			// does not block when handling workflow events in debug mode
			b = new WorkflowEventListenerBehaviour(id, execAid, executionId);
			b.setListeners(types, ll);
			listeners.put(id, b);
			myAgent.addBehaviour(tbf.wrap(b));
		}
	}
	
	public void resetEventListener(AID execAid, String executionId) {
		String id = buildListenerId(execAid, executionId);
		
		WorkflowEventListenerBehaviour b = (WorkflowEventListenerBehaviour) listeners.remove(id);
		if (b != null) {
			Thread t = tbf.getThread(b);
			if (t != null) {
				t.interrupt();
			}
		}
	}
	
	public void manageChangedExecutionId(AID execAid, String oldExecutionId, String newExecutionId) {
		String id = buildListenerId(execAid, oldExecutionId);
		synchronized (listeners) {
			WorkflowEventListenerBehaviour b = (WorkflowEventListenerBehaviour) listeners.remove(id);
			if (b != null) {
				String newId = buildListenerId(execAid, newExecutionId);
				b.changedExecutionId(newExecutionId, newId);
				listeners.put(newId, b);
			}
		}
	}
	
	/**
	   Set the execution control information of a given workflow.
	 */
	public void setControlInfo(ControlInfo cInfo, AID execAid, String executionId) throws WorkflowException {
		SetControlInfo sci = new SetControlInfo(executionId, cInfo);
		Action aExpr = new Action(execAid, sci);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
		catch (Exception e) {
			throw new WorkflowException("Error setting execution control information. ", e);
		}
	}
		
	/**
	   Update the execution control information of a given workflow.
	 */
	public void updateControlInfo(ControlInfoChanges cInfoChanges, AID execAid, String executionId) throws WorkflowException {
		UpdateControlInfo uci = new UpdateControlInfo(executionId, cInfoChanges);
		Action aExpr = new Action(execAid, uci);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
		catch (Exception e) {
			throw new WorkflowException("Error updating execution control information. ", e);
		}
	}
						
	/**
	   Get the value of a given Workflow Relevant Data
	 */
	public Object getWRD(String wrd, AID execAid, String executionId) throws WorkflowException {
		GetWRD gwrd = new GetWRD(executionId, wrd);
		Action aExpr = new Action(execAid, gwrd);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, msg);
			Result r = (Result) myAgent.getContentManager().extractContent(reply);
			return r.getValue();
		}
		catch (Exception e) {
			throw new WorkflowException("Error retrieving value of WRD "+wrd+". ", e);
		}
	}
				
	/**
	   Set the value of a given Workflow Relevant Data
	 */
	public void setWRD(String wrd, Object value, AID execAid, String executionId) throws WorkflowException {
		SetWRD swrd = new SetWRD(executionId, wrd, value);
		Action aExpr = new Action(execAid, swrd);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
		catch (Exception e) {
			throw new WorkflowException("Error setting value of WRD "+wrd+". ", e);
		}
	}
		
	/**
	   Get the current pool size of a given WorkflowEngineAgent
	 */
	public int getPoolSize(AID execAid) throws WorkflowException {
		GetPoolSize gps = new GetPoolSize();
		Action aExpr = new Action(execAid, gps);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, msg);
			Result r = (Result) myAgent.getContentManager().extractContent(reply);
			return ((Integer) r.getValue()).intValue();
		}
		catch (Exception e) {
			throw new WorkflowException("Error retrieving pool size. ", e);
		}
	}
				
	/**
	   Set the pool size of a given WorkflowEngineAgent
	 */
	public void setPoolSize(AID execAid, int poolSize) throws WorkflowException {
		SetPoolSize sps = new SetPoolSize(poolSize);
		Action aExpr = new Action(execAid, sps);
		
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(execAid);
		msg.setLanguage(codec.getName());
		msg.setOntology(onto.getName());
		try {
			myAgent.getContentManager().fillContent(msg, aExpr);
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
		catch (Exception e) {
			throw new WorkflowException("Error setting pool size. ", e);
		}
	}
		
	
	////////////////////////////////////////////////////////////
	// Methods that can be redefined to customize the default
	// behaviour of the DispatchingCapabilities
	////////////////////////////////////////////////////////////
	protected long getMaxResponseTime(AID executor, WorkflowDescriptor wd) {
		return 20000;
	}

	
	
	/**
	   Inner class ExecutionControllerBehaviour
	   This behaviour delegates the execution of a workflow to a 
	   WorkflowEngineAgent and gets back the result.
	 */
	private class ExecutionControllerBehaviour extends AchieveREInitiator {
		private WorkflowResultListener myResultListener;
		private AID executor;
		private String executionId;
		private String myId;
		private int executionMode;
		
		private ExecutionControllerBehaviour(ACLMessage request, WorkflowResultListener resultListener, int executionMode) {
			super(null, request);
			myResultListener = resultListener;
			Iterator it = request.getAllReceiver();
			executor = (AID) it.next();
			this.executionMode = executionMode;
			myId = request.getConversationId();
		}
		
		public void onStart() {
			if (myLogger.isLoggable(Logger.CONFIG)) {
				myLogger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" started");
			}
			controllers.put(myId, this);
			super.onStart();
		}
		
		public int onEnd() {
			int ret = super.onEnd();
			if (myLogger.isLoggable(Logger.CONFIG)) {
				myLogger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" terminated.");
			}
			controllers.remove(myId);
			return ret;
		}
		
		protected void handleAgree(ACLMessage agree) {
			// The workflow has been accepted. Get its execution ID and
			// notify the listener
			String exId = agree.getContent();
			// Null execution ID means a confirmation sent back in response to a Verify REQUEST
			if (exId != null) {
				String oldExecutionId = executionId;
				executionId = exId;
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" AGREE received. Execution-id = "+executionId);
				}
				myResultListener.handleAssignedId(executor, executionId);
				if (executionMode == Constants.ASYNCH) {
					// We will receive no result notification --> terminate
					forceTransitionTo(DUMMY_FINAL);
				}
			}
		}
		
		// Note that due to the VERIFY mechanism this is not a real FIPA-Request protocol since we may receive more than 
		// one AGREE message.
		protected void handleOutOfSequence(ACLMessage msg) {
			if (msg.getPerformative() == ACLMessage.AGREE) {
				handleAgree(msg);
			}
			else {
				myLogger.log(Logger.WARNING, "Unexpected message received. "+msg);
			}
		}
		
		protected void handleInform(ACLMessage inform) {
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" INFORM received.");
			}
			// The workflow has been completed successfully
			if (myResultListener != null) {
				try {
					Predicate p = (Predicate) myAgent.getContentManager().extractContent(inform);
					if (p instanceof Result) {
						myResultListener.handleExecutionCompleted(((Result) p).getItems(), executor, executionId);
					}
				}
				catch (Exception e) {
					// FIXME: Should we call handleNotificationError()?
					e.printStackTrace();
				}
			}
		}
		
		protected void handleRefuse(ACLMessage refuse) {
			myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" REFUSE received.");
			if (myResultListener != null) {
				try {
					Predicate p = (Predicate) myAgent.getContentManager().extractContent(refuse);
					if (p instanceof GenericError) {
						myResultListener.handleLoadError(((GenericError) p).getReason());
					}
				}
				catch (Exception e) {
					// FIXME: Should we call handleLoadError()?
					e.printStackTrace();
				}
			}
		}
		
		protected void handleNotUnderstood(ACLMessage notUnderstood) {
			myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" NOT_UNDERSTOOD received.");
			if (myResultListener != null) {
				try {
					Predicate p = (Predicate) myAgent.getContentManager().extractContent(notUnderstood);
					if (p instanceof GenericError) {
						myResultListener.handleLoadError(((GenericError) p).getReason());
					}
				}
				catch (Exception e) {
					// FIXME: Should we call handleLoadError()?
					e.printStackTrace();
				}
			}
		}
		
		protected void handleFailure(ACLMessage failure) {
			myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" FAILURE received.");
			if (failure.getSender().equals(myAgent.getAMS())) {
				myResultListener.handleLoadError("Executor "+executor.getLocalName()+" does not exist.");
			}
			else {
				try {
					Predicate p = (Predicate) myAgent.getContentManager().extractContent(failure);
					if (p instanceof GenericError) {
						myResultListener.handleLoadError(((GenericError) p).getReason());
					}
					else if (p instanceof NotificationError) {
						myResultListener.handleNotificationError(executor, executionId);
					}
					else if (p instanceof ExecutionError) {
						myResultListener.handleExecutionError((ExecutionError) p, executor, executionId);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					myResultListener.handleNotificationError(executor, executionId);
				}
			}			
		}
		
		protected void handleAllResponses(Vector responses) {
			Vector notifications = (Vector) getDataStore().get(ALL_RESULT_NOTIFICATIONS_KEY);
			if (responses.size() == 0 && notifications.size() == 0) {
				// Timeout expired --> Behave as if there was a failure
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getLocalName()+" - Controller "+myId+" timeout expired.");
				myResultListener.handleLoadError("Timeout expired");
			}
		}
	} // END of inner class ExecutionControllerBehaviour
	
	
	/**
	   Inner class WorkflowEventListenerBehaviour
	   This behaviour intercepts the events generated during the execution of
	   a workflow and passes them up to the proper listeners.
	 */
	private class WorkflowEventListenerBehaviour extends Behaviour {
		private volatile String myId;	
		private volatile String executionId;	
		private AID executor;
		private MessageTemplate tpl;
		private boolean finished = false;
		private Hashtable infos = new Hashtable();
		
		private WorkflowEventListenerBehaviour(String id, AID ex, String exId) {
			super();
			myId = id;
			executor = ex;
			executionId = exId;
			
			tpl = new MessageTemplate(new MessageTemplate.MatchExpression() {
				public boolean match(ACLMessage msg) {
					return (executor.equals(msg.getSender()) && 
							executionId.equals(msg.getConversationId()) && 
							infos.containsKey(msg.getProtocol()));
				}
			} );
		}

		/**
		 * If a new REQUEST in Verify mode is sent the execution ID may change
		 */
		public void changedExecutionId(String exId, String id) {
			executionId = exId;
			myId = id;
		}
		
		public void setListeners(String[] types, WorkflowEventListener[] ls) {
			if (types.length != ls.length) {
				throw new IllegalArgumentException("Types and listeners number mismatch");
			}
			
			// This synchronization block is there to guarantee the correct order in
			// event handling
			synchronized (infos) {
				for (int i = 0; i < types.length; ++ i) {
					WorkflowEventListener l = ls[i];
					String type = types[i];
					if (l != null) {
						ListenerInfo info = (ListenerInfo) infos.get(type);
						if (info != null) {
							info.setListener(l);
						}
						else {
							info = new ListenerInfo(type, l);
							infos.put(type, info);
						}
					}
					else {
						infos.remove(type);
					}
				}
			}
		}
		
		public void onStart() {
			if (myLogger.isLoggable(Logger.CONFIG)) {
				myLogger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+" - Event-listener "+myId+" started.");
			}
		}
			
		public int onEnd() {
			listeners.remove(myId);
			if (myLogger.isLoggable(Logger.CONFIG)) {
				myLogger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+" - Event-listener "+myId+" terminated.");
			}
			return 0;
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive(tpl);
			if (msg != null) {
				String type = (msg.getProtocol() != null ? msg.getProtocol() : "UNKNOWN");
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						Occurred o = (Occurred) myAgent.getContentManager().extractContent(msg);
						if (myLogger.isLoggable(Logger.FINER)) {
							myLogger.log(Logger.FINER, "Agent "+myAgent.getLocalName()+" - Event-listener "+myId+" received event of type "+type+": "+o.getEvent());
						}
						ListenerInfo info = (ListenerInfo) infos.get(type);
						if (info != null) {
							info.getListener().handleEvent(o.getTime(), o.getEvent(), executor, executionId);
						}
						
						if (msg.getReplyWith() != null) {
							// Workflow events are handled in debug mode --> send back a reply
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.INFORM);
							myAgent.send(reply);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if (msg.getPerformative() == ACLMessage.CANCEL) {
					if (myLogger.isLoggable(Logger.FINER)) {
						myLogger.log(Logger.FINER, "Agent "+myAgent.getLocalName()+" - Event-listener "+myId+" received termination notification for type "+type);
					}
					ListenerInfo info = (ListenerInfo) infos.remove(type);
					if (info != null) {
						info.getListener().handleExecutionCompleted(executor, executionId);
						finished = (infos.size() == 0);
					}
				}					
			}
			else {
				block();
			}			
		}
		
		public boolean done() {
			return finished;
		}		
	} // END of inner class WorkflowEventListenerBehaviour
	
	
	/**
	 * Inner class ListenerInfo
	 */
	private class ListenerInfo {
		private WorkflowEventListener myListener;
		private String myType;
		
		public ListenerInfo(String type, WorkflowEventListener listener) {
			myType = type;
			myListener = listener;
		}
		
		public String getType() {
			return myType;
		}
		
		public void setListener(WorkflowEventListener listener) {
			myListener = listener;			
		}
		
		public WorkflowEventListener getListener() {
			return myListener;
		}
	} // END of inner class ListenerInfo
	
	
	///////////////////////////////
	// Private methods
	///////////////////////////////
	private String buildConversationId() {
		return myAgent.getLocalName()+"-"+String.valueOf(controllersCnt)+"-"+System.currentTimeMillis();
	}
	
	private String buildListenerId(AID executor, String executionId) {
		return executor.getName()+'#'+executionId;
	}	
}