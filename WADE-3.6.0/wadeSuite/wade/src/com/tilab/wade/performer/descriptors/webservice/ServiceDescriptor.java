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
package com.tilab.wade.performer.descriptors.webservice;

import jade.content.onto.Ontology;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.holders.Holder;

import org.apache.axis.AxisFault;
import org.apache.axis.AxisProperties;
import org.apache.axis.Handler;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleChain;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.client.AxisClient;
import org.apache.axis.client.Stub;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.handlers.SimpleSessionHandler;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.message.addressing.AddressingHeaders;
import org.apache.axis.message.addressing.AttributedURI;
import org.apache.axis.message.addressing.EndpointReference;
import org.apache.axis.message.addressing.MessageID;
import org.apache.axis.message.addressing.handler.AddressingHandler;
import org.apache.axis.transport.http.HTTPSender;
import org.apache.axis.transport.http.HTTPTransport;
import org.apache.axis.types.URI;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.wsdl.toJava.Utils;
import org.apache.ws.axis.security.WSDoAllReceiver;
import org.apache.ws.axis.security.WSDoAllSender;
import org.apache.ws.axis.security.handler.WSDoAllHandler;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.token.UsernameToken;

import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.WebService;
import com.tilab.wade.performer.WebServiceAddressingContext;
import com.tilab.wade.performer.WebServiceException;
import com.tilab.wade.performer.WebServiceSecurityContext;
import com.tilab.wade.performer.descriptors.Parameter;

public abstract class ServiceDescriptor implements Serializable {

	protected Logger myLogger = Logger.getMyLogger(ServiceDescriptor.class.getName());
	
	public static final String SERVICE_DESCRIPTOR_SUFFIX = "Descriptor";

	private static final String DEFAULT_PORT = "_DEFAULT_PORT_";

	protected Map<String, PortDescriptor> portDescriptors = new HashMap<String, PortDescriptor>();
	
	public enum ServiceUsage { CLIENT, SERVER, CLIENT_SERVER };

	private boolean multiport;


	public ServiceDescriptor() {
		multiport = this instanceof MultiportDescriptor;
		if (!multiport) {
			portDescriptors.put(DEFAULT_PORT, new PortDescriptor());
		}
	}

	protected abstract Remote getService() throws ServiceException;
	public abstract void setEndpointAddress(String endpointAddress);
	public abstract String getServiceName();

	/**
	 * This method checks if a ServiceDescriptor class matches a given usage and purpose.
	 * @param serviceDescriptorClass ServiceDescriptor class.
	 * @param searchedServiceUsage ServiceUsage (CLIENT, SERVER, CLIENT_SERVER) to check (if null all usages are valid)
	 * @param searchedSServicePurpose Purpose (if null all purposes are valid)
	 * @return true if check is satisfied
	 */
	public static boolean check(Class serviceDescriptorClass, ServiceUsage searchedServiceUsage, String searchedSServicePurpose) {
		// Check class type
		if (!ServiceDescriptor.class.isAssignableFrom(serviceDescriptorClass)) {
			return false;
		}
		
		// If no filter -> is a service! 
		if (searchedServiceUsage == null &&
			searchedSServicePurpose == null) {
			return true;
		}

		// Get descriptor annotation
		ServiceUsage serviceUsage = null;
		String servicePurpose = null;
		ServiceMetaInfo descriptorType = (ServiceMetaInfo) serviceDescriptorClass.getAnnotation(ServiceMetaInfo.class);
		if (descriptorType != null) {
			// Annotation present -> new generation of services 
			serviceUsage = descriptorType.usage();
			String annotServicePurpose = descriptorType.purpose();
			if (!annotServicePurpose.equals(ServiceMetaInfo.NULL)) {
				servicePurpose = annotServicePurpose; 
			}
		} else {
			// Annotation not present -> unknown or old generation of services
			// assumo che siano di tipo CLIENT 
			serviceUsage = ServiceUsage.CLIENT;
		}
		
		// Check service-usage/service-purpose
		if (searchedServiceUsage == null || serviceUsage == searchedServiceUsage) {
			if (searchedSServicePurpose == null) {
				return true;
			} else {
				return searchedSServicePurpose.equals(servicePurpose);
			}
		}
		
		return false;
	}
	
	public Ontology getOntology() {
		return null;
	}
	
	public PortDescriptor getPortDescriptor(String portName) {
		return portDescriptors.get(portName);
	}

	public void addPortDescriptor(String portName, PortDescriptor pd) {
		portDescriptors.put(portName, pd);
	}

	public OperationDescriptor getOperationDescriptor(String operName) {
		PortDescriptor portDescriptor = portDescriptors.get(DEFAULT_PORT);
		return portDescriptor.getOperationDescriptor(operName);
	}

	public void addOperationDescriptor(String operName, OperationDescriptor operDesc) {
		PortDescriptor portDescriptor = portDescriptors.get(DEFAULT_PORT);
		portDescriptor.addOperationDescriptor(operName, operDesc);
	}

	public Set<String> getOperationNames() {
		PortDescriptor portDescriptor = portDescriptors.get(DEFAULT_PORT);
		return portDescriptor.getOperationNames();
	}

	public Set<String> getPortNames() {
		return portDescriptors.keySet();
	}

	@SuppressWarnings("deprecation")
	public void invoke(WebService ws) throws Exception {
		
		// Check webservice consistency
		String serviceName = getServiceName();
		if (serviceName == null) {
			throw new WebServiceException("Missing webservice name");
		}
		String operationName = ws.getOperation();
		if (operationName == null) {
			throw new WebServiceException("Missing operation name for webservice "+serviceName);
		}
		
		// Get service stub
		Stub stub;
		try {
			if (multiport && ws.getPort() != null) {
				stub = (Stub)((MultiportDescriptor)this).getService(ws.getPort());
			} else {
				stub = (Stub)getService();	
			}
		} catch(ServiceException se) {
			throw new WebServiceException("No webservice stub class found for webservice "+serviceName, se);
		}
		
		// Set service call timeout
		int serviceCallTimeout = ws.getTimeout();
		if (serviceCallTimeout >= 0) {
			stub.setTimeout(serviceCallTimeout);
		}
		
		// Set endpoit
		if (ws.getEndpointAddress() != null) {
			stub._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ws.getEndpointAddress());
		}

		// Set Axis Client Configuration
		Handler sessionHandler = (Handler)new SimpleSessionHandler(); 
		
		SimpleChain reqHandlers = new SimpleChain();
		reqHandlers.addHandler(sessionHandler);
		reqHandlers.addHandler(new PrintSOAPHandler(true));
		
		SimpleChain respHandlers = new SimpleChain(); 
		respHandlers.addHandler(sessionHandler); 
		respHandlers.addHandler(new PrintSOAPHandler(false));
		
		Handler pivot = (Handler)new HTTPSender(); 
		Handler transport = new SimpleTargetedChain(reqHandlers, pivot, respHandlers);
		
		SimpleProvider clientConfig = new SimpleProvider();
		clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME, transport); 

		org.apache.axis.client.Service service = (org.apache.axis.client.Service)stub._getService();
		service.setEngineConfiguration(clientConfig); 
		service.setEngine(new AxisClient(clientConfig)); 
		
		// Manage security
		WebServiceSecurityContext sc = ws.getSecurityContext();
		if (sc != null) {
			// Set HTTP Basic Authentication
			String httpUsername = sc.getHttpUsername();
			String httpPassword = sc.getHttpPassword();
			if (httpUsername != null && httpPassword != null) {
				stub.setUsername(httpUsername);
				stub.setPassword(httpPassword);
			}

			// Set WS-Security Username token profile
			String wssUsername = sc.getWSSUsername();
			String wssPassword = sc.getWSSPassword();
			String wssPasswordType = sc.getWSSPasswordType();
			Boolean wssMustUnderstand = sc.isWSSMustUnderstand();
			Integer wssTimeToLive = sc.getWSSTimeToLive();
			
			if ((wssUsername != null && wssPassword != null) ||
				wssTimeToLive != null) {

				// Add handler in service
				WSDoAllSender wssSenderHandler = new WSDoAllSender();
				reqHandlers.addHandler(wssSenderHandler);

				WSDoAllReceiver wssReceiverHandler = new WSDoAllReceiver();
				if (wssTimeToLive != null) {
					// Receiver handler only for timestamp
					respHandlers.addHandler(wssReceiverHandler);
				}
				
				// Set global WS-Security 
	            if (wssMustUnderstand != null) {
	            	stub._setProperty(WSHandlerConstants.MUST_UNDERSTAND, wssMustUnderstand.toString());
	            }
	            
				// Set WS-Security Username Token
				if (wssUsername != null && wssPassword != null) {
				
					// Add Username Token management only in sender handler
					addHandlerAction(wssSenderHandler, WSHandlerConstants.USERNAME_TOKEN);
					
					stub._setProperty(UsernameToken.PASSWORD_TYPE, wssPasswordType);
					stub._setProperty(WSHandlerConstants.USER, wssUsername);
					
					WSSPasswordCallback passwordCallback = new WSSPasswordCallback(wssPassword);
					stub._setProperty(WSHandlerConstants.PW_CALLBACK_REF, passwordCallback);
				}
				
	            // Set WS-Security Timestamp
	            if (wssTimeToLive != null) {
	            	// Add timestamp management in sender & receiver handlers
	            	addHandlerAction(wssSenderHandler, WSHandlerConstants.TIMESTAMP);
	            	addHandlerAction(wssReceiverHandler, WSHandlerConstants.TIMESTAMP);

	            	stub._setProperty(WSHandlerConstants.TTL_TIMESTAMP, wssTimeToLive.toString());
	            }
			}
			
			// Certificates
			String trustStore = sc.getTrustStore();
			String trustStorePassword = sc.getTrustStorePassword();
			if (trustStore != null && trustStorePassword != null) {
				System.setProperty("javax.net.ssl.trustStore", trustStore);
				System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
			}
			Boolean enableCertificateChecking = sc.isEnableCertificateChecking();
			if (enableCertificateChecking != null) {
				if (enableCertificateChecking.booleanValue()) {
					AxisProperties.setProperty("axis.socketSecureFactory", "");
				} else {
					AxisProperties.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
				}
			}
		}		
		
		// Manage addressing
		WebServiceAddressingContext ac = ws.getAddressingContext();
		if (ac != null) {
			// Add handler in service
			AddressingHandler addressingHandler = new AddressingHandler();
			reqHandlers.addHandler(addressingHandler);
			respHandlers.addHandler(addressingHandler);
			
			// Prepare header
			AddressingHeaders addressingHeaders = new AddressingHeaders();
			
			Boolean mustUnderstand = ac.isMustUnderstand();
			if (mustUnderstand != null) {
				stub._setProperty(org.apache.axis.message.addressing.Constants.ENV_ADDRESSING_SET_MUST_UNDERSTAND, mustUnderstand.toString());
			}

			Boolean sendDefaultMessageID = ac.isSendDefaultMessageID();
			if (sendDefaultMessageID != null) {
				stub._setProperty(org.apache.axis.message.addressing.Constants.SEND_DEFAULT_MESSAGEID, sendDefaultMessageID.toString());
			}
			
			Boolean sendDefaultFrom = ac.isSendDefaultFrom();
			if (sendDefaultFrom != null) {
				stub._setProperty(org.apache.axis.message.addressing.Constants.SEND_DEFAULT_FROM, sendDefaultFrom.toString());
			}
			
			Boolean sendDefaultTo = ac.isSendDefaultTo();
			if (sendDefaultTo != null) {
				stub._setProperty(org.apache.axis.message.addressing.Constants.SEND_DEFAULT_TO, sendDefaultTo.toString());
			}

			String version = ac.getVersion();
			if (version != null) {
				stub._setProperty(org.apache.axis.message.addressing.Constants.ENV_ADDRESSING_NAMESPACE_URI, version);
			}
			
			String messageID = ac.getMessageID();
			if (messageID != null) {
				addressingHeaders.setMessageID(new MessageID(new URI(messageID)));
			}
			
			String action = ac.getAction();
			if (action != null) {
				addressingHeaders.setAction(action);
			}
			
			String from = ac.getFrom();
			if (from != null) {
				addressingHeaders.setFrom(new EndpointReference(from));
			}
			
			String to = ac.getTo();
			if (to != null) {
				addressingHeaders.setTo(new AttributedURI(to));
			}
			
			String replyTo = ac.getReplyTo();
			if (replyTo != null) {
				addressingHeaders.setReplyTo(new EndpointReference(replyTo));
			}
			
			String faultTo = ac.getFaultTo();
			if (faultTo != null) {
				addressingHeaders.setFaultTo(new EndpointReference(faultTo));
			}
			
//			ReferenceParametersType referenceParameters = ac.getReferenceParameters();
//			if (referenceParameters != null) {
//				addressingHeaders.setReferenceParameters(referenceParameters);
//			}
//			
//			ReferencePropertiesType referenceProperties = ac.getReferenceProperties();
//			if (referenceProperties != null) {
//				addressingHeaders.setReferenceProperties(referenceProperties);
//			}
			
			stub._setProperty(org.apache.axis.message.addressing.Constants.ENV_ADDRESSING_REQUEST_HEADERS, addressingHeaders);
		}
		
		// Get operation name and method name
		String methodName = Utils.xmlNameToJava(operationName);

		// Get port name
		String portName;
		if (multiport) {
			portName = ws.getPort();
		} else {
			portName = DEFAULT_PORT;
		}

		// Get operation descriptor
		PortDescriptor portDescriptor = portDescriptors.get(portName);
		if (portDescriptor == null) {
			throw new WebServiceException("Descriptor of port "+portName+" not found in webservice descrptor "+getClass().getName());
		}
		OperationDescriptor operationDescriptor = portDescriptor.getOperationDescriptor(operationName);
		if (operationDescriptor == null) {
			throw new WebServiceException("Descriptor of operation "+operationName+" not found in webservice descrptor "+getClass().getName());
		}

		// Get formal and actual parameters
		jade.util.leap.Map actualParams = ws.getParams();
		List formalParams = operationDescriptor.getFormalParams();
		
		// Get formal and actual headers
		jade.util.leap.Map actualHeaders = ws.getHeaders();
		List formalHeaders = operationDescriptor.getFormalHeaders();
		
		// Get return value name
		String returnValueName = operationDescriptor.getReturnValueName();
		
		// Create vector of method parameters 
		Vector<Parameter> methodParams = getMethodParams(formalParams, actualParams, formalHeaders, actualHeaders, returnValueName);

		// Get owner for service ClassLoader
		ClassLoader serviceClassLoader;
		if(ws.getOwner() != null) {
			serviceClassLoader = ws.getOwner().getClass().getClassLoader();
		} else {
			serviceClassLoader = this.getClass().getClassLoader();
		}
			
		// Array of method parameters class
		Class[] methodValuesClass = new Class[methodParams.size()];
		
		// Array of method parameters value
		Object[] methodValuesObj = new Object[methodParams.size()]; 
		
		// Loop all method parameters to create array of values and class
		for (int index=0; index<methodParams.size(); index++) {
			Parameter methodParam = methodParams.get(index);

			// Add method parameter class to array
			Class methodParamClass = getParamClass(methodParam, serviceClassLoader);
			methodValuesClass[index] = methodParamClass;
			
			// Get method parameter value
			Object methodParamValue = getParamValue(methodParam, methodParamClass);
			
			// Convert value in Holder if required
			if (javax.xml.rpc.holders.Holder.class.isAssignableFrom(methodParamClass)) {				
				methodParamValue = JavaUtils.convert(methodParamValue, methodParamClass);
			}

			// Add method parameter value to array
			methodValuesObj[index] = methodParamValue;
		}
		
		// Loop for all explicit headers and set it into web service
		Iterator formalHeaderIt = formalHeaders.iterator();
		while(formalHeaderIt.hasNext()) {
			Header formalHeader = (Header)formalHeaderIt.next();
			String headerName = formalHeader.getName();
			int signaturePosition = formalHeader.getSignaturePosition();

			// If header explicit and is of mode IN or IN-OUT
			if (signaturePosition == Header.EXPLICIT_HEADER &&
				(formalHeader.getMode() == Constants.IN_MODE ||
				formalHeader.getMode() == Constants.INOUT_MODE)) {
				
				// If exist header value -> set it in webservice call
				
				Object actualHeaderValue = actualHeaders.get(headerName);
				if (actualHeaderValue != null) {
					SOAPHeaderElement header = new SOAPHeaderElement(formalHeader.getNamespace(), headerName, actualHeaderValue);
					header.setActor(formalHeader.getActor());
					header.setMustUnderstand(formalHeader.isMustUnderstand());
					header.setRelay(formalHeader.isRelay());
					stub.setHeader(header);
				}
			}
		}
		
		// Get operation method
		Method operationMethod;
		try {
			operationMethod = stub.getClass().getMethod(methodName, methodValuesClass);
		} catch (Exception e) {
			throw new WebServiceException("WebService "+serviceName+": method "+methodName+" not found in service stub", e);
		}
		
		// Invoke operation method
		Object returnValue = null;
		try {
			returnValue = operationMethod.invoke(stub, methodValuesObj);
		} catch (InvocationTargetException ie) {
			if (ie.getCause() instanceof RemoteException) {
				throw (RemoteException)ie.getCause();
			} else {
				throw new WebServiceException("WebService "+serviceName+": operation "+operationName+" invocation error", ie.getCause());
			}
		} catch (Exception e) {
			throw new WebServiceException("WebService "+serviceName+": method "+methodName+" invocation error", e);
		}

		// Read explicit headers from webservice call
		formalHeaderIt = formalHeaders.iterator();
		while(formalHeaderIt.hasNext()) {
			Header formalHeader = (Header)formalHeaderIt.next(); 
			String headerName = formalHeader.getName();
			int signaturePosition = formalHeader.getSignaturePosition();
			
			// If header is explicit and of mode OUT or IN-OUT
			if (signaturePosition == Header.EXPLICIT_HEADER &&
				(formalHeader.getMode() == Constants.OUT_MODE ||
				formalHeader.getMode() == Constants.INOUT_MODE)) {
		
				// Get response header value
				Object headerValue = getHeaderValue(stub, formalHeader, serviceClassLoader);
				if (headerValue != null) {

					// Insert output values into headers map
					formalHeader.setValue(headerValue);
					ws.getHeaders().put(headerName, headerValue);
				}
			}
		}
		
		// Loop all method parameters to read method output values
		for (int index=0; index<methodParams.size(); index++) {
			Parameter methodParam = methodParams.get(index);
			String paramName = methodParam.getName();
			int paramMode = methodParam.getMode();

			// Elaborate only output params
			if (paramMode == Constants.OUT_MODE ||
				paramMode == Constants.INOUT_MODE) {

				// Get holder value
				Holder paramHolderValue = (Holder)methodValuesObj[index];

				// Convert holder value in real value
				Class paramValueClass = JavaUtils.getHolderValueType(paramHolderValue.getClass());
				Object methodParamValue = JavaUtils.convert(paramHolderValue, paramValueClass);
				
				// Set value
				methodParam.setValue(methodParamValue);
				
				// Set methodParam in actual headers or params map
				if (methodParam instanceof Header) {
					ws.getHeaders().put(paramName, methodParamValue);
				} else {
					ws.getParams().put(paramName, methodParamValue);
				}
			}			
		}		

		// Set return value
		// See WebService.setReturnValue(xxx) 
		ws.setReturnValue(returnValue);		
		if (returnValueName != null) {
			ws.getParams().put(returnValueName, returnValue);
		}
	}
	
	private static void addHandlerAction(WSDoAllHandler handler, String action) {
    	String prevAction = (String)handler.getOption(WSHandlerConstants.ACTION);
    	if (prevAction != null) {
    		action = prevAction + " " + action;
    	}
    	
    	handler.setOption(WSHandlerConstants.ACTION, action);
	}
	
	private Object getHeaderValue(Stub service, Header formalHeader, ClassLoader headersClassLoader) throws WebServiceException {

		String name = formalHeader.getName();
		
		// Try with namespace
		SOAPHeaderElement header = service.getResponseHeader(formalHeader.getNamespace(), name);
		if (header == null) {
			// Try without namespace
			header = service.getResponseHeader(null, name);
		}
		Object headerValue = null;
		if (header != null) {
			// Get header value class
			Class headerClass = getParamClass(formalHeader, headersClassLoader);
			
			// Get value
			try {
				headerValue = header.getObjectValue(headerClass);
			} catch (Exception e) {
				throw new WebServiceException("Header "+name+" error getting value");
			}
		}
		return headerValue;
	}
	
	private Vector<Parameter> getMethodParams(List formalParams, jade.util.leap.Map actualParams, List formalHeaders, jade.util.leap.Map actualHeaders, String returnValueName) throws WebServiceException {
		Vector<Parameter> methodParams = new Vector<Parameter>();
		int methodParamsSize = 0;
		
		// Fill methodParams with header that go in ws method call
		Iterator it = formalHeaders.iterator();
		while(it.hasNext()) {
			Header formalHeader = (Header)it.next();
			int signaturePosition = formalHeader.getSignaturePosition();
			// Current header is in method -> add it
			if (signaturePosition != Header.EXPLICIT_HEADER) {
				// Check if methodParams size is sufficient 
				if (methodParams.size() <= signaturePosition) {
					// Increase methodParams size
					methodParams.setSize(signaturePosition+1);
				}
				
				// Create method param and add to methodParams
				Parameter methodParam = createMethodParam(formalHeader, actualHeaders);
				methodParams.set(signaturePosition, methodParam);
				
				methodParamsSize++;
			}
		}
		
		// Calculate total size of methodParams -> set it
		methodParamsSize = methodParamsSize + formalParams.size();
		
		// ATTENZIONE! Nella nuova gestione non esiste più il returnValueType ma c'è il returnValueName
		// che definisce il nome del return value definito tra i parametri di output.
		// Nell'invocazione dello stub axis deve essere quindi escluso
		if (returnValueName != null) {
			methodParamsSize = methodParamsSize - 1;
		}
		methodParams.setSize(methodParamsSize);
		
		// Fill empty hole in methodParams with parameters 
		it = formalParams.iterator();
		for(int i=0; i<methodParamsSize; i++) {
			if (methodParams.get(i) == null) {
				Parameter formalParam = (Parameter)it.next();

				// Skip return value (see up)
				if (returnValueName != null && 
					formalParam.getName().equals(returnValueName) && 
					formalParam.getMode() == Constants.OUT_MODE) {
					continue;
				}
				
				// Create method param and add to methodParams
				Parameter methodParam = createMethodParam(formalParam, actualParams);
				methodParams.set(i, methodParam);
			}
		}		
		
		return methodParams;
	}
	
	private Parameter createMethodParam(Parameter formal, jade.util.leap.Map actuals) throws WebServiceException {
		// Create method param
		Parameter methodParam = cloneParam(formal);
		String name = formal.getName();
		int mode = formal.getMode();

		// If mode is IN or IN-OUT -> set value
		if (mode == Constants.IN_MODE ||
			mode == Constants.INOUT_MODE) {
			
			// Set header value in methodParam 
			methodParam.setValue(actuals.get(name));
		}

		return methodParam;
	}
	
	private Parameter cloneParam(Parameter param) throws WebServiceException {
		if (param == null) {
			throw new WebServiceException("Parameter clone error: param = null"); 
		}
		
		Parameter cloneParam;
		if (param instanceof Header) {
			cloneParam = new Header();
		} else {
			cloneParam = new Parameter();
		}
		cloneParam.setName(param.getName());
		cloneParam.setMode(param.getMode());
		cloneParam.setType(param.getType());
		cloneParam.setValue(param.getValue());
		return cloneParam;
	}

	private Class getParamClass(Parameter methodParam, ClassLoader paramsClassLoader) throws WebServiceException {
		try {
			return methodParam.getTypeClass(false, paramsClassLoader);
		} catch (Exception e) {
			throw new WebServiceException("Parameter "+methodParam.getName()+" class ("+methodParam.getType()+") not found");			
		}
	}
	
	private Object getParamValue(Parameter methodParam, Class methodParamClass) throws WebServiceException {
		Object paramValue;
		if (methodParam.getMode() == Constants.OUT_MODE) {
			// Required output value -> create holder object 
			try {
				paramValue = methodParamClass.newInstance();
			} catch (Exception e) {
				throw new WebServiceException("Parameter "+methodParam.getName()+" error creating instance of "+methodParamClass);
			}
		} else {
			// Assigned input or in-out value
			paramValue = methodParam.getValue();
		}
		
		return paramValue;
	}

	
	// Inner class to manage WS-Security Username token
	private class WSSPasswordCallback implements CallbackHandler {
		
		private String password;

		public WSSPasswordCallback(String password) {
			this.password = password;
		}
		
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (int i = 0; i < callbacks.length; i++) {
				if (callbacks[i] instanceof WSPasswordCallback) {
					WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
					pc.setPassword(password);
				} else {
					throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
				}
			}
		}
	}
	
	
	// Inner class to manage print of SOAP request/response
	private class PrintSOAPHandler extends BasicHandler {  
		  
		private boolean request;
		
		public PrintSOAPHandler(boolean request) {
			this.request = request;
		}
		
	    public void invoke(MessageContext msgContext) throws AxisFault {  
	        try {  
	        	String message;
	        	if (request) {
		            message = msgContext.getRequestMessage().getSOAPPartAsString();
	        	} else {
	        		message = msgContext.getResponseMessage().getSOAPPartAsString();
	        	}
	        	
	        	StringBuilder sb = new StringBuilder();
	        	sb.append("---- SOAP "+(request?"request":"response")+" ----");
	        	sb.append(System.getProperty("line.separator"));
	        	sb.append("Endpoint URL: "+msgContext.getProperty(MessageContext.TRANS_URL));
	        	sb.append(System.getProperty("line.separator"));
	        	sb.append(message);
	        	sb.append(System.getProperty("line.separator"));
	        	sb.append("----");
	        	myLogger.log(Level.CONFIG, sb.toString());
	        } catch (Exception e) {  
	            throw new AxisFault("Failed to manage printing of SOAP message");  
	        }  
	    }
	}  
}



