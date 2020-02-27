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
package com.tilab.wade.event;

import jade.content.Concept;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;

import java.util.ArrayList;
import java.util.List;

import com.tilab.wade.performer.OntologyHolder;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.OntologyUtils;

/**
 * This class represents a template that must be specified when registering to the WADE Event System 
 * and is intended to identify the event the registering agent is interested to receive.
 * More in details such template specifies the type of the event and a boolean expression that
 * will be evaluated against incoming events.  
 */
public class EventTemplate implements OntologyHolder, Concept {
	private static final long serialVersionUID = 112345678L;
	
	public static final String TAG_PROPERTY = "Event-template-tag";
	
	private String eventType;
	private String eventIdentificationExpression;
	private List<Parameter> params = new ArrayList<Parameter>();
	// A tag that can be attached to an event template: when an event matching the template 
	// will be received, the EventSystemAgent will include that tag among the event properties
	// at the TAG_PROPERTY key
	private String tag;
	
	private transient Ontology onto;
	
	
	public EventTemplate() {
	}
	
	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventIdentificationExpression() {
		return eventIdentificationExpression;
	}

	public void setEventIdentificationExpression(String eventIdentificationExpression) {
		this.eventIdentificationExpression = eventIdentificationExpression;
	}

	public List<Parameter> getParams() {
		return params;
	}

	public void setParams(List<Parameter> params) {
		this.params = params;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}


	//////////////////////////////////////////////////////
	// Fill and extract section
	//////////////////////////////////////////////////////
	/**
	 * Fill a parameter that will be used when evaluating the event-identification-expression against 
	 * incoming events. Such parameters corresponds to variables referenced in the event-identification-expression.  
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
	 * Retrieve the value of a parameter. 
	 */
	public final Object extract(String key) {
		Object value = null;
		for (int i=0; i<params.size(); i++){
			Parameter param = (Parameter)params.get(i);
			if (param.getName().equals(key)){
				value = param.getValue();
				break;
			}
		}
		return value;
	}
	
	public Ontology getOntology() throws Exception {
		if (onto == null) {
			onto = createOntology(); 
		}
		return onto;
	}
	
	private Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("EventTemplateParameterOnto");

		for (Parameter param : params) {
			OntologyUtils.addActualParameterToOntology(onto, param);
		}

		return onto;
	}
}
