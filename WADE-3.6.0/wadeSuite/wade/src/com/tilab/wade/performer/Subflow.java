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

import java.lang.reflect.Constructor;

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.Ontology;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;

/**
 * The building-block corresponding to the execution (possibly delegated to a different agent) of another workflow.
 * @see SubflowDelegationBehaviour
 * @see SubflowJoinBehaviour
 * @see WorkflowBehaviour
 */
public class Subflow extends BuildingBlock {
	private String subflowId;
	private String format = null;
	private String representation = null;
	private String performer;
	private boolean asynch = false;
	private boolean independent = false;
	private List params = new ArrayList();
	private String executionId;
	
	public Subflow(HierarchyNode activity) {
		this(null, activity);
	}
	
	public Subflow(String subflowId, HierarchyNode activity) {
		super(activity);
		
		this.subflowId = subflowId;
	}

	// Only used to manage SubflowJoinBehaviour subflow list (see: handleAsynchronousFlowSuccess)
	public void setActivity(HierarchyNode activity) {
		this.activity = activity;
	}

	public String getSubflowId() {
		return subflowId;
	}
	
	public void setSubflowId(String subflowId) {
		this.subflowId = subflowId;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getRepresentation() {
		return representation;
	}

	public void setRepresentation(String representation) {
		this.representation = representation;
	}
	
	/**
	 * Fill an input parameter
	 * The fill() methods are called by the Engine just before executing this subflow 
	 */
	public final void fill(String key, Object value) {
		Parameter p = new Parameter(value);
		p.setName(key);
		params.add(p);
	}

	public final void fill(String key, int value) {
		fill(key,  new Integer(value));
	}

	public final void fill(String key, long value) {
		fill(key,  new Long(value));
	}

	public final void fill(String key, boolean value) {
		fill(key,  new Boolean(value));
	}

	public final void fill(String key, float value) {
		fill(key,  new Float(value));
	}

	public final void fill(String key, double value) {
		fill(key,  new Double(value));
	}

	/**
	 * @deprecated Use extract() instead
	 */
	public final Object get(String key) {
		return extract(key);
	}	

	/**
	   Retrieve the value of an output parameter after the execution of the subflow
	 */
	public final Object extract(String key) {
		Object value = null;
		for (int i=0; i<params.size(); i++){
			Parameter param = (Parameter)params.get(i);
			if (param.getName().equals(key)){
				if (int.class.getName().equals(param.getType()) && param.getValue() instanceof Long){
					param.setValue(new Integer(((Long)param.getValue()).intValue()));					
				}else if (float.class.getName().equals(param.getType()) && param.getValue() instanceof Double){
					param.setValue(new Float(((Double)param.getValue()).floatValue()));					
				}
				value = param.getValue();
				break;
			}
		}
		return value;
	}
	
	public void setPerformer(String performer) {
		this.performer = performer;
	}
	
	public String getPerformer() {
		return performer;
	}
	
	public void setAsynch(boolean  asynch) {
		this.asynch = asynch;
	}
	
	public boolean getAsynch() {
		return asynch;
	}
	
	public void setIndependent(boolean  independent) {
		this.independent = independent;
	}
	
	public boolean getIndependent() {
		return independent;
	}
	
	List getParams() {
		return params;
	}
	
	void setParams(List params) {
		this.params = params;
	}
	
	WorkflowDescriptor getDescriptor() {
		WorkflowDescriptor dsc = new WorkflowDescriptor(subflowId, params);
		dsc.setFormat(format);
		dsc.setRepresentation(representation);
		if (asynch && independent) {
			// Note that, only in the case that this Subflow object represents an asynchornous independent subflow, 
			// the delegation is actually asynchronous. In all other cases in facts the delegating workflow needs to 
			// get the subflow result anyway.
			// The difference is only that the result is received by an external behaviour and not by the 
			// delegating workflow itself (see TerminationNotificationReceiver)
			dsc.setExecution(Constants.ASYNCH);
		}
		return dsc;
	}

	public void reset() {
		params.clear();
	}

	
	///////////////////////////////////////////
	// Building block methods
	///////////////////////////////////////////

	private jade.util.leap.List getFormalParameters() {
		WorkflowBehaviour sb;
		try {
			sb = getSubflowBehaviour();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return sb.getFormalParameters();
	}

	private Parameter getFormalDescriptor(String key, int mode) {
		jade.util.leap.List formalParams = getFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key) && 
				(p.getMode() == mode ||
				 p.getMode() == Constants.INOUT_MODE)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public Parameter getInputParameter(String key) {
		return getFormalDescriptor(key, Constants.IN_MODE);
	}

	@Override
	public Parameter getOutputParameter(String key) {
		return getFormalDescriptor(key, Constants.OUT_MODE);
	}

	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		Class parameterClass = null;
		Iterator it = params.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key)) {
				parameterClass = p.getTypeClass(false, getSubflowClassLoader()); 
			}
		}

		if (parameterClass == null) {
			throw new Exception("Parameter class type not found for key "+key);
		}

		return AbsHelper.createAbsTemplate(parameterClass, getOntology());
	}

	@Override
	public Ontology createOntology() throws Exception {
		return getSubflowBehaviour().getOntology();
	}

	@Override
	public Object getInput(String key) {
		return extract(key);
	}

	@Override
	public Object getOutput(String key) {
		return extract(key);
	}

	private java.util.List<String> getParameterNames(int mode) {
		java.util.List<String> parameterNames = new java.util.ArrayList<String>();
		jade.util.leap.List formalParams = getFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getMode() == mode ||
				p.getMode() == Constants.INOUT_MODE) {
				parameterNames.add(p.getName());
			}
		}
		
		return parameterNames;
	}
	
	@Override
	public java.util.List<String> getInputParameterNames() {
		return getParameterNames(Constants.IN_MODE);
	}
	
	@Override
	public java.util.List<String> getOutputParameterNames() {
		return getParameterNames(Constants.OUT_MODE);
	}

	@Override
	public boolean isInputEmpty(String key) {
		Iterator it = params.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean requireAbsParameters() {
		return false;
	}

	@Override
	public void setInput(String key, Object value) {
		fill(key, value);
	}

	@Override
	public void setOutput(String key, Object value) {
		fill(key, value);
	}
	
	private ClassLoader getSubflowClassLoader() {
		WorkflowEngineAgent.WorkflowExecutor rootExecutor = (WorkflowEngineAgent.WorkflowExecutor)(getActivity().root());
		String classLoaderIdentifier = rootExecutor.getDescriptor().getClassLoaderIdentifier();
		return CAServices.getInstance(getActivity().getAgent()).getClassLoader(classLoaderIdentifier);
	}
	
	private WorkflowBehaviour getSubflowBehaviour() throws Exception {
		if (getActivity() instanceof WorkflowBehaviour) {
			// In case of inline subflows we already have the workflow object.
			// Furthermore the default constructor may not be available
			return (WorkflowBehaviour)getActivity(); 
		}
		else {
			return EngineHelper.createWorkflowBehaviour(getDescriptor(), getSubflowClassLoader());	
		}
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public String getExecutionId() {
		return executionId;
	}
}
