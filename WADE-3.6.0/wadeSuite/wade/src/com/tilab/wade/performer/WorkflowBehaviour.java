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

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.leap.LEAPCodec;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.performer.DefaultParameterValues.DefaultValue;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.ExecutionErrorEvent;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.layout.WorkflowSkipped;
import com.tilab.wade.performer.ontology.ExecuteWorkflow;
import com.tilab.wade.performer.ontology.KillWorkflow;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;
import com.tilab.wade.performer.transaction.ApplicationEntry;
import com.tilab.wade.performer.transaction.SubflowEntry;
import com.tilab.wade.performer.transaction.TransactionManager;
import com.tilab.wade.utils.OntologyUtils;

/**
   The base class for all classes implementing workflows 
   @author Giovanni Caire - TILAB
 */
@WorkflowSkipped
public class WorkflowBehaviour extends FSMBehaviour implements HierarchyNode, OntologyHolder {
	public static final int INITIAL = 1;
	public static final int FINAL = 2;
	public static final int INITIAL_AND_FINAL = 3;
	public static final int INTERMEDIATE = 0;
	
	protected static final String START_STATE = "__Start-state__";
	protected static final String COLLECT_ASYNCH_SUBFLOWS_STATE = "__Collect-asynch-subflows__";
	protected static final String END_STATE = "__End-state__";
	protected static final String ERROR_STATE = "__Error-state__";
	protected static final String FROZEN_STATE = "__Frozen-state__";
	
	protected transient WorkflowEngineAgent.WorkflowExecutor rootExecutor;

	protected Logger myLogger = Logger.getMyLogger(WorkflowBehaviour.class.getName());
	
	protected Throwable lastException;
	protected List formalParams = null;
	protected boolean useDataStore = false;
	private boolean interruptable = false;
	private boolean interrupted = false;
	protected boolean frozen = false;
	private boolean thawed = false;
	private transient boolean suspendAsap = false;
	private String abortMessage = null;
	private WorkflowBehaviour rollbackWorkflow = null;
	private BindingManager bindingManager;
	private transient Ontology onto;
	private WorkflowDetails workflowDetails;
	
	// Variables used when this workflow is used as an inline subflow
	private boolean inline;
	private Subflow subflow;
	private WorkflowDescriptor inlineDescriptor;
	private OutgoingTransitions outgoingTransitions;
	private boolean errorActivity = false;
	private boolean executed = false;
	private boolean inlineInterrupted = false;
	private Set<String> managedFieldNames;
	private boolean requireSave = false;
	

	public WorkflowBehaviour() {
		super();

		// START state
		registerFirstState(new OneShotBehaviour() {
			public void action() {
				try {
					configure();
				}
				catch (Throwable t) {
					EngineHelper.fireExecutionErrorEvent(WorkflowBehaviour.this, t, Constants.SEVERE_LEVEL);
					handleException(t);
				}
			}
		}, START_STATE);

		// COLLECT_ASYNCH_SUBFLOWS_STATE state
		registerState(new AsynchSubflowCollector(this), COLLECT_ASYNCH_SUBFLOWS_STATE);
			
		// END state
		registerLastState(new OneShotBehaviour() {
			public void action() {
			}

			public int onEnd() {
				return Constants.SUCCESS;
			}
		}, END_STATE);	

		// ERROR state
		registerLastState(new OneShotBehaviour() {
			public void action() {
				if (lastException == null) {
					// We got here following the execution of an activity marked as error activity --> Retrieve its name.
					lastException = new TerminatedInErrorActivity(WorkflowBehaviour.this.getPrevious().getBehaviourName());
				}
			}

			public int onEnd() {
				return Constants.FAILURE;
			}
		}, ERROR_STATE);	
		
		// FROZEN state
		registerLastState(new OneShotBehaviour() {
			public void action() {
			}

			public int onEnd() {
				onFrozen();
				return Constants.FROZEN;
			}
		}, FROZEN_STATE);	
		
		registerTransition(COLLECT_ASYNCH_SUBFLOWS_STATE, END_STATE, Constants.SUCCESS);
		registerTransition(COLLECT_ASYNCH_SUBFLOWS_STATE, ERROR_STATE, Constants.FAILURE);
		
		fillFormalParameters();
		
		if (!getClass().equals(WorkflowBehaviour.class)) {
			init(getClass());
		}
		
		// Create default WorkflowDetails reading name and documentation from class annotation
		workflowDetails = EngineHelper.buildWorkflowDetails(getClass());
	}

	/**
	 * This constructor is used when this workflow is used as an inline subflow
	 * @param activityName The name of the subflow activity whose execution corresponds to the execution of this workflow
	 */
	public WorkflowBehaviour(String activityName) {
		this();
		inline = true;
		setBehaviourName(activityName.replace(' ', '-'));
		outgoingTransitions = new OutgoingTransitions();
	}

	private void init(Class c) {
		Class superclass = c.getSuperclass();
		if (!superclass.equals(WorkflowBehaviour.class)) {
			init(superclass);
		}
		// Execute the defineActivities() and defineTransitions() methods of class c
		executeDefineMethod(c, "defineActivities");
		executeDefineMethod(c, "defineTransitions");
	}
	
	private void executeDefineMethod(Class c, String methodName) {
		try {
			Method m = c.getDeclaredMethod(methodName, new Class[0]);
			if (m != null) {
				if (!m.isAccessible()) {
					try {
						m.setAccessible(true);
						m.invoke(this, new Object[0]);
					}
					catch (SecurityException se) {
						throw new WorkflowInitializationException("Method "+methodName+"() of class "+c.getName()+" is not accessible.", se);
					}
					catch (Exception e) {
						throw new WorkflowInitializationException("Error invoking method "+methodName+"() of class "+c.getName(), e);
					}
				}
			}
		}
		catch (NoSuchMethodException nsme) {
			// Method not present in class c. Just do nothing
		}
	}
	
	private class WorkflowInitializationException extends RuntimeException {
		private WorkflowInitializationException(String msg, Exception nested) {
			super(msg, nested);
		}
	}
	
	/**
	 * This method determines whether or not this workflow is long-running (default false).
	 * A long-running workflow survives the a system shutdown and successive restart. 
	 * That is, if a system shutdown occurs while a long-running workflow is in 
	 * progress, the workflow is suspended and as soon as the system restarts the
	 * workflow is automatically restarted.  
	 * In order to enable that a long-running workflow persists its state after the 
	 * execution of each activity that requires that (see HyerarchyNode.requireSave()) 
	 * and before suspending in wait of events through the Workflow Status Manager Agent.
	 */
	public boolean isLongRunning() {
		return false;
	}

	/**
	 * This method determines whether or not this workflow is volatile (default false).
	 * Executions of a volatile workflow are not traced at all by the Workflow Status Manager Agent
	 */
	public boolean isVolatile() {
		return false;
	}

	/**
	 * This method determines whether or not this workflow use the compression for the serializated state (default false).
	 */
	public boolean isCompressionActive() {
		return false;
	}
	
	/**
	 * This method determines whether or not this workflow supports creating TAGS that can 
	 * be successively reloaded thus making the workflow jump back to a previous point (default false).
	 */
	public boolean supportTags() {
		return false;
	}
	
	/**
	 * This method determines whether or not this workflow can be enqueued (default true). 
	 * If it returns false (enqueuing not supported) and a busy WorkflowEngineAgent is requested to
	 * execute this workflow, instead of enqueuing it, the agent will reply with a FAILURE
	 */
	public boolean supportEnqueuing() {
		return true;
	}
	
	boolean isFrozen() {
		return frozen;
	}
	
	private void setFrozen() {
		frozen = true;
		
		// If the current activity is an inline subflow, initialize its frozen flag too.
		// This is important when freeze a workflow
		Behaviour currentBehaviour = getCurrent();
		if (currentBehaviour != null && currentBehaviour instanceof WorkflowBehaviour) {
			 ((WorkflowBehaviour)currentBehaviour).setFrozen();
		}
	}

	public WorkflowDetails getDetails() {
		return workflowDetails; 
	}

	public void SetDetails(WorkflowDetails workflowDetails) {
		this.workflowDetails = workflowDetails;
	}

	/**
	 * The method responsible for initializing the formal parameters list of the workflow process implemented 
	 * by this WorkflowBehaviour.
	 * The default implementation fills the formal parameters list on the basis of the <code>@FormalParameter</code>
	 * annotations 
	 */
	protected void fillFormalParameters() {
		formalParams = new ArrayList();
		EngineHelper.fillFormalParameters(this, WorkflowBehaviour.class, formalParams);
	}
	
	/**
	 * @return The list of formal parameters of the workflow process implemented by this WorkflowBehaviour.
	 */
	public List getFormalParameters() {
		return formalParams;
	}

	protected void setUseDataStore(boolean b) {
		useDataStore = b;
	}
	
	protected void initRootExecutor() {
		rootExecutor = (WorkflowEngineAgent.WorkflowExecutor) root();
		
		// If the current activity is an inline subflow, initialize its rootExecutor too.
		// This is important when deserializing a workflow
		Behaviour currentBehaviour = getCurrent();
		if (currentBehaviour != null && currentBehaviour instanceof WorkflowBehaviour) {
			 ((WorkflowBehaviour)currentBehaviour).initRootExecutor();
		}
	}
	
	/**
	 * This method implements basic workflow initializations. Developers should not redefine or use it. 
	 * In order to perform workflow initialization configurations the <code>configure()</code> method 
	 * should be used instead
	 */
	public void onStart() {
		initRootExecutor();
		try {
			if (!inline) {
				// Normal execution
				getAgent().handleBeginWorkflow(rootExecutor);
			}
			else {
				// Inline subflow execution
				copyInputParams();
				
				getOwner().handleBeginActivity(this);
			}
		}
		catch (Exception e) {
			EngineHelper.fireExecutionErrorEvent(this, e, Constants.SEVERE_LEVEL);
			handleException(e);
		}
	}

	// This is used only for inline subflow execution 
	private void copyInputParams() throws Exception {
		subflow = new Subflow(getClass().getName(), this);
		String methodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.BEFORE_METHOD_TYPE);
		EngineHelper.checkMethodName(methodName, "activity", getBehaviourName());
		MethodInvocator invocator = new MethodInvocator((WorkflowBehaviour) parent, methodName, subflow, Subflow.class);
		invocator.invoke();
		
		// Resolve binding
		manageBindings(subflow);
		
		// At this point subflow includes input actual parameters
		EngineHelper.copyInputParameters(this, subflow.getParams());
	}
	
	public int onEnd() {
		int ret = super.onEnd();

		if (supportTags() && !inline) {
			try {
				WorkflowSerializationManager.deleteTags(this);
			} catch (Exception e) {
				myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Executor "+rootExecutor.getId()+": Error deleting TAGS ");
			}
		}
		
		if (!frozen) {
			try {
				if (!inline) { 
					getAgent().handleEndWorkflow(rootExecutor, ret);
				}
				else {
					manageOutputBindings(subflow);
					restoreOutputParams();
					return EngineHelper.endActivity(this);
				}
			}
			catch (Exception e) {
				// If a workflow execution exception occurred, do not override it 
				if (lastException == null) {
					lastException = e;
				}
			}
		}
		
		return ret;
	}

	// This is used only for inline subflow execution 
	private void restoreOutputParams() throws Exception {
		extractOutputParameters();

		// At this point subflow includes output actual parameters
		String methodName = EngineHelper.activityName2Method(getBehaviourName(), EngineHelper.AFTER_METHOD_TYPE);
		EngineHelper.checkMethodName(methodName, "activity", getBehaviourName());
		MethodInvocator invocator = new MethodInvocator((WorkflowBehaviour) parent, methodName, subflow, Subflow.class);
		invocator.invoke();
	}
	
	// To support introspection
	public final Behaviour getCurrent() {
		return super.getCurrent();
	}

	/**
	 * Set the value of a data-field of the workflow process implemented by this WorkflowBehaviour
	 */
	final void setFieldValue(String key, Object value) {
		if (useDataStore) {
			getDataStore().put(key, value);
		}
		else {
			EngineHelper.setFieldValue(key, value, this, cachedFields);
		}
	}

	/**
	 * Get the value of a data-field of the workflow process implemented by this WorkflowBehaviour
	 */
	public final Object getFieldValue(String key) {
		if (useDataStore) {
			return getDataStore().get(key);
		}
		else {
			return EngineHelper.getFieldValue(key, this, cachedFields);
		}
	}

	/**
	 * Get the class type informations of a data-field of the workflow process implemented by this WorkflowBehaviour
	 */
	public FieldInfo getFieldType(String key) {
		if (useDataStore) {
			// How to get the type of a field in this case is delegated to subclasses that keep 
			// fields in the DataStore. The default implementation just provide a best effort 
			// implementation.
			return null;
		}
		else {
			try {
				Field field = EngineHelper.getField(key, this, cachedFields);
				return new FieldInfo(field.getType(), EngineHelper.getElementTypeClass(field));
			} catch (NoSuchFieldException nsfe) {
				throw new ReflectiveException("Connot find field "+key+" in class "+this.getClass().getName(), nsfe);
			}
		}
	}
	
	/**
	 * Retrieve the default priority of the workflow process implemented by this WorkflowBehaviour.
	 * This default implementation returns <code>Constants.DEFAULT_PRIORITY</code>
	 * Subclasses may redefine this method to specify a different default priority.
	 */
	public int getDefaultPriority() {
		return Constants.DEFAULT_PRIORITY;
	}

	/**
	 * Retrieve the default timeout (in ms) of the workflow process implemented by this WorkflowBehaviour.
	 * This default implementation returns <code>Constants.INFINITE_TIMEOUT</code>
	 * Subclasses may redefine this method to specify a different default timeout.
	 */
	public long getLimit() {
		return Constants.INFINITE_TIMEOUT;
	}
	
	/**
	 * Retrieve the <code>WorkflowDescriptor</code> used to request the execution of the workflow process
	 * implemented by this WorkflowBehaviour
	 */
	public final WorkflowDescriptor getDescriptor() {
		if (!inline) {
			return rootExecutor.getDescriptor();
		}
		else {
			if (inlineDescriptor == null) {
				if (subflow != null) {
					inlineDescriptor = subflow.getDescriptor();
					inlineDescriptor.importInfo(rootExecutor.getDescriptor());
				}
			}
			return inlineDescriptor;
		}
	}
	
	/**
	   @return the execution ID of the workflow under execution
	 */
	public final String getExecutionId() {
		return rootExecutor.getId();
	}
	
	/**
	 * Set a message that will be used as failure reason in the FAILURE response sent back to the requester
	 * in case the execution of the workflow process implemented by this WorkflowBehaviour fails. 
	 * This method is typically used within error activities.  
	 */
	public final void setFailureReason(String reason) {
		rootExecutor.setFailureReason(reason);
	}
	
	/**
	 * Retrieve the last ExecutionErrorEvent, if any, occurred during the execution of this workflow
	 * @return the last ExecutionErrorEvent, if any, occurred during the execution of this workflow
	 */
	public final ExecutionErrorEvent getLastErrorEvent() {
		return rootExecutor.getLastErrorEvent();
	}
	
	/**
	 * @return The <code>Modifier</code> objects that are activated on the workflow
	 * that is executing this Application. Subclasses should use modifiers information 
	 * to customize the behaviour of the execute(), isTransactional(), commit() and rollback()
	 * methods. 
	 */
	public final List getModifiers() {
		return rootExecutor.getModifiers();
	}
	
	public final Modifier getModifier(String name) {
		List modifiers = getModifiers();
		return Modifier.getModifier(name, modifiers);
	}
	
	/**
	   Fire an event of a given type according to the 
	   execution control information of the workflow under execution
	 */
	public final void fireEvent(String type, WorkflowEvent ev, int level) {
		rootExecutor.getEventEmitter().fireEvent(type, ev, level);
	}
	
	public final boolean isFireable(String type, int level) {
        return rootExecutor.getEventEmitter().isFireable(type, level);
    }
	
	/**
	   Subclasses should redefine this method to configure the FSM
	 */
	protected void configure() {
	}

	protected boolean commit() {
		TransactionManager tm = rootExecutor.getTransactionManager();
		if (tm != null) {
			return tm.commit();
		} else {
			return false; 
		}
	}
	
	protected boolean rollback() {
		TransactionManager tm = rootExecutor.getTransactionManager();
		if (tm != null) {
			return tm.rollback();
		} else {
			return false; 
		}
	}

	/**
	 * This method is responsible for creating a user-defined rollback workflow to be executed in
	 * case this workflow must be rolled back. If null is returned the default rollback process is executed. 
	 * The default implementation searches for a workflow whose fully qualified class name is the same
	 * as this workflow plus the "_RB" suffix and returns it if present.
	 * Subclasses may redefine this method to implement application specific rollback workflow loading mechanisms 
	 * @return The user-defined rollback workflow to be executed in
	 * case this workflow must be rolled back or null to use the default rollback process. 
	 */
	protected WorkflowBehaviour getRollbackWorkflow() {
		return getAgent().loadRollbackWorkflow(rootExecutor, getClass().getClassLoader());
	}
	
	/**
	 * Retrieves the TransactionManager object managing commit/rollback information of this workflow.
	 * @return the TransactionManager of this workflow or null if this workflow is not executed transactionally.
	 */
	public final TransactionManager getTransactionManager() {
		return rootExecutor.getTransactionManager();
	}


	// If this WorkflowBehaviour defines a rollback workflow, this method is called by the engine just after the 
	// rollback workflow creation. The interrupted state is transferred to the rollback workflow (in case the 
	// interruption occurred just before the creation of the rollback workflow) and any successive 
	// call to interrupt() will be redirected to the rollback workflow.
	synchronized void setRollbackWorkflow(WorkflowBehaviour wb) {
		rollbackWorkflow = wb;
		rollbackWorkflow.interrupted = interrupted;
		rollbackWorkflow.abortMessage = abortMessage;
	}

	public Hashtable getExecutionContext() {
		return rootExecutor.getExecutionContext();
	}
	
	protected void performApplication(Application appl) throws Exception {
		
		handleBeginApplication(appl, (HierarchyNode)getCurrent());
		
		// Execute the application
		// FIXME: This try/catch block will go away as soon as Application.execute() will throw Exception instead of Throwable
		try {
			appl.checkParameters();
			appl.execute();
		}
		catch (Throwable t) {
			if (t instanceof Exception) {
				throw (Exception) t;
			}
			else {
				throw (Error) t;
			}
		}
		
		handleEndApplication(appl, (HierarchyNode)getCurrent());
		
		// Handle transactional applications
		TransactionManager tm = rootExecutor.getTransactionManager();
		if (tm != null && appl.isTransactional()) {
			// FIXME: check the validity of this transaction entry ID
			String entryId = generateApplicationEntryId(appl.getName());
			tm.addEntry(new ApplicationEntry(entryId, appl, appl.getDataStore()));
		}
		
	}
	
	private java.util.List<Subflow> getOngoingSubflows() {
		java.util.List<Subflow> subflows = new java.util.ArrayList<Subflow>(); 
		Iterator it = getChildren().iterator();
		while (it.hasNext()) {
			Behaviour b = (Behaviour) it.next();
			if (b instanceof SubflowDelegationBehaviour) {
				SubflowDelegationBehaviour sdb = (SubflowDelegationBehaviour) b;
				if (sdb == getCurrent() && !sdb.getSubflow().getAsynch()) {
					subflows.add(sdb.getSubflow());
				} else {
					java.util.Iterator<TerminationNotificationReceiver> it2 = sdb.getAllAsynchronousDelegations();
					while (it2.hasNext()) {
						TerminationNotificationReceiver tnr = it2.next();
						subflows.add(tnr.getSubflow());
					}
				}
			} 
			else if (b instanceof WorkflowBehaviour) {
				WorkflowBehaviour wb = (WorkflowBehaviour) b;
				java.util.List<Subflow> ongoingSubflows = wb.getOngoingSubflows();
				subflows.addAll(ongoingSubflows);
			}
		}
		return subflows;
	}
	
	boolean propagateKill(KillWorkflow kw) throws FIPAException, CodecException, OntologyException {
		java.util.List<Subflow> subflows = getOngoingSubflows();
		
		// If no one subflow is present return false to indicate that the propagation is failed
		if (subflows.isEmpty()) {
			return false;
		}
		
		// Send the kill action to all subflow
		for (Subflow subflow : subflows) {
			
			kw.setExecutionId(subflow.getExecutionId());
			AID performer = new AID(subflow.getPerformer(), AID.ISLOCALNAME);

			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);		
			msg.setOntology(WorkflowManagementOntology.getInstance().getName());
			msg.addReceiver(performer);
			// TODO manage timeout
			//msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

			Action action = new Action();
			action.setActor(performer);
			action.setAction(kw);

			myAgent.getContentManager().fillContent(msg, action);
			
			FIPAService.doFipaRequestClient(myAgent, msg);
		}
			
		return true;
	}
	
	protected void performSubflow(Subflow sbfl) throws Exception {
		manageBindings(sbfl);
				
		// Execute subflow
		WorkflowDescriptor rootDescriptor = rootExecutor.getDescriptor();
		WorkflowDescriptor sbflDescriptor = sbfl.getDescriptor();
		
		sbflDescriptor.importInfo(rootDescriptor);
		
		boolean transactionScope = rootDescriptor.getTransactional() && (!sbfl.getIndependent());
		
		// Update the delegation chain
		String rootDelegationChain = rootDescriptor.getDelegationChain();
		String prefix = (rootDelegationChain != null ? rootDelegationChain+DelegationChainElement.DELEGATION_CHAIN_SEPARATOR : "");
		sbflDescriptor.setDelegationChain(prefix+myAgent.getName()+DelegationChainElement.ELEMENT_SEPARATOR+rootExecutor.getId());
		
		// Propagate the control-information of the enclosing workflow
		Hashtable infos = rootExecutor.getEventEmitter().getControlInfo();
		Object[] tmp = infos.values().toArray();
		List cInfo = new ArrayList(tmp.length);
		for (int i = 0; i < tmp.length; ++i) {
			cInfo.add(tmp[i]);
		}
		
		// Propagate the modifiers
		List modifiers = propagateModifier(sbfl);
		
		// Prepare and send the request to execute the workflow
		int perf = (transactionScope ? ACLMessage.CFP : ACLMessage.REQUEST);
		ACLMessage request = new ACLMessage(perf);
		AID performer = (sbfl.getPerformer() != null ? new AID(sbfl.getPerformer(), AID.ISLOCALNAME) : myAgent.getAID());
		request.addReceiver(performer);		
		String delegationId = generateDelegationId(sbfl.getPerformer(), myAgent.getLocalName());
		request.setConversationId(delegationId);
		request.setOntology(getAgent().getOntology().getName());
		
		//request.setLanguage(getAgent().getLanguage().getName());
		request.setLanguage(LEAPCodec.NAME);
		
		
		// FIXME: Set a proper timeout
		ExecuteWorkflow ew = new ExecuteWorkflow(sbflDescriptor, cInfo);
		ew.setModifiers(modifiers);
		Action aExpr = new Action(performer, ew);
		
		myAgent.getContentManager().fillContent(request, aExpr);
		getAgent().addConversation(request);
		myAgent.send(request);
		MessageTemplate template = MessageTemplate.MatchConversationId(delegationId);
		template = EngineHelper.adjustReplyTemplate(template, request);
		
		SubflowEntry myEntry = null;
		if (transactionScope) {
			TransactionManager tm = rootExecutor.getTransactionManager();
			myEntry = new SubflowEntry(delegationId, getBehaviourName(), myAgent, performer, sbflDescriptor);
			tm.addEntry(myEntry);
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+rootExecutor.getId()+": Added Subflow Entry "+delegationId);
		}

		String delegatedExecutionId = null;
		boolean finished = false;
		ACLMessage reply = null;
		while (!finished) {
			// FIXME: Use a proper timeout
			reply = myAgent.blockingReceive(template);
			switch (reply.getPerformative()) {
			case ACLMessage.AGREE:
				// Delegation accepted
				delegatedExecutionId = reply.getContent();
				sbfl.setExecutionId(delegatedExecutionId);
				sbfl.setPerformer(performer.getLocalName());
				if (sbfl.getIndependent()) {
					finished = true;
				}
				else {
					getAgent().handleDelegatedSubflow(rootExecutor, sbflDescriptor.getId(), sbfl.getPerformer(), delegatedExecutionId);
					if (transactionScope) {
						myEntry.setAgree(reply);
						myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+rootExecutor.getId()+": Set AGREE message to Subflow Entry "+delegationId+". Delegated executionID = "+delegatedExecutionId);
					}
					CAServices.getInstance(myAgent).registerExpectedReply(request);
					
					if (sbfl.getAsynch()) {
						// Don't wait for the result notification in asynchronous delegations. 
						// On the other hand prepare to receive it at a later step
						TerminationNotificationReceiver recv = new TerminationNotificationReceiver(rootExecutor, delegatedExecutionId, delegationId, sbflDescriptor.getId(), template, myEntry);
						addAsynchronousDelegation(recv);
						myAgent.addBehaviour(recv);
						finished = true;
					}
				}
				break;
			case ACLMessage.INFORM:
				getAgent().removeConversation(reply);
				// NO break statement --> Fall through
			case ACLMessage.PROPOSE:
				CAServices.getInstance(myAgent).expectedReplyReceived(reply);
				storeSubflowTransactionalNotification(myEntry, reply);
				
				// Delegated workflow execution successful
				getAgent().handleCompletedSubflow(rootExecutor, delegatedExecutionId, Constants.SUCCESS);
				Result r = (Result) myAgent.getContentManager().extractContent(reply);
				// Put output parameters back in the Subflow 
				sbfl.setParams(r.getItems());
				manageOutputBindings(sbfl);
				finished = true;
				break;
			case ACLMessage.FAILURE:
				getAgent().removeConversation(reply);
				CAServices.getInstance(myAgent).expectedReplyReceived(reply);				
				storeSubflowTransactionalNotification(myEntry, reply);
				
				handleSubflowFailure(delegatedExecutionId, reply);
			default:
				// This covers REFUSE, NOT_UNDERSTOOD and unexpected messages
				getAgent().removeConversation(reply);
				storeSubflowTransactionalNotification(myEntry, reply);
				throw new FailedSubflow("Performer replied with "+ACLMessage.getPerformative(reply.getPerformative()));
			}
		}
	}
	
	protected void handleSubflowFailure(String delegatedExecutionId, ACLMessage reply) throws Exception {
		if (delegatedExecutionId != null) {
			getAgent().handleCompletedSubflow(rootExecutor, delegatedExecutionId, Constants.FAILURE);
		}
		SubflowDelegationBehaviour.handleSubflowFailure(getAgent(), reply);
	}

	protected List propagateModifier(Subflow sbfl) {
		return rootExecutor.getModifiers();
	}

	//ASYNCHSUBFLOW
	private void addAsynchronousDelegation(TerminationNotificationReceiver recv) {
		// Note that when this method is called there is no doubt that the current activity is a SubflowDelegationBehaviour
		SubflowDelegationBehaviour sdb = (SubflowDelegationBehaviour) getCurrent();
		sdb.addAsynchronousDelegation(recv);
	}
	
	private void storeSubflowTransactionalNotification(SubflowEntry entry, ACLMessage msg) {
		if (entry != null) {
			entry.setNotification(msg);
			myLogger.log(Logger.INFO, "Agent "+myAgent.getName()+" - Executor "+rootExecutor.getId()+": Set "+ACLMessage.getPerformative(msg.getPerformative())+" notification to Subflow Entry "+entry.getId());
		}
	}
	
	// Generates a unique id that identifies an application entry on the basis of the name of the 
	// application
	private static long applicationEntryCnt = 0;
	private synchronized static String generateApplicationEntryId(String applName) {
		String id = "A_"+applName+'_'+applicationEntryCnt;
		applicationEntryCnt++;
		return id;
	}	
	// Generates a unique id that identifies a delegation on the basis of the names of the 
	// delegator and delegated agents
	private static long delegationCnt = 0;
	private synchronized static String generateDelegationId(String dlegated, String delegator) {
		String id = "D_"+System.currentTimeMillis()+'-'+delegator+'-'+dlegated+'-'+delegationCnt;
		delegationCnt++;
		return id;
	}	
	
	protected final boolean checkModifier(String name) {
		List modifiers = rootExecutor.getModifiers();
		return Modifier.getModifier(name, modifiers) != null;
	}

	/**
	 * Register an INTERMEDIATE activity within the workflow implemented by this WorkflowBehaviour.
	 * If an activity with the same name already exists it is overridden.
	 */
	public final void registerActivity(HierarchyNode activity) {
		registerActivity(activity, INTERMEDIATE);
	}
	
	/**
	 * Register an activity within the workflow implemented by this WorkflowBehaviour specifying whether this is the INITIAL activity
	 * a FINAL activity or an INTERMEDIATE activity.
	 * If an activity with the same name already exists it is overridden.
	 */
	public final void registerActivity(HierarchyNode activity, int order) {
		boolean isFirst = (order == INITIAL || order == INITIAL_AND_FINAL);
		boolean isLast = (order == FINAL || order == INITIAL_AND_FINAL);
		// If an activity with the same name already exists and is overridden, preserve its outgoing transition
		HierarchyNode previous = (HierarchyNode) getState(activity.getBehaviourName());
		if (previous != null) {
			OutgoingTransitions oldOt = previous.getOutgoingTransitions();
			activity.getOutgoingTransitions().init(oldOt);
		}
		registerState((Behaviour) activity, activity.getBehaviourName());
		activity.setDataStore(getDataStore());
		if (isFirst) {
			registerDefaultTransition(START_STATE, activity.getBehaviourName());
		}
		if (isLast) {
			if (activity.isError()) {
				registerTransition(activity.getBehaviourName(), ERROR_STATE, Constants.DEFAULT_OK_EXIT_VALUE);
			}
			else {
				registerTransition(activity.getBehaviourName(), COLLECT_ASYNCH_SUBFLOWS_STATE, Constants.DEFAULT_OK_EXIT_VALUE);
			}
		}
	}
	
	/**
	 * Deregister the activity with a given name from the workflow process implemented by this WorkflowBehaviour
	 */
	public final HierarchyNode deregisterActivity(String name) {
		return (HierarchyNode) deregisterState(name);
	}
	
	/**
	 * Change the order of a given activity. This is useful when extending a WorkflowBehaviour to 
	 * change the INITIAL or FINAL activities defined in the base workflow.
	 */
	public final void changeActivityOrder(String name, int order) {
		HierarchyNode activity = deregisterActivity(name);
		if (activity != null) {
			registerActivity(activity, order);
			// When de-registering the activity we reset its outgoing JADE transitions. Restore them
			OutgoingTransitions ot = activity.getOutgoingTransitions();
			Iterator it = ot.getTransitions().iterator();
			while (it.hasNext()) {
				Transition t = (Transition) it.next();
				registerTransition(t.getSource(), t.getDestination(), t.getExitValue());
			}
			it = ot.getExceptionTransitions().iterator();
			while (it.hasNext()) {
				Transition t = (Transition) it.next();
				registerTransition(t.getSource(), t.getDestination(), t.getExitValue());
			}
		}
	}
	
	/**
	 * Registers a transition within the workflow implemented by this WorkflowBehaviour.
	 * If a transition from the same source activity to the same destination activity already exists 
	 * it is overridden
	 */
	public final void registerTransition(Transition t, String source, String destination) {
		t.setSource(source);
		t.setDestination(destination);
		HierarchyNode sourceAct = (HierarchyNode) getState(source);
		Transition old = sourceAct.getOutgoingTransitions().putTransition(t);
		if (old != null) {
			deregisterTransition(source, old.getExitValue());
		}
		registerTransition(source, destination, t.getExitValue());
	}
	
	/**
	 * Deregister the transition from a given source activity to a given destination activity 
	 * from the workflow implemented by this WorkflowBehaviour.
	 */
	public final void deregisterTransition(String source, String destination, boolean exception) {
		HierarchyNode sourceAct = (HierarchyNode) getState(source);
		if (sourceAct != null) {
			Transition t = sourceAct.getOutgoingTransitions().removeTransition(destination, exception);
			if (t != null) {
				deregisterTransition(source, t.getExitValue());
			}
		}
	}
	
	/**
	 * Suspend the workflow executor. The Thread executing the workflow terminates after the 
	 * completion of the current activity. This method has no effect if called outside the workflow Thread.
	 */
	public void suspend() { 	
		rootExecutor.suspend();
	}

	/**
	 * Resume the workflow executor after a suspension
	 */
	public void resume() {
		// FIXME: We should check if we are actually suspended
		getAgent().submitWorkflow(rootExecutor);
	}

	void suspendAsap(boolean suspendAsap) {
		this.suspendAsap = suspendAsap;
		
		// If the current activity is an inline subflow, initialize its suspendAsap flag too.
		// This is important when deserializing a workflow
		Behaviour currentBehaviour = getCurrent();
		if (currentBehaviour != null && currentBehaviour instanceof WorkflowBehaviour) {
			 ((WorkflowBehaviour)currentBehaviour).suspendAsap(suspendAsap);
		}
	}
	
	/**
	   Propagate the setting to all its children
	 */
	public void setDataStore(DataStore ds) {
		Iterator it = getChildren().iterator();
		while (it.hasNext()) {
			Behaviour b = (Behaviour) it.next();
			b.setDataStore(ds);
		}  	
		super.setDataStore(ds);
	}


	public final WorkflowEngineAgent getAgent() {
		return (WorkflowEngineAgent) myAgent;
	}

	protected void handleStateEntered(Behaviour state) {
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Agent "+myAgent.getName()+" - WorkflowBehaviour "+getBehaviourName()+": Entering state "+state.getBehaviourName());
		}
		if (state instanceof HierarchyNode) {
			((HierarchyNode) state).reinit();
		}
	}

	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public OutgoingTransitions getOutgoingTransitions() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		return outgoingTransitions;
	}
	
	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public boolean hasJADEDefaultTransition() {
		// Outgoing transitions (including the default one if any) make no sense if this is a top level workflow
		if (!inline) {
			return false;
		}
		else {
			return ((WorkflowBehaviour) parent).hasDefaultTransition(getBehaviourName());
		}
	}
	
	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public void mark() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		if (inline) {
			executed = true;
		}
	}

	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public void reinit() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		if (inline && executed) {
			reset();
		}
	}

	@Override
	public void reset() {
		if (inline) {
			executed = false;
			lastException = null;
			inlineDescriptor = null;
			
			// Reset managed fields 
			if (managedFieldNames == null) {
				managedFieldNames = EngineHelper.initManagedFields(this, WorkflowBehaviour.class);
			}
			for (String fieldName : managedFieldNames) {
				setFieldValue(fieldName, null);
			}
		}
		super.reset();
	}
	
	////////////////////////////////////////////
	// Exception handling 
	////////////////////////////////////////////
	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public boolean isError() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		return errorActivity;
	}

	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public void setError(boolean b) {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		if (inline) {
			errorActivity = true;
		}
	}

	/**
	 * This method returns a meaningful Exception in case the workflow failed and null otherwise.
	 * Therefore it can be used (typically in the onEnd() method) to univokely test if the 
	 * workflow succeeded or failed.
	 * More in details the following cases are possible<br/>
	 * 1) The workflow was killed or timed out: the method returns an Aborted instance<br/>
	 * 2) The workflow FSM is inconsistent: the method returns a generic WorkflowException instance. If the 
	 * inconsistence depends on an uncaught exception occurred in an activity, such exception is nested in the
	 * WorkflowException.<br/>
	 * 3) There was an exception in an initialization method redefined by the user (e.g. onStart()): the method 
	 * returns the occurred exception.  
	 * 4) The workflow terminated in an activity explicitly marked as ERROR: the method returns a TerminatedInErrorActivity instance<br/>
	 * NOTE that, while cases 1, 2 and 3 correspond to un-managed flow (and not activity) problems, 
	 * case 4 corresponds to a failure by design. Very often this is the consequence of a caught exception 
	 * occurred in an activity. In these cases such exception can be retrieved by means of the 
	 * getLastErrorEvent() method.  
	 */
	public final Throwable getLastException() {
		return lastException;
	}

	/**
	 * This method is used internally by the framework. Developers should not call it.
	 */
	public final void handleException(Throwable t) {
		if (frozen) {
			// Make the FSM terminate in the FROZEN_STATE
			forceTransitionTo(FROZEN_STATE);
		} else {
			// Store the exception and make the FSM terminate in the ERROR_STATE
			lastException = t;
			
			forceTransitionTo(ERROR_STATE);
		}
	}

	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public void propagateException(Throwable t) {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		if (inline) {
			((HierarchyNode) parent).handleException(t);
		}
	}

	/**
	 * This method is used internally by the framework. Developers should not call it.
	 */
	protected void handleInconsistentFSM(String current, int exitValue) {
		// The FSM is inconsistent or there was an unexpected exception 
		// in the current state
		Throwable t = null;
		HierarchyNode node = null;
		if (current.equals(START_STATE)) {
			node = this;
			t = new WorkflowException("Inconsistent FSM: missing initial activity");
		}
		else {
			node = (HierarchyNode) getState(current);
			Throwable nested = node.getLastException();
			if (nested != null) {
				t = new WorkflowException("Uncaught exception in activity  "+current, nested);
			}
			else {
				t = new WorkflowException("Inconsistent FSM: activity "+current+", event "+exitValue);
			}		
		}

		EngineHelper.fireExecutionErrorEvent(node, t, Constants.SEVERE_LEVEL);

		// Store the Exception and force a transition to the error state 
		handleException(t);
		// Re-schedule so that the forced transition is actually fired
		scheduleNext(true, 0);
	}	

	//////////////////////////////////////////////
	// Workflow interruption section
	//////////////////////////////////////////////
	public boolean enterInterruptableSection() {
		if (!inline) {
			synchronized (this) {
				if (!interruptable) {
					if (interrupted) {
						// This WorkflowBehaviour was interrupted outside an interruptable section. Now make the interruption take effect
						throw new Agent.Interrupted();
					}
					interruptable = true;
					return true;
				}
				return false;
			}
		}
		else {
			// Propagate to the parent 
			return ((WorkflowBehaviour) parent).enterInterruptableSection();
		}
	}
	
	public void exitInterruptableSection(HierarchyNode current) {
		if (!inline) {
			synchronized (this) {
				interruptable = false;
				// If this WorkflowBehaviour was interrupted from the outside, create an ad-hoc Aborted exception, 
				// reset the interrupted state and notify that the interruption  took effect 
				if (interrupted) {
					myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - WorkflowBehaviour "+getExecutionId()+" interrupted");
					if (current != null) {
						current.setInterrupted();
						// Possibly the interruption caused an exception different than InterruptedException, Agent.Interrupted or ThreadDeath
						handleException(new Aborted(abortMessage, current.getLastException()));
					}
					else {
						handleException(new Aborted(abortMessage));
					}
					clearInterruption();
					// In case the workflow suspended itself at the same time of the interruption, cancel the suspension.
					// Note that this call has no effect in all other cases
					rootExecutor.resume();
					notifyAll();
				}
			}
		}
		else {
			// FIXME: Should we handle the last Exception (if any) in the current activity?
			// Propagate to the parent 
			((WorkflowBehaviour) parent).exitInterruptableSection(this);			
		}
	}
	
	void clearInterruption() {
		// Reset the interrupted flag and the abortMessage
		interrupted = false;
		abortMessage = null;
		// Also clear the Thread interrupted state in case it is set
		Thread.interrupted();
	}
	
	@SuppressWarnings("deprecation")
	synchronized void interrupt(WorkflowEngineAgent.WorkflowExecutor executor, ThreadedBehaviourFactory tbf, String abortMessage, boolean frozen) {
		if (rollbackWorkflow != null) {
			// My rollback workflow is in execution --> redirect the interrupt() to it
			rollbackWorkflow.interrupt(executor, tbf, abortMessage, frozen);
		}
		else {
			// If the interrupt is caused by a freeze -> mark the subflow as frozen
			if (frozen) {
				setFrozen();
			}

			this.abortMessage = abortMessage;
			interrupted = true;
			if (interruptable) {
				// Interrupt (stop if necessary) the executor Thread. 
				try {
					// Note that we don't use tbf.interrupt() since we don't want the executor's Thread to terminate.
					// Note also that t can't be null within an interruptable section
					Thread t = tbf.getThread(executor);
					t.interrupt();
					// Wait for the interruption to take effect
					wait(10000);
					if (interrupted) {
						// The interruption did not take effect (in that case the interrupted flag would have been reset) --> Try with Thread.stop()
						myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - Workflow "+getExecutionId()+" did not react to interruption. Stop it!");
						t.stop();
					}
				}
				catch (InterruptedException ie) {
					// This may happen if the Agent is killed while interrupting a workflow. We just print a stack trace and go on.
					ie.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public boolean isInterrupted() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		return inlineInterrupted;
	}
	
	/**
	 * This method is used internally by the framework. Developers should not call or redefine it.
	 */
	public void setInterrupted() {
		// This makes sense (and can be called) only if this workflow is used as an inline subflow
		if (inline) {
			// The parent Workflow was interrupted --> set the interrupted state, force a transition to 
			// the error state and notify downwards 
			inlineInterrupted = true;
			forceTransitionTo(ERROR_STATE);
			((HierarchyNode) getCurrent()).setInterrupted();
		}
	}
	
	protected boolean checkTermination(boolean currentDone, int currentResult) {
		
		// If this is a long-running workflow and the save is required -> save the state
		if (currentDone) {
			// If the workflow was requested to suspend as soon as possible, do it
			if (suspendAsap) {
				suspend();
				suspendAsap(false);
			}
			// If the workflow needs to be saved, do it
			if (!isVolatile() && (isLongRunning() || supportTags())) {
				// Avoid saving the state if the current activity was just interrupted
				// Save only if current activity requires save or if it is the hidden start state 
				// (this is necessary for instance to thaw a wf that was frozen in the initial activity)
				if (getCurrent().getBehaviourName().equals(START_STATE) || 
				   (getCurrent() instanceof HierarchyNode && ((HierarchyNode)getCurrent()).requireSave() && !((HierarchyNode)getCurrent()).isInterrupted())) {
					WorkflowSerializationManager.save(this);
				}
			}
		}
		
		// The following code is for debugging purposes only
		boolean b = super.checkTermination(currentDone, currentResult);
		boolean b1 = false;
		if (currentDone) {
			b1 = lastStates.contains(currentName);
		}
		if (b != b1) {
			myLogger.log(Logger.WARNING, "Agent "+myAgent.getName()+" - WorkflowBehaviour "+getBehaviourName()+": FSM.checkTermination() returned "+b+" with Last-states = "+lastStates+" and currentName = "+currentName);
		}
		return b1;
	}

	@Override
	protected void scheduleNext(boolean currentDone, int currentResult) {
		if (thawed) {
			manageThawed();
		}
		
		super.scheduleNext(currentDone, currentResult);
	}
	
	//////////////////////////////////////
	// Methods and fields retrieval
	//////////////////////////////////////
	
	private transient Map<String, Method> cachedMethods = new HashMap<String, Method>();	
	
	/**
	 * Retrieve a method with a given name and param types (possibly declared in a superclass)
	 * making it accessible if necessary
	 */
	Method getMethod(String methodName, Class[] paramTypes) throws NoSuchMethodException {
		Method m = cachedMethods.get(methodName);
		if (m == null) {
			m = getMethod(methodName, this.getClass(), paramTypes);
			if (m != null) {
				if (!m.isAccessible()) {
					try {
						m.setAccessible(true);
					}
					catch (SecurityException se) {
						throw new NoSuchMethodException("Method "+methodName+"() of class "+getClass().getName()+" not accessible.");
					}
				}
				cachedMethods.put(methodName, m);
			}
			else {
				throw new NoSuchMethodException("Method "+methodName+"() not found class "+getClass().getName());
			}
		}
		return m;
	}
	
	Method getMethod(String methodName, Class currentClass, Class[] paramTypes) throws NoSuchMethodException {
		Method m = null;
		// Search first in the current class
		try {
			m = currentClass.getDeclaredMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException nsme) {
			// Method not declared in the current class --> Try in the superclasses
			Class superClass = currentClass.getSuperclass();
			if (!FSMBehaviour.class.equals(superClass)) {
				m = getMethod(methodName, superClass, paramTypes);
			}
		}
		return m;
	}
	
	private transient Map<String, Field> cachedFields = new HashMap<String, Field>();
	
	/**
	 * Initialize local fields (including those defined in superclasses) with values taken from
	 * matching (name and type) fields in sourceWorkflow.
	 * This is used when activating a rollback workflow to give it access to the data-fields as they were
	 * left by the direct workflow
	 */ 
	void copyFieldsValue(WorkflowBehaviour sourceWorkflow) {
		copyFields(sourceWorkflow, this.getClass());
	}
	
	private void copyFields(WorkflowBehaviour sourceWorkflow, Class currentClass) {
		// First recursively copy fields declared in super classes
		Class superClass = currentClass.getSuperclass();
		if (!WorkflowBehaviour.class.equals(superClass)) {
			copyFields(sourceWorkflow, superClass);
		}
		// Then copy fields declared in the current class
		Field[] myFields = currentClass.getDeclaredFields();
		for (int i = 0; i < myFields.length; ++i) {
			Field myField = myFields[i];
			String fieldName = myField.getName();
			try {
				Field f = EngineHelper.getField(fieldName, sourceWorkflow, sourceWorkflow.cachedFields);
				if (f.getType().equals(myField.getType())) {
					// A field with the same name and type (class) exists in the sourceWorkflow --> Copy its value
					try {
						Object value = f.get(sourceWorkflow);
						myField.setAccessible(true);
						myField.set(this, value);
					}
					catch (Exception e) {
						myLogger.log(Logger.WARNING, "Cannot copy value of field "+fieldName+". "+e.getMessage());
					}
				}
			}
			catch (NoSuchFieldException nsfe) {
				// This field is not defined in the sourceWorkflow --> Nothing to copy
			}
		}
	}
	
	/**
	   This method can be called by an Application to generate messages
	   that can be traced by a remote controller. The latter must be
	   specified in the ControlInfo object associated to the 
	   Constants.TRACING_TYPE type of events
	 */
	public final void trace(int level, String msg) {
		rootExecutor.getTracer().trace(level, msg);
	}

	public final void trace(String msg) {
		trace(Constants.INFO_LEVEL, msg);
	}

	public Tracer getTracer() {
		return rootExecutor.getTracer();
	}
	
	protected void performBuildingBlock(InvocableBuildingBlock ibb) throws Exception {
		manageBindings(ibb);
		
		ibb.invoke();
		
		manageOutputBindings(ibb);
	}

	protected void performRestService(RestService rs) throws Exception {
		performBuildingBlock(rs);
	}
	
	protected void performWebService(WebService ws) throws Exception {
		performBuildingBlock(ws);
	}

	protected void performDynamicWebService(DynamicWebService dws) throws Exception {
		performBuildingBlock(dws);
	}
	
	public BindingManager getBindingManager() {
		if (bindingManager == null) {
			bindingManager = new BindingManager(this);
		}
		return bindingManager;
	}

	private BindingManager getBindingManager(String activityName) throws BindingException {
		BindingManager bindingManager = null; 
		HierarchyNode activity = (HierarchyNode) getState(activityName);
		if (activity != null) {
			bindingManager = activity.getBindingManager();
		} else {
			throw new BindingException("Activity "+activityName+" not present in "+getBehaviourName());
		}
		return bindingManager;
	}
	
	protected AbsObject getAbsValue(String activityName, String key, int mode, String part) throws Exception {
		BindingManager bm = getBindingManager(activityName);
		return bm.getAbsValue(key, mode, part);
	}
	
	/**
	 * Retrieve the value of a parameter (or a part of it) of a given BuildingBlock 
	 * used in a given activity
	 * @param activityName The name of the activity 
	 * @param key The key identifying the parameter. For activities managing a single 
	 * BuildingBlock (such as WebService or Subflow activities) this key is just 
	 * the parameter name. For activities managing more than one BuildingBlock (such as
	 * Tool or WaitEvent activities) this key has the form <code>param-name@building-block-id</code>.
	 * The building-block-id depends on the activity. Look at the specific activity type 
	 * javadoc for details. 
	 * @param mode Determines whether the parameter is an input parameter (Constants.IN_MODE),
	 * an output parameter (Constants.OUT_MODE) or an input/output parameter (Constants.INOUT_MODE).
	 * @param part Defines the part within the parameter whose value must be retrieved. For
	 * instance, for a parameter of class <code>Person</code> including attributes <code>name</code> 
	 * and <code>age</code>, it is possible to retrieve the value of the <code>age</code> 
	 * attribute only by specifying "age" as <code>part</code>. 
	 * If part is null the value of the whole parameter is returned. 
	 * @return The value of the indicated parameter (or a part of it)
	 * @throws Exception
	 */
	protected Object getValue(String activityName, String key, int mode, String part) throws Exception {
		BindingManager bm = getBindingManager(activityName);
		return bm.getValue(key, mode, part);
	}
	
	/**
	 * Retrieve the case occurred in a Wait-Multiple-Cases activity 
	 * @param activityName The name of the Wait-Multiple-Cases activity
	 * @return The ID of the occurred case.
 	 * @throws IllegalArgumentException If the indicated activity does not exist or 
 	 * does not represent a Wait-Multiple-Cases activity
	 */
	protected String getOccurredCase(String activityName) throws IllegalArgumentException {
		HierarchyNode activity = (HierarchyNode) getState(activityName);
		if ((activity != null) && (activity instanceof WaitMultipleCasesBehaviour)) {
			return ((WaitMultipleCasesBehaviour) activity).getOccurredCase();
		}
		else {
			throw new IllegalArgumentException("Activity "+activityName+" does not exist or is not a WaitMultipleCases activity");
		}
	}
	
	protected void manageBindings(BuildingBlock bb) throws Exception {
		// Resolve bindings
		bb.getActivity().getBindingManager().resolveBindings(bb);
		
		// Handle default parameters
		handleDefaultParameters(bb);
				
		// Handle ungrounded parameters
		handleUngroundedParameters(bb);
		
		// Convert parameters into correct type
		bb.getActivity().getBindingManager().convertParameters(bb);
	}

	protected void manageOutputBindings(BuildingBlock bb) throws Exception {
		getBindingManager().resolveOutputBindings(bb);
	}
	
	protected void handleDefaultParameters(BuildingBlock bb) throws Exception {
		
		// Get default values modifier
		DefaultParameterValues dpv = EngineHelper.extractDefaultParameterValues(this);
		if (dpv != null) {
			for (String parameterName : bb.getInputParameterNames()) {
				Object parameterValue = bb.getInput(parameterName);
				java.util.List<DefaultValue> defaultValues = dpv.getValues(bb.getActivity().getBehaviourName(), parameterName);
				if (defaultValues != null && defaultValues.size() > 0) {
				
					// Set default values (if any) only if:
					// - parameter is null 
					//   (null significa che non è mai stato fillato/bindato oppure è stato assegnato a null;
					//    anche se sarebbe possibile sapere se è stato assegnato con BuildingBlock.isInputEmpty  
					//    per essere coerenti con l'impossibilità di sapere se uno slot è stato assegnato a null 
					//    viene genericamente assunto che: un parametro o uno slot a null significa indeterminazione
					//    e quindi il valore di default può sovrascriverlo)
					// - parameter is an AbsObject not grounded
					AbsObject parameterAbsValue = null;
					boolean applyDefaults = false;
					if (parameterValue == null) {
						applyDefaults = true;
					} else if (parameterValue != null && parameterValue instanceof AbsObject) {
						parameterAbsValue = (AbsObject)parameterValue;
						if (!parameterAbsValue.isGrounded()) {
							applyDefaults = true;
						}					
					}
					if (applyDefaults) {
						if (parameterValue == null) {
							// If parameter is null replace it with the template
							parameterAbsValue = bb.createAbsTemplate(parameterName);
						}
						
						// Loop all defaults
						for (DefaultValue defaultValue : defaultValues) {
							String defPart = defaultValue.getPart();
							Object defValue = defaultValue.getValue();

							// Get parameter part template
							AbsObject templateAbsValue;
							if (defPart == null) {
								templateAbsValue = parameterAbsValue;
							} else {
								templateAbsValue = BindingManager.getAttribute(parameterAbsValue, defPart);
							}
							
							// Apply default to template
							AbsObject defAbsValue = bb.getOntology().fromObject(defValue);
							AbsObject defaultedAbsValue = AbsHelper.applyDefaultValues(templateAbsValue, defAbsValue);
							
							// Set parameter defaulted part 
							if (defPart == null) {
								parameterAbsValue = defaultedAbsValue;
								myLogger.log(Logger.INFO, "Total default value of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName+" = "+parameterAbsValue);
								// Un default su tutto il parametro vince su tutti gli altri 
								// e quindi non ha più senso proseguire con gli altri default  
								break;
							} else {
								BindingManager.setAttribute(parameterAbsValue, defPart, defaultedAbsValue);
								myLogger.log(Logger.INFO, "Partial default of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName+"."+defPart+" = "+defaultedAbsValue);
							}
						}
						
						// Fill parameter
						bb.setInput(parameterName, parameterAbsValue);
					}
				}
			}
		}
	}
	
	protected void handleUngroundedParameters(BuildingBlock bb) throws Exception {
		
		for (String parameterName : bb.getInputParameterNames()) {
			Object parameterValue = bb.getInput(parameterName);
			
			// If parameter is an Abs check if is not grounded 
			if (parameterValue != null && parameterValue instanceof AbsObject) {
				AbsObject parameterAbs = (AbsObject)parameterValue;
				if (!parameterAbs.isGrounded()) {

					// Put to null all AbsVariable
					bb.setInput(parameterName, AbsHelper.nullifyVariables(parameterAbs, false));
				}
			}
		}
	}

	public BuildingBlock getBuildingBlock(String id) {
		if (inline && subflow != null) {
			// In this case this is the behaviour that execute the inline subflow
			return subflow;
		}
		return null;
	}

	protected void handleBeginActivity(HierarchyNode activity) {
		getAgent().handleBeginActivity(rootExecutor, activity.getBehaviourName());
	}
	
	protected void handleEndActivity(HierarchyNode activity) {
		activity.getAgent().handleEndActivity(rootExecutor, activity.getBehaviourName());
	}

	protected void handleBeginApplication(Application appl, HierarchyNode activity) {
		getAgent().handleBeginApplication(rootExecutor, appl.getName());
	}

	protected void handleEndApplication(Application appl, HierarchyNode activity) {
		getAgent().handleEndApplication(rootExecutor, appl.getName());
	}
	
	public WorkflowBehaviour getOwner() {
		if (inline) {
			return (WorkflowBehaviour)getParent();
		}
		return null;
	}
	
	/**
	 * Get (create if not present) the ontology associated to 
	 * workflow formal parameters  
	 */
	public Ontology getOntology() throws Exception {
		if (onto == null) {
			onto = createOntology(); 
		}
		return onto;
	}
	
	private Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("WfFormalParametersOnto");

		jade.util.leap.List formalParams = getFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter param = (Parameter)it.next();
			OntologyUtils.addFormalParameterToOntology(onto, param, getClass().getClassLoader());
		}

		return onto;
	}

	public void onSuspended() {
	}

	public void onResumed() {
	}

	protected void onFrozen() {
	}

	protected void onThawed() {
	}
	
	protected void manageThawed() {
		if (inline) {
			((WorkflowBehaviour) parent).manageThawed();
		} else {
			onThawed();
			setThawed(false);
		}
	}
	
	void setThawed(boolean thawed) {
		this.thawed = thawed;
		
		// If the current activity is an inline subflow propagate the thawed flag.
		Behaviour currentBehaviour = getCurrent();
		if (currentBehaviour != null && currentBehaviour instanceof WorkflowBehaviour) {
			 ((WorkflowBehaviour)currentBehaviour).setThawed(thawed);
		}
	}

	public boolean requireSave() {
		return requireSave;
	}
	
	public void setRequireSave(boolean requireSave) {
		this.requireSave = requireSave;
	}
	
	protected void tag(String tagName) throws Exception {
		WorkflowSerializationManager.tag(tagName, this);
	}
		
	protected void reloadTag(String tagName) throws Exception {
		WorkflowSerializationManager.reloadTag(tagName, this);
	}

	/**
	 * This method is called to every event notifications and permit a event customization 
	 */
	protected WorkflowEvent customizeEvent(String id, long time, String type, WorkflowEvent ev, List controllers) {
		return ev;
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		
		cachedFields = new HashMap<String, Field>();
		cachedMethods = new HashMap<String, Method>();
	}
	
	protected boolean isInline() {
		return inline;
	}

	protected List extractOutputParameters() throws WorkflowException {
		List parameters = getDescriptor().getParameters();
		EngineHelper.extractOutputParameters(this, parameters);
		return parameters;
	}
	
	protected final boolean isSubflow() {
		if (inline) {
			return true;
		} else {
			return getDescriptor().getDelegationChain() != null;
		}
	}
}

