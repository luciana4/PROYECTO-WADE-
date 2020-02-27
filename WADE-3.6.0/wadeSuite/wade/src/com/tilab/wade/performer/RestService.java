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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.BuildingBlock;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.rest.MethodDescriptor;
import com.tilab.wade.performer.descriptors.rest.ResourceBaseDescriptor;
import com.tilab.wade.performer.descriptors.rest.ResourceDescriptor;
import com.tilab.wade.performer.descriptors.rest.RestDescriptor;

/**
 * The building-block corresponding to the (static) invocation of REST service.
 * The parameters keys are:
 * - header.<param-name>
 * - template.<param-name>
 * - matrix.<param-name>
 * - query.<param-name>
 * - body.request
 * - body.response
 * - http.statusCode
 * - fault.<status-code>
 * 
 * @see RestServiceInvocationBehaviour
 */
public class RestService extends InvocableBuildingBlock {

	transient private RestDescriptor descriptor;
	
	private WorkflowBehaviour owner;
	private String descriptorClassName;
	private String baseUri;
	private String resourceId;
	private String methodId;
	private String requestMediaTypeElement;
	private String responseMediaTypeElement;
	private String faultMediaTypeElement;
	private int timeout;
	private String endpointAddress;
	private Map<String, Object> templateParameters = new HashMap<String, Object>();
	private Map<String, Object> matrixParameters = new HashMap<String, Object>();
	private Map<String, Object> headerParameters = new HashMap<String, Object>();
	private Map<String, Object> queryParameters = new HashMap<String, Object>();
	private Map<String, Object> bodyParameters = new HashMap<String, Object>();
	private Map<String, Object> httpParameters = new HashMap<String, Object>();
	
	
    public RestService(WorkflowBehaviour owner, RestServiceInvocationBehaviour activity) {
		super(activity);
		
		this.owner = owner;
		this.timeout = -1;
	}

	public String getDescriptorClassName() {
		return descriptorClassName;
	}
	
	public void setDescriptorClassName(String descriptorClassName) {
		this.descriptorClassName = descriptorClassName;
	}
    
	public String getBaseUri() {
		return baseUri;
	}
	
	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}
	
	public String getResourceId() {
		return resourceId;
	}
	
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	public String getMethodId() {
		return methodId;
	}
	
	public void setMethodId(String methodId) {
		this.methodId = methodId;
	}

	public String getRequestMediaTypeElement() {
		return requestMediaTypeElement;
	}

	public void setRequestMediaTypeElement(String requestMediaTypeElement) {
		this.requestMediaTypeElement = requestMediaTypeElement;
	}

	public String getResponseMediaTypeElement() {
		return responseMediaTypeElement;
	}

	public void setResponseMediaTypeElement(String responseMediaTypeElement) {
		this.responseMediaTypeElement = responseMediaTypeElement;
	}
	
	public String getFaultMediaTypeElement() {
		return faultMediaTypeElement;
	}

	public void setFaultMediaTypeElement(String faultMediaTypeElement) {
		this.faultMediaTypeElement = faultMediaTypeElement;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void setEndpointAddress(String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	public String getEndpointAddress() {
		return endpointAddress;
	}
	
	public WorkflowBehaviour getOwner() {
		return owner;
	}

	RestDescriptor getDescriptor() throws Exception {
		if (descriptor == null) {
			ClassLoader wcl = CAServices.getInstance(owner.getAgent()).getDefaultClassLoader();
			descriptor = (RestDescriptor) Class.forName(descriptorClassName, true, wcl).newInstance();
		}
		return descriptor;
	}
	
	public void invoke() throws Exception {
		// Get service descriptor
		RestDescriptor restDescriptor;
		try {
			restDescriptor = getDescriptor();
		} catch(Exception e) {
			throw new RestException("No descriptor found ("+descriptorClassName+")", e);
		}
		
		// Invoke service
		restDescriptor.invoke(this);
	}
	
	public final void fillBody(String key, Object value) {
		bodyParameters.put(key, value);
	}

	public final Object extractBody(String key) {
		return bodyParameters.get(key);
	}

	public final void fillTemplate(String key, Object value) {
		templateParameters.put(key, value);
	}

	public final Object extractTemplate(String key) {
		return templateParameters.get(key);
	}
	
	public final void fillMatrix(String key, Object value) {
		matrixParameters.put(key, value);
	}

	public final Object extractMatrix(String key) {
		return matrixParameters.get(key);
	}

	public final void fillHeader(String key, Object value) {
		headerParameters.put(key, value);
	}

	public final Object extractHeader(String key) {
		return headerParameters.get(key);
	}
	
	public final void fillQuery(String key, Object value) {
		queryParameters.put(key, value);
	}

	public final Object extractQuery(String key) {
		return queryParameters.get(key);
	}

	public final void fillHttp(String key, Object value) {
		httpParameters.put(key, value);
	}

	public final Object extractHttp(String key) {
		return httpParameters.get(key);
	}
	
	public void reset() {
		templateParameters.clear();
		matrixParameters.clear();
		headerParameters.clear();
		queryParameters.clear();
		httpParameters.clear();
		bodyParameters.clear();
	}

	public Map<String, Object> getTemplateParameters() {
		return templateParameters;
	}

	
	///////////////////////////////////////////
	// Building block methods

	private MethodDescriptor getMethodDescriptor(){
		try {	
		
			ResourceBaseDescriptor rbd;
			ResourceDescriptor rd;
			MethodDescriptor md;
						
			rbd =getDescriptor().getResourceBase(baseUri);
			if(rbd==null){
				throw new RuntimeException("The resource base cannot be obtained out of the given base uri "+baseUri);
			}
		
			rd=rbd.getResource(resourceId);
			if(rd==null){
				throw new RuntimeException("The given resource (" +resourceId+ ") does not exist");
			}
					
			md= rd.getMethod(methodId);
			
			if(md==null){
				throw new RuntimeException("The given method (" +methodId+ ") does not exist");
			}
			
			return md;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean requireAbsParameters() {
		return false;
	}
	
	@Override
	protected Ontology createOntology() throws Exception {
		return getDescriptor().getOntology();
	}

	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		Parameter param = getParameter(key);
		if (param != null) {
			return AbsHelper.createAbsTemplate(param.getSchema());	
		} else {
			throw new Exception("No abs schema found for key "+key);
		}
	}

	private Parameter getParameter(List<Parameter> params, String name) {
		for (Parameter param : params) {
			if (param.getName().equalsIgnoreCase(name)) {
				return param;
			}
		}
		return null;
	}
	
	private Parameter getParameter(String key) {
		int sepPos = key.indexOf(Constants.REST_PREFIX_SEPARATOR);
		String prefix = key.substring(0, sepPos);
		String name = key.substring(sepPos+1);		
		
		if (prefix.equalsIgnoreCase(Constants.REST_HEADER_PREFIX)) {
			return getParameter(getMethodDescriptor().getHeaderParameters(), name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_TEMPLATE_PREFIX)) {
			return getParameter(getMethodDescriptor().getTemplateParameters(), name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_MATRIX_PREFIX)) {
			return getParameter(getMethodDescriptor().getMatrixParameters(), name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_QUERY_PREFIX)) {
			return getParameter(getMethodDescriptor().getQueryParameters(), name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_HTTP_PREFIX)) {
			return getParameter(getMethodDescriptor().getHttpParameters(), name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_BODY_PREFIX)) {
			if (name.equalsIgnoreCase(Constants.REST_BODY_REQUEST)) {
				return getMethodDescriptor().getRequestBody(requestMediaTypeElement);
			}
			if (name.equalsIgnoreCase(Constants.REST_BODY_RESPONSE)) {
				return getMethodDescriptor().getResponseBody(responseMediaTypeElement);
			}
		}
		if (prefix.equalsIgnoreCase(Constants.REST_FAULT_PREFIX)) {
			return getMethodDescriptor().getFaultBody(Long.parseLong(name), faultMediaTypeElement);
		}
		
		return null;
	}

	private List<String> getParameterNames(List<Parameter> params, int mode, String prefix) {
		List<String> names = new ArrayList<String>();
		for (Parameter param : params) {
			if (param.getMode() == mode || param.getMode() == Constants.INOUT_MODE) {
				names.add(prefix+Constants.REST_PREFIX_SEPARATOR+param.getName());
			}
		}
		return names;
	}
	
	@Override
	public List<String> getInputParameterNames() {
		List<String> inputs = new ArrayList<String>();
		inputs.addAll(getParameterNames(getMethodDescriptor().getHeaderParameters(), Constants.IN_MODE, Constants.REST_HEADER_PREFIX));
		inputs.addAll(getParameterNames(getMethodDescriptor().getTemplateParameters(), Constants.IN_MODE, Constants.REST_TEMPLATE_PREFIX));
		inputs.addAll(getParameterNames(getMethodDescriptor().getMatrixParameters(), Constants.IN_MODE, Constants.REST_MATRIX_PREFIX));
		inputs.addAll(getParameterNames(getMethodDescriptor().getQueryParameters(), Constants.IN_MODE, Constants.REST_QUERY_PREFIX));
		inputs.addAll(getParameterNames(getMethodDescriptor().getHttpParameters(), Constants.IN_MODE, Constants.REST_HTTP_PREFIX));

		if (getMethodDescriptor().getRequestBody(requestMediaTypeElement) != null) {
			inputs.add(Constants.REST_BODY_PREFIX + Constants.REST_PREFIX_SEPARATOR + Constants.REST_BODY_REQUEST);
		}
		
		return inputs;
	}

	@Override
	public List<String> getOutputParameterNames() {
		List<String> outputs = new ArrayList<String>();
		outputs.addAll(getParameterNames(getMethodDescriptor().getHeaderParameters(), Constants.OUT_MODE, Constants.REST_HEADER_PREFIX));
		outputs.addAll(getParameterNames(getMethodDescriptor().getHttpParameters(), Constants.OUT_MODE, Constants.REST_HTTP_PREFIX));
		
		if (getMethodDescriptor().getResponseBody(responseMediaTypeElement) != null) {
			outputs.add(Constants.REST_BODY_PREFIX + Constants.REST_PREFIX_SEPARATOR + Constants.REST_BODY_RESPONSE);
		}
		
		for (Long faultStatus : getMethodDescriptor().getFaultStatus()) {
			outputs.add(Constants.REST_FAULT_PREFIX + Constants.REST_PREFIX_SEPARATOR + faultStatus.toString());
		}

		return outputs;
	}

	@Override
	public Parameter getInputParameter(String key) {
		Parameter param = getParameter(key);
		if (param.getMode() == Constants.IN_MODE ||
			param.getMode() == Constants.INOUT_MODE) {
			return param;
		}
		return null;
	}

	@Override
	public Parameter getOutputParameter(String key) {
		Parameter param = getParameter(key);
		if (param.getMode() == Constants.OUT_MODE ||
			param.getMode() == Constants.INOUT_MODE) {
			return param;
		}
		return null;
	}

	private Object getValue(String key) {
		int sepPos = key.indexOf(Constants.REST_PREFIX_SEPARATOR);
		String prefix = key.substring(0, sepPos);
		String name = key.substring(sepPos+1);		
		
		if (prefix.equalsIgnoreCase(Constants.REST_HEADER_PREFIX)) {
			return extractHeader(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_TEMPLATE_PREFIX)) {
			return extractTemplate(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_MATRIX_PREFIX)) {
			return extractMatrix(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_QUERY_PREFIX)) {
			return extractQuery(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_HTTP_PREFIX)) {
			return extractHttp(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_BODY_PREFIX)) {
			return extractBody(name);
		}
		if (prefix.equalsIgnoreCase(Constants.REST_FAULT_PREFIX)) {
			return extractBody(name);
		}
		return null;
	}
	
	@Override
	public Object getInput(String key) {
		return getValue(key);
	}

	@Override
	public Object getOutput(String key) {
		return getValue(key);
	}

	private void setValue(String key, Object value) {
		int sepPos = key.indexOf(Constants.REST_PREFIX_SEPARATOR);
		String prefix = key.substring(0, sepPos);
		String name = key.substring(sepPos+1);		
		
		if (prefix.equalsIgnoreCase(Constants.REST_HEADER_PREFIX)) {
			fillHeader(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_TEMPLATE_PREFIX)) {
			fillTemplate(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_MATRIX_PREFIX)) {
			fillMatrix(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_QUERY_PREFIX)) {
			fillQuery(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_HTTP_PREFIX)) {
			fillHttp(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_BODY_PREFIX)) {
			fillBody(name, value);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_FAULT_PREFIX)) {
			fillBody(name, value);
		}
	}
	
	@Override
	public void setInput(String key, Object value) {
		setValue(key, value);
	}

	@Override
	public void setOutput(String key, Object value) {
		setValue(key, value);
	}

	@Override
	public boolean isInputEmpty(String key) {
		int sepPos = key.indexOf(Constants.REST_PREFIX_SEPARATOR);
		String prefix = key.substring(0, sepPos);
		String name = key.substring(sepPos+1);		
		
		if (prefix.equalsIgnoreCase(Constants.REST_HEADER_PREFIX)) {
			return !headerParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_TEMPLATE_PREFIX)) {
			return !templateParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_MATRIX_PREFIX)) {
			return !matrixParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_QUERY_PREFIX)) {
			return !queryParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_HTTP_PREFIX)) {
			return !httpParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_BODY_PREFIX)) {
			return !bodyParameters.containsKey(name);
		}
		else if (prefix.equalsIgnoreCase(Constants.REST_FAULT_PREFIX)) {
			return !bodyParameters.containsKey(name);
		}
		return true;
	}
}
