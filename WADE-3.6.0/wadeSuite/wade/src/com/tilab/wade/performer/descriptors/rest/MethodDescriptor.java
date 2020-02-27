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
package com.tilab.wade.performer.descriptors.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tilab.wade.performer.descriptors.Parameter;

public class MethodDescriptor implements Serializable {

	private static final long serialVersionUID = -4817898390981176509L;
	public enum MethodType {GET, POST, PUT, DELETE}
	
	private String id;
	private MethodType type;
	private String documentation;
	private Map<String, BodyDescriptor> requestBodies = new HashMap<String, BodyDescriptor>();
	private Map<String, BodyDescriptor> responseBodies = new HashMap<String, BodyDescriptor>();
	private Map<Long, Map<String, BodyDescriptor>> faultBodies = new HashMap<Long, Map<String, BodyDescriptor>>();
	private List<Parameter> templateParameters = new ArrayList<Parameter>();
	private List<Parameter> queryParameters = new ArrayList<Parameter>(); 
	private List<Parameter> matrixParameters = new ArrayList<Parameter>();
	private List<Parameter> headerParameters = new ArrayList<Parameter>();
	private List<Parameter> httpParameters = new ArrayList<Parameter>();
	
	
	public MethodDescriptor(MethodType type) {
		this(null, type);
	}

	public MethodDescriptor(String id, MethodType type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

	public MethodType getType() {
		return type;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	public Set<String> getRequestBodyMediaTypesElements() {
		return requestBodies.keySet();
	}
	
	public BodyDescriptor getRequestBody(String mediaTypeElement) {
		return requestBodies.get(mediaTypeElement);
	}
	
	public void addRequestBody(BodyDescriptor body) {
		requestBodies.put(body.getMediaTypeElement(), body);
	}

	public Set<String> getResponseBodyMediaTypesElements() {
		return responseBodies.keySet();
	}
	
	public BodyDescriptor getResponseBody(String mediaTypeElement) {
		return responseBodies.get(mediaTypeElement);
	}
	
	public void addResponseBody(BodyDescriptor body) {
		responseBodies.put(body.getMediaTypeElement(), body);
	}
	
	public Set<Long> getFaultStatus() {
		return faultBodies.keySet();
	}
	
	public Set<String> getFaultBodyMediaTypes(Long status) {
		return faultBodies.get(status).keySet();
	}
	
	public Map<String, BodyDescriptor> getFaultBodies(Long status) {
		return faultBodies.get(status);
	}
	
	public BodyDescriptor getFaultBody(Long status, String mediaTypeElement) {
		Map<String, BodyDescriptor> faultBodiesByStatus = faultBodies.get(status);
		if (faultBodiesByStatus != null) {
			return faultBodiesByStatus.get(mediaTypeElement);
		} else {
			return null;
		}
	}
	
	public void addFaultBody(Long status, BodyDescriptor body) {
		Map<String, BodyDescriptor> faultBodiesByStatus = faultBodies.get(status);
		if (faultBodiesByStatus == null) {
			faultBodiesByStatus = new HashMap<String, BodyDescriptor>();
			faultBodies.put(status, faultBodiesByStatus);
		}
		faultBodiesByStatus.put(body.getMediaTypeElement(), body);
	}

	public List<Parameter> getTemplateParameters() {
		return templateParameters;
	}
	
	public void addTemplateParameter(Parameter param) {
		templateParameters.add(param);
	}

	public List<Parameter> getQueryParameters() {
		return queryParameters;
	}
	
	public void addQueryParameter(Parameter param) {
		queryParameters.add(param);
	}

	public List<Parameter> getMatrixParameters() {
		return matrixParameters;
	}

	public void addMatrixParameter(Parameter param) {
		matrixParameters.add(param);
	}
	
	public List<Parameter> getHeaderParameters() {
		return headerParameters;
	}
	
	public void addHeaderParameter(Parameter param) {
		headerParameters.add(param);
	}
	
	public List<Parameter> getHttpParameters() {
		return httpParameters;
	}
	
	public void addHttpParameter(Parameter param) {
		httpParameters.add(param);
	}
}
