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

import jade.content.lang.leap.LEAPCodec;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;
import jade.wrapper.gateway.DynamicJadeGateway;
import jade.wrapper.gateway.GatewayAgent;
import jade.wrapper.gateway.GatewayListener;
import jade.wrapper.gateway.JadeGateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.ca.ModuleDeployer;
import com.tilab.wade.ca.ModuleReverter;
import com.tilab.wade.ca.ModuleUndeployer;
import com.tilab.wade.ca.ontology.ModuleInfo;
import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.interactivity.ontology.InteractivityOntology;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;
import com.tilab.wade.performer.ontology.WorkflowManagementVocabulary;
import com.tilab.wade.utils.GUIDGenerator;
import com.tilab.wade.wsma.ontology.QueryExecutions;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;

/**
 * This class provides a powerful gateway between non-WADE application and a WADE platform.
 * Allows to perform workflows, control the flows and manage the interactions. 
 */
public class EngineProxy implements GatewayListener {

	private static final String DEFAULT_WORKFLOW_ID = "UNKNOWN";
	private static final long MAX_PROXY_STARTUP_TIME = 5 * 1000;
	private static final long CLEAN_SESSION_PERIOD = 5 * 60 * 1000;
	private static final long COMMAND_DEFAULT_TIMEOUT = 0;

	private static Logger myLogger = Logger.getMyLogger(EngineProxy.class.getName());
	private static Map<DynamicJadeGateway, EngineProxy> engineProxies = new HashMap<DynamicJadeGateway, EngineProxy>();

	private DFAgentDescription executorTemplate;
	private Map<String, WorkflowController> controllers = new HashMap<String, WorkflowController>();
	private List<AID> executors;
	private int currentExecutorIndex = 0;
	private boolean proxyActive = false;
	private EngineProxyHandlers engineProxyHandlers;
	DynamicJadeGateway dynamicJadeGateway;
	private boolean customExecutors;

	
	/**
	 * Get a EngineProxy with:
	 * - default JadeGateway 
	 * - default executors template (WORKFLOW_EXECUTOR_ROLE)
	 */
	public static synchronized EngineProxy getEngineProxy() {
		return getEngineProxy(null, null, null);
	}
	
	/**
	 * Get a EngineProxy with:
	 * - specific JadeGateway 
	 * - default executors template (WORKFLOW_EXECUTOR_ROLE)
	 */
	public static synchronized EngineProxy getEngineProxy(DynamicJadeGateway djg) {
		return getEngineProxy(djg, null, null);
	}

	/**
	 * Get a EngineProxy with:
	 * - default JadeGateway
	 * - specific executors template
	 */
	public static synchronized EngineProxy getEngineProxy(DFAgentDescription executorTemplate) {
		return getEngineProxy(null, executorTemplate, null);
	}

	/**
	 * Get a EngineProxy with:
	 * - default JadeGateway
	 * - specific executors list
	 */
	public static synchronized EngineProxy getEngineProxy(List<AID> executors) {
		return getEngineProxy(null, null, executors);
	}
	
	/**
	 * Get a EngineProxy with:
	 * - specific JadeGateway 
	 * - specific executors template
	 */
	public static synchronized EngineProxy getEngineProxy(DynamicJadeGateway djg, DFAgentDescription executorTemplate) {
		return getEngineProxy(djg, executorTemplate, null);
	}

	/**
	 * Get a EngineProxy with:
	 * - specific JadeGateway 
	 * - specific executors list
	 */
	public static synchronized EngineProxy getEngineProxy(DynamicJadeGateway djg, List<AID> executors) {
		return getEngineProxy(djg, null, executors);
	}

	private static synchronized EngineProxy getEngineProxy(DynamicJadeGateway djg, DFAgentDescription executorTemplate, List<AID> executors) {
		// If dynamic-jade-gateway not specified use default gateway 
		if (djg == null) {
			djg = JadeGateway.getDefaultGateway();
		}

		EngineProxy ep = engineProxies.get(djg);
		if (ep == null) {
			ep = new EngineProxy(djg, executorTemplate, executors);
			engineProxies.put(djg, ep);
		}
		return ep;
	}
	
	private EngineProxy(DynamicJadeGateway dynamicJadeGateway, DFAgentDescription executorTemplate, List<AID> executors) {

		// Initialize java type preservation
		if (System.getProperty(SLCodec.PRESERVE_JAVA_TYPES) == null) {
			System.setProperty(SLCodec.PRESERVE_JAVA_TYPES, "true");
		}
		
		dynamicJadeGateway.addListener(this);
		this.dynamicJadeGateway = dynamicJadeGateway;
		
		if (executors == null) {
			// If executor-template not specified use default (all agents with role "Workflow Executor") 
			if (executorTemplate == null) {
				executorTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.addProperties(new Property(WadeAgent.AGENT_ROLE, WadeAgent.WORKFLOW_EXECUTOR_ROLE));
				executorTemplate.addServices(sd);
			}
			this.customExecutors = false;
			this.executorTemplate = executorTemplate;
			this.executors = new ArrayList<AID>();
		} else {
			// Use specific list of executors
			this.customExecutors = true;
			this.executors = executors;
		}
		
		// Create handles
		engineProxyHandlers = new EngineProxyHandlers(this);
		
		myLogger.log(Logger.INFO, "Engine proxy created");
		
		// If gateway is already active -> activate the proxy
		if (dynamicJadeGateway.isGatewayActive()) {
			activate();
		}
	}

	private void activate() {
		try {
			dynamicJadeGateway.execute(new OneShotBehaviour() {
				
				@Override
				public void action() {
					myAgent.getContentManager().registerLanguage(new SLCodec());
					myAgent.getContentManager().registerLanguage(new LEAPCodec());
					myAgent.getContentManager().registerOntology(InteractivityOntology.getInstance());
					myAgent.getContentManager().registerOntology(WorkflowManagementOntology.getInstance());
				}
			});

			// If not custom reset the executors
			if (!customExecutors) {
				executors.clear();
			}
			
			// Start handlers
			engineProxyHandlers.startNotificationHandler();
			engineProxyHandlers.startResultHandler();
			if (executorTemplate != null) {
				engineProxyHandlers.startExecutorHandler();
			}
	
			// Reset controllers map
			controllers.clear();
			getHandlers().cleanResultHanhler();
			
			proxyActive = true;
			myLogger.log(Logger.INFO, "EngineProxy active");
			
		} catch (Exception e) {
			myLogger.log(Logger.SEVERE,"Error activating engine proxy", e);
		}
	}

	public void execute(Object command) throws EngineProxyException {
		execute(command, COMMAND_DEFAULT_TIMEOUT);
	}
	
	public void execute(Object command, long timeout) throws EngineProxyException {
		synchronized (this) {
			if (!proxyActive) {
				// Active gateway
				try {
					synchronized (dynamicJadeGateway) {
						dynamicJadeGateway.checkJADE();
					}
				} catch (Exception e) {
					throw new EngineProxyException("Gateway starting error", e);
				}
				
				// Wait for activation (see handleGatewayConnected() callback method)
				long startTime = System.currentTimeMillis();
				while (!proxyActive && (System.currentTimeMillis()-startTime) < MAX_PROXY_STARTUP_TIME) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// Final activation check 
				if (!proxyActive) {
					throw new EngineProxyException("Gateway command aborted becouse EngineProxy not correctly activated");
				}
			}
		}
		
		try {
			dynamicJadeGateway.execute(command, timeout);
		} catch (Exception e) {
			throw new EngineProxyException("Error executing engine-proxy command", e);
		}
	}
	
	/**
	 * Launch a workflow
	 * @param wd Workflow descriptor
	 * @param resultListener Workflow result listener
	 * @param interactiveMode Enable/disable the interactivity mode
	 * @return Workflow controller
	 * @throws EngineProxyException
	 */
	public WorkflowController launch(WorkflowDescriptor wd, WorkflowResultListener resultListener, boolean interactiveMode) throws EngineProxyException {
		return launch(wd, resultListener, (EventGenerationConfiguration)null, null, interactiveMode);
	}

	/**
	 * Launch a workflow
	 * @param wd Workflow descriptor
	 * @param resultListener Workflow result listener
	 * @param eventListener Workflow event listener
	 * @param context Workflow context
	 * @param interactiveMode Enable/disable the interactivity mode
	 * @return Workflow controller
	 * @throws EngineProxyException
	 */
	public WorkflowController launch(WorkflowDescriptor wd, WorkflowResultListener resultListener, WorkflowEventListener eventListener, WorkflowContext context, boolean interactiveMode) throws EngineProxyException {
		
		// Generate the default event configuration (FLOW_TYPE -> ACTIVITY_LEVEL and TRACING_TYPE -> INFO_LEVEL)
		// and associate it to specified WorkflowEventListener
		EventGenerationConfiguration eventCfg = null;
		if (eventListener != null) {
			eventCfg = EventGenerationConfiguration.getDefault(eventListener);
		}
		
		return launch(wd, resultListener, eventCfg, context, interactiveMode);
	}
	
	/**
	 * Launch a workflow
	 * @param wd Workflow descriptor
	 * @param resultListener Workflow result listener
	 * @param eventCfg Event generation configuration
	 * @param context Workflow context
	 * @param interactiveMode Enable/disable the interactivity mode
	 * @return Workflow controller
	 * @throws EngineProxyException
	 */
	public WorkflowController launch(WorkflowDescriptor wd, WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) throws EngineProxyException {
		
		// Check workflow-id
		if (wd.getId() == null) {
			if (wd.getFormat() != null && wd.getRepresentation() != null) {
				wd.setId(DEFAULT_WORKFLOW_ID);
			} else {
				String errorMsg = "WorkflowDescriptor without workflowId";
				myLogger.log(Logger.SEVERE, errorMsg);
				throw new EngineProxyException(errorMsg);
			}
		}
		
		// Check session-id
		if (wd.getSessionId() == null) {
			wd.setSessionId(GUIDGenerator.getGUID());
		}
		
		// Prapare and execute behaviour
		WorkflowLauncherBehaviour launcherBehaviour = new WorkflowLauncherBehaviour(this, wd, resultListener, eventCfg, context, interactiveMode);
		execute(launcherBehaviour);
		
		// Check if behaviour was aborted (eg. no executor available, error sending request,...)
		if (launcherBehaviour.isAborted()) {
			myLogger.log(Logger.WARNING, launcherBehaviour.getAbortMessage());
			throw new EngineProxyException(launcherBehaviour.getAbortMessage(), launcherBehaviour.getAbortException());
		}
		
		// Get controller
		WorkflowController wc = launcherBehaviour.getWorkflowController();
		myLogger.log(Logger.INFO, "Workflow "+wd.getId()+" launched, executionId="+wc.getExecutionId());

		return wc;
	}

	/**
	 * Recover a running/freezed workflow
	 * @param executionId Workflow execution-id
	 * @param resultListener Workflow result listener
	 * @return controller
	 * @throws EngineProxyException
	 */
	public WorkflowController recover(String executionId, WorkflowResultListener resultListener, boolean interactiveMode) throws EngineProxyException {
		return recover(executionId, resultListener, (EventGenerationConfiguration)null, null, interactiveMode);
	}
	
	/**
	 * Recover a running/freezed workflow
	 * @param executionId Workflow execution-id
	 * @param resultListener Workflow result listener
	 * @param eventListener Workflow event listener
	 * @param context Workflow context
	 * @return controller
	 * @throws EngineProxyException
	 */
	public WorkflowController recover(String executionId, WorkflowResultListener resultListener, WorkflowEventListener eventListener, WorkflowContext context, boolean interactiveMode) throws EngineProxyException {
		
		// Generate the default event configuration (FLOW_TYPE -> ACTIVITY_LEVEL and TRACING_TYPE -> INFO_LEVEL)
		// and associate it to specified WorkflowEventListener
		EventGenerationConfiguration eventCfg = null;
		if (eventListener != null) {
			eventCfg = EventGenerationConfiguration.getDefault(eventListener);
		}

		return recover(executionId, resultListener, eventCfg, null, interactiveMode);
	}
	
	/**
	 * Recover a running/freezed workflow
	 * @param executionId Workflow execution-id
	 * @param resultListener Workflow result listener
	 * @param eventCfg Event generation configuration
	 * @param context Workflow context
	 * @return controller
	 * @throws EngineProxyException
	 */
	public WorkflowController recover(String executionId, WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) throws EngineProxyException {
		WorkflowExecutionInfo wei = getSafeExecution(executionId);
		WorkflowController wc = controllers.get(wei.getSessionId());
		
		if (wc != null && !wc.isTerminated()) {
			// There is already a WorkflowController associated to sessionId
			wc.markAsRecovered();
			wc.update(resultListener, eventCfg, context, interactiveMode);
		}
		else {
			// No WorkflowController associated to sessionId -> send appropriate request to executor 
			if (wei.getStatus() == WorkflowExecutionInfo.WorkflowStatus.FROZEN) {
				myLogger.log(Logger.INFO, "Workflow with executionId="+executionId+" is FREEZED -> thaw it");
				byte[] workflowSerializedState = getSerializedState(executionId);
				WorkflowThawBehaviour thawBehaviour = new WorkflowThawBehaviour(this, wei.getSessionId(), workflowSerializedState, resultListener, eventCfg, context, interactiveMode);
				execute(thawBehaviour);
				wc = thawBehaviour.getWorkflowController();
			}
			
			else if (wei.getStatus() == WorkflowExecutionInfo.WorkflowStatus.ACTIVE ||
					 wei.getStatus() == WorkflowExecutionInfo.WorkflowStatus.ROLLBACK ||
					 wei.getStatus() == WorkflowExecutionInfo.WorkflowStatus.SUSPENDED) {
				myLogger.log(Logger.INFO, "Workflow with executionId="+executionId+" is already ACTIVE or SUSPENDED -> recover it");
				WorkflowRecoverBehaviour recoverBehaviour = new WorkflowRecoverBehaviour(this, wei, resultListener, eventCfg, context, interactiveMode);
				execute(recoverBehaviour);
				wc = recoverBehaviour.getWorkflowController();
			}
			
			else {
				String errorMsg = "Workflow with executionId="+executionId+" present in the platform but with not correct status ("+wei.getStatus()+")";
				myLogger.log(Logger.SEVERE, errorMsg);
				throw new EngineProxyException(errorMsg);
			}
		}
		
		return wc;
	}

	/**
	 * Kill the current workflow
	 * @throws EngineProxyException
	 */
	public void kill(String executionId) throws EngineProxyException {
		kill(executionId, WorkflowManagementVocabulary.SCOPE_TARGET_ONLY, null);
	}
	
	/**
	 * Kill the workflow with a specific scope
	 * @throws EngineProxyException
	 */
	public void kill(String executionId, int scope, String message) throws EngineProxyException {
		WorkflowExecutionInfo wei = getSafeExecution(executionId);

		// Check if operation is permitted
		if (wei.getStatus() == WorkflowStatus.TERMINATED) {
			throw new EngineProxyException("Operation not permitted, workflow current state is "+wei.getStatus());
		}

		if (wei.getStatus() == WorkflowStatus.FROZEN) {
			// Workflow not in executor -> mark it as TERMINATED
			
			SetExecutionStatusBehaviour setExecutionStatusBehaviour = new SetExecutionStatusBehaviour(executionId, WorkflowStatus.TERMINATED, "Killed from the outside");
			execute(setExecutionStatusBehaviour);
			setExecutionStatusBehaviour.checkError();

		} else {
			// Workflow in executor -> kill it
			AID executor = wei.getExecutor();
			
			KillWorkflowBehaviour killWorkflowBehaviour = new KillWorkflowBehaviour(executor, executionId, false, scope, message);
			execute(killWorkflowBehaviour);
			killWorkflowBehaviour.checkError();
			
			// Explicitly mark the interaction as terminated to properly deal with situations where
			// workflow results are requested just after killing the workflow, but before receiving 
			// the FAILURE message from the performer.
			WorkflowController wc = controllers.get(wei.getSessionId());
			if(wc != null) {
				wc.setInteractionTerminated("Workflow aborted");
			}
		}
	}

	/**
	 * Freezes the workflow execution
	 * @throws EngineProxyException
	 */
	public void freeze(String executionId) throws EngineProxyException {
		freeze(executionId, WorkflowManagementVocabulary.SCOPE_TARGET_ONLY);
	}
	
	/**
	 * Freezes the workflow execution
	 * @throws EngineProxyException
	 */
	public void freeze(String executionId, int scope) throws EngineProxyException {
		WorkflowExecutionInfo wei = getSafeExecution(executionId);
		
		// Check if operation is permitted
		if (wei.getStatus() == WorkflowStatus.TERMINATED || wei.getStatus() == WorkflowStatus.FROZEN) {
			throw new EngineProxyException("Operation not permitted, workflow current state is "+wei.getStatus());
		}
		
		String sessionId = wei.getSessionId();
		AID executor = wei.getExecutor();
		
		KillWorkflowBehaviour killWorkflowBehaviour = new KillWorkflowBehaviour(executor, executionId, true, scope, null);
		execute(killWorkflowBehaviour);
		killWorkflowBehaviour.checkError();
	}
	
	/**
	 * Get the list of workflows
	 * @return list of workflows
	 * @throws EngineProxyException
	 */
	public List<WorkflowDetails> getWorkflows() throws EngineProxyException {
		return getWorkflows(null, null);
	}

	/**
	 * Get the list of workflows
	 * @param category category of workflows (can be null)
	 * @param moduleName name of module containing the workflows (can be null)
	 * @return list of workflows
	 * @throws EngineProxyException
	 */
	public List<WorkflowDetails> getWorkflows(String category, String moduleName) throws EngineProxyException {
		return getWorkflows(category, moduleName, false);
	}

	/**
	 * Get the list of workflows
	 * @param category category of workflows (can be null)
	 * @param moduleName name of module containing the workflows (can be null)
	 * @param componentsOnly permit to select only the component-workflow (can be null -> false)
	 * @return list of workflows
	 * @throws EngineProxyException
	 */
	public List<WorkflowDetails> getWorkflows(String category, String moduleName, Boolean componentsOnly) throws EngineProxyException {
		GetWorkflowsBehaviour getWfsBehaviour = new GetWorkflowsBehaviour(category, moduleName, componentsOnly); 
		execute(getWfsBehaviour);
		
		myLogger.log(Logger.INFO, "EngineProxy: Request workflows list");

		return getWfsBehaviour.getWorkflowsDetails();
	}	
	
	
	/**
	 * Get the list of workflow parameters
	 * @param workflowId workflow identifier
	 * @return list of formal parameters
	 * @throws EngineProxyException
	 */
	public List<Parameter> getWorkflowParameters(String workflowId) throws EngineProxyException {
		GetWorkflowParametersBehaviour getWfParametersBehaviour = new GetWorkflowParametersBehaviour(workflowId); 
		execute(getWfParametersBehaviour);
		
		myLogger.log(Logger.INFO, "EngineProxy: Request parameters for workflow "+workflowId);

		// Return workflow parameters 
		return getWfParametersBehaviour.getParameters();
	}

	/**
	 * Get the list of modules
	 * @return list of modules
	 * @throws EngineProxyException
	 */
	public List<ModuleInfo> getModules() throws EngineProxyException {
		GetModulesBehaviour getModulesBehaviour = new GetModulesBehaviour(); 
		execute(getModulesBehaviour);
		
		myLogger.log(Logger.INFO, "EngineProxy: Request modules list");

		return getModulesBehaviour.getModulesInfo();
	}

	/**
	 * Deploy a WADE module
	 * @param jarFileName jar file path-name
	 * @param applyPendingDeployments apply all pending deployments 
	 * @throws EngineProxyException
	 */
	public void deployModule(String jarFileName, boolean applyPendingDeployments) throws EngineProxyException {
		myLogger.log(Logger.INFO, "EngineProxy: Request deploy jar "+jarFileName+" with applyPendingDeployments="+applyPendingDeployments);
		
		try {
			ModuleDeployer moduleDeployer = new ModuleDeployer(jarFileName, applyPendingDeployments); 
			execute(moduleDeployer);
	
			if (!moduleDeployer.isSuccessfullyDeployed()) {
				throw new EngineProxyException("Error deploying jar "+ jarFileName+". "+moduleDeployer.getFailureReason());
			}
		} catch(IOException ioe) {
			throw new EngineProxyException("Error reading jar "+ jarFileName, ioe);			
		}
	}

	/**
	 * Deploy a WADE module
	 * @param moduleContent byte-array of jar file
	 * @param moduleName name of module
	 * @param applyPendingDeployments apply all pending deployments 
	 * @throws EngineProxyException
	 */
	public void deployModule(byte[] moduleContent, String moduleName, boolean applyPendingDeployments) throws EngineProxyException {
		myLogger.log(Logger.INFO, "EngineProxy: Request deploy module "+moduleName+" with applyPendingDeployments="+applyPendingDeployments);
		
		ModuleDeployer moduleDeployer = new ModuleDeployer(moduleContent, moduleName, applyPendingDeployments); 
		execute(moduleDeployer);

		if (!moduleDeployer.isSuccessfullyDeployed()) {
			throw new EngineProxyException("Error deploying module "+ moduleName+". "+moduleDeployer.getFailureReason());
		}
	}

	/**
	 * Undeploy a WADE module
	 * @param moduleName nome of module to undeploy
	 * @param applyPendingDeployments apply all pending deployments 
	 * @throws EngineProxyException
	 */
	public void undeployModule(String moduleName, boolean applyPendingDeployments) throws EngineProxyException {
		myLogger.log(Logger.INFO, "EngineProxy: Request undeploy module "+moduleName+" with applyPendingDeployments="+applyPendingDeployments);

		ModuleUndeployer moduleUndeployer = new ModuleUndeployer(moduleName, applyPendingDeployments); 
		execute(moduleUndeployer);

		if (!moduleUndeployer.isSuccessfullyUndeployed()) {
			throw new EngineProxyException("Error undeploying module "+ moduleName+". "+moduleUndeployer.getFailureReason());
		}
	}

	/**
	 * Revert a WADE module
	 * @param moduleName nome of module to revert
	 * @param applyPendingDeployments apply all pending deployments 
	 * @throws EngineProxyException
	 */
	public void revertModule(String moduleName, boolean applyPendingDeployments) throws EngineProxyException {
		myLogger.log(Logger.INFO, "EngineProxy: Request revert module "+moduleName+" with applyPendingDeployments="+applyPendingDeployments);

		ModuleReverter moduleReverter = new ModuleReverter(moduleName, applyPendingDeployments); 
		execute(moduleReverter);

		if (!moduleReverter.isSuccessfullyUndeployed()) {
			throw new EngineProxyException("Error reverting module "+ moduleName+". "+moduleReverter.getFailureReason());
		}
	}
	
	/**
	 * Get the WorkflowExecutionInfo of a specific executionId
	 * @param executionId Workflow execution-id
	 * @return WorkflowExecutionInfo
	 * @throws EngineProxyException
	 */
	public WorkflowExecutionInfo getExecution(String executionId) throws EngineProxyException {
		GetExecutionBehaviour getExecutionBehaviour = new GetExecutionBehaviour(executionId);
		execute(getExecutionBehaviour);

		myLogger.log(Logger.INFO, "EngineProxy: Request workflow execution info for executionId "+executionId);
		
		return getExecutionBehaviour.getExecution();
	}
	
	private WorkflowExecutionInfo getSafeExecution(String executionId) throws EngineProxyException {
		WorkflowExecutionInfo wei = getExecution(executionId);
		if (wei == null) {
			String errorMsg = "Workflow with executionId="+executionId+" not present in the platform";
			myLogger.log(Logger.WARNING, errorMsg);
			throw new EngineProxyException(errorMsg);
		}
		return wei;
	}

	/**
	 * Get WorkflowExecutionInfo list of specific requester 
	 * @param requester Workflow requester
	 * @return WorkflowExecutionInfo
	 */
	public List<WorkflowExecutionInfo> getPendingExecutions(String requester) throws EngineProxyException {
		GetPendingExecutionsBehaviour getPendingExecutionsBehaviour = new GetPendingExecutionsBehaviour(requester);
		execute(getPendingExecutionsBehaviour);

		myLogger.log(Logger.INFO, "EngineProxy: Request pending executions for requester "+requester);
		
		return getPendingExecutionsBehaviour.getExecutions();
	}
	
	/**
	 * Get supported query dialect usable in getSessionExecutions methods
	 * @return query dialect
	 */
	public String getQueryDialect() throws EngineProxyException {
		GetQueryDialectBehaviour getQueryDialect = new GetQueryDialectBehaviour();
		execute(getQueryDialect);

		myLogger.log(Logger.INFO, "EngineProxy: Request query dialect");
		
		return getQueryDialect.getQueryDialect();
	}
	
	/**
	 * Get WorkflowExecutionInfo list of specific sessionId
	 * @param sessionId Workflow session-id
	 * @return WorkflowExecutionInfo
	 */
	public List<WorkflowExecutionInfo> getSessionExecutions(String sessionId) throws EngineProxyException {
		GetSessionExecutionsBehaviour getSessionExecutionsBehaviour = new GetSessionExecutionsBehaviour(sessionId);
		execute(getSessionExecutionsBehaviour);

		myLogger.log(Logger.INFO, "EngineProxy: Request session executions for sessionId "+sessionId);
		
		return getSessionExecutionsBehaviour.getExecutions();
	}
	
	/**
	 * Return the list associated to specific query
	 * The list elements can be:
	 * - Object with field value if only one field is required in WHAT 
	 * - WorkflowExecutionInfo object partially filled if more than one fields (but not all) are required in WHAT
	 * - WorkflowExecutionInfo object if all fields are required (WHAT=null or *)
	 * @param what list of fields required (eg. sessionId,status)
	 * @param condition where condition (eg. status='TERMINATED' and ...)
	 * @param order order by (eg. startTime desc)
	 * @return result list
	 * @throws EngineProxyException
	 */
	public List queryExecutions(String what, String condition, String order) throws EngineProxyException {
		return queryExecutions(what, condition, order, 0, QueryExecutions.ALL_RESULTS);
	}

	/**
	 * Return the list associated to specific query
	 * The list elements can be:
	 * - Object with field value if only one field is required in WHAT 
	 * - WorkflowExecutionInfo object partially filled if more than one fields (but not all) are required in WHAT
	 * - WorkflowExecutionInfo object if all fields are required (WHAT=null or *)
	 * @param what list of fields required (eg. sessionId,status)
	 * @param condition where condition (eg. status='TERMINATED' and ...)
	 * @param order order by (eg. startTime desc)
	 * @param firstResult first result of query (0: start from begin)
	 * @param maxResult number max of result (Query.ALL_RESULTS: return all elements) 
	 * @return result list
	 * @throws EngineProxyException
	 */
	public List queryExecutions(String what, String condition, String order, int firstResult, int maxResult) throws EngineProxyException {
		QueryExecutionsBehaviour queryExecutionsBehaviour = new QueryExecutionsBehaviour(what, condition, order, firstResult, maxResult);
		execute(queryExecutionsBehaviour);

		myLogger.log(Logger.INFO, "EngineProxy: Request query executions for: what="+what+", condition="+condition+", order="+order);
		
		return queryExecutionsBehaviour.getResults();
	}
	
	/**
	 * Remove the specific entry from storage
	 * @param executionId execution to remove
	 * @throws EngineProxyException
	 */
	public void remove(String executionId) throws EngineProxyException {
		RemoveExecutionBehaviour removeExecutionBehaviour = new RemoveExecutionBehaviour(executionId);
		execute(removeExecutionBehaviour);

		myLogger.log(Logger.INFO, "EngineProxy: Request remove execution for executionId "+executionId);
	}
	
	
	/**
	 * Get the specific WorkflowController.
	 * Return only active controller present in EngineProxy  
	 */
	public WorkflowController getControllerByExecution(String executionId) {
		synchronized (controllers) {
			Iterator<WorkflowController> it = controllers.values().iterator();
			while (it.hasNext()) {
				WorkflowController wc = it.next();
				if (wc.getExecutionId().equals(executionId)) {
					return wc;
				}
			}
		}
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////
	// Internal methods
	//
	void addExecutor(AID agent) {
		if (!executors.contains(agent)) {
			executors.add(agent);
			myLogger.log(Logger.CONFIG, "EngineProxy: Added executor "+agent);
		}
	}

	void removeExecutor(AID agent) {
		executors.remove(agent);
		myLogger.log(Logger.CONFIG, "EngineProxy: Removed executor "+agent);
	}
	
	AID nextExecutor(Agent gatewayAgent) {
		AID executorAgent = null;
		
		// Check if executors list is empty -> search directly on DF
		if (executors.size() == 0) {
			
			// Search executor agents with specific template 
			if (executorTemplate != null) {
				try {
					DFAgentDescription[] dfds = DFService.search(gatewayAgent, gatewayAgent.getDefaultDF(), executorTemplate);
					for (int i = 0; i < dfds.length; ++i) {
						AID agent = dfds[i].getName();
						addExecutor(agent);
					}
				} catch (FIPAException e1) {
					myLogger.log(Logger.SEVERE,"EngineProxy: Error searching executor agents");
				}
			}
		}
		
		// Get executor from executors list
		int executorsSize = executors.size();
		if (executorsSize != 0){
			executorAgent = executors.get(currentExecutorIndex++ % executorsSize);

			if (currentExecutorIndex >= executorsSize) {
				currentExecutorIndex = 0;
			}
		}
		
		return  executorAgent;
	}

	DFAgentDescription getExecutorTemplate() {
		return executorTemplate;
	}

	List<AID> getExecutors() {
		return executors;
	}
	
	WorkflowController getController(String sessionId) {
		return controllers.get(sessionId);
	}

	void addController(String sessionId, WorkflowController controller) {
		controllers.put(sessionId, controller);
	}
	
	EngineProxyHandlers getHandlers() {
		return engineProxyHandlers;
	}
	
	void cleanSession(WorkflowManagementBehaviour wmb, String message) {
		// Remove the controller from engine-proxy and WMB from result-handler.
		// Per il WMB l'operazione è delegata ad un WakerBehaviour con ritardo di 5 minuti 
		// in quanto gli eventi potrebbero essere elaborati con ritardo rispetto alla terminazione 
		// del wf (INFORM o FAILURE del WMB). 
		
		// Remove WMB from result handler
		getHandlers().removeFromResultHandler(wmb.getConversationId());	
		
		String sessionId = wmb.getSessionId();
		WorkflowController controller = controllers.get(sessionId);
		if (controller != null) {
			// Mark controller as terminated
			controller.setTerminated();
		
			// Prepare request to remove controller
			try {
				execute(new CleanSessionBehaviour(sessionId, CLEAN_SESSION_PERIOD));
			} catch (EngineProxyException e) {
				myLogger.log(Logger.SEVERE,"EngineProxy: Error cleaning session "+sessionId, e);
			}
		} else {
			// Abort the WMB
			// Il controller non è stato ancora creato in quanto il wf non è stato lanciato
			// (es. il wf non esiste)
			wmb.abort("Error launching workflow: "+message);
		}
	}

	private byte[] getSerializedState(String executionId) throws EngineProxyException {
		GetSerializedStateBehaviour getSerializedStateBehaviour = new GetSerializedStateBehaviour(executionId);
		execute(getSerializedStateBehaviour);

		byte[] serializedState = getSerializedStateBehaviour.getSerializedState();
		if (serializedState == null) {
			String errorMsg = "Workflow serialized state with executionId="+executionId+" not present in the storage";
			myLogger.log(Logger.WARNING, errorMsg);
			throw new EngineProxyException(errorMsg);
		}

		return serializedState;
	}

	
	/**
	 * Inner class CleanSessionBehaviour
	 */
	private class CleanSessionBehaviour extends WakerBehaviour {

		private String sessionId;
		
		public CleanSessionBehaviour(String sessionId, long period) {
			super(null, period);
			
			this.sessionId = sessionId;
		}

		@Override
		public void onStart() {
			super.onStart();
			
			((GatewayAgent)myAgent).releaseCommand(this);
		}
		
		@Override
		protected void onWake() {
			WorkflowController controller = controllers.get(sessionId);
			// Se il controller è ancora presente ed è marcato come terminato -> elimina
			// Potrebbe essere NON terminato a causa di un recover precedente alla pulizia della sessione
			synchronized (controllers) {
				if (controller != null && controller.isTerminated()) {
					controllers.remove(sessionId);	
				}
			}
		}
	}

	
	
	///////////////////////////////////////////////////////////////////////
	// GatewayListener handle methods
	//
	public void handleGatewayConnected() {
		activate();
	}

	public void handleGatewayDisconnected() {
		myLogger.log(Logger.INFO, "EngineProxy not active");
		
		proxyActive = false;
	}
}
