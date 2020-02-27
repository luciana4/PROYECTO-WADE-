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
package com.tilab.wade.performer;

import java.util.logging.Level;

import jade.content.Predicate;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.List;
import jade.util.leap.Serializable;

import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.ontology.GetCurrentLoad;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.performer.WorkflowEngineAgent.WorkflowExecutor;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;
import com.tilab.wade.utils.DFUtils;
import com.tilab.wade.wsma.ontology.StatusChanged;
import com.tilab.wade.wsma.ontology.Started;
import com.tilab.wade.wsma.ontology.SerializedStateChanged;
import com.tilab.wade.wsma.ontology.Terminated;
import com.tilab.wade.wsma.ontology.Thawed;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowStatusOntology;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;

class StatusManager implements Serializable {

	private static final long serialVersionUID = -5152409003723640917L;

	protected Logger myLogger = Logger.getMyLogger(StatusManager.class.getName());

	private static final String WSMA_POLICY_ATTRIBUTE = "WsmaPolicy";
	private static final String DEFAULT_WSMA_POLICY = WsmaPolicyType.CURRENT_LOAD.name();
	private static final String GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE = "GetCurrentLoadTimeout";
	private static final long DEFAULT_GET_CURRENT_LOAD_TIMEOUT = 5000;
	
	private enum WsmaPolicyType { ROUND_ROBIN, CURRENT_LOAD };

	private WsmaPolicyType wsmaPolicyType;
	private long getCurrentLoadTimeout;
	private AID[] wsmaAids;
	private int currentAidIndex = 0;
	private WorkflowEngineAgent workflowEngineAgent;
	private Ontology onto = WorkflowStatusOntology.getInstance();


	public StatusManager(WorkflowEngineAgent workflowEngineAgent) {
		this.workflowEngineAgent = workflowEngineAgent;

		// Get wsma-policy-type, try before in configuration arguments and than in types file   
		String wsmaPolicyTypeAttribute = workflowEngineAgent.getArgument(WSMA_POLICY_ATTRIBUTE, null);
		if (wsmaPolicyTypeAttribute == null) {
			wsmaPolicyTypeAttribute = workflowEngineAgent.getTypeProperty(WSMA_POLICY_ATTRIBUTE, DEFAULT_WSMA_POLICY);
		}
		wsmaPolicyType = WsmaPolicyType.valueOf(wsmaPolicyTypeAttribute);

		// Get get-current-load-timeout, try before in configuration arguments and than in types file   
		getCurrentLoadTimeout = workflowEngineAgent.getLongArgument(GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE, -1);
		if (getCurrentLoadTimeout == -1) {
			getCurrentLoadTimeout = workflowEngineAgent.getLongTypeProperty(GET_CURRENT_LOAD_TIMEOUT_ATTRIBUTE, DEFAULT_GET_CURRENT_LOAD_TIMEOUT);
		}
		
		// Get RequireWSMA attribute
		boolean requireWSMA;
		String requireWSMAStr = workflowEngineAgent.getArgument(WorkflowEngineAgent.REQUIRE_WSMA_ATTRIBUTE, null);
		if (requireWSMAStr != null) {
			requireWSMA = ((Boolean) BasicOntology.adjustPrimitiveValue(requireWSMAStr, Boolean.class)).booleanValue();
		} else {
			requireWSMA = workflowEngineAgent.getBooleanTypeProperty(WorkflowEngineAgent.REQUIRE_WSMA_ATTRIBUTE, false);
		} 
		
		// Get WSMASearchTimeout attribute
		long wsmaSearchTimeout = workflowEngineAgent.getLongArgument(WorkflowEngineAgent.WSMA_SEARCH_TIMEOUT_ATTRIBUTE, -1);
		if (wsmaSearchTimeout == -1) {
			wsmaSearchTimeout = workflowEngineAgent.getLongTypeProperty(WorkflowEngineAgent.WSMA_SEARCH_TIMEOUT_ATTRIBUTE, WorkflowEngineAgent.DEFAULT_WSMA_SEARCH_TIMEOUT);
		}
		
		// Get WSMA aids in specific timeout
		long startTime = System.currentTimeMillis();
		do {
			try {
				DFAgentDescription[] dfds = DFUtils.searchAllByType(workflowEngineAgent, WadeAgent.WSMA_AGENT_TYPE, null);
				wsmaAids = DFUtils.getAIDs(dfds);
				if (wsmaAids.length > 0) {
					break;
				}
				Thread.sleep(500);
			} catch (Exception e) {
				if (myLogger.isLoggable(Logger.WARNING)) {
					myLogger.log(Logger.WARNING, "Error searching WSMA agents into DF.", e);
				}
			}
		} while ((System.currentTimeMillis()-startTime) < wsmaSearchTimeout);
			
		// Check WSMA availability
		if (wsmaAids.length > 0) {
			// Register WorkflowStatusOntology
			workflowEngineAgent.getContentManager().registerOntology(onto);
			workflowEngineAgent.getContentManager().registerOntology(WorkflowManagementOntology.getInstance());
		} else {
			if (requireWSMA) {
				throw new RuntimeException("Agent "+workflowEngineAgent.getName()+" require workflow status manager agent but it is not available!");
			} 

			if (myLogger.isLoggable(Logger.WARNING)) {
				myLogger.log(Logger.WARNING, "Workflow status manager agents not available but not required. Workflow persistence functions are disabled.");
			}
		}
	}

	public void notifyStarted(WorkflowExecutor wfExecutor) {
		AID wsmaAid = getWSMA();
		if (wsmaAid != null) {
			wfExecutor.setWSMA(wsmaAid);

			WorkflowDescriptor wd = wfExecutor.getDescriptor();
			WorkflowBehaviour wb = wfExecutor.getWorkflow();
			WorkflowDetails wDetail = wb.getDetails();
			WorkflowExecutionInfo wei = new WorkflowExecutionInfo();
			wei.setExecutionId(wfExecutor.getId());
			wei.setRequester(wd.getRequester());
			wei.setExecutorName(workflowEngineAgent.getAID().getName());
			wei.setSessionId(wd.getSessionId());
			wei.setLongRunning(wb.isLongRunning());
			wei.setTransactional(wd.getTransactional());
			wei.setInteractive(EngineHelper.isInteractive(wfExecutor));
			wei.setStatus(WorkflowStatus.ACTIVE);
			wei.setWorkflowId(wd.getId());
			wei.setName(wDetail.getName());
			wei.setDocumentation(wDetail.getDocumentation());
			
			// Set parameters
			if (wd.getParameters() != null) {
				wei.setWadeParameters(((ArrayList)wd.getParameters()).toList());
			}
			
			// Set ParentExecutionid
			String dc = wd.getDelegationChain();
			if (dc != null) {
				DelegationChainElement[] delegationChainArray = DelegationChainElement.parseDelegationChain(dc);
				DelegationChainElement dce = delegationChainArray[delegationChainArray.length-1];
				wei.setParentExecutionid(dce.getExecutionId());
			}

			sendStatusNotification(wsmaAid, new Started(wei));
		}
	}

	public void notifyThawed(WorkflowExecutor wfExecutor) {
		AID wsmaAid = getWSMA();
		if (wsmaAid != null) {
			wfExecutor.setWSMA(wsmaAid);
			
			String executionId = wfExecutor.getId();
			String executorName = wfExecutor.getWorkflow().getAgent().getName();
			sendStatusNotification(wsmaAid, new Thawed(executionId, executorName));
		}
	}
	
	public void notifyTerminated(WorkflowExecutor wfExecutor, List wadeParameters, String errorMessage) {
		AID wsmaAid = wfExecutor.getWSMA();
		if (wsmaAid != null) {
			String executionId = wfExecutor.getId();
			java.util.List<Parameter> parameters = new java.util.ArrayList<Parameter>();
			if (wadeParameters != null) {
				parameters = ((ArrayList)wadeParameters).toList();
			}

			sendStatusNotification(wsmaAid, new Terminated(executionId, parameters, errorMessage));
		}
	}
	
	public void notifySerializedStateChanged(WorkflowExecutor wfExecutor, byte[] serializedState) {
		AID wsmaAid = wfExecutor.getWSMA();
		if (wsmaAid != null) {
			String executionId = wfExecutor.getId();
			String currentActivity = wfExecutor.getWorkflow().getCurrent().getBehaviourName();
			sendStatusNotification(wsmaAid, new SerializedStateChanged(executionId, currentActivity, serializedState), true);
		}
	}

	public void notifyStatusChanged(WorkflowExecutor wfExecutor, WorkflowStatus status) {
		AID wsmaAid = wfExecutor.getWSMA();
		if (wsmaAid != null) { 
			String executionId = wfExecutor.getId();
			sendStatusNotification(wsmaAid, new StatusChanged(executionId, status));
		}
	}

	private AID getWSMA() {
		AID wsma = null;
		if (wsmaAids.length > 0) {
			if (wsmaPolicyType == WsmaPolicyType.ROUND_ROBIN) {
				wsma = getRoundRobinWsma();
			} 
			else if (wsmaPolicyType == WsmaPolicyType.CURRENT_LOAD) {
				wsma = getLowerLoadWsma();
			}
			else {
				myLogger.log(Logger.WARNING, "Agent "+workflowEngineAgent.getName()+" - Wsma policy "+wsmaPolicyType+" not supported");
			}
		}
		
		return wsma;
	}
	
	private AID getRoundRobinWsma() {
		AID wsmaAid = wsmaAids[currentAidIndex++ % wsmaAids.length];

		if (currentAidIndex >= wsmaAids.length) {
			currentAidIndex = 0;
		}
		
		return  wsmaAid;
	}

	private AID getLowerLoadWsma() {
		AID smallerLoadWsma = null;
		int smallerLoadValue = WadeAgent.CURRENT_LOAD_UNKNOWN;
		for (AID wsma : wsmaAids) {
			int currentLoad = getCurrentLoad(wsma);
			if (currentLoad != WadeAgent.CURRENT_LOAD_UNKNOWN) {
				if (smallerLoadValue == WadeAgent.CURRENT_LOAD_UNKNOWN ||
					currentLoad < smallerLoadValue) {
					smallerLoadWsma = wsma;
					smallerLoadValue = currentLoad;
				}
			}
		}

		if (smallerLoadWsma == null) {
			smallerLoadWsma = getRoundRobinWsma();
		}
		
		return smallerLoadWsma;
	}

	private int getCurrentLoad(AID wsma) {
		Action action = new Action(wsma, new GetCurrentLoad());
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(wsma);
		msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		msg.setOntology(WadeManagementOntology.getInstance().getName());
		try {
			workflowEngineAgent.getContentManager().fillContent(msg, action);
			ACLMessage reply = FIPAService.doFipaRequestClient(workflowEngineAgent, msg, getCurrentLoadTimeout);
			if (reply != null) {
				Result r = (Result) workflowEngineAgent.getContentManager().extractContent(reply);
				return ((Integer) r.getValue()).intValue();
			} else {
				return WadeAgent.CURRENT_LOAD_UNKNOWN;
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Agent "+workflowEngineAgent.getName()+" - Error getting current load from agent "+wsma.getName(), e);
			return WadeAgent.CURRENT_LOAD_UNKNOWN;
		}
	}
	
	private void sendStatusNotification(AID wsmaAid, Predicate predicate) {
		sendStatusNotification(wsmaAid, predicate, false);
	}
	
	private void sendStatusNotification(AID wsmaAid, Predicate predicate, boolean useBinaryEncoding) {
		String languageName;
		if (useBinaryEncoding) {
			// Use binary encoding (LEAP)
			languageName = LEAPCodec.NAME;
		}
		else {
			// Use normal human-readable encoding (SL)
			languageName = FIPANames.ContentLanguage.FIPA_SL;
		}

		ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
		inform.setLanguage(languageName);
		inform.setOntology(onto.getName());
		inform.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		inform.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
		inform.addReceiver(wsmaAid);

		try {
			workflowEngineAgent.getContentManager().fillContent(inform, predicate);
			
			myLogger.log(Logger.FINE, "Forwarding status notification "+predicate+" to agent "+wsmaAid);
			workflowEngineAgent.send(inform);
		} catch (Exception e) {
			// Should never happen
			myLogger.log(Level.SEVERE, "Error encoding status notification request", e);
		}
	}
}
