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

import jade.core.behaviours.DataStore;
import jade.util.Logger;
import jade.util.leap.List;

import java.util.Hashtable;
import java.util.Iterator;

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.ExecutionErrorEvent;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.ontology.Modifier;
import com.tilab.wade.performer.transaction.TransactionManager;

/**
 * Base class for applications that can be executed in a Tool activity of a workflow
 * This class SHOULD NOT be extended directly by developers to create domain specific Applications.
 * The <code>BaseApplication</code> class should be used instead.
 */
public abstract class Application {
	protected Logger myLogger = Logger.getMyLogger(BaseApplication.class.getName());
	
	/**
	 * The <code>WorkflowEngineAgent</code> executing the workflow
	 * this application is invoked from. This allows applications 
	 * to access global agent data and features such as sending/receiving messages
	 */
	protected WorkflowEngineAgent myAgent;

	/**
	 * The execution ID of the process that invokes this Application object;
	 */
	protected String myExecutionId;

	/**
  	 * The session ID of the process that invokes this Application object;
	 */
	protected String mySessionId;


	/**
	 * The list of formal parameters of this application. Subclasses should fill this 
	 * list in their constructors.
	 */
	protected List formalParams = null;
	
	protected String myName;
	
	private WorkflowEngineAgent.WorkflowExecutor myExecutor;
	private DataStore myStore;

	public Application() {
		this(null, new DataStore());
	}

	public Application(String n, DataStore ds) {
		myName = n;
		myStore = ds;
	}

	public void fill(String key, Object value) {
		myStore.put(key, value);
	}

	public final void fill(String key, int value) {
		fill(key, new Integer(value));
	}

	public final void fill(String key, long value) {
		fill(key, new Long(value));
	}

	public final void fill(String key, boolean value) {
		fill(key, new Boolean(value));
	}

	public final void fill(String key, float value) {
		fill(key, new Float(value));
	}

	public final void fill(String key, double value) {
		fill(key, new Double(value));
	}

	/**
	   Set the actual value of output parameter <code>key</code>
	 */
	public final void set(String key, Object value) {
		myStore.put(key, value);
	}

	/**
	   Retrieve the actual value of input parameter <code>key</code>
	 */
	public Object extract(String key) {
		Object value = myStore.get(key);
		if (!myStore.containsKey(key)){
			myLogger.log(Logger.FINE, "Application "+getClass().getSimpleName()+": parameter "+key+" not assigned");
		}
		if (!myStore.containsKey(key) || myStore.get(key)==null){
			//if key type is a wrapper, assign to key its primitive default value;
			String type = getParamType(key);
			if (type != null){
				value = EngineHelper.getDefaultValue(type);
				
			}			
		}
		return value;
	}

	public final Object get(String key){
		return myStore.get(key);
	}
	
	private String getParamType(String key){
		String type = null;
		for (int i=0; i<formalParams.size(); i++){
			Parameter param = (Parameter)formalParams.get(i);
			if (param.getName().equals(key))
				type = param.getType();
		}
		return type;
	}

	public Object getValid(String key) {
		Object obj = myStore.get(key);
		if (obj != null) {
			// FIXME: Should check the type
			return obj;
		}
		else {
			throw new RuntimeException("Application "+getClass().getName()+": parameter "+key+" not assigned");
		}
	}
	
	protected void checkParameters(){
		if (formalParams != null){
			for (int i=0; i<formalParams.size(); i++){
				Parameter param = (Parameter)formalParams.get(i);
				if ((param.getMode() == Constants.IN_MODE || param.getMode() == Constants.INOUT_MODE) && !myStore.containsKey(param.getName())){
					StringBuffer sb = new StringBuffer("DataStore {");
					Iterator<String> iter = myStore.keySet().iterator();
					String k;
					Object v;
					while (iter.hasNext()) {
						k = iter.next();
						v = myStore.get(k);
						sb.append(k);
						sb.append('=');
						sb.append(v);
						sb.append(' ');
					}
					sb.append('}');
					throw new IllegalArgumentException("Application "+getClass().getName()+": parameter "+param.getName()+" not assigned in input");
				}
				
			}
		}
	}


	public final List getFormalParameters() {
		return formalParams;
	}

	final void setAgent(WorkflowEngineAgent a) {
		myAgent = a;
	}

	final void setExecutor(WorkflowEngineAgent.WorkflowExecutor we) {
		myExecutor = we;
		myExecutionId = myExecutor.getId();
		mySessionId = myExecutor.getDescriptor().getSessionId();
	}

	final String getName() {
		return (myName != null ? myName : getClass().getName());
	}

	/**
	 * This method is called by the transaction mechanism only and must 
	 * not be used by developers.
	 */
	public void setDataStore(DataStore ds) {
		myStore = ds;
	}

	public DataStore getDataStore() {
		return myStore;
	}
	
	
	/**
	 * @return The <code>WorkflowDescriptor</code> of the workflow that is invoking this application.
	 */
	public WorkflowDescriptor getWorkflowDescriptor(){
		return myExecutor.getDescriptor();
	}
	
	/**
	 * Execute this Application
	 */
	public abstract void execute() throws Throwable;

	/**
	 * States whether or not this Application is transactional
	 */
	public boolean isTransactional() {
		return false;
	}

	/**
	 * Commit this Application
	 */
	public void commit() throws Throwable {
	}

	/**
	 * Rollback this Application
	 */
	public void rollback() throws Throwable {
	}

	/**
	 * @return The <code>Modifier</code> objects that are activated on the workflow
	 * that is executing this Application. Subclasses should use modifiers information 
	 * to customize the behaviour of the execute(), isTransactional(), commit() and rollback()
	 * methods. 
	 */
	public final List getModifiers() {
		return myExecutor.getModifiers();
	}

	public final Modifier getModifier(String name) {
		List modifiers = getModifiers();
		return Modifier.getModifier(name, modifiers);
	}

	/**
	   This method can be called by an Application to generate messages
	   that can be traced by a remote controller. The latter must be
	   specified in the ControlInfo object associated to the 
	   Constants.TRACING_TYPE type of events
	 */
	public final void trace(int level, String msg) {
		myExecutor.getTracer().trace(level, msg);
	}

	public final void trace(String msg) {
		trace(Constants.INFO_LEVEL, msg);
	}
	
	public Tracer getTracer() {
		return myExecutor.getTracer();
	}
	
	/**
	   Fire an event of a given type according to the 
	   execution control information of the workflow under execution
	 */
	public final void fireEvent(String type, WorkflowEvent ev, int level) {
		myExecutor.getEventEmitter().fireEvent(type, ev, level);
	}

	public final TransactionManager getTransactionManager() {
		return myExecutor.getTransactionManager();
	}

	public Hashtable getControlInfo() {
		return myExecutor.getEventEmitter().getControlInfo();
	}


	/**
	 * This method allows retrieving the last <code>ExecutionErrorEvent</code> (if any) fired by 
	 * the execution of the workflow that called this <code>Application</code>.
	 * ExecutionErrorEvent-s are fired 
	 * i) when an exception occurs executing an <code>Application</code>,
	 * ii) when a subflow delegation fails, 
	 * iii) when application/subflow parameter passing/restoring fails,
	 * iv) when subflow performer selection fails,
	 * v) when transition evaluation fails
	 * vi) when the FSM is inconsistent 
	 */
	public final ExecutionErrorEvent getWorkflowLastErrorEvent() {
		return myExecutor.getLastErrorEvent();
	}

	/**
	 * This method allows setting a meaningful 
	 * message that will be returned to the requester of the execution of the workflow that 
	 * called this <code>Application</code> in case that workflow fails.
	 */
	public final void setWorkflowFailureReason(String reason) {
		myExecutor.setFailureReason(reason);
	}

	/**
	 * This method allows retrieving the <code>failure-reason</code> (if any) set for the workflow
	 * that called this <code>Application</code>.
	 * @see #setWorkflowFailureReason(String)
	 */
	public final String getWorkflowFailureReason() {
		return myExecutor.getFailureReason();
	}
	
	/**
	 * Retrieve the execution context (in form of an Hashtable) of the WorkflowExecutor.
	 * @return The execution context of the WorkflowExecutor.
	 */
	public Hashtable getExecutionContext() {
		return myExecutor.getExecutionContext();
	}
}
