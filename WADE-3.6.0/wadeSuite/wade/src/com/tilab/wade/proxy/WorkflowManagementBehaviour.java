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

import java.util.Date;

import jade.content.AgentAction;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.DefaultParameterValues;
import com.tilab.wade.performer.WebServiceSecurityContext;
import com.tilab.wade.performer.ontology.ControlInfo;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;

public abstract class WorkflowManagementBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 5512366647972793254L;

	protected static final long BEHAVIOUR_TIMEOUT = 10000;

	protected static Logger logger = Logger.getMyLogger(WorkflowManagementBehaviour.class.getName());

	private static int controllersCnt = 0;
	private Ontology onto = WorkflowManagementOntology.getInstance();
	private WorkflowController controller;
	
	protected EngineProxy engineProxy;
	protected WorkflowResultListener resultListener;
	protected EventGenerationConfiguration eventCfg;
	protected WorkflowContext context;
	protected String executionId;
	protected String sessionId;
	protected String conversationId;
	protected AID executor;
	protected boolean recovered;
	protected boolean aborted;
	protected String abortMessage;
	protected Exception abortException;
	protected boolean interactiveMode;
	
	protected abstract AgentAction prepareAgentAction();

	
	public WorkflowManagementBehaviour(EngineProxy engineProxy, WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) {
		super();
		this.engineProxy = engineProxy;
		this.resultListener = resultListener;
		this.eventCfg = eventCfg;
		this.context = context;
		this.interactiveMode = interactiveMode;
		this.recovered = false;
		this.aborted = false;
	}
	
	@Override
	public void onStart() {
		if (logger.isLoggable(Logger.CONFIG)) {
			logger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+": "+getClass().getSimpleName()+" started, sessionId="+sessionId);
		}

		if (!aborted) {
			try {
				conversationId = buildConversationId();
				engineProxy.getHandlers().addToResultHandler(conversationId, this);
				
				sendRequest();
				
			} catch (Exception e) {
				logger.log(Logger.SEVERE, "Agent "+myAgent.getLocalName()+": "+getClass().getSimpleName()+" Error sending request, sessionId="+sessionId, e);
				
				engineProxy.getHandlers().removeFromResultHandler(conversationId);
				setAbortMessage(e.getMessage(), e);
			}
		}
	}
	
	@Override
	public int onEnd() {
		if (logger.isLoggable(Logger.CONFIG)) {
			logger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+": "+getClass().getSimpleName()+" terminated, sessionId="+sessionId);
		}

		return super.onEnd();
	}

	@Override
	public void action() {
		if (!aborted) {
			// Block the behaviour until ResultHandler receive AGREE and set the executionId 
			if (executionId == null) {
				block();
			} else {
				// Create WorkflowController, add controller to engine-proxy map and terminate the behaviour
				controller = new WorkflowController(engineProxy, executor, executionId, sessionId, eventCfg, recovered, this);
				
				engineProxy.addController(sessionId, controller);
			}
		}
	}

	@Override
	public boolean done() {
		return aborted || controller!=null;
	}
	
	WorkflowController getWorkflowController() {
		return controller;
	}
	
	void abort(String abortMessage) {
		setAbortMessage(abortMessage, null);
		restart();
	}

	boolean isAborted() {
		return aborted;
	}
	
	protected void setAbortMessage(String abortMessage, Exception abortException) {
		this.abortMessage = abortMessage;
		this.abortException = abortException;
		this.aborted = true;
	}
	
	String getAbortMessage() {
		return abortMessage;
	}
	
	Exception getAbortException() {
		return abortException;
	}
	
	void setAgreeInfo(String executionId, AID executor) {
		this.executionId = executionId;
		this.executor = executor;
		restart();
	}

	String getExecutionId() {
		return executionId;
	}

	String getConversationId() {
		return conversationId;
	}

	String getSessionId() {
		return sessionId;
	}

	WorkflowResultListener getResultListener() {
		return resultListener;
	}

	private void sendRequest() throws CodecException, OntologyException {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.addReceiver(executor);
		//request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		request.setLanguage(LEAPCodec.NAME);
		
		request.setOntology(onto.getName());
		request.setReplyByDate(new Date(System.currentTimeMillis() + BEHAVIOUR_TIMEOUT));
		request.setConversationId(conversationId);

		AgentAction aa = prepareAgentAction();
		Action action = new Action(executor, aa);

		myAgent.getContentManager().fillContent(request, action);
		myAgent.send(request);
		
		if (logger.isLoggable(Logger.CONFIG)) {
			logger.log(Logger.CONFIG, "Agent "+myAgent.getLocalName()+": "+getClass().getSimpleName()+" send request, action="+action+", sessionId="+sessionId);
		}
	}
	
	private String buildConversationId() {
		controllersCnt++;
		return myAgent.getLocalName()+"-"+String.valueOf(controllersCnt)+"-"+System.currentTimeMillis();
	}	
	
	void update(WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) {
		this.resultListener = resultListener;
		this.eventCfg = eventCfg;
		this.context = context;
		this.interactiveMode = interactiveMode;
	}
	
	protected List prepareControlInfos() {
		// Only for FLOW and TRACE type add my controller (if not already present)
		List cInfos = null;
		if (eventCfg != null) {
			cInfos = eventCfg.getControlInfos();
			Iterator it = cInfos.iterator();
			while(it.hasNext()) {
				ControlInfo cInfo = (ControlInfo)it.next();
				String type = cInfo.getType();
				if (type.equals(Constants.FLOW_TYPE) ||
					type.equals(Constants.TRACING_TYPE)) {
					List controllers = cInfo.getControllers();
					if (controllers == null || !controllers.contains(myAgent.getAID())) {
						cInfo.setController(myAgent.getAID());
					}
				}
			}
		}
		return cInfos;
	}
	
	protected List prepareModifiers() {
		List modifiers = null;
		if (context != null) {
			modifiers = new ArrayList();

			// Default parameter values
			DefaultParameterValues dpv = context.getDefaultParameterValues();
			if (dpv != null) {
				dpv.apply(modifiers);
			}
			
			// Web-services default security context
			WebServiceSecurityContext wssc = context.getWebServiceDefaultSecurityContext();
			if (wssc != null) {
				wssc.apply(modifiers);
			}
			
			// Web-services activities security context
			java.util.List<WebServiceSecurityContext> activitiesWssc = context.getWebServiceActivitiesSecurityContext();
			if (activitiesWssc != null) {
				for (WebServiceSecurityContext activitywssc : activitiesWssc) {
					activitywssc.apply(modifiers);
				}
			}
		}
		
		// If interactiveMode is true -> add interactive modifier
		if (interactiveMode) {
			Modifier proxyMod = new Modifier(Constants.INTERACTIVE_MODIFIER);
			proxyMod.setProperty(Constants.INTERACTIVE_AID, myAgent.getAID());
			if (modifiers == null) {
				modifiers = new ArrayList();
			}
			modifiers.add(proxyMod);
		}
		return modifiers;
	}
}
