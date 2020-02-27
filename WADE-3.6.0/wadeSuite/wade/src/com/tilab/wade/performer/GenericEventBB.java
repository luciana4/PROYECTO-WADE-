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
import jade.content.onto.Ontology;

import java.util.ArrayList;
import java.util.List;

import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.performer.descriptors.Parameter;

public class GenericEventBB extends BuildingBlock {
	private static final long serialVersionUID = 5328319295692404789L;
	
	public static String ID = "EVENT"; 
	
	private GenericEvent genericEvent;
	
	
	public GenericEventBB(GenericEvent genericEvent, HierarchyNode activity) {
		super(activity);
		
		this.genericEvent = genericEvent;
	}
	
	GenericEvent getGenericEvent() {
		return genericEvent;
	}
	
	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		AbsObject absValue = null;
		Object value = genericEvent.extract(key);
		if (value != null) {
			absValue = AbsHelper.createAbsTemplate(value.getClass(), getOntology());
		}
		return absValue;
	}

	@Override
	protected Ontology createOntology() throws Exception {
		return genericEvent.getOntology();
	}

	@Override
	public Object getInput(String key) {
		// GenericEvent not have input parameters
		return null;
	}

	@Override
	public Parameter getInputParameter(String key) {
		// GenericEvent not have input parameters
		return null;
	}

	@Override
	public List<String> getInputParameterNames() {
		// GenericEvent not have input parameters
		return new ArrayList<String>();
	}

	@Override
	public Object getOutput(String key) {
		return genericEvent.extract(key);
	}

	@Override
	public Parameter getOutputParameter(String key) {
		for (Parameter param : genericEvent.getParams()) {
			if (param.getName().equals(key)) {
				return param;
			}
		}
		return null;
	}

	@Override
	public List<String> getOutputParameterNames() {
		List<String> parameterNames = new ArrayList<String>();
		for (Parameter param : genericEvent.getParams()) {
			parameterNames.add(param.getName());
		}
		return parameterNames;
	}

	@Override
	public boolean isInputEmpty(String key) {
		// GenericEvent not have input parameters
		return false;
	}

	@Override
	public boolean requireAbsParameters() {
		return false;
	}

	@Override
	public void reset() {
		genericEvent.getParams().clear();
	}

	@Override
	public void setInput(String key, Object value) {
		// GenericEvent not have input parameters
	}

	@Override
	public void setOutput(String key, Object value) {
		genericEvent.fill(key, value);
	}
}
