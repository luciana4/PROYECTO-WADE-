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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tilab.wade.performer.OntologyHolder;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.OntologyUtils;

import jade.content.Concept;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.domain.FIPAAgentManagement.Property;

/**
 * This class represents a generic event managed by the WADE Event System.
 * Each event is characterized by 
 * - a type
 * - a set of parameters, each one identified by a name, that can be filled by  means of the <code>fill()</code> 
 * method and retrieved by means of the <code>extract()</code> method 
 * - an optional time-to-leave that determines how long the WADE Event System will keep it  
 * 
 * Events can be submitted to the WADE Event System my means of an <code>EventChannel</code>
 * Workflows can suspend until a given event occurs 
 *
 * @see EventChannel
 * @see com.tilab.wade.performer.WaitEventBehaviour
 */
public class GenericEvent implements OntologyHolder, Concept {
	private static final long serialVersionUID = 198765432L;
	
	private String type;
	private Date timeToLeave;
	private List<Parameter> params = new ArrayList<Parameter>();
	private List<Property> properties;
	
	private transient Ontology onto;
	
	
	public GenericEvent() {
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Date getTimeToLeave() {
		return timeToLeave;
	}
	public void setTimeToLeave(Date timeToLeave) {
		this.timeToLeave = timeToLeave;
	}
	public List<Parameter> getParams() {
		return params;
	}
	public void setParams(List<Parameter> params) {
		this.params = params;
	}
	public List<Property> getProperties() {
		return properties;
	}
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	//////////////////////////////////////////////////////
	// Fill and extract section
	//////////////////////////////////////////////////////
	/**
	 * Fill a parameter of this event
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
	 * Retrieve the value of a parameter of this event
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

	public Ontology getOntology() throws Exception {
		if (onto == null) {
			onto = createOntology(); 
		}
		return onto;
	}
	
	private Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("GenericEventParameterOnto");

		for (Parameter param : params) {
			OntologyUtils.addActualParameterToOntology(onto, param);
		}

		return onto;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("(");
		sb.append(type);
		for (Parameter p : params) {
			sb.append(' ');
			sb.append(p.getName()+'='+p.getValue());
		}
		sb.append(')');
		return sb.toString();
	}
}
