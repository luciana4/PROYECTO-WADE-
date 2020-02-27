package com.tilab.wade.commons;

import jade.content.Concept;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.OntologyHolder;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.OntologyUtils;

public class EventType implements OntologyHolder, Concept {
	private static final long serialVersionUID = -5490226772716599593L;
	
	private String description;
	private List<Parameter> parameters = new ArrayList<Parameter>();
	private transient Ontology onto;

	
	public EventType() {
	}
	
	public EventType(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Parameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}
	
	public Map<String, Parameter> getParametersMap() {
		Map<String, Parameter> paramsMap = new HashMap<String, Parameter>();
		if (parameters != null) {
			for (Parameter param : parameters) {
				paramsMap.put(param.getName(), param);
			}
		}
		return paramsMap;
	}

	public void setOntology(Ontology onto) {
		this.onto = onto;
	}

	public Ontology getOntology() throws Exception {
		if (onto == null) {
			onto = createOntology(); 
		}
		return onto;
	}
	
	private Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("EventTypeParameterOnto");

		for (Parameter param : parameters) {
			OntologyUtils.addFormalParameterToOntology(onto, param, getClass().getClassLoader());
		}

		return onto;
	}
}
