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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import bsh.EvalError;
import bsh.Interpreter;

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.OntologyUtils;

/**
 * The building-block corresponding to the execution of a script activity.
 * @see ScriptExecutionBehaviour
 */
public class Script extends InvocableBuildingBlock {

	private static final Logger myLogger = Logger.getMyLogger(Script.class.getName());;

	public enum ScriptType { BEAN_SHELL, GROOVY };
	
	private WorkflowBehaviour owner;
	private String script;
	private ScriptType scriptType = ScriptType.BEAN_SHELL;
	private ClassLoader classLoader;
	private List<String> imports = new ArrayList<String>();
	private Map<String, Parameter> formalParameters = new HashMap<String, Parameter>(); 
	private Map<String, Object> actualValues = new HashMap<String, Object>();
	
	public Script(WorkflowBehaviour owner, ScriptExecutionBehaviour activity) {
		super(activity);
		
		this.owner = owner;
	}

	public String getScript() {
		return script;
	}
	
	public void setScript(String script) {
		this.script = script;
	}

	public ScriptType getScriptType() {
		return scriptType;
	}
	
	public void setScriptType(ScriptType scriptType) {
		this.scriptType = scriptType;
	}
	
	public void importClass(String className) {
		imports.add(className);
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public void defineVariable(String name, String type) {
		formalParameters.put(name, new Parameter(name, type, Constants.INOUT_MODE));
	}
	
	/**
	 * Fill an input parameter
	 * The fill() methods are called by the Engine just before executing this script 
	 */
	public final void fill(String key, Object value) {
		if (!formalParameters.containsKey(key)) {
			defineVariable(key, value.getClass().getName());
		}
		
		actualValues.put(key, value);
	}

	/**
	   Retrieve the value of an output parameter after the execution of the script
	 */
	public final Object extract(String key) {
		return actualValues.get(key);
	}

	@Override
	public void invoke() throws Exception {
		if (scriptType.equals(ScriptType.BEAN_SHELL)) {
			invokeBsh();
		} else {
			invokeGroovy();
		}
	}
	
	private void invokeGroovy() {
		Binding binding = new Binding();
		for (Entry<String, Object> entry : actualValues.entrySet()) {
			binding.setVariable(entry.getKey(), entry.getValue());	
		}

		ImportCustomizer ic = new ImportCustomizer();
		for (String className : imports) {
			ic.addImports(className);
		}
		CompilerConfiguration cc = new CompilerConfiguration();
		cc.addCompilationCustomizers(ic);
		
		ClassLoader cl = classLoader != null ? classLoader : owner.getClass().getClassLoader();
		GroovyShell shell = new GroovyShell(cl, binding, cc);
		
		shell.evaluate(script);
		
		Iterator it = binding.getVariables().entrySet().iterator();
		while(it.hasNext()) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();
			actualValues.put(key, binding.getVariable(key));
		}
	}

	private void invokeBsh() throws EvalError {
		Interpreter interpreter = new Interpreter();
		
		if (classLoader != null) {
			interpreter.setClassLoader(classLoader);
		}
		for (String className : imports) {
			interpreter.getNameSpace().importClass(className);	
		}
		for (Entry<String, Object> entry : actualValues.entrySet()) {
			interpreter.set(entry.getKey(), entry.getValue());	
		}

		interpreter.eval(script);

		for (String key : interpreter.getNameSpace().getVariableNames()) {
			actualValues.put(key, interpreter.get(key));
		}
	}

	
	///////////////////////////////////////////
	// Building block methods
	///////////////////////////////////////////

	@Override
	public List<String> getInputParameterNames() {
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.addAll(formalParameters.keySet());
		return parameterNames;
	}

	@Override
	public List<String> getOutputParameterNames() {
		return getInputParameterNames();
	}

	@Override
	public Parameter getInputParameter(String key) {
		return formalParameters.get(key);
	}

	@Override
	public Parameter getOutputParameter(String key) {
		return formalParameters.get(key);
	}

	@Override
	public Object getInput(String key) {
		return extract(key);
	}

	@Override
	public Object getOutput(String key) {
		return extract(key);
	}

	@Override
	public void setInput(String key, Object value) {
		fill(key, value);
	}

	@Override
	public void setOutput(String key, Object value) {
		fill(key, value);
	}

	@Override
	public boolean isInputEmpty(String key) {
		return (extract(key) == null);
	}

	@Override
	public boolean requireAbsParameters() {
		return false;
	}

	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		Parameter param = getInputParameter(key);
		if (param == null) {
			throw new Exception("Variable "+key+" not present in script");
		}
		ClassLoader cl = classLoader != null ? classLoader : owner.getClass().getClassLoader();
		Class paramClass = param.getTypeClass(false, cl);
		return AbsHelper.createAbsTemplate(paramClass, getOntology());
	}

	@Override
	protected Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("ScriptOnto");

		ClassLoader cl = classLoader != null ? classLoader : owner.getClass().getClassLoader();
		for (Parameter param : formalParameters.values()) {
			OntologyUtils.addFormalParameterToOntology(onto, param, cl);
		}
		
		return onto;
	}
	
	@Override
	public void reset() {
		actualValues.clear();
	}
}
