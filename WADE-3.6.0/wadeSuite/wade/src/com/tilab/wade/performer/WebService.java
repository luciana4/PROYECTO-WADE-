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

import java.util.ArrayList;
import java.util.List;

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.util.leap.HashMap;
import jade.util.leap.Iterator;
import jade.util.leap.Map;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.webservice.Header;
import com.tilab.wade.performer.descriptors.webservice.OperationDescriptor;
import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor;
import com.tilab.wade.utils.OntologyUtils;

/**
 * The building-block corresponding to the (static) invocation of web service.
 * @see WebServiceInvocationBehaviour
 */
public class WebService extends InvocableBuildingBlock {

	transient private ServiceDescriptor descriptor;
	
	private WorkflowBehaviour owner;
	private Map params = new HashMap();
	private Map headers = new HashMap();
	private Object returnValue;
	private String port;
	private String operation;
	private String descriptorClassName;
	private int timeout;
	private String endpointAddress;
	private WebServiceSecurityContext securityContext;
	private WebServiceAddressingContext addressingContext;
	
	
	public WebService(WorkflowBehaviour owner, WebServiceInvocationBehaviour activity) {
		super(activity);
		
		this.owner = owner;
		timeout = -1;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public WebServiceSecurityContext getSecurityContext() {
		return securityContext;
	}

	public void setSecurityContext(WebServiceSecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public WebServiceAddressingContext getAddressingContext() {
		return addressingContext;
	}

	public void setAddressingContext(WebServiceAddressingContext addressingContext) {
		this.addressingContext = addressingContext;
	}
	
	public WorkflowBehaviour getOwner() {
		return owner;
	}

	public String getDescriptorClassName() {
		return descriptorClassName;
	}
	
	public void setDescriptorClassName(String descriptorClassName) {
		this.descriptorClassName = descriptorClassName;
	}

	ServiceDescriptor getDescriptor() throws Exception {
		if (descriptor == null) {
			ClassLoader wcl = CAServices.getInstance(owner.getAgent()).getDefaultClassLoader();
			descriptor = (ServiceDescriptor) Class.forName(descriptorClassName, true, wcl).newInstance();
		}
		return descriptor;
	}
	
	public void setEndpointAddress(String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	public String getEndpointAddress() {
		return endpointAddress;
	}
	
	public void invoke() throws Exception {
		// Before invoke the webservice set the security-context in this order:
		// - runtime activity specific
		// - runtime default
		// - wf activity specific

		// Try to get the runtime webservice-security passed as modifier in workflow (activity specific or default)
		// If runtime webservice-security are passed use it, otherwise use building-block security
		WebServiceSecurityContext wfSecurityContext = EngineHelper.extractWebServiceSecurityContext(owner, activity.getBehaviourName());
		if (wfSecurityContext != null) {
			securityContext = wfSecurityContext; 
		}

		// Try to get the runtime webservice-addressing passed as modifier in workflow
		// If runtime webservice-addressing is passed use it, otherwise use building-block addressing
		WebServiceAddressingContext wfAddressingContext = EngineHelper.extractWebServiceAddressingContext(owner, activity.getBehaviourName());
		if (wfAddressingContext != null) {
			addressingContext = wfAddressingContext; 
		}
		
		// Get service descriptor
		ServiceDescriptor serviceDescriptor;
		try {
			serviceDescriptor = getDescriptor();
		} catch(Exception e) {
			throw new WebServiceException("No descriptor found ("+getDescriptorClassName()+")", e);
		}

		// Invoke service
		serviceDescriptor.invoke(this);
	}
	
	/**
	 * Fill an input parameter
	 * The fill() methods are called by the Engine just before executing this webservice 
	 */
	public final void fill(String key, Object value) {
		params.put(key, value);
	}

	/**
	   Retrieve the value of an output parameter after the execution of the webservice
	 */
	public final Object extract(String key) {
		return params.get(key);
	}

	/**
	 * Fill an input header
	 * The fillHeader() methods are called by the Engine just before executing this webservice 
	 */
	public final void fillHeader(String key, Object value) {
		headers.put(key, value);
	}
	
	/**
	   Retrieve the value of an output header after the execution of the webservice
	 */
	public final Object extractHeader(String key) {
		return headers.get(key);
	}
	
	public Map getParams() {
		return params;
	}
	
	public Map getHeaders() {
		return headers;
	}

	@Deprecated
	public Object getReturnValue() {
		return returnValue;
	}

	@Deprecated
	// Il ReturnValue è un concetto di Axis 1.x, in wsdl esistono solo parametri di IN e OUT
	// La prima versione del descrittore di webservice (generato da wolf) generava
	// per ogni operazione un xxx.setReturnValueType(nome-classe) per identificare il tipo 
	// di oggetto ritornato dai metodi dello stub.
	// Nella nuova gestione è stato sostituito da xxx.setReturnValueName(nome-param) che 
	// identifica nella collezione di parametri di output quello che Axis gestisce come 
	// return-value nei metodi dello stub.
	// Questo metodo con la relativa gestione nella invoke() del ServiceDescriptor è
	// mantenuto per compatibilità con i vecchi descrittori già generati da wolf.
	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}
	
	public void reset() {
		params.clear();
		headers.clear();
		returnValue = null;
	}

	
	///////////////////////////////////////////
	// Building block methods
	
	private OperationDescriptor getOperationDescriptor() {
		try {
			if(port != null) {
				return getDescriptor().getPortDescriptor(port).getOperationDescriptor(operation);
			} else {
				return getDescriptor().getOperationDescriptor(operation);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean requireAbsParameters() {
		return false;
	}

	@Override
	public Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("WebServiceOnto");
		
		OperationDescriptor operationDescriptor = getOperationDescriptor();

		// Params to onto
		jade.util.leap.List formalParams = operationDescriptor.getFormalParams();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			OntologyUtils.addFormalParameterToOntology(onto, p, owner.getClass().getClassLoader());
		}
		
		// Headers to onto
		jade.util.leap.List formalHeaders = operationDescriptor.getFormalHeaders();
		it = formalHeaders.iterator();
		while(it.hasNext()) {
			Header h = (Header)it.next();
			OntologyUtils.addFormalParameterToOntology(onto, h, owner.getClass().getClassLoader());
		}
		
		return onto;
	}
	
	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		Class parameterClass = getParameterClass(key);
		return AbsHelper.createAbsTemplate(parameterClass, getOntology());
	}
	
	private Class getParameterClass(String key) throws Exception {
		OperationDescriptor operationDescriptor = getOperationDescriptor();
		jade.util.leap.List params = null;
		String name = null;
		Class paramClass = null;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			name = key.substring(sepPos+1);
			params = operationDescriptor.getFormalHeaders();
		} else {
			// Parameter
			name = key;
			params = operationDescriptor.getFormalParams();
		}

		// Search in params or headers
		Iterator it = params.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(name)) {
				paramClass = p.getTypeClass(false, owner.getClass().getClassLoader());
				break;
			}
		}
		
		if (paramClass == null) {
			throw new WebServiceException("Parameter class type not found for key "+key);
		}
		
		return paramClass;
	}

	private List<String> getParameterNames(int mode) {
		OperationDescriptor operationDescriptor = getOperationDescriptor();
		
		// Add parameters (<parameterName>)
		List<String> inputParameterNames = new ArrayList<String>();
		jade.util.leap.List formalParams = operationDescriptor.getFormalParams();
		Iterator it = formalParams.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getMode() == mode ||
				p.getMode() == Constants.INOUT_MODE) {
				inputParameterNames.add(p.getName());
			}
		}

		// Add headers (header.<headerName>)
		jade.util.leap.List formalHeaders = operationDescriptor.getFormalHeaders();
		it = formalHeaders.iterator();
		while(it.hasNext()) {
			Header h = (Header)it.next();
			if (h.getMode() == mode ||
				h.getMode() == Constants.INOUT_MODE) {
				inputParameterNames.add(Constants.WS_HEADER_PREFIX+Constants.WS_PREFIX_SEPARATOR+h.getName());
			}
		}
		
		return inputParameterNames;
	}
	
	@Override
	public List<String> getInputParameterNames() {
		return getParameterNames(Constants.IN_MODE);
	}

	@Override
	public List<String> getOutputParameterNames() {
		return getParameterNames(Constants.IN_MODE);
	}
	
	private Object getValue(String key) {
		Object value;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			value = extractHeader(key.substring(sepPos+1));
		} else {
			// Parameter
			value = extract(key);
		}
		
		return value;
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
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			fillHeader(key.substring(sepPos+1), value);
		} else {
			// Parameter
			fill(key, value);
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
		boolean inputEmpty;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			inputEmpty = !headers.containsKey(key.substring(sepPos+1));
		} else {
			// Parameter
			inputEmpty = !params.containsKey(key);
		}
		return inputEmpty;
	}

	private Parameter getFormalDescriptor(String key, int mode) {
		OperationDescriptor operationDescriptor = getOperationDescriptor();
		
		jade.util.leap.List formals;
		String name;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			formals = operationDescriptor.getFormalHeaders();
			name = key.substring(sepPos+1);
		} else {
			// Parameter
			formals = operationDescriptor.getFormalParams();
			name = key;
		}

		Iterator it = formals.iterator();
		while(it.hasNext()) {
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(name) && 
				(p.getMode() == mode ||
				 p.getMode() == Constants.INOUT_MODE)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public Parameter getInputParameter(String key) {
		return getFormalDescriptor(key, Constants.IN_MODE);
	}
	
	@Override
	public Parameter getOutputParameter(String key) {
		return getFormalDescriptor(key, Constants.OUT_MODE);
	}
}
