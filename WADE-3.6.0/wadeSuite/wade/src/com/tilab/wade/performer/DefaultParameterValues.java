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

import jade.util.leap.List;
import jade.util.leap.Serializable;

import com.tilab.wade.performer.ontology.Modifier;


public class DefaultParameterValues implements Serializable {

	public final static String DEFAULT_PARAMETER_VALUES_MODIFIER = "DEFAULT_PARAMETER_VALUES_MODIFIER";

	private Modifier dpvModifier;
	
	public DefaultParameterValues() {
		dpvModifier = new Modifier(DEFAULT_PARAMETER_VALUES_MODIFIER);
	}
	
	DefaultParameterValues(Modifier dpvModifier) {
		this.dpvModifier = dpvModifier;
	}

	/**
	 * Set a default parameter value for all activities
	 * @param name parameter name
	 * @param value parameter value
	 */
	public void setParameterValue(String name, Object value) {
		setParameterValue(null, name, null, value);
	}

	/**
	 * Set a default parameter-part value for all activities
	 * @param name parameter name
	 * @param part parameter part (sub part separated by PART_SEPARATOR)
	 * @param value parameter value
	 */
	public void setParameterValue(String name, String part, Object value) {
		setParameterValue(null, name, part, value);
	}
	
	/**
	 * Set a default parameter-part value for the specific activity
	 * @param activity activity name (null for all activities)
	 * @param name parameter name
	 * @param part parameter part (sub part separated by PART_SEPARATOR)
	 * @param value parameter value
	 */
	public void setParameterValue(String activity, String name, String part, Object value) {
		setValue(activity, name, part, value);
	}

	/**
	 * Set a default header value for all activities
	 * @param name header name
	 * @param value header value
	 */
	public void setHeaderValue(String name, Object value) {
		setHeaderValue(null, name, null, value);
	}

	/**
	 * Set a default header-part value for all activities
	 * @param name header name
	 * @param part header part (sub part separated by PART_SEPARATOR)
	 * @param value header value
	 */
	public void setHeaderValue(String name, String part, Object value) {
		setHeaderValue(null, name, part, value);
	}
	
	/**
	 * Set a default header-part value for the specific activity
	 * @param activity activity name (null for all activities)
	 * @param name header name
	 * @param part header part (sub part separated by PART_SEPARATOR)
	 * @param value header value
	 */
	public void setHeaderValue(String activity, String name, String part, Object value) {
		setValue(activity, Constants.WS_HEADER_PREFIX+Constants.WS_PREFIX_SEPARATOR+name, part, value);
	}
	
	private void setValue(String activity, String name, String part, Object value) {
		
		String key;
		if (activity != null) {
			// Set default-values specific for the activity
			key = name+"@"+activity;
		} else {
			// Set default-values generic for all activities
			key = name;
		}
		
		java.util.List<DefaultValue> values = (java.util.List<DefaultValue>)dpvModifier.getProperty(key);
		if (values == null) {
			values = new java.util.ArrayList<DefaultValue>();
		}
		values.add(new DefaultValue(activity, name, part, value));
		dpvModifier.setProperty(key, values);
	}

	/**
	 * Return all default-values
	 * Generic valid for all activities added to specific for the activity (if activity not null)  
	 */
	java.util.List<DefaultValue> getValues(String activity, String name) {
		
		java.util.List<DefaultValue> values = new java.util.ArrayList<DefaultValue>();
		
		// Get default-values generic for all activities
		java.util.List<DefaultValue> dvGeneric = (java.util.List<DefaultValue>)dpvModifier.getProperty(name);
		if (dvGeneric != null) {
			values.addAll(dvGeneric);
		}
		
		if (activity != null) {
			// Get default-values specific for the activity
			java.util.List<DefaultValue> dvSpecific = (java.util.List<DefaultValue>)dpvModifier.getProperty(name+"@"+activity);
			if (dvSpecific != null) {
				values.addAll(dvSpecific);
			}
		}
		
		return values;
	}
	
	// Helper
	public void apply(List modifiers) {
		modifiers.add(dpvModifier);
	}
	
	

	// DefaultValue inner class
	class DefaultValue implements Serializable {
		
		private String activity;
		private String name;
		private String part;
		private Object value;
		
		public DefaultValue(String activity, String name, String part, Object value) {
			this.activity = activity;
			this.name = name;
			this.part = part;
			this.value = value;
		}

		public String getActivity() {
			return activity;
		}
		
		public String getName() {
			return name;
		}
		
		public String getPart() {
			return part;
		}
		
		public Object getValue() {
			return value;
		}
	}
}
