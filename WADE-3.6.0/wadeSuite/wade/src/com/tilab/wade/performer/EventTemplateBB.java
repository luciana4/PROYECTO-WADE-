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

import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.performer.descriptors.Parameter;

public class EventTemplateBB extends BuildingBlock {
	private static final long serialVersionUID = -2136428625255667035L;
	
	public static String ID = "EVENT_TEMPLATE";
	
	private EventTemplate eventTemplate;
	
	public EventTemplateBB(EventTemplate eventTemplate, HierarchyNode activity) {
		super(activity);
		
		this.eventTemplate = eventTemplate;
	}
	
	EventTemplate getEventTemplate() {
		return eventTemplate;
	}
	
	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		AbsObject absValue = null;
		Object value = eventTemplate.extract(key);
		if (value != null) {
			absValue = AbsHelper.createAbsTemplate(value.getClass(), getOntology());
		}
		return absValue;
	}

	@Override
	protected Ontology createOntology() throws Exception {
		return eventTemplate.getOntology();
	}

	@Override
	public Object getInput(String key) {
		return eventTemplate.extract(key);
	}

	@Override
	public Parameter getInputParameter(String key) {
		for (Parameter param : eventTemplate.getParams()) {
			if (param.getName().equals(key)) {
				return param;
			}
		}
		return null;
	}

	@Override
	public List<String> getInputParameterNames() {
		List<String> parameterNames = new ArrayList<String>();
		for(Parameter param : eventTemplate.getParams()) {
			parameterNames.add(param.getName());
		}
		return parameterNames;
	}

	@Override
	public Object getOutput(String key) {
		// An EventTemplate does not have output parameters
		return null;
	}

	@Override
	public Parameter getOutputParameter(String key) {
		// An EventTemplate does not have output parameters
		return null;
	}

	@Override
	public List<String> getOutputParameterNames() {
		// An EventTemplate does not have output parameters
		return null;
	}

	@Override
	public boolean isInputEmpty(String key) {
		return getInputParameter(key)==null;
	}

	@Override
	public boolean requireAbsParameters() {
		return false;
	}

	@Override
	public void reset() {
		eventTemplate.getParams().clear();
	}

	@Override
	public void setInput(String key, Object value) {
		eventTemplate.fill(key, value);
	}

	@Override
	public void setOutput(String key, Object value) {
		// An EventTemplate does not have output parameters
	}
}
