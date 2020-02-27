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

import jade.content.AgentAction;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPANames;
import jade.domain.introspection.IntrospectionServer;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ConversationList;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.LinkedList;
import jade.util.leap.List;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.util.Date;
import java.util.Hashtable;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.ca.WadeClassLoader;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.AttributeSetter;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.event.EventOntology;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.AbortedTransaction;
import com.tilab.wade.performer.event.BeginActivity;
import com.tilab.wade.performer.event.BeginApplication;
import com.tilab.wade.performer.event.BeginWorkflow;
import com.tilab.wade.performer.event.CommittedTransaction;
import com.tilab.wade.performer.event.DelegatedSubflow;
import com.tilab.wade.performer.event.EndActivity;
import com.tilab.wade.performer.event.EndApplication;
import com.tilab.wade.performer.event.EndSubflow;
import com.tilab.wade.performer.event.EndWorkflow;
import com.tilab.wade.performer.event.EventEmitter;
import com.tilab.wade.performer.event.ExecutionErrorEvent;
import com.tilab.wade.performer.event.OpenedTransaction;
import com.tilab.wade.performer.event.SuccessfulTerminationEvent;
import com.tilab.wade.performer.event.UnsuccessfulTerminationEvent;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.ontology.ControlInfo;
import com.tilab.wade.performer.ontology.ExecuteWorkflow;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.performer.ontology.ExecutorInfo;
import com.tilab.wade.performer.ontology.Frozen;
import com.tilab.wade.performer.ontology.GetPoolSize;
import com.tilab.wade.performer.ontology.GetSessionStatus;
import com.tilab.wade.performer.ontology.GetWRD;
import com.tilab.wade.performer.ontology.KillWorkflow;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.ontology.RecoverWorkflow;
import com.tilab.wade.performer.ontology.ResetControlInfos;
import com.tilab.wade.performer.ontology.ResetModifiers;
import com.tilab.wade.performer.ontology.SetControlInfo;
import com.tilab.wade.performer.ontology.SetPoolSize;
import com.tilab.wade.performer.ontology.SetWRD;
import com.tilab.wade.performer.ontology.ThawWorkflow;
import com.tilab.wade.performer.ontology.UpdateControlInfo;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;
import com.tilab.wade.performer.ontology.WorkflowManagementVocabulary;
import com.tilab.wade.performer.transaction.TransactionEntry;
import com.tilab.wade.performer.transaction.TransactionManager;
import com.tilab.wade.utils.GUIDGenerator;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;

import com.tilab.wade.performer.interactivity.ontology.InteractivityOntology;

/**
   Base class for agents able to execute tasks defined according to the workflow metaphor.
   @author Giovanni Caire - TILAB
 */
public class WorkflowEngineAgent extends WadeAgentImpl {
	private static final boolean TRACE_ACTIVITIES = false;

	// WorkflowExecutor status values 
	public static final int IDLE_STATUS = 0;        // WorkflowExecutor created but not yet started (onStart() method not yet executed)
	public static final int EXECUTING_STATUS = 1;   // WorkflowExecutor executing the actual workflow
	public static final int TERMINATING_STATUS = 2; // WorkflowExecutor after the execution of the actual workflow and (when relevant) the reception of the ACCEPT/REJECT message
	public static final int WAITING_STATUS = 3;     // WorkflowExecutor waiting for the reception of the ACCEPT/REJECT message
	public static final int DONE_STATUS = 4;        // WorkflowExecutor no more known by the agent (after the terminate() method)
	public static final int SUSPENDED_STATUS = 5;   // WorkflowExecutor suspended (e.g. due to a SubflowJoin activity)

	// Attribute IDs
	public static final String POOL_SIZE_ATTRIBUTE = "PoolSize";
	public static final String BUSY_EXECUTORS_ATTRIBUTE = "BusyExecutors";
	public static final String THREAD_CNT_ATTRIBUTE = "ThreadCnt";
	public static final String WORKFLOW_CNT_ATTRIBUTE = "WorkflowCnt";
	public static final String ENQUEUED_CNT_ATTRIBUTE = "EnqueuedCnt";
	public static final String ACTIVE_CNT_ATTRIBUTE = "ActiveCnt";
	public static final String DEFAULT_WORKFLOW_TIMEOUT_ATTRIBUTE = "DefaultWorkflowTimeout";
	public static final String REQUIRE_WSMA_ATTRIBUTE = "RequireWSMA";
	public static final String WSMA_SEARCH_TIMEOUT_ATTRIBUTE = "WSMASearchTimeout";

	// WorkflowExecutor FSM states
	static final String EXECUTE = "execute";
	private static final String HANDLE_SUCCESS = "handle-success";
	private static final String HANDLE_FAILURE = "handle-failure";
	private static final String HANDLE_FROZEN = "handle-frozen";
	private static final String WAIT_COMMIT = "wait-commit";
	private static final String PREPARE_ROLLBACK = "prepare-rollback";
	private static final String COMMIT = "commit";
	private static final String ROLLBACK = "rollback";
	private static final String HANDLE_COMMIT_SUCCESS = "handle-commit-success";
	private static final String HANDLE_ROLLBACK_SUCCESS = "handle-rollback-success";
	private static final String HANDLE_TRANSACTION_FAILURE = "handle-transaction-failure";
	private static final String SEND_REPLY = "send-reply";

	// WorkflowExecutor FSM states exit values
	private static final int TRANSACTION = -11;
	
	// Other configurations
	public static final String DELAY_BEFORE_KILL_ON_SHUTDOWN = "delayBeforeKillOnShutdown";

	// WorkflowExecutor abort conditions
	static final int NONE = 0;
	static final int KILLED = 1;
	static final int TIMED_OUT = 2;

	private static ThreadLocal<Hashtable> executionContexts = new ThreadLocal<Hashtable>();
	/**
	 * Retrieve the execution context (in form of an Hashtable) of the WorkflowExecutor being carried out
	 * by the current Thread. 
	 * @return The execution context of the WorkflowExecutor being carried out by the current Thread.
	 */
	public static Hashtable getExecutionContext() {
		return executionContexts.get();
	}

	// The ontology known by this agent to manage workflows
	protected transient Ontology onto = getOntology();

	// Free running counter of workflow executions (used to build execution-id)
	private long wfCnt = 0; 
	// The table holding all WorkflowExecutors (running or enqueued).
	protected ExecutorsTable executors = new ExecutorsTable();
	// The Factory to create ThreadedBehaviours executing workflows
	protected transient ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	// The maximum amount of workflows/sessions to be executed in parallel
	private int poolSize;
	private static final int DEFAULT_POOL_SIZE = 3;
	private long defaultWorkflowTimeout;
	private static final long DEFAULT_WORKFLOW_TIMEOUT = 1800000;
	public static final long DEFAULT_WSMA_SEARCH_TIMEOUT = 10000;

	// Name of the type property defining where to place TAG files
	public static final String TAG_DIR = "tagDir";
	
	// The list of workflows waiting to be executed
	private List workflowQueue = new LinkedList();
	// Lock used to synchronize the executions of code blocks related to workflow enqueuing/dequeuing
	private transient Object enqueuingLock = new Object();	
	// The list of conversations corresponding to workflows requested to/by this agent. This is used to identify spurious messages
	// that can be received by this agent after e.g. a crash-&-restart
	private ConversationList conversations;
	// The list of conversations corresponding to ongoing interactivity sessions
	private ConversationList interactivityConversations;

	private transient StatusManager statusManager;


	protected void agentSpecificSetup() throws AgentInitializationException {
		// Register ontologies
		getContentManager().registerOntology(onto);
		getContentManager().registerOntology(EventOntology.getInstance());
		getContentManager().registerOntology(InteractivityOntology.getInstance());

		// The behaviour serving actions of the Workflow-Management ontology
		addBehaviour(new CyclicBehaviour() {
			private MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchOntology(onto.getName()),
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
							MessageTemplate.MatchPerformative(ACLMessage.CFP)
					)
			);

			public void action() {
				ACLMessage msg = myAgent.receive(template);
				if (msg != null) {
					if (myLogger.isLoggable(Logger.FINEST)) {
						myLogger.log(Logger.FINEST, "Agent "+getName()+" - Incoming request received: "+msg);
					}
					serveRequest(msg);
				}
				else {
					block();
				}
			}
		} );

		// The behaviour handling spurious Workflow-Management messages that can be received in case this agent 
		// crashed and restarted while a workflow was running. We intercept only PROPOSE, ACCEPT_PROPOSAL
		// and REJECT_PROPOSAL messages that need explicit replies to be sent back. Other possible spurious 
		// messages such as INFORM or FAILURE are ignored to avoid stealing messages like responses to 
		// subflow kill requests. 
		conversations = new ConversationList(this);
		addBehaviour(new CyclicBehaviour() {
			private MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchOntology(onto.getName()),
					MessageTemplate.and(
							MessageTemplate.or(
									MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
									MessageTemplate.or(
											MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
											MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
									)
							),
							conversations.getMessageTemplate()
					)
			);

			public void action() {
				ACLMessage msg = myAgent.receive(template);
				if (msg != null) {
					if (msg.getPerformative() == ACLMessage.PROPOSE){
						// We likely died and restarted while another agent was executing a delegated workflow --> Rollback it
						ACLMessage reply = msg.createReply();
						myLogger.log(Logger.WARNING, "Agent "+getName()+" - Spurious PROPOSE message received from "+msg.getSender().getName()+". Reject it");
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setContent(getName() + ": refuse unknown PROPOSE message");
						send(reply);
					} 
					else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL || msg.getPerformative() == ACLMessage.REJECT_PROPOSAL){
						// We likely died and restarted while waiting for a commit/rollback indication: We can neither commit nor rollback --> reply with FAILURE
						ACLMessage reply = msg.createReply();
						myLogger.log(Logger.WARNING, "Agent "+getName()+" - Spurious "+ACLMessage.getPerformative(msg.getPerformative())+" message received from "+msg.getSender().getName() + ". Reply with FAILURE");
						reply.setPerformative(ACLMessage.FAILURE);
						reply.setContent(getName() + ": refuse unknown ACCEPT-PROPOSAL or REJECT_PROPOSAL message");
						send(reply);
					}
				}
				else {
					block();
				}
			}
		} );
		
		// The behaviour handling spurious Interactivity messages 
		interactivityConversations = new ConversationList(this);
		addBehaviour(new CyclicBehaviour() {
			// Match all InteractivityOntology messages that do not belong to any
			// known interactivity conversations
			private MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchOntology(InteractivityOntology.getInstance().getName()),
					interactivityConversations.getMessageTemplate()
			);

			public void action() {
				ACLMessage msg = myAgent.receive(template);
				if (msg != null) {
					if (msg.getPerformative() == ACLMessage.REQUEST){
						// We likely died and restarted while another agent was executing a delegated workflow --> Rollback it
						myLogger.log(Logger.WARNING, "Agent "+getName()+" - Spurious interactivity REQUEST message received from "+msg.getSender().getName()+". Conversation-id="+msg.getConversationId()+". Send back a FAILURE");
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.FAILURE);
						reply.setContent("Unknown session "+msg.getConversationId()+". Workflow has probably expired");
						send(reply);
					} 
				}
				else {
					block();
				}
			}
		} );

		// Add the behaviour serving introspection actions
		addBehaviour(new IntrospectionServer(this));

		// Get pool-size, try before in configuration arguments and than in types file   
		poolSize = getIntArgument(POOL_SIZE_ATTRIBUTE, -1);
		if (poolSize == -1) {
			poolSize = getIntTypeProperty(POOL_SIZE_ATTRIBUTE, DEFAULT_POOL_SIZE);
		}
		
		// Get default-workflow-timeout, try before in configuration arguments and than in types file   
		defaultWorkflowTimeout = getLongArgument(DEFAULT_WORKFLOW_TIMEOUT_ATTRIBUTE, -1);
		if (defaultWorkflowTimeout == -1) {
			defaultWorkflowTimeout = getLongTypeProperty(DEFAULT_WORKFLOW_TIMEOUT_ATTRIBUTE, DEFAULT_WORKFLOW_TIMEOUT);
		}
		
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Ready ...");
	}

	protected void takeDown() {
		// Before terminating abort all running workflows  
		tbf.interrupt();
		if (myLogger != null) {
			// If the setup() method is redefined myLogger may be null
			myLogger.log(Logger.INFO, "Agent "+getName()+" - Terminated");
		}
	}

	protected void beforeMove() {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Wait for currently active executors to complete before moving...");
		// This is called by the Agent thread --> Even if the Threads terminate the 
		// Wrapper behaviours do not --> they remain in the behaviour pool and cause
		// serialization problems --> Explicitly remove them!
		Behaviour[] wrappers = tbf.getWrappers();
		boolean ok = tbf.waitUntilEmpty(60000);
		if (ok) {
			myLogger.log(Logger.INFO, "Agent "+getName()+" - All executors completed");
		}
		else {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - "+tbf.size()+" executors still active. Interrupt them.");
			// FIXME: Here we should kill all RUNNING WorkflowExecutors
			tbf.interrupt();
			tbf.waitUntilEmpty(5000);
		}
		for (int i = 0; i < wrappers.length; ++i) {
			removeBehaviour(wrappers[i]);
		}
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Move now");
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		
		// Restore transient fields 
		tbf = new ThreadedBehaviourFactory();
		enqueuingLock = new Object();
		onto = getOntology();
		
		// Re-initialize the ContentManager
		getContentManager().registerOntology(onto);
		getContentManager().registerOntology(EventOntology.getInstance());
	}	


	//////////////////////////////////////////////
	// WadeAgent methods
	//////////////////////////////////////////////
	@Override
	public int getCurrentLoad() {
		return executors.size();
	}

	@Override
	public boolean isWorking() {
		return executors.size() > 0;
	}

	private boolean shortRunningWFKilled = false;
	
	@Override
	public boolean prepareForShutdown() {
		boolean killShortRunningWF = false;
		long delayBeforeKillOnShutdown = getLongConfig(DELAY_BEFORE_KILL_ON_SHUTDOWN, 60000);
		if (delayBeforeKillOnShutdown > 0) {
			if (System.currentTimeMillis() - getShutdownInitiationTime().getTime() > delayBeforeKillOnShutdown) {
				killShortRunningWF = true;
			}
		}
		
		// For all executors:
		// - short-running: kill (only first time)
		// - long-running suspended: remove from executors
		// - long-running active: suspendAsap 
		Iterator it = executors.getStatus().iterator();
		while(it.hasNext()) {
			ExecutorInfo ei = (ExecutorInfo)it.next();
			WorkflowExecutor we = executors.get(ei.getId());
			boolean longRunning = we.getWorkflow().isLongRunning();
			if (longRunning) {
				if (we.getStatus() == SUSPENDED_STATUS) {
					executors.remove(ei.getId());
				} else {
					we.getWorkflow().suspendAsap(true);
				}
			} else {
				if (killShortRunningWF && !shortRunningWFKilled) {
					we.kill(false, true);
				}
			}
		}
		
		if (killShortRunningWF) {
			shortRunningWFKilled = true;
		}
		
		return super.prepareForShutdown();
	}

	
	//////////////////////////////////////////////
	// WorkflowManagementOntology serving methods
	//////////////////////////////////////////////
	/**
	   WorkflowManagement requests serving entry point 
	 */
	private final void serveRequest(ACLMessage msg) {
		try {
			Action aExpr = (Action) getContentManager().extractContent(msg);
			AgentAction act = (AgentAction) aExpr.getAction();

			if (act instanceof ExecuteWorkflow) {
				serveExecuteWorkflow(msg, aExpr, (ExecuteWorkflow) act);
			}
			else if (act instanceof ThawWorkflow) {
				serveThawWorkflow(msg, aExpr, (ThawWorkflow) act);
			}
			else if (act instanceof RecoverWorkflow) {
				serveRecoverWorkflow(msg, aExpr, (RecoverWorkflow) act);
			}
			else if (act instanceof KillWorkflow) {
				serveKillWorkflow(msg, aExpr, (KillWorkflow) act);
			}
			else if (act instanceof SetControlInfo) {
				serveSetControlInfo(msg, aExpr, (SetControlInfo) act);
			}
			else if (act instanceof UpdateControlInfo) {
				serveUpdateControlInfo(msg, aExpr, (UpdateControlInfo) act);
			}
			else if (act instanceof GetWRD) {
				serveGetWRD(msg, aExpr, (GetWRD) act);
			}
			else if (act instanceof SetWRD) {
				serveSetWRD(msg, aExpr, (SetWRD) act);
			}
			else if (act instanceof GetPoolSize) {
				serveGetPoolSize(msg, aExpr, (GetPoolSize) act);
			}
			else if (act instanceof SetPoolSize) {
				serveSetPoolSize(msg, aExpr, (SetPoolSize) act);
			}
			else if (act instanceof GetSessionStatus) {
				serveGetSessionStatus(msg, aExpr, (GetSessionStatus) act);
			}
			else if (act instanceof ResetControlInfos) {
				serveResetControlInfos(msg, aExpr, (ResetControlInfos) act);
			}
			else if (act instanceof ResetModifiers) {
				serveResetModifiers(msg, aExpr, (ResetModifiers) act);
			}
			else {
				// Action unknown
				handleUnknownAction(act, aExpr, msg);
			}
		}
		catch (OntologyException oe) {
			handleError(oe, msg, ACLMessage.NOT_UNDERSTOOD, "Malformed request. ");
		}
		catch (Codec.CodecException ce) {
			handleError(ce, msg, ACLMessage.NOT_UNDERSTOOD, "Malformed request. ");
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Unexpected error. ");
		}
	}

	/**
	 * Serve an incoming ExecuteWorkflow request
	 */
	protected void serveExecuteWorkflow(ACLMessage msg, Action aExpr, ExecuteWorkflow ew) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving ExecuteWorkflow request... ");
		}
		updateSessionStartup(ew.getWhat());
		try {
			if (isShuttingDown()) {
				throw new IllegalStateException("Shut-down in progress");
			}
			
			checkVerifyModifier(ew, aExpr, msg);
			synchronized (enqueuingLock) {
				WorkflowExecutor we = new WorkflowExecutor(this, ew, msg, aExpr);
				
				if (mustEnqueue(we) && !we.getWorkflow().supportEnqueuing()) {
					throw new UnsupportedEnqueuingException();
				}

				// For not volatile wf send the STARTED notification to WSMA
				if (!we.getWorkflow().isVolatile()) { 
					getStatusManager().notifyStarted(we);
				}
				
				// Send the AGREE message before activating the WF to be sure the AGREE is sent before any workflow event
				reply(msg, ACLMessage.AGREE, we.getId());

				handleIncomingWorkflow(we, msg.getSender());
				executors.insert(we);
			}
			if (myLogger.isLoggable(Logger.FINER)) {
				myLogger.log(Logger.FINER, "Agent "+getName()+" - ExecuteWorkflow request served.");
			}
		}
		catch (UnsupportedEnqueuingException uee) {
			handleError(uee, msg, ACLMessage.FAILURE, "Agent busy and workflow does not support enqueuing "+ew.getWhat().getId()+". ");
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error loading Workflow "+ew.getWhat().getId()+". ");
		}								
	}

	/**
	 * Serve an incoming ThawWorkflow request
	 */
	protected void serveThawWorkflow(ACLMessage msg, Action aExpr, ThawWorkflow dw) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving ThawWorkflow request... ");
		}
		try {
			synchronized (enqueuingLock) {
				WorkflowExecutor we = WorkflowSerializationManager.deserializeWorkflow(dw.getWorkflowSerializedState(), this);
				we.reply = msg.createReply();

				we.getWorkflow().setThawed(true);

				// Update controlInfos, modifiers and execution
				we.getEventEmitter().setControlInfo(dw.getControlInfos());
				we.setModifiers(dw.getModifiers());
				if (dw.getExecution() != null) {
					we.getDescriptor().setExecution(dw.getExecution());
				}

				if (mustEnqueue(we) && !we.getWorkflow().supportEnqueuing()) {
					throw new UnsupportedEnqueuingException();
				}

				// For not volatile wf send the CHANGE-STATE notification to WSMA
				if (!we.getWorkflow().isVolatile()) { 
					getStatusManager().notifyThawed(we);
				}

				// Send the AGREE message before activating the WF to be sure the AGREE is sent before any workflow event
				reply(msg, ACLMessage.AGREE, we.getId());
				
				handleIncomingWorkflow(we, msg.getSender());
				executors.insert(we);
			}
			if (myLogger.isLoggable(Logger.FINER)) {
				myLogger.log(Logger.FINER, "Agent "+getName()+" - ThawWorkflow request served.");
			}
		}
		catch (UnsupportedEnqueuingException uee) {
			handleError(uee, msg, ACLMessage.FAILURE, "Agent busy and workflow does not support enqueuing "+dw.getExecution()+". ");
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error thawing workflow");
		}								
	}

	/**
	 * Serve an incoming RecoverWorkflow request
	 */
	protected void serveRecoverWorkflow(ACLMessage msg, Action aExpr, RecoverWorkflow rw) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving RecoverWorkflow request... ");
		}
		try {
			if (isShuttingDown()) {
				throw new IllegalStateException("Shut-down in progress");
			}
			
			WorkflowExecutor we = executors.get(rw.getExecutionId());
			if (we == null) {
				throw new WorkflowException("Workflow "+rw.getExecutionId()+" not present. ");
			}

			we.reply = msg.createReply();

			// Update controlInfos, modifiers and execution
			List newControlInfos = rw.getControlInfos();
			if (newControlInfos != null) {
				we.getEventEmitter().setControlInfo(newControlInfos);
			}
			List newModifiers = rw.getModifiers();
			if (newModifiers != null) {
				we.setModifiers(newModifiers);
			}
			if (rw.getExecution() != null) {
				we.getDescriptor().setExecution(rw.getExecution());
			}
			
			if (we.getStatus() == SUSPENDED_STATUS) {
				we.resume();
			} 

			reply(msg, ACLMessage.AGREE, we.getId());

			if (myLogger.isLoggable(Logger.FINER)) {
				myLogger.log(Logger.FINER, "Agent "+getName()+" - RecoverWorkflow request served.");
			}
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error recovering Workflow "+rw.getExecutionId()+". ");
		}								
	}

	private void updateSessionStartup(WorkflowDescriptor wd) {
		if (wd.getSessionId() != null && wd.getSessionStartup() == null) {
			wd.setSessionStartup(new Date());
		}
	}

	private boolean checkVerifyModifier(ExecuteWorkflow ew, Action aExpr, ACLMessage request) {
		List modifiers = ew.getModifiers();
		Modifier verifyModifier = Modifier.getModifier(Constants.VERIFY_MODIFIER, modifiers);
		if (verifyModifier != null) {
			cleanPendingSessions(ew, aExpr, request);
		}
		return false;
	}

	private void cleanPendingSessions(ExecuteWorkflow ew, Action aExpr, ACLMessage request) {
		String sessionId = ew.getWhat().getSessionId();
		if (sessionId != null) {
			// If there are pending workflows with the same sessionID, destroy them.
			Date startup = ew.getWhat().getSessionStartup();
			List ee = executors.getPendingExecutors(sessionId, startup);
			Iterator it = ee.iterator();
			while (it.hasNext()) {
				WorkflowExecutor we = (WorkflowExecutor) it.next();
				myLogger.log(Logger.INFO, "Agent "+getName()+" - Killing pending execution "+we.getId()+" with verified session id "+sessionId);
				we.kill(false, true);
			}
		}
	}

	/**
	 * Serve an incoming KillWorkflow request
	 */
	protected void serveKillWorkflow(final ACLMessage msg, final Action aExpr, final KillWorkflow kw) {
		Thread t = new Thread() {
			public void run() {
				String executionId = kw.getExecutionId();
				myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving KillWorkflow request. ID = "+executionId+" ... ");
				WorkflowExecutor we = executors.get(executionId);
				if (we != null) {
					try { 
						boolean freeze = kw.getFreeze() != null ? kw.getFreeze().booleanValue() : false;
						
						// Check if freeze operation is possible
						if (freeze && !we.getWorkflow().isLongRunning()) {
							reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Cannot freeze a short-running workflow"));
							
						} else {
							boolean smooth = kw.getSmooth() != null ? kw.getSmooth().booleanValue() : true;
							boolean requireKill = kw.getScope() == WorkflowManagementVocabulary.SCOPE_TARGET_ONLY ||
							                      kw.getScope() == WorkflowManagementVocabulary.SCOPE_ALL; 
							
							// Nel caso di SCOPE_INNER_MOST su un wf che non ha subflow devo uccidere il wf target.
							// Il metodo propagateKill() ritorna false nel caso il wf non abbia subflow attivi.
							boolean propagated = true;
							if (kw.getScope() != WorkflowManagementVocabulary.SCOPE_TARGET_ONLY) {
								// Propagate the kill to all subflows
								propagated = we.getWorkflow().propagateKill(kw);
							}
							
							if(requireKill || !propagated) {
								// Kill the target (i.e. this WF)
								we.kill(false, smooth, freeze, kw.getMessage());
							}

							reply(msg, ACLMessage.INFORM, null);
							myLogger.log(Logger.INFO, "Agent "+getName()+" - KillWorkflow request served.");
						}
					}
					catch (Throwable t) {
						handleError(t, msg, ACLMessage.FAILURE, "Error killing Workflow "+executionId+". ");
					}
				}
				else {
					// Send back a generic error
					reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Workflow not assigned"));
				}
			}
		};
		t.start();
	}

	/**
	 * Serve an incoming SetControlInfo request
	 */
	protected void serveSetControlInfo(ACLMessage msg, Action aExpr, SetControlInfo sci) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving SetControlInfo request... ");
		}
		WorkflowExecutor we = executors.get(sci.getExecutionId());
		if (we != null) {
			we.getEventEmitter().setControlInfo(sci.getInfo());
			reply(msg, ACLMessage.INFORM, null);
			if (myLogger.isLoggable(Logger.FINER)) {
				myLogger.log(Logger.FINER, "Agent "+getName()+" - SetControlInfo request served.");
			}
		}
		else {
			// Send back a generic error
			reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Workflow not assigned"));
		}					
	}

	/**
	 * Serve an incoming UpdateControlInfo request
	 */
	protected void serveUpdateControlInfo(ACLMessage msg, Action aExpr, UpdateControlInfo uci) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving UpdateControlInfo request... ");
		}
		WorkflowExecutor we = executors.get(uci.getExecutionId());
		if (we != null) {
			we.getEventEmitter().updateControlInfo(uci.getInfo());
			reply(msg, ACLMessage.INFORM, null);
			if (myLogger.isLoggable(Logger.FINER)) {
				myLogger.log(Logger.FINER, "Agent "+getName()+" - UpdateControlInfo request served.");
			}
		}
		else {
			// Send back a generic error
			reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Workflow not assigned"));
		}					
	}

	/**
	 * Serve an incoming GetWRD request
	 */
	protected void serveGetWRD(ACLMessage msg, Action aExpr, GetWRD gwrd) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving GetWRD request... ");
		}
		WorkflowExecutor we = executors.get(gwrd.getExecutionId());
		if (we != null) {
			// Get the value of the indicated WRD and notify it to the requester
			try {
				Object value = we.getWorkflow().getFieldValue(gwrd.getWrd());
				if (value == null) {
					value = Constants.NULL_VALUE;
				}
				Result r = new Result(aExpr, value);
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				getContentManager().fillContent(reply, r);
				send(reply);
			}
			catch (Throwable t) {
				handleError(t, msg, ACLMessage.FAILURE, "Error retrieving value of WRD "+gwrd.getWrd()+". ");
			}
		}
		else {
			// Send back a generic error
			reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Workflow not assigned"));
		}					
	}

	/**
	 * Serve an incoming SetWRD request
	 */
	protected void serveSetWRD(ACLMessage msg, Action aExpr, SetWRD swrd) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving SetWRD request... ");
		}
		WorkflowExecutor we = executors.get(swrd.getExecutionId());
		if (we != null) {
			// Set the value of the indicated WRD
			we.getWorkflow().setFieldValue(swrd.getWrd(), swrd.getValue());
			reply(msg, ACLMessage.INFORM, null);
		}
		else {
			// Send back a generic error
			reply(msg, ACLMessage.REFUSE, createGenericError(msg, "Workflow not assigned"));
		}					
	}

	/**
	 * Serve an incoming GetPoolSize request
	 */
	protected void serveGetPoolSize(ACLMessage msg, Action aExpr, GetPoolSize gps) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving GetPoolSize request... ");
		}
		try {
			Result r = new Result(aExpr, new Integer(getPoolSize()));
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			getContentManager().fillContent(reply, r);
			send(reply);
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error retrieving pool size. ");
		}
	}

	/**
	 * Serve an incoming SetPoolSize request
	 */
	protected void serveSetPoolSize(ACLMessage msg, Action aExpr, SetPoolSize sps) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving SetPoolSize request... ");
		}
		setPoolSize(sps.getValue());

		reply(msg, ACLMessage.INFORM, null);
		if (myLogger.isLoggable(Logger.FINER)) {
			myLogger.log(Logger.FINER, "Agent "+getName()+" - SetPoolSize request served.");
		}
	}

	@AttributeSetter(defaultValue="3")
	public void setPoolSize(int s) {
		synchronized (enqueuingLock) {
			poolSize = s;
			flush();
		}
	}

	/**
	 * Serve an incoming GetSessionStatus request
	 */
	protected void serveGetSessionStatus(ACLMessage msg, Action aExpr, GetSessionStatus gss) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving GetSessionStatus request... ");
		}
		try {
			Result r = new Result(aExpr, executors.getStatus(gss.getSessionId()));
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			getContentManager().fillContent(reply, r);
			send(reply);
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error retrieving session status. ");
		}
	}

	private void serveResetModifiers(ACLMessage msg, Action aExpr, ResetModifiers rm) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving ResetModifiers request... ");
		}
		try {
			WorkflowExecutor we = executors.get(rm.getExecutionId());
			if (we == null) {
				throw new WorkflowException("Workflow "+rm.getExecutionId()+" not present. ");
			}
	
			we.setModifiers(rm.getModifiers());
	
			reply(msg, ACLMessage.INFORM, null);
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error serving ResetModifiers for workflow "+rm.getExecutionId()+". ");
		}				
	}

	private void serveResetControlInfos(ACLMessage msg, Action aExpr, ResetControlInfos rci) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving ResetControlInfos request... ");
		}
		try {
			WorkflowExecutor we = executors.get(rci.getExecutionId());
			if (we == null) {
				throw new WorkflowException("Workflow "+rci.getExecutionId()+" not present. ");
			}
			
			we.getEventEmitter().setControlInfo(rci.getControlInfos());
	
			reply(msg, ACLMessage.INFORM, null);
		}
		catch (Throwable t) {
			handleError(t, msg, ACLMessage.FAILURE, "Error serving ResetControlInfos for workflow "+rci.getExecutionId()+". ");
		}				
	}

	
	///////////////////////////////////////////////////////
	// Methods that can be redefined to customize the 
	// WorkflowEngineAgent behaviour
	////////////////////////////////////////////////////////
	/**
	   This method is called when a WorkflowEngineAgent is requested to 
	   perform an action different from the basic acions of the 
	   WorkflowManagement ontology.
	   Subclasses may redefine this method to serve "extended" actions
	   appropriately.
	 */
	protected void handleUnknownAction(AgentAction act, Action aExpr, ACLMessage msg) {
		String errorMsg = "Unknown requested action "+act.getClass().getName();
		myLogger.log(Logger.SEVERE, errorMsg);
		reply(msg, ACLMessage.REFUSE, createGenericError(msg, errorMsg));
	}

	/**
	   This method is called when a request to execute a workflow is received.
	   Subclasses may redefine this method to implement special scheduling
	   policies.
	 */
	protected void handleIncomingWorkflow(WorkflowExecutor we, AID requester) {
		if (EngineHelper.isInteractive(we)) {
			// NOTE that interactivity conversations use the session-id as conversation-id
			addInteractivityConversation(we.getDescriptor().getSessionId());
		}
		submitWorkflow(we);
	}

	/**
	 	Add, enqueue or resume a workflow executor
	 */
	void submitWorkflow(WorkflowExecutor we) {
		synchronized(enqueuingLock) {
			if (mustEnqueue(we)) {
				enqueue(we);
			}
			else {
				if (we.getStatus() == SUSPENDED_STATUS) {
					we.resume();
				} 
				else {
					addBehaviour(tbf.wrap(we));	
				}
			}
		}
	}
	
	private boolean mustEnqueue(WorkflowExecutor we) {
		return getActiveCnt() >= getPoolSize() && !executors.belongsToRunningSession(we);
	}
	
	/**
	   This method is called when WorkflowExecutor terminates.
	   Subclasses may redefine this method to implement special clean-up actions.
	 */
	protected void handleCleanupWorkflow(WorkflowExecutor we) {
	}

	/**
	   Retrieve the ClassLoader that will be used to load the Workflow 
	   indicated by a given WorkflowDescriptor.
	   Subclasses may redefine it to return a domain specific ClassLoader.
	   @return the ClassLoader that will be used to load the Workflow 
	   indicated by a given WorkflowDescriptor.
	 */
	protected ClassLoader getWorkflowClassLoader(WorkflowDescriptor wd) {
		return CAServices.getInstance(this).getClassLoader(wd.getClassLoaderIdentifier());
	}

	/**
	 * Retrieve the ontology used by this WorkflowEngineAgent. The default implementation
	 * returns the singleton instance of the WorkflowManagementOntology. Subclasses may redefine 
	 * this method to use an extended ontology.
	 */
	public Ontology getOntology() {
		return WorkflowManagementOntology.getInstance();	
	}

	public Codec getLanguage() {
		return codec;
	}

	/**
	 * Adjust the control information related to a given type of event for the execution
	 * of a given workflow.
	 * This method is called when a ControlInfo object is received with the selfConfig flag
	 * set to true. The default implementation does nothing. Subclasses may redefine it to 
	 * implement application specific control information adjustment policies.
	 */
	protected void adjustControlInfo(ControlInfo cInfo, WorkflowDescriptor descriptor) {		
	}

	@AttributeGetter(name="Default workflow timeout")
	public long getDefaultWorkflowTimeout() {
		return defaultWorkflowTimeout;
	}

	public long getRollbackTimeout() {
		return 2000;
	}

	public long getCommitTimeout() {
		return 0;
	}


	//////////////////////////////////////////////
	// Information retrieval methods
	//////////////////////////////////////////////

	@AttributeGetter(name="Executors pool size")
	public int getPoolSize() {
		return poolSize;
	}

	@AttributeGetter(name="Busy executors")
	public int getBusyExecutors() {
		return tbf.size();
	}

	public final Object[] getExecutorsTableStatus() {
		List l = executors.getStatus();
		return l.toArray();
	}

	@AttributeGetter(name="Active threads")
	public final int getThreadCnt() {
		return tbf.size();
	}

	@AttributeGetter(name="Executed workflows")
	public final long getWorkflowCnt() {
		return wfCnt;
	}

	//////////////////////////////////////////////
	// Execution flow handling methods
	//////////////////////////////////////////////
	/**
	   This method is called when the execution of a workflow begins. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleBeginWorkflow(WorkflowExecutor we) {
		if (we.getEventEmitter().isFireable(Constants.FLOW_TYPE, Constants.WORKFLOW_LEVEL)) {
			// If the RBDescriptor is != null --> We are rolbacking -->
			// Use it. Otherwise use the direct workflow descriptor
			WorkflowDescriptor wd = we.getActualDescriptor();
			we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new BeginWorkflow(wd.getId(), wd.getId(), wd.getSessionId()), Constants.WORKFLOW_LEVEL);
		}
	}

	/**
	   This method is called when the execution of a workflow ends. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleEndWorkflow(WorkflowExecutor we, int exitValue) {
		if (we.getEventEmitter().isFireable(Constants.FLOW_TYPE, Constants.WORKFLOW_LEVEL)) {
			// If the RBDescriptor is != null --> We are rollbacking -->
			// Use it. Otherwise use the direct workflow descriptor
			WorkflowDescriptor wd = we.getActualDescriptor();
			we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new EndWorkflow(wd.getId(), wd.getId(), exitValue, wd.getSessionId()), Constants.WORKFLOW_LEVEL);
		}
	}

	/**
	   This method is called when the execution of an activity begins. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleBeginActivity(WorkflowExecutor we, String name) {
		if (TRACE_ACTIVITIES) {
			System.out.println(">>> Begin activity "+name);
		}
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new BeginActivity(name, wd.getId(), wd.getSessionId()), Constants.ACTIVITY_LEVEL);
	}

	/**
	   This method is called when the execution of an activity ends. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleEndActivity(WorkflowExecutor we, String name) {
		if (TRACE_ACTIVITIES) {
			System.out.println(">>> End activity "+name);
		}
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new EndActivity(name, wd.getId(), wd.getSessionId()), Constants.ACTIVITY_LEVEL);
	}

	/**
	   This method is called when the execution of an application begins. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleBeginApplication(WorkflowExecutor we, String name) {
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new BeginApplication(name, wd.getId(), wd.getSessionId()), Constants.APPLICATION_LEVEL);
	}

	/**
	   This method is called when the execution of an application ends. 
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleEndApplication(WorkflowExecutor we, String name) {
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new EndApplication(name, wd.getId(), wd.getSessionId()), Constants.APPLICATION_LEVEL);
	}

	/**
	   This method is called when a subflow is delegated to another 
	   WorkflowEngineAgent.
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleDelegatedSubflow(WorkflowExecutor we, String workflowId, String performer, String executionId) {
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new DelegatedSubflow(workflowId, performer, executionId, wd.getId(), wd.getSessionId()), Constants.WORKFLOW_LEVEL);
	}

	/**
	   This method is called when a subflow, previously delegated to another 
	   WorkflowEngineAgent, completes.
	   Subclasses may redefine this method to react to this event 
	   appropriately.
	 */
	protected void handleCompletedSubflow(WorkflowExecutor we, String executionId, int exitValue) {
		WorkflowDescriptor wd = we.getActualDescriptor();
		we.getEventEmitter().fireEvent(Constants.FLOW_TYPE, new EndSubflow(executionId, exitValue, wd.getId(), wd.getSessionId()), Constants.WORKFLOW_LEVEL);
	}

	protected void handleEvent(String executionId, String type, Object event) {
		myLogger.log((Constants.WARNING_TYPE.equals(type) ? Logger.WARNING : Logger.INFO), "Agent "+getName()+" - Executor "+executionId+": "+event);
	}

	protected String generateWorkflowExecutionId(ExecuteWorkflow ew) {
		String requester = ew.getWhat().getRequester();
		return (requester != null ? (requester+"$") : "") + GUIDGenerator.getGUID();
	}
	
	/**
	   Inner class WorkflowExecutor.
	   This Behaviour is responsible for actually executing a workflow.
	   More in details it loads the WorkflowBehaviour implementing the
	   workflow to be executed, initializes its input parameters, runs it 
	   and sends back to the requester the output parameters.
	   Moreover it maintains the execution control information associated
	   to the workflow.
	   If the workflow is executed in the scope of a transaction this
	   behaviour takes care of handling commit and rollback requests.
	 */
	protected class WorkflowExecutor extends FSMBehaviour {

		private int status = IDLE_STATUS;
		private Action requestedAction;
		private WorkflowDescriptor myDescriptor, myRBDescriptor;
		private EventEmitter eventEmitter;
		private Tracer tracer;
		private List modifiers;
		private WorkflowBehaviour myWorkflow;
		private String myId;
		private Hashtable myContext = new Hashtable();
		private boolean transactionScope;
		private ACLMessage reply;
		private WatchDog watchDog = null;
		private int abortCondition = NONE;
		private ExecutionErrorEvent lastErrorEvent = null;
		private String failureReason;
		private TransactionManager myTransactionManager;
		private transient List parameters;
		private transient String errorMessage;
		private transient AID wsmaAid;
		transient byte[] lastSerializedState;

		private WorkflowExecutor(Agent agent, ExecuteWorkflow ew, ACLMessage rq, Action a) throws Throwable {
			super(agent);

			// Create the unique ID of the workflow
			myId = generateWorkflowExecutionId(ew);
			setBehaviourName(myId);

			myDescriptor = ew.getWhat();
			modifiers = ew.getModifiers();
			wfCnt++;
			transactionScope = rq.getPerformative() == ACLMessage.CFP;
			requestedAction = a;
			reply = rq.createReply();
			reply.setSender(myAgent.getAID());

			// Be sure a conversationID is set
			if (reply.getConversationId() == null) {
				reply.setConversationId(myId+"R");
			}

			// Save reply conversation id
			addConversation(reply);

			// Get workflow classloader and set the ID in wf descriptor (if not already present and is a WadeClassLoader)
			// Note that the setting of ID is necessary for wf serialization/deserialization
			// see WorkflowSerializationManager class
			ClassLoader wcl = getWorkflowClassLoader(myDescriptor);
			if (myDescriptor.getClassLoaderIdentifier() == null && wcl instanceof WadeClassLoader) {
				myDescriptor.setClassLoaderIdentifier(((WadeClassLoader) wcl).getId());
			}

			// Create the actual WorkflowBehaviour
			myWorkflow = EngineHelper.createWorkflowBehaviour(myDescriptor, wcl);

			// Create event emitter
			eventEmitter = new WEAEventEmitter(agent, myId, onto.getName(), codec.getName(), myDescriptor, myWorkflow); 

			tracer = new Tracer(myDescriptor.getSessionId(), eventEmitter);
			List l = ew.getHow();
			if (l != null) {
				Iterator it = l.iterator();
				while (it.hasNext()) {
					ControlInfo cInfo = (ControlInfo) it.next();
					eventEmitter.setControlInfo(cInfo);
				}
			}
			adjustWarningControlInfo();

			// Pass the WorkflowBehaviour DataStore to the executor
			setDataStore(myWorkflow.getDataStore());

			// Adjust the priority
			if (myDescriptor.getPriority() == Constants.NO_PRIORITY) {
				// Use the default priority of the workflow
				myDescriptor.setPriority(myWorkflow.getDefaultPriority());
			}

			// If this is executed in a transaction scope set the transactional attribute of the WD if not yet set.
			if (getTransactionScope()) {
				myDescriptor.setTransactional(true);
			}
			if (myDescriptor.getTransactional()) {
				myTransactionManager = new TransactionManager(myId, eventEmitter);
				handleOpenedTransaction(this);
			}

			registerTransitions();
			registerStates();

			// Finally copy input parameters
			EngineHelper.copyInputParameters(myWorkflow, myDescriptor.getParameters());

			// For debugging purposes only
			checkTermination(true, 0);
		}

		// Only used when deserializing workflows
		WorkflowExecutor(WorkflowExecutorReplacer replacer) {
			super(replacer.getEnclosingAgent());
			myId = replacer.getExecutionId();
			status = replacer.getStatus();
			reply = replacer.getReply();
			requestedAction = replacer.getRequestedAction();
			myDescriptor = replacer.getDescriptor();
			modifiers = replacer.getModifiers();
			eventEmitter = replacer.getEventEmitter();
			eventEmitter.setAgent(replacer.getEnclosingAgent());
			tracer = new Tracer(myDescriptor.getSessionId(), eventEmitter);
			myWorkflow = replacer.getWorkflow();
			myContext = replacer.getContext();
			abortCondition = replacer.getAbortCondition();
			lastErrorEvent = replacer.getLastErrorEvent();
			failureReason = replacer.getFailureReason();
			transactionScope = replacer.getTransactionScope();
			myTransactionManager = replacer.getTransactionManager();

			registerTransitions();
			registerStates();

			// For debugging purposes only
			checkTermination(true, 0);
		}

		private Object writeReplace() throws ObjectStreamException {
			return new WorkflowExecutorReplacer(myAgent, myId, status, reply, requestedAction, myDescriptor, modifiers, eventEmitter, myWorkflow, myContext, abortCondition, lastErrorEvent, failureReason, transactionScope,  myTransactionManager);
		}

		private void registerTransitions() {
			registerTransition(EXECUTE, HANDLE_SUCCESS, Constants.SUCCESS);
			registerTransition(EXECUTE, HANDLE_FAILURE, Constants.FAILURE);
			registerTransition(EXECUTE, HANDLE_FROZEN, Constants.FROZEN);

			registerDefaultTransition(HANDLE_SUCCESS, SEND_REPLY);
			registerDefaultTransition(HANDLE_FAILURE, SEND_REPLY);
			registerDefaultTransition(HANDLE_FROZEN, SEND_REPLY);

			if (myDescriptor.getTransactional()) {
				// The following transitions are only required if the workflow is transactional
				registerTransition(HANDLE_FAILURE, PREPARE_ROLLBACK, TRANSACTION);
				if (getTransactionScope()) {
					registerTransition(HANDLE_SUCCESS, WAIT_COMMIT, TRANSACTION);
					registerTransition(WAIT_COMMIT, COMMIT, ACLMessage.ACCEPT_PROPOSAL); // Commit
					registerDefaultTransition(WAIT_COMMIT, PREPARE_ROLLBACK);
				}
				else {
					registerTransition(HANDLE_SUCCESS, COMMIT, TRANSACTION);
				}
				registerDefaultTransition(PREPARE_ROLLBACK, ROLLBACK);
				registerTransition(COMMIT, HANDLE_COMMIT_SUCCESS, Constants.SUCCESS);
				registerTransition(COMMIT, HANDLE_TRANSACTION_FAILURE, Constants.FAILURE);
				registerTransition(ROLLBACK, HANDLE_ROLLBACK_SUCCESS, Constants.SUCCESS);
				registerTransition(ROLLBACK, HANDLE_TRANSACTION_FAILURE, Constants.FAILURE);
				registerDefaultTransition(HANDLE_COMMIT_SUCCESS, SEND_REPLY);
				registerDefaultTransition(HANDLE_ROLLBACK_SUCCESS, SEND_REPLY);
				registerDefaultTransition(HANDLE_TRANSACTION_FAILURE, SEND_REPLY);
			}
		}

		private void registerStates() {
			// EXECUTE state (the first state): The WorkflowBehaviour
			registerFirstState(myWorkflow, EXECUTE);
			// Note: we set the name here otherwise registerFirstState() would overwrite it.
			myWorkflow.setBehaviourName(myDescriptor.getId().replace(' ', '-'));

			// HANDLE_SUCCESS state
			registerState(new TerminationHandler() {
				public void handle() throws Exception {
					errorMessage = null;
					
					if (getTransactionScope()){
						status = WAITING_STATUS;
						reply.setPerformative(ACLMessage.PROPOSE);
						CAServices.getInstance(WorkflowEngineAgent.this).registerExpectedReply(reply);
					}else{
						status = TERMINATING_STATUS;
						reply.setPerformative(ACLMessage.INFORM);
					}

					// Fill values for output parameters
					parameters = myWorkflow.extractOutputParameters();
					if (parameters == null) {
						parameters = new ArrayList();
					}
					Result result = new Result(requestedAction, parameters);
					eventEmitter.fireEvent(Constants.TERMINATION_TYPE, new SuccessfulTerminationEvent(result), Constants.DEFAULT_LEVEL);

					myAgent.getContentManager().fillContent(reply, result);
					myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": SUCCEEDED.");
				}
			}, HANDLE_SUCCESS);

			// HANDLE_FAILURE state
			registerState(new TerminationHandler() {
				public void handle() throws Exception {
					status = TERMINATING_STATUS;
					// If we got a KillWorkflow between the last workflow interruptable section termination and the 
					// activation of this state, this would just have the effect of killing the rollback procedure 
					// as soon as it starts --> clear the interruption. Note that we don't need any additional 
					// synchronized block since here we are inside a synchronized block on WorkflowExecutor and 
					// WorkflowBehaviour.interrupt() is only called within WorkflowExecutor.kill() that is synchronized
					// on WorkflowExecutor too.
					myWorkflow.clearInterruption();
					reply.setPerformative(ACLMessage.FAILURE);

					// Fill values for output parameters 
					parameters = myWorkflow.extractOutputParameters();

					// Build the ExecutionError response
					errorMessage = "UNKNOWN";
					String errorType = "UNKNOWN";
					Throwable t = myWorkflow.getLastException();
					if (t != null) {
						errorMessage = t.getMessage();
						errorType = getErrorType(t);
					}
					// If a failure reason was set, use it as error message
					if (failureReason != null) {
						errorMessage = failureReason;
					}
					ExecutionError er = new ExecutionError(errorType, errorMessage, parameters);
					eventEmitter.fireEvent(Constants.TERMINATION_TYPE, new UnsuccessfulTerminationEvent(er), Constants.DEFAULT_LEVEL);

					myAgent.getContentManager().fillContent(reply, er);
					myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": FAILED.");
				}					
			}, HANDLE_FAILURE);

			// HANDLE_FROZEN
			registerState(new TerminationHandler() {
				public void handle() throws Exception {
					errorMessage = null;
					
					reply.setPerformative(ACLMessage.INFORM);
					myAgent.getContentManager().fillContent(reply, new Frozen());
					myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": Frozen");
				}
			}, HANDLE_FROZEN);

			// SEND_REPLY state
			registerLastState(new OneShotBehaviour() {
				public void action() {
					// Actually send the reply only if the workflow was executed in synchronous mode.
					if (myDescriptor.getExecution() == Constants.SYNCH && reply != null) {
						if (myLogger.isLoggable(Logger.FINE)) {
							myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - Executor "+myId+": Final reply "+ACLMessage.getPerformative(reply.getPerformative())+" sent back.");
						}
						reply.addUserDefinedParameter(ACLMessage.IGNORE_FAILURE, "true");
						myAgent.send(reply);
					}

					// Notify workflow frozen or terminate to WSMA
					if (!getWorkflow().isVolatile()) {
						if (getWorkflow().isFrozen()) {
							getStatusManager().notifyStatusChanged(WorkflowExecutor.this, WorkflowStatus.FROZEN);
						} else {
							getStatusManager().notifyTerminated(WorkflowExecutor.this, parameters, errorMessage);	
						}
					}
				}
			}, SEND_REPLY);


			if (myDescriptor.getTransactional()) {
				// The following states are only required if the workflow is transactional

				if (getTransactionScope()) {
					// WAIT_COMMIT
					registerState(new OneShotBehaviour(myAgent) {
						private ACLMessage msg = null;

						public void action() {
							// For non volatile wf send the CHANGE-STATE notification to WSMA
							if (!WorkflowExecutor.this.getWorkflow().isVolatile()) {
								getStatusManager().notifyStatusChanged(WorkflowExecutor.this, WorkflowStatus.WAIT_COMMIT);
							}

							// Send the reply prepared in the previous state
							myAgent.send(reply);
							// Wait for the ACCEPT/REJECT_PROPOSAL
							// Be sure not to intercept CFP/REQUEST messages (VERIFY requests) or AGREE/PROPOSE messages (previously sent in case of auto-delegations)
							MessageTemplate tpl = MessageTemplate.and(
									MessageTemplate.MatchConversationId(reply.getConversationId()),
									MessageTemplate.or(
											MessageTemplate.MatchPerformative(ACLMessage.FAILURE),
											MessageTemplate.or(
													MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
													MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL) ) ) );
							try {
								myWorkflow.enterInterruptableSection();
								// FIXME: Set a proper timeout
								msg = myAgent.blockingReceive(tpl);
								myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - Executor "+myId+": End-transaction message "+ACLMessage.getPerformative(msg.getPerformative())+" received.");
								CAServices.getInstance(WorkflowEngineAgent.this).expectedReplyReceived(msg);

								// Prepare the confirmation that will be sent back after the commit/rollback (SEND_REPLY state)
								reply = msg.createReply();
								reply.setPerformative(ACLMessage.INFORM);
							}
							catch (Throwable t) {
								// This can only be an InterruptedException, Agent.Interrupted or ThreadDeath due to a kill-workflow
								myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": killed while waiting for commit");
								// Remove the conversation related to this workflow ASAP. In this way if the ACCEPT/REJECT_PROPOSAL is received 
								// while we are rollbacking it is properly served by the behaviour dealing with spurious messages
								removeConversation(reply);
								// No final reply will be sent as the requester is not waiting for it
								reply = null;
							}
							finally {
								status = TERMINATING_STATUS;
								myWorkflow.exitInterruptableSection(null);
							}
						}

						public int onEnd() {
							int ret = (msg != null ? msg.getPerformative() : ACLMessage.REJECT_PROPOSAL);
							return ret;
						}
					}, WAIT_COMMIT);
				}

				// PREPARE_ROLLBACK state
				registerState(new WakerBehaviour(myAgent, getRollbackTimeout()) {
					public void onWake() {
						myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": Activating rollback procedure");
						// FIXME: Fire a BeginRollback Transaction event
						// The Rollback workflow (if any) is loaded using the same ClassLoader
						// of the direct workflow
						WorkflowBehaviour rollbackWorkflow = myWorkflow.getRollbackWorkflow();
						if (rollbackWorkflow != null) {
							myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": User-defined rollback workflow found");
							myWorkflow.setRollbackWorkflow(rollbackWorkflow);
							registerState(rollbackWorkflow, ROLLBACK);
							// Note: we set the name here otherwise registerState() would overwrite it.
							rollbackWorkflow.setBehaviourName(myRBDescriptor.getId().replace(' ', '-'));
							// We use the same DataStore for the direct workflow and the rollback workflow
							rollbackWorkflow.setDataStore(myWorkflow.getDataStore());

							// Copy fields value of direct wf into rollback wf
							rollbackWorkflow.copyFieldsValue(myWorkflow);
						}

						// For non volatile wf send the CHANGE-STATE notification to WSMA
						if (!WorkflowExecutor.this.getWorkflow().isVolatile()) {
							getStatusManager().notifyStatusChanged(WorkflowExecutor.this, WorkflowStatus.ROLLBACK);
						}
					}
				}, PREPARE_ROLLBACK);

				// COMMIT state
				registerState(new WakerBehaviour(myAgent, getCommitTimeout()) {					
					private int ret = Constants.SUCCESS;

					public void onWake() {
						try {
							myWorkflow.enterInterruptableSection();
							if (!myWorkflow.commit()) {
								ret = Constants.FAILURE;
							}
						}
						catch (Agent.Interrupted ai) {
							ret = Constants.FAILURE;
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": commit procedure interrupted");
						}
						catch (ThreadDeath td) {
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": commit procedure interrupted (ThreadDeath)");
						}
						finally {
							myWorkflow.exitInterruptableSection(null);
						}
					}

					public int onEnd() {
						return ret;
					}
				}, COMMIT);

				// HANDLE_COMMIT_SUCCESS
				registerState(new OneShotBehaviour() {
					public void action() {
						handleCommittedTransaction(WorkflowExecutor.this);
					}
				}, HANDLE_COMMIT_SUCCESS);

				// ROLLBACK state (This state may be overridden by a user-defined rollback workflow)
				registerState(new OneShotBehaviour() {
					private int ret = Constants.SUCCESS;
					public void action() {
						try {
							handleBeginWorkflow(WorkflowExecutor.this);
						}
						catch (Exception e) {
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": Unexpected error in handleBeginWorkflow() of rollback procedure", e);
						}

						try {
							myWorkflow.enterInterruptableSection();
							if (!myWorkflow.rollback()) {
								ret = Constants.FAILURE;
							}
						}
						catch (Agent.Interrupted ai) {
							ret = Constants.FAILURE;
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": rollback procedure interrupted");
						}
						catch (ThreadDeath td) {
							ret = Constants.FAILURE;
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": rollback procedure interrupted (ThreadDeath)");
						}
						finally {
							myWorkflow.exitInterruptableSection(null);
						}

						try {
							handleEndWorkflow(WorkflowExecutor.this, ret);
						}
						catch (Exception e) {
							myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": Unexpected error in handleEndWorkflow() of rollback procedure", e);
						}
					}

					public int onEnd() {
						return ret;
					}
				}, ROLLBACK);

				// HANDLE_ROLLBACK_SUCCESS
				registerState(new OneShotBehaviour() {
					public void action() {
						checkPendingSubflows();
						handleAbortedTransaction(WorkflowExecutor.this);
					}
				}, HANDLE_ROLLBACK_SUCCESS);

				// HANDLE_TRANSACTION_FAILURE
				registerState(new OneShotBehaviour() {
					public void action() {
						myLogger.log(Logger.SEVERE, "Agent "+myAgent.getName()+" - Executor "+myId+": TRANSACTION FAILED!!!!!! Session-id = "+myDescriptor.getSessionId());
						handleFailedTransaction(WorkflowExecutor.this);
						checkPendingSubflows();
						if (reply != null) {
							reply.setPerformative(ACLMessage.FAILURE);
						}
					}
				}, HANDLE_TRANSACTION_FAILURE);
			}
		}


		/**
		 * This method is called at the end of a transaction and forces the rollback of subflows that are still
		 * waiting for a commit/rollback notification. If the workflow succeeded or if the workflow failed and 
		 * the default rollback procedure was used there are NO such subflows. However if the workflow failed and  
		 * a user-defined rollback workflow is available, there is no guarantee that that rollback workflow
		 * properly rollback all subflows.  
		 */
		private void checkPendingSubflows() {
			if (!myTransactionManager.rollback(TransactionEntry.SUBFLOW_TYPE, null)) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": Unexpected failure rollbacking orphan subflows");
			}
		}

		// For debugging purposes only
		protected void handleStateEntered(Behaviour state) {
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - Executor "+myId+": Entering state "+state.getBehaviourName());
			}
		}

		// For debugging purposes only
		protected boolean checkTermination(boolean currentDone, int currentResult) {
			boolean b = super.checkTermination(currentDone, currentResult);
			boolean b1 = false;
			if (currentDone) {
				b1 = lastStates.contains(currentName);
			}
			if (b != b1) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": FSM.checkTermination() returned "+b+" with Last-states = "+lastStates+" and currentName = "+currentName);
			}
			return b1;
		}

		// Mutual exclusion with kill()
		public synchronized void onStart() {
			status = EXECUTING_STATUS;
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": STARTED.");
			
			// For short-running WF only, if a timeout is set (in the descriptor, in the workflow or in the Agent), create the watchDog behaviour and start it.
			if (!myWorkflow.isLongRunning()) {
				long timeout = myDescriptor.getTimeout();
				if (timeout <= 0) {
					timeout = myWorkflow.getLimit();
				}
				if (timeout <= 0) {
					timeout = getDefaultWorkflowTimeout();
				}
				if (timeout > 0) {
					watchDog = new WatchDog(myAgent, timeout, WorkflowExecutor.this);
					myAgent.addBehaviour(watchDog);
					myLogger.log(Logger.CONFIG, "Agent "+myAgent.getName()+" - Executor "+myId+": WatchDog timer activated.");
				}
			}
			
			// Put the execution-context in the static ThreadLocale to make it accessible by the executor Thread wherever it is. 
			executionContexts.set(myContext);
		}

		public int onEnd() {
			eventEmitter.close();
			terminate();
			return super.onEnd();
		}

		private void terminate() {
			status = DONE_STATUS;

			// WF execution terminated --> remove the related conversation 
			removeConversation(reply);
			
			// If this was an Interactive workflow remove the interactivity conversation too
			if (EngineHelper.isInteractive(this)) {
				removeInteractivityConversation(myDescriptor.getSessionId());
			}

			// CleanupWorkflow callback method
			try {
				handleCleanupWorkflow(this);
			} catch (RuntimeException re)  {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+myId+": handleCleanupWorkflow throw a RuntimeException.", re);
			}

			synchronized (enqueuingLock) {
				executors.remove(myId);
				// Flush workflows waiting in the queue if any
				flush();
			}
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": TERMINATED.");
		}

		void suspend() {
			tbf.suspend(this);
		}

		void resume() {
			tbf.resume(this);
		}

		public void onSuspended() {
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": SUSPENDED.");

			synchronized (enqueuingLock) {
				status = SUSPENDED_STATUS;

				flush();
			}

			// For non volatile wf send the CHANGE-STATE notification to WSMA
			if (!WorkflowExecutor.this.getWorkflow().isVolatile()) {
				getStatusManager().notifyStatusChanged(WorkflowExecutor.this, WorkflowStatus.SUSPENDED);
			}

			myWorkflow.onSuspended();
		}

		public void onResumed() {
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+myId+": RESUMED.");

			// Put the execution-context in the static ThreadLocale to make it accessible by the executor Thread wherever it is. 
			executionContexts.set(myContext);

			synchronized (enqueuingLock) {
				status = EXECUTING_STATUS;
			}

			// For non volatile wf send the CHANGE-STATE notification to WSMA
			if (!WorkflowExecutor.this.getWorkflow().isVolatile()) {
				getStatusManager().notifyStatusChanged(WorkflowExecutor.this, WorkflowStatus.ACTIVE);
			}

			myWorkflow.onResumed();
		}

		/**
		   @return the execution ID of the workflow under execution
		 */
		public final String getId() {
			return myId;
		}

		/**
		   @return the descriptor of the direct workflow under execution
		 */
		public final WorkflowDescriptor getDescriptor() {
			return myDescriptor;
		}

		/**
		   @return the descriptor of the RollBack workflow under execution.
		   This is != null only after the direct workflow has failed and 
		   the RB workflow (if any) has been successfully created. 
		 */
		public final WorkflowDescriptor getRBDescriptor() {
			return myRBDescriptor;
		}

		/**
		   @return the descriptor of the workflow actually under execution.
		   This is the descriptor of the direct workflow or the descriptor of the rollback 
		   workflow depending which workflow is currently in execution. 
		 */
		public final WorkflowDescriptor getActualDescriptor() {
			WorkflowDescriptor wd = getRBDescriptor();
			if (wd == null) {
				wd = getDescriptor();
			}
			return wd;
		}

		//public final ClassLoader getClassLoader() {
		//	return classLoader;
		//}

		/**
		   Set the RollBack workflow descriptor.
		   This is used internally by the framework.
		 */
		private final void setRBDescriptor(WorkflowDescriptor wd) {
			myRBDescriptor = wd;
		}

		/**
		   @return <code>true</code> if the workflow under execution is
		   performed within the scope of a transaction (i.e. it was requested 
		   by means of a CFP message). 
		   Subclasses may use this information to perform application-specific 
		   transaction management actions. 
		 */
		public final boolean getTransactionScope() {
			return transactionScope;
		}

		/**
		   @return the <code>TransactionManager</code> associated to 
		   this <code>WorkflowExecutor</code>. The latter is made accessible 
		   so that proper transaction-related applications can be created to build 
		   user defined rollback workflows.
		 */
		final TransactionManager getTransactionManager() {
			return myTransactionManager;
		}

		/**
		 * @return the <code>Modifier</code> objects, if any, activated for this workflow 
		 */
		public final List getModifiers() {
			return modifiers;
		}

		/**
		 * Set the <code>Modifier</code> objects for this workflow 
		 */
		public final void setModifiers(List modifiers) {
			this.modifiers = modifiers; 
		}

		/**
		 * Retrieve the execution context (in form of an Hashtable) of this WorkflowExecutor. 
		 * This can be used by WorkflowEngineAgent extensions to attach to an executor 
		 * application specific additional information.
		 * @return The execution context of this WorkflowExecutor.
		 */
		public final Hashtable getExecutionContext() {
			return myContext;
		}

		public EventEmitter getEventEmitter() {
			return eventEmitter;
		}

		public Tracer getTracer() {
			return tracer;
		}

		// Shortcut methods maintained for backward compatibility
		public final Hashtable<String, ControlInfo> getControlInfo() {
			return eventEmitter.getControlInfo();
		}
		public final void fireEvent(String type, WorkflowEvent ev, int level) {
			eventEmitter.fireEvent(type, ev, level);
		}
		public final boolean isFireable(String type, int level) {
			return eventEmitter.isFireable(type, level);
		}

		final void setFailureReason(String reason) {
			failureReason = reason;
		}

		final String getFailureReason() {
			return failureReason;
		}

		final void setLastErrorEvent(ExecutionErrorEvent ev) {
			lastErrorEvent = ev;
		}

		final ExecutionErrorEvent getLastErrorEvent() {
			return lastErrorEvent;
		}

		final WorkflowBehaviour getWorkflow() {
			return myWorkflow;
		}

		final void setWorkflow(WorkflowBehaviour wb) {
			myWorkflow = wb;
			if (eventEmitter instanceof WEAEventEmitter) {
				((WEAEventEmitter) eventEmitter).setWorkflow(wb);
			}
		}
		
		// To support introspection
		public final Behaviour getCurrent() {
			return super.getCurrent();
		}

		final int getStatus() {
			return status;
		}

		final int getAbortCondition() {
			return abortCondition;
		}

		final WatchDog getWatchDog() {
			return watchDog;
		}

		private void adjustWarningControlInfo() {
			ControlInfo wInfo = eventEmitter.getControlInfo().get(Constants.WARNING_TYPE);
			if (wInfo == null) {
				wInfo = new ControlInfo();
				wInfo.setType(Constants.WARNING_TYPE);
				wInfo.setVerbosityLevel(Constants.DEFAULT_LEVEL);
				eventEmitter.setControlInfo(wInfo);
			}
		}		

		/**
		 * Kill the workflow executed by this WorkflowExecutor.
		 * This method is always executed by the Agent Thread. It is synchronized to guarantee mutual
		 * exclusion with onStart()
		 */
		void kill(boolean isTimeout, boolean smooth) {
			kill(isTimeout, smooth, false, null);
		}

		synchronized void kill(boolean isTimeout, boolean smooth, boolean freeze, String message) {
			abortCondition = (isTimeout ? TIMED_OUT : KILLED);
			eventEmitter.disableSynchEvents();
			boolean enqueued = false;
			// Note that since this method is always executed by the agent thread there is no 
			// synchronization issue with workflow insertion in the queue (that is performed by the agent thread too)
			synchronized (enqueuingLock) {
				enqueued = removeFromQueue(this);
			}

			if (enqueued) {
				// The workflow was waiting in the queue --> directly perform clean-up operations
				myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Workflow "+myId+" removed while in queue");
				handleAbortedWhileInQueue();
			}
			else {
				// The workflow is running --> stop the watch-dog (if any) and interrupt it. Clean-up operations 
				// will be performed by the workflow executor thread.
				if (watchDog != null) {
					watchDog.stop();
				}
				if (status == TERMINATING_STATUS && smooth) {
					// If the executor is already terminating and the smooth-kill flag is set, just do nothing
					myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Smooth-kill for workflow "+myId+" while terminating --> just do nothing");
					return;
				}
				if (message == null) {
					message = isTimeout ? "Timed out" : "Killed from the outside";
				}
				myWorkflow.interrupt(this, tbf, message, freeze);
				if (status == SUSPENDED_STATUS) {
					// If the executor was suspended, resume it 
					resume();
				}
			}
		}

		/**
		   This WorkflowExecutor was aborted before execution i.e. when it was
		   still waiting in the queue. 
		   Send back a proper FAILURE to the agent that requested the 
		   execution of this workflow. Then explicitly call the onEnd() 
		   method to perform clean-up operations.
		 */
		private void handleAbortedWhileInQueue() {
			try {
				reply.setPerformative(ACLMessage.FAILURE);

				// Note that NO output parameter is returned 			
				ExecutionError	er = new ExecutionError(Constants.ABORTED, "Aborted while in queue", null);
				myAgent.getContentManager().fillContent(reply, er);
				myAgent.send(reply);
				
				// For not volatile WF explicitly send the TERMINATED notification to WSMA since the WF itself cannot do it
				if (!getWorkflow().isVolatile()) {
					getStatusManager().notifyTerminated(this, null, "Aborted while in queue");
				}
			}
			catch (Exception e) {
				// Should never happen since there are no parameters
				e.printStackTrace();
			}
			onEnd();
		}

		AID getWSMA() {
			return wsmaAid;
		}
		
		void setWSMA(AID wsmaAid) {
			this.wsmaAid = wsmaAid;
		}
		
		/**
		 * Inner class TerminationHandler
		 * Common base class of behaviours registered in the HANDLE_SUCCESS and HANDLE_FAILURE
		 */
		private class TerminationHandler extends OneShotBehaviour {
			public void action() {
				// Mutual exclusion with WorkflowExecutor.kill()
				synchronized(WorkflowExecutor.this) {
					// Stop the watch dog if any (no need for synchronization since this can never occur in parallel with onStart())
					if (watchDog != null) {
						watchDog.stop();
					}
					try {
						handle();
					}
					catch (Exception e) {
						e.printStackTrace();
						reply.setPerformative(ACLMessage.FAILURE);
						reply.setContent("(("+WorkflowManagementVocabulary.NOTIFICATION_ERROR+"))");						
					}
				}
			}

			public int onEnd() {
				return (myDescriptor.getTransactional() ? TRANSACTION : 0); 
			}

			protected void handle() throws Exception {
			}
		} // END of inner class TerminationHandler
	} // END of inner class WorkflowExecutor


	////////////////////////////////////////////
	// Enqueued workflows management methods
	////////////////////////////////////////////
	/**
	 * Put a WorkflowExecutor into the queue of workflows waiting to be executed. 
	 */
	protected void enqueue(WorkflowExecutor we) {
		workflowQueue.add(we);
	}

	/**
	 * Get (and remove) the first WorkflowExecutor from the queue of workflows waiting to be executed. 
	 */
	protected WorkflowExecutor dequeue() {
		if (workflowQueue.size() > 0) {
			return (WorkflowExecutor) workflowQueue.remove(0);
		}
		else {
			return null;
		}
	}

	/**
	 * @return The number of workflows waiting to be executed 
	 */
	@AttributeGetter(name="Enqueued workflows")
	public int getEnqueuedCnt() {
		return workflowQueue.size();
	}

	/**
	 * @return The number of workflows suspended 
	 */
	@AttributeGetter(name="Suspended workflows")
	public int getSuspendedCnt() {
		return executors.suspendedCnt();
	}

	/**
	 * Remove a given WorkflowExecutor from the queue of workflows waiting to be executed 
	 */
	protected boolean removeFromQueue(WorkflowExecutor we) {
		return workflowQueue.remove(we);
	}

	// This is always executed within a synchronized block on the enqueuingLock object
	private void flush() {
		while (getActiveCnt() < getPoolSize()) {
			WorkflowExecutor w = dequeue();
			if (w != null) {
				addBehaviour(tbf.wrap(w));
			}
			else {
				break;
			}
		}
	}

	/**
	 * @return the number of workflows currently in execution. This is computed as
	 * the number of "known" workflows minus those waiting to be executed as returned
	 * by getEnqueuedCnt() and minus the suspended executor as returned by getSuspendedCnt().
	 */
	@AttributeGetter(name="Active workflows")
	public final int getActiveCnt() {
		return executors.size() - getEnqueuedCnt() - getSuspendedCnt();		
	}

	////////////////////////////////////////////
	// Transaction related methods
	////////////////////////////////////////////
	/**
	   This method is invoked when a transactional workflow is loaded and has the effect of
	   firing an OpenedTransaction event (if transaction events are enabled).
	   Subclasses may redefine this method to react to this event in an application specific way.
	 */
	protected void handleOpenedTransaction(WorkflowExecutor we) {
		we.getEventEmitter().fireEvent(Constants.TRANSACTION_TYPE, new OpenedTransaction(we.getId()), Constants.INFO_LEVEL);
	}

	/**
	   This method is invoked when a transactional workflow successfully completed and committed
	   and has the effect of firing a CommittedTransaction event (if transaction events are enabled).
	   Subclasses may redefine this method to react to this event in an application specific way.
	 */
	protected void handleCommittedTransaction(WorkflowExecutor we) {
		we.getEventEmitter().fireEvent(Constants.TRANSACTION_TYPE, new CommittedTransaction(we.getId()), Constants.INFO_LEVEL);
	}

	/**
	   This method is invoked when a transactional workflow failed and correctly rolled back
	   and has the effect of firing an AbortedTransaction event (if transaction events are enabled).
	   Subclasses may redefine this method to react to this event in an application specific way.
	 */
	protected void handleAbortedTransaction(WorkflowExecutor we) {
		we.getEventEmitter().fireEvent(Constants.TRANSACTION_TYPE, new AbortedTransaction(we.getId()), Constants.INFO_LEVEL);
	}

	/**
	   This method is invoked when a transactional workflow could not commit or rollback properly
	   and has the effect of firing a FailedTransaction event (if transaction events are enabled).
	   Subclasses may redefine this method to react to this event in an application specific way.
	 */
	protected void handleFailedTransaction(WorkflowExecutor we) {
		// FIXME: To be implemented
		//we.getEventEmitter().fireEvent(Constants.TRANSACTION_TYPE, new AbortedTransaction(we.getId()), Constants.INFO_LEVEL);
	}

	/**
	 * Load the WorkflowBehaviour implementing a user defined rollback workflow that will 
	 * be used to override the default rollback process.
	 * This default implementation tries to load a WorkflowBehaviour with ID equals to the ID of the direct workflow 
	 * with the _RB suffix. Subclasses may redefine this method to implement application specific 
	 * loading mechanisms.
	 */
	protected WorkflowBehaviour loadRollbackWorkflow(WorkflowExecutor we, ClassLoader loader) {
		WorkflowDescriptor wd = we.getDescriptor();		
		String wfId = wd.getId();
		WorkflowDescriptor rbWd = new WorkflowDescriptor(wfId+"_RB", wd.getParameters());
		// Propagate requester, sessionId and delegationChain.
		rbWd.setRequester(wd.getRequester());
		rbWd.setSessionId(wd.getSessionId());
		rbWd.setDelegationChain(wd.getDelegationChain());
		// Propagate the priority. Note that this is only useful in case the rollback workflow delegates some subflow
		rbWd.setPriority(wd.getPriority());
		we.setRBDescriptor(rbWd);
		WorkflowBehaviour wb = null;
		try {
			wb = (WorkflowBehaviour) Class.forName(rbWd.getId(), true, loader).newInstance();
		}
		catch (Exception e) {
			// Rollback workflow not defined --> The default rollback process will be used
		}
		return wb;
	}

	/////////////////////////////////////////////////////
	// Utility methods
	/////////////////////////////////////////////////////
	protected final void handleError(Throwable t, ACLMessage msg, int replyPerformative, String info) {
		String errorMsg = info+t.getMessage();
		myLogger.log(Logger.SEVERE, errorMsg);
		t.printStackTrace();
		reply(msg, replyPerformative, createGenericError(msg, errorMsg));
	}

	protected final void reply(ACLMessage msg, int performative, String content) {
		ACLMessage reply = msg.createReply();
		reply.setPerformative(performative);
		reply.setContent(content);
		send(reply);
	}

	protected final  String createGenericError(ACLMessage msg, String reason) {
		msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		return "(("+WorkflowManagementVocabulary.GENERIC_ERROR+" \""+reason+"\"))";
	}

	void addConversation(ACLMessage msg){
		if (msg != null) {
			synchronized (conversations){
				conversations.registerConversation(msg.getConversationId());
			}
		}
	}

	public void removeConversation(ACLMessage msg){
		if (msg != null) {
			// msg can be null in case the workflow is interrupted while waiting for COMMIT/ROLLBACK notification.
			// In that case the conversation was already removed.
			synchronized (conversations){
				conversations.deregisterConversation(msg.getConversationId());
			}
		}
	}	

	void addInteractivityConversation(String id){
		if (id != null) {
			synchronized (interactivityConversations){
				// NOTE Since interactivity-conversations use the session-id as conversation-id,
				// in case of delegations within the same Agent, the same id my be registered 
				// twice or more. This is not a problem however: as soon as each WF in the delegation
				// chain terminates, it removes one entry only. When the last WF completes, the last 
				// entry is removed and the Interactivity-Cleaner behaviour will start processing
				// further messages with that conversation-id again.
				interactivityConversations.registerConversation(id);
			}
		}
	}

	public void removeInteractivityConversation(String id){
		if (id != null) {
			synchronized (interactivityConversations){
				interactivityConversations.deregisterConversation(id);
			}
		}
	}	

	private String getErrorType(Throwable t) {
		if (t instanceof WorkflowException) {
			Throwable nested = t.getCause();
			if (nested != null) {
				return nested.getClass().getName();
			}
		}
		return t.getClass().getName();
	}

	// This is only used for WorkflowExecutor deserialization
	WorkflowExecutor createWorkflowExecutor(WorkflowExecutorReplacer replacer) {
		return new WorkflowExecutor(replacer);
	}

	public StatusManager getStatusManager() {
		if (statusManager == null) {
			statusManager = new StatusManager(this);
		}
		return statusManager;
	}

}
