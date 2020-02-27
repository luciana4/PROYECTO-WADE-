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
package com.tilab.wade.performer.descriptors;

import java.util.HashMap;
import java.util.Map;

import jade.content.Concept;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

/**
   Descriptor for a generic computational element that accepts
   input and output parameters
   @author Giovanni Caire - TILAB
 */
public class ElementDescriptor implements Concept {
	private String id;
	private String name;
	private List packages;
	private List parameters;
	
	public ElementDescriptor() {
		parameters = new ArrayList();
	}
	
	public ElementDescriptor(String id) {
		this.id = id;
		parameters = new ArrayList();
	}
	
	public ElementDescriptor(String id, Map<String, Object> paramsMap) {
		this.id = id;
		parameters = mapToParamList(paramsMap);
	}
	
	public ElementDescriptor(String id, List parameters) {
		this.id = id;
		this.parameters = parameters;
	}
	
	/**
	 * @deprecated Use ElementDescriptor(String id, List parameters) or ElementDescriptor(String id, Map paramsMap) instead
	 */
	public ElementDescriptor(String id, List packages, List parameters) {
		this.id = id;
		this.parameters = parameters;
		this.packages = packages;
	}
	
	public final void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @deprecated Use setId(String id) instead
	 */
	public final void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @deprecated packages are no longer used in descriptors
	 */
	public final void setPackages(List packages) {
		this.packages = packages;
	}
	
	public final void setParameters(List parameters) {
		this.parameters = parameters;
	}
	
	public final void setParametersMap(Map<String, Object> paramsMap) {
		parameters = mapToParamList(paramsMap);
	}
	
	public final String getId() {
		return id;
	}
	
	/**
	 * @deprecated Use getId() instead
	 */
	public final String getName() {
		return (name != null ? name : id);
	}
	
	/**
	 * @deprecated packages are no longer used in descriptors
	 */
	public final List getPackages() {
		return packages;
	}
	
	public final List getParameters() {
		return parameters;
	}
	
	public final Map<String, Object> getParametersMap() {
		return paramListToMap(parameters);
	}
	
	////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////
	/**
	 * Utility method to convert a list of actual parameters (typically the output of a workflow execution) 
	 * into a Map associating the name of a parameter to its value 
	 */
	public static Map<String, Object> paramListToMap(List params) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (params != null) {
			for(int i=0;i<params.size();i++) {
				Parameter p = (Parameter) params.get(i);
				map.put(p.getName(), p.getValue());
			}
		}
		return map;
	}
	
	/**
	 * Utility method to convert a Map associating the name of a parameter to its value to a list of actual parameters 
	 * (typically the input parameters of a workflow execution) 
	 */
	public static List mapToParamList(Map<String, Object> map) {
		List pp = new ArrayList();
		if (map != null) {
			for(String paramName : map.keySet()) {
				Parameter p = new Parameter(paramName, map.get(paramName));
				pp.add(p);
			}
		}
		return pp;
	}
}
