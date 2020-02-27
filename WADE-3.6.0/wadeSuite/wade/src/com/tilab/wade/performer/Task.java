package com.tilab.wade.performer;

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.core.behaviours.Behaviour;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.utils.OntologyUtils;

/**
 * The building-block corresponding to the execution of a generic task implemented by a JADE behaviour.
 * @see TaskExecutionBehaviour
 */
public class Task extends BuildingBlock {

	private Behaviour myBehaviour;
	private List params = new ArrayList();
	private List formalParameters = null;
	
	public Task(Behaviour task, HierarchyNode activity) {
		super(activity);
		myBehaviour = task;
	}
	
	private List getTaskFormalParameters() {
		if (formalParameters == null) {
			formalParameters = new ArrayList();
			EngineHelper.fillFormalParameters(myBehaviour, Behaviour.class, formalParameters);
		}
		return formalParameters;
	}
	
	/**
	 * Fill an input parameter
	 * The fill() methods are called by the Engine just before executing the task referenced by this TaskReference 
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
	 * Retrieve the value of an output parameter after the execution of the behaviour referenced by this Task object
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
	
	List getParams() {
		return params;
	}
	
	void setParams(List params) {
		this.params = params;
	}
	
	private ClassLoader getClassLoader() {
		WorkflowEngineAgent.WorkflowExecutor rootExecutor = (WorkflowEngineAgent.WorkflowExecutor)(getActivity().root());
		String classLoaderIdentifier = rootExecutor.getDescriptor().getClassLoaderIdentifier();
		return CAServices.getInstance(getActivity().getAgent()).getClassLoader(classLoaderIdentifier);
	}
	
	
	///////////////////////////////////////////
	// Building block methods
	///////////////////////////////////////////
	
	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		Class parameterClass = null;
		List formalParameters = getTaskFormalParameters();
		Iterator it = formalParameters.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key)) {
				parameterClass = p.getTypeClass(false, getClassLoader()); 
			}
		}

		if (parameterClass == null) {
			throw new Exception("Parameter type not found for key"+key);
		}

		return AbsHelper.createAbsTemplate(parameterClass, getOntology());
	}

	@Override
	protected Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("TaskParametersOnto");

		jade.util.leap.List formalParams = getTaskFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter param = (Parameter)it.next();
			OntologyUtils.addFormalParameterToOntology(onto, param, getClassLoader());
		}

		return onto;
	}

	@Override
	public Object getInput(String key) {
		return extract(key);
	}

	@Override
	public void setInput(String key, Object value) {
		fill(key, value);
	}

	@Override
	public Parameter getInputParameter(String key) {
		jade.util.leap.List formalParams = getTaskFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key) && 
				(p.getMode() == Constants.IN_MODE ||
				 p.getMode() == Constants.INOUT_MODE )) {
				return p;
			}
		}		
		return null;
	}

	@Override
	public Parameter getOutputParameter(String key) {
		jade.util.leap.List formalParams = getTaskFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(key) && 
				(p.getMode() == Constants.OUT_MODE ||
				 p.getMode() == Constants.INOUT_MODE )) {
				return p;
			}
		}		
		return null;
	}
	
	@Override
	public java.util.List<String> getInputParameterNames() {
		java.util.List<String> inParameterNames = new java.util.ArrayList<String>();
		jade.util.leap.List formalParams = getTaskFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getMode() == Constants.IN_MODE ||
				p.getMode() == Constants.INOUT_MODE )
			inParameterNames.add(p.getName());
		}		
		return inParameterNames;
	}

	@Override
	public Object getOutput(String key) {
		return extract(key);
	}

	@Override
	public void setOutput(String key, Object value) {
		fill(key, value);
	}
	
	@Override
	public java.util.List<String> getOutputParameterNames() {
		java.util.List<String> outParameterNames = new java.util.ArrayList<String>();
		jade.util.leap.List formalParams = getTaskFormalParameters();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getMode() == Constants.OUT_MODE ||
				p.getMode() == Constants.INOUT_MODE )
			outParameterNames.add(p.getName());
		}		
		return outParameterNames;
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
	public void reset() {
		params.clear();
	}
}
