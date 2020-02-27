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

import jade.content.onto.Ontology;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.rest.MethodDescriptor.MethodType;
import com.tilab.wade.performer.RestException;
import com.tilab.wade.performer.RestService;

public class RestDescriptor implements Serializable {

	private static final long serialVersionUID = -6387445684968098563L;

	public String serviceName;
	private String title;
	private String documentation;
	private String descriptorClassName;
	private Map<String, ResourceBaseDescriptor> resourceBases = new HashMap<String, ResourceBaseDescriptor>();  	

	public RestDescriptor() {
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public Ontology getOntology() {
		return null;
	}

	public Set<String> getResourceBaseURIs() {
		return resourceBases.keySet();
	}

	public ResourceBaseDescriptor getResourceBase(String baseUri) {
		return resourceBases.get(baseUri);
	}

	public void addResourceBase(ResourceBaseDescriptor resourceBase) {
		resourceBases.put(resourceBase.getBaseURI(), resourceBase);
	}

	public String getDescriptorClassName() {
		return descriptorClassName;
	}

	public void setDescriptorClassName(String descriptorClassName) {
		this.descriptorClassName = descriptorClassName;
	}	

	public void invoke(RestService rs) throws Exception {
		String baseURI= rs.getBaseUri();
		if (baseURI == null) {
			throw new RestException("Missing Rest base URI");
		}

		//obtains the <resources> node of the WADL from the baseUri		
		ResourceBaseDescriptor resourceBaseDescriptor= getResourceBase(baseURI);	  	    				
		if (resourceBaseDescriptor==null) {
			throw new RestException("Status 404 not found. Base URI ("+baseURI+") not found");
		}

		Set<String> resourcesID = resourceBaseDescriptor.getResourceIds();				
		ResourceDescriptor resourceDescriptor = null;

		//verifies if the resource's ID  corresponds to one of the resources within the resourceBaseDescriptor
		//and obtains the <resource> node of the WADL from the resourceID given by the user
		if (resourcesID.contains(rs.getResourceId())) {			
			resourceDescriptor = resourceBaseDescriptor.getResource(rs.getResourceId());
		}else{			
			throw new RestException("RestService "+serviceName+": resource "+rs.getResourceId()+" not found in RestService stub");
		}	

		Set<String> methodsID = resourceDescriptor.getMethodIds();
		MethodDescriptor methodDescriptor = null;

		//verifies if the method's ID corresponds to one of the methods within the resourceDescriptor
		//and obtains the <method> node of the WADL from the methodID given by the user
		if (methodsID.contains(rs.getMethodId())) {			
			methodDescriptor = resourceDescriptor.getMethod(rs.getMethodId());
		}else{			
			throw new RestException("RestService "+serviceName+": method "+rs.getMethodId()+" not found in RestService stub");
		}		

		//Obtains the current's resource path as a String
		String path = resourceDescriptor.getPath();

		URI base=null;
		try {
			base = new URI(baseURI);
		} catch (URISyntaxException e) {
			throw new RestException("RestService "+serviceName+": URI Syntax Exception",e);
		}

		//Assigns to a List<Parameter> the different parameters from the methodDescriptor,
		//Header, Matrix and Query respectively. 
		List<Parameter> headerParameters=methodDescriptor.getHeaderParameters();				
		List<Parameter> matrixParameters=methodDescriptor.getMatrixParameters();				
		List<Parameter> queryParameters=methodDescriptor.getQueryParameters();	

		//Builds the localUriBuilder from the baseURI obtained out of the RestDescriptor, 
		//and then adds to it the resource path
		UriBuilder uriBuilder = UriBuilder.fromUri(base).path(path);

		//adds the matrixParams given by the user
		for (Parameter matrixParameter : matrixParameters) {
			String matrixParamName= matrixParameter.getName();
			Object matrixParameterValue = rs.extractMatrix(matrixParamName);
			if (matrixParameter.getMandatory() && matrixParameterValue == null) {
				throw new RestException("RestService "+serviceName+": mandatory matrix parameter "+matrixParamName+" not found");
			}
			if (matrixParameterValue != null) {
				uriBuilder = uriBuilder.replaceMatrixParam(matrixParamName, matrixParameterValue);
			}
		}	

		//adds the templateParamas given by the user
		WebResource resource = Client.create().resource(uriBuilder.buildFromMap(rs.getTemplateParameters()));

		//adds  the queryParams given by the user
		for (Parameter queryParameter : queryParameters) {
			String queryParamName = queryParameter.getName();
			Object queryParameterValue = rs.extractQuery(queryParamName);
			if (queryParameter.getMandatory() && queryParameterValue == null) {
				throw new RestException("RestService "+serviceName+": mandatory query parameter "+queryParamName+" not found");
			}
			if (queryParameterValue != null) {
				resource = resource.queryParam(queryParamName, queryParameterValue.toString());
			}
		}


		Builder	resourceBuilder = resource.getRequestBuilder();

		//adds the headerParams given by the user
		for (Parameter headerParameter : headerParameters) {
			String headerParamName= headerParameter.getName();
			Object headerParameterValue = rs.extractHeader(headerParamName);
			if (headerParameter.getMandatory() && headerParameterValue == null) {
				throw new RestException("RestService "+serviceName+": mandatory header parameter "+headerParamName+" not found");
			}
			if (headerParameterValue != null) {
				resourceBuilder = resourceBuilder.header(headerParamName, headerParameterValue);
			}
		}

		BodyDescriptor responseBody=null;
		if(rs.getResponseMediaTypeElement()!=null){
			//set the accepted Media Type for the response representation
			//FIXME: forse occorre aggiungere anche quelli per il fault 
			responseBody = methodDescriptor.getResponseBody(rs.getResponseMediaTypeElement());
			if(responseBody != null){
				resourceBuilder.accept(MediaType.valueOf(responseBody.getMediaType()));  
			}else{
				throw new RestException("RestService "+serviceName+": Response Media Type element"+rs.getResponseMediaTypeElement()+" not found");
			}	
		}		

		MethodType methodType = methodDescriptor.getType();        
		String type="null";
		switch (methodType) {
		case POST:					
			type= "POST";
			break;

		case PUT:					
			type= "PUT";
			break;

		case DELETE:					
			type= "DELETE";
			break;

		case GET:					
			type= "GET";
			break;	

		default:
			break;
		}      

		ClientResponse response = null;        

		if(methodType == MethodType.POST || methodType == MethodType.PUT ){ 

			if(!methodDescriptor.getRequestBodyMediaTypesElements().isEmpty()){
				//set the Content-Type for the request representation
				BodyDescriptor requestBody;
				if (rs.getRequestMediaTypeElement() != null) {
					requestBody = methodDescriptor.getRequestBody(rs.getRequestMediaTypeElement());
				} else {
					String mediaTypesElement = methodDescriptor.getRequestBodyMediaTypesElements().iterator().next();
					requestBody = methodDescriptor.getRequestBody(mediaTypesElement);
				}
				if(requestBody != null){
					resourceBuilder.type(MediaType.valueOf(requestBody.getMediaType())); 
				}else{
					throw new RestException("RestService "+serviceName+": Request Media Type element "+rs.getRequestMediaTypeElement()+" not found");
				}        	
	
				Object requestValue = rs.extractBody(Constants.REST_BODY_REQUEST);        	
				if(requestValue!=null){
					response = resourceBuilder.method(type, ClientResponse.class,requestValue); 
				}else if(requestBody.getElementType()!=null){
					throw new RestException("RestService "+serviceName+": Request Object cannot be null, it must be of type "+requestBody.getType());
				}else{
					response = resourceBuilder.method(type, ClientResponse.class); 
				}
			}else{
				response = resourceBuilder.method(type, ClientResponse.class);        	
			}     

		}else{
			response = resourceBuilder.method(type, ClientResponse.class);        	
		}     

		Status responseStatus= response.getClientResponseStatus();
		Date date = response.getResponseDate();  

		rs.fillHttp(Constants.HTTP_STATUS_CODE, response.getStatus());
		
		if(response.getStatus() >= 400){

			//loop to verify if the response status is equal to the faults presented in the REST service	 
			if(!methodDescriptor.getFaultStatus().isEmpty()){
				BodyDescriptor faultBody = methodDescriptor.getFaultBody(Long.valueOf(response.getStatus()), rs.getFaultMediaTypeElement());
				if (faultBody != null && faultBody.getType() != null) {
					rs.fillBody(String.valueOf(response.getStatus()), response.getEntity(faultBody.getTypeClass()));
					throw new RestException("A REST response fault has been presented. ERROR status: "+response.getStatus()+" "+responseStatus+" "+date+".  "+faultBody.getDocumentation(), response.getStatus(), response.getEntity(faultBody.getTypeClass()));
				}else{
					String res = response.getEntity(String.class);
					rs.fillBody(String.valueOf(response.getStatus()), res);
					throw new RestException("ERROR status: "+response.getStatus()+" "+responseStatus+" "+date, response.getStatus(), res);    
				}
			}else{
				if(response.hasEntity()){
					String res = response.getEntity(String.class);
					rs.fillBody(String.valueOf(response.getStatus()), res);
					throw new RestException("ERROR status: "+response.getStatus()+" "+responseStatus+" "+date, response.getStatus(), res);    
				}				
			}
		}else{
			if (responseBody != null) {
				if(response.hasEntity() && responseBody.getType()!=null){   
					rs.fillBody(Constants.REST_BODY_RESPONSE, response.getEntity(responseBody.getTypeClass()));
				}else{
					if(response.getStatus()!=204){
						rs.fillBody(Constants.REST_BODY_RESPONSE, response.getEntity(String.class));
					}
				} 
			}else{
				if(response.hasEntity()){
					rs.fillBody(Constants.REST_BODY_RESPONSE, response.getEntity(String.class));
				}
			}
		}
	}	
}
