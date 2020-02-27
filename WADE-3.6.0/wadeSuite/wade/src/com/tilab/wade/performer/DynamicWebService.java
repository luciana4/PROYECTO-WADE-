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

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.onto.BasicOntology;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.TermSchema;
import jade.util.leap.Iterator;
import jade.webservice.dynamicClient.AddressingProperties;
import jade.webservice.dynamicClient.DynamicClient;
import jade.webservice.dynamicClient.DynamicClientCache;
import jade.webservice.dynamicClient.DynamicClientException;
import jade.webservice.dynamicClient.DynamicClientProperties;
import jade.webservice.dynamicClient.OperationInfo;
import jade.webservice.dynamicClient.ParameterInfo;
import jade.webservice.dynamicClient.SecurityProperties;
import jade.webservice.dynamicClient.WSData;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.tilab.wade.performer.descriptors.Parameter;

/**
 * The building-block corresponding to the dynamic invocation of web service.
 * @see DynamicWebServiceInvocationBehaviour
 */
public class DynamicWebService extends InvocableBuildingBlock {

	private WorkflowBehaviour owner;

	private URI wsdl;
	private URL endpoint;
	private String serviceName;
	private String portName;
	private String operationName;
	private int timeout;
	private WebServiceSecurityContext securityContext;
	private WebServiceAddressingContext addressingContext;
	private boolean noWrap;
	private String packageName;

	private WSData wsInputData;
	private WSData wsOutputData;
	private transient DynamicClient dynamicClient;
	private transient SecurityProperties dcSecurityProperties;
	private transient AddressingProperties dcAddressingProperties;
	private transient String wsdlHttpUsername;
	private transient String wsdlHttpPassword;


	public DynamicWebService(WorkflowBehaviour owner, DynamicWebServiceInvocationBehaviour activity) {
		super(activity);

		this.owner = owner;
		timeout = -1;
		noWrap = false;

		wsInputData = new WSData();
		wsOutputData = new WSData();
	}

	public URI getWsdl() {
		return wsdl;
	}

	public void setWsdl(URI wsdl) {
		this.wsdl = wsdl;
		dynamicClient = null;
	}

	public URL getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(URL endpoint) {
		this.endpoint = endpoint;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
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
	
	public boolean isNoWrap() {
		return noWrap;
	}

	public void setNoWrap(boolean noWrap) {
		this.noWrap = noWrap;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}

	public WorkflowBehaviour getOwner() {
		return owner;
	}

	public void reset() {
		wsInputData = new WSData();
		wsOutputData = new WSData();
	}

	public final void fill(String key, Object value) {
		wsInputData.setParameter(key, getAbsValue(value));
	}
	public final void fill(String key, String value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, boolean value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, int value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, long value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, float value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, double value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, Date value) {
		wsInputData.setParameter(key, value);
	}
	public final void fill(String key, byte[] value) {
		wsInputData.setParameter(key, value);
	}

	public final void fillHeader(String key, Object value) {
		wsInputData.setHeader(key, getAbsValue(value));
	}
	public final void fillHeader(String key, String value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, boolean value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, int value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, long value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, float value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, double value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, Date value) {
		wsInputData.setHeader(key, value);
	}
	public final void fillHeader(String key, byte[] value) {
		wsInputData.setHeader(key, value);
	}

	public final Object extract(String key) {
		return getOutput(key);
	}
	public final Object extract(String key, Class clazz) throws OntologyException {
		return castValue(extract(key), clazz);
	}

	public final Object extractHeader(String key) {
		return getOutput(Constants.WS_HEADER_PREFIX+Constants.WS_PREFIX_SEPARATOR+key);
	}
	public final Object extractHeader(String key, Class clazz) throws OntologyException {
		return castValue(extractHeader(key), clazz);
	}

	private static void addClassToOnto(BeanOntology beanOnto, Class clazz) throws OntologyException {
		if (beanOnto.getSchema(clazz) == null) {
			beanOnto.add(clazz);
		}
	}

	private static AbsObject getAbsValue(Object value) {
		AbsObject absValue;

		// Check if value is null
		if (value == null) {
			absValue = null;
		}

		// Check if value is already an abs
		else if (value instanceof AbsObject) {
			absValue = (AbsObject)value;
		}

		// Convert value into abs		
		else {
			try {
				BeanOntology beanOnto = new BeanOntology("TEMP-ONTO");

				// java array
				if (value.getClass().isArray()) {
					jade.util.leap.List jadeList = new jade.util.leap.ArrayList();
					for (int i = 0; i < Array.getLength(value) ; i++) {
						Object o = Array.get(value, i);
						jadeList.add(o);
						addClassToOnto(beanOnto, o.getClass());
					}
					value = jadeList;
				}
				// java collection
				else if (value instanceof java.util.Collection) {
					jade.util.leap.List jadeList = new jade.util.leap.ArrayList();
					java.util.Collection javaList = (java.util.Collection)value; 
					for (Object o : javaList) {
						jadeList.add(o);
						addClassToOnto(beanOnto, o.getClass());
					}
					value = jadeList;
				}
				// jade collection 
				else if (value instanceof jade.util.leap.Collection) {
					Iterator it = ((jade.util.leap.Collection)value).iterator();
					while(it.hasNext()) {
						addClassToOnto(beanOnto, it.next().getClass());
					}
				}
				// object
				else {
					addClassToOnto(beanOnto, value.getClass());
				}

				absValue = beanOnto.fromObject(value);

			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		return absValue;
	}

	private static Object getObjectValue(AbsObject abs, Class clazz) throws OntologyException {
		BeanOntology beanOnto = new BeanOntology("TEMP-ONTO");

		// Add element array class
		if (clazz.isArray() && abs instanceof AbsAggregate) {
			beanOnto.add(clazz.getComponentType());
		}
		// Add generic class 
		else {
			beanOnto.add(clazz);
		}

		Object obj = beanOnto.toObject(abs);

		// Java array
		if (clazz.isArray() && obj instanceof jade.util.leap.Collection) {

			jade.util.leap.List jadeColl = (jade.util.leap.List)obj;
			Object javaArray = Array.newInstance(clazz.getComponentType(), jadeColl.size());
			for (int i = 0; i < jadeColl.size(); i++) {
				Array.set(javaArray, i, jadeColl.get(i));
			}
			obj = javaArray;
		}

		return obj;
	}

	private static Object castValue(Object obj, Class clazz) throws OntologyException {
		Object value = null;

		// Directly assignable
		if (clazz.isAssignableFrom(obj.getClass())) {
			value = obj;
		}
		// Abs value 
		else if (obj instanceof AbsObject) {
			value = getObjectValue((AbsObject)obj, clazz);
		}
		// Not supported
		else {
			throw new ClassCastException("Cannot convert "+obj+" in "+clazz);
		}
		return value;
	}

	private static Object getPrimitiveValue(AbsPrimitive absPrimitive) {
		Object value = null;
		if (BasicOntology.BOOLEAN.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getBoolean();
		}
		else if (BasicOntology.STRING.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getString();
		}
		else if (BasicOntology.INTEGER.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getInteger();
		}
		else if (BasicOntology.FLOAT.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getFloat();
		}
		else if (BasicOntology.DATE.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getDate();
		}
		else if (BasicOntology.BYTE_SEQUENCE.equals(absPrimitive.getTypeName())) {
			value = absPrimitive.getByteSequence();
		}
		return value;
	}

	public void invoke() throws Exception {
		// Get DynamicClient instance
		DynamicClient dc = getDynamicClient();

		// Invoke web service
		wsOutputData = dc.invoke(serviceName, portName, operationName, endpoint, timeout, dcSecurityProperties, dcAddressingProperties, wsInputData);
	}

	/**
	 *	Discover WSDL if not already present in DynamicClientCache
	 */
	private DynamicClient getDynamicClient() throws DynamicClientException {

		// Check if dynamic client already present in this building block
		if (dynamicClient == null) {

			// Prepare WSDC properties
			DynamicClientProperties dcp = new DynamicClientProperties();
			dcp.setNoWrap(noWrap);
			dcp.setPackageName(packageName);

			// Get DynamicClientCache instance
			DynamicClientCache dcc = DynamicClientCache.getInstance();

			// Apply security context
			applySecurityContext(dcc);

			// Apply addressing context
			applyAddressingContext(dcc);
			
			// Discover WSDL
			dynamicClient = dcc.get(wsdl, wsdlHttpUsername, wsdlHttpPassword, dcp);
		}
		return dynamicClient;
	}

	private void applySecurityContext(DynamicClientCache dcc) {
		// Before invoke the webservice or discovery the wsdl set the security-context in this order:
		// - runtime activity specific
		// - runtime default
		// - wf activity specific
		
		// Try to get the runtime webservice-security passed as modifier in workflow (activity specific or default)
		// If runtime webservice-security is not present use building-block security
		WebServiceSecurityContext currentScurityContext = EngineHelper.extractWebServiceSecurityContext(owner, activity.getBehaviourName());
		if (currentScurityContext == null) {
			currentScurityContext = securityContext; 
		}

		// Prepare wsdc SecurityProperties to invoke and apply security to discovery wsdl  
		dcSecurityProperties = null;
		wsdlHttpUsername = null;
		wsdlHttpPassword = null;
		if (currentScurityContext != null) {
			wsdlHttpUsername = currentScurityContext.getWsdlHttpUsername();
			wsdlHttpPassword = currentScurityContext.getWsdlHttpPassword();

			dcSecurityProperties = new SecurityProperties();
			dcSecurityProperties.setHttpUsername(currentScurityContext.getHttpUsername());
			dcSecurityProperties.setHttpPassword(currentScurityContext.getHttpPassword());
			dcSecurityProperties.setWSSUsername(currentScurityContext.getWSSUsername());
			dcSecurityProperties.setWSSPassword(currentScurityContext.getWSSPassword());
			dcSecurityProperties.setWSSPasswordType(currentScurityContext.getWSSPasswordType());
			if (currentScurityContext.isWSSMustUnderstand() != null) {
				dcSecurityProperties.setWSSMustUnderstand(currentScurityContext.isWSSMustUnderstand());
			}
			if (currentScurityContext.getWSSTimeToLive() != null) {
				dcSecurityProperties.setWSSTimeToLive(currentScurityContext.getWSSTimeToLive());
			}
			if (currentScurityContext.getTrustStore() != null &&
					currentScurityContext.getTrustStorePassword() != null) {
				dcc.setTrustStore(currentScurityContext.getTrustStore());
				dcc.setTrustStorePassword(currentScurityContext.getTrustStorePassword());
			}
			if (currentScurityContext.isEnableCertificateChecking() != null) {
				if (currentScurityContext.isEnableCertificateChecking().booleanValue()) {
					dcc.enableCertificateChecking();
				} else {
					dcc.disableCertificateChecking();
				}
			}
		}
	}

	private void applyAddressingContext(DynamicClientCache dcc) {
		// Before invoke the webservice or discovery the wsdl set the addressing-context

		// Try to get the runtime webservice-addressing passed as modifier in workflow
		// If runtime webservice-addressing is passed use it, otherwise use building-block addressing
		WebServiceAddressingContext currentAddressingContext = EngineHelper.extractWebServiceAddressingContext(owner, activity.getBehaviourName());
		if (currentAddressingContext == null) {
			currentAddressingContext = addressingContext; 
		}
		
		// Prepare wsdc AddressingProperties to invoke and apply addressing to discovery wsdl 
		dcAddressingProperties = null;
		if (currentAddressingContext != null) {
			dcAddressingProperties = new AddressingProperties();
			dcAddressingProperties.setVersion(currentAddressingContext.getVersion());
			dcAddressingProperties.setMustUnderstand(currentAddressingContext.isMustUnderstand());
			dcAddressingProperties.setSendDefaultMessageID(currentAddressingContext.isSendDefaultMessageID());
			dcAddressingProperties.setMessageID(currentAddressingContext.getMessageID());
			dcAddressingProperties.setAction(currentAddressingContext.getAction());
			dcAddressingProperties.setSendDefaultFrom(currentAddressingContext.isSendDefaultFrom());
			dcAddressingProperties.setFrom(currentAddressingContext.getFrom());
			dcAddressingProperties.setSendDefaultTo(currentAddressingContext.isSendDefaultTo());
			dcAddressingProperties.setTo(currentAddressingContext.getTo());
			dcAddressingProperties.setFaultTo(currentAddressingContext.getFaultTo());
			dcAddressingProperties.setReplyTo(currentAddressingContext.getReplyTo());
			//dcAddressingProperties.setReferenceParametersType(currentAddressingContext.getReferenceParameters());
			//dcAddressingProperties.setReferencePropertiesType(currentAddressingContext.getReferenceProperties());
			//dcAddressingProperties.setRelatesTo(currentAddressingContext.getRelatesTo());
		}
	}
	
	public static void clearDynamicClientsCache() {
		DynamicClientCache.getInstance().clear();
	}

	public static void removeDynamicClientCache(URI wsdl) {
		DynamicClientCache.getInstance().remove(wsdl);
	}


	/////////////////////////////////////////////
	// Building Block method

	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {

		// Get operation descriptor 
		OperationInfo oi = getDynamicClient().getService(serviceName).getPort(portName).getOperation(operationName);

		// Get schema of key
		TermSchema schema;
		ParameterInfo pi;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			pi = oi.getInputHeader(key.substring(sepPos+1));
		} else {
			// Parameter
			pi = oi.getInputParameter(key);
		}

		return AbsHelper.createAbsTemplate(pi.getSchema());
	}

	@Override
	public Ontology createOntology() throws Exception {
		return getDynamicClient().getOntology();
	}

	private List<String> getParameterNames(int mode) {
		List<String> parameterNames = new ArrayList<String>();
		try {
			OperationInfo oi = getDynamicClient().getService(serviceName).getPort(portName).getOperation(operationName);

			Set<String> pNames;
			Set<String> hNames;
			if (mode == Constants.IN_MODE) {
				pNames = oi.getInputParameterNames();
				hNames = oi.getInputHeaderNames();
			} else {
				pNames = oi.getOutputParameterNames();
				hNames = oi.getOutputHeaderNames();
			}

			// Add parameters
			parameterNames.addAll(pNames);

			// Add headers
			for (String hName : hNames) {
				parameterNames.add(Constants.WS_HEADER_PREFIX+Constants.WS_PREFIX_SEPARATOR+hName);
			}
		} catch (DynamicClientException e) {
			throw new RuntimeException(e);
		}
		return parameterNames;
	}

	@Override
	public List<String> getInputParameterNames() {
		return getParameterNames(Constants.IN_MODE);
	}

	@Override
	public List<String> getOutputParameterNames() {
		return getParameterNames(Constants.OUT_MODE);
	}

	@Override
	public boolean requireAbsParameters() {
		return true;
	}

	private Object getValue(String key, WSData wsData) {
		// Get value
		AbsObject abs;
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			abs = wsData.getHeader(key.substring(sepPos+1));
		} else {
			// Parameter
			abs = wsData.getParameter(key);
		}

		// Convert in primitive value (if necessary)
		Object value = null;
		if (abs != null && abs.getAbsType() == AbsObject.ABS_PRIMITIVE) {
			value = getPrimitiveValue((AbsPrimitive)abs);
		} else {
			value = abs;
		}
		return value;
	}

	@Override
	public Object getInput(String key) {
		return getValue(key, wsInputData);
	}

	@Override
	public Object getOutput(String key) {
		return getValue(key, wsOutputData);
	}

	private void setValue(String key, Object value, WSData wsData) {
		// Convert value in abs (if necessary)
		AbsObject absValue = getAbsValue(value);

		// Set value
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			wsData.setHeader(key.substring(sepPos+1), absValue);
		} else {
			// Parameter
			wsData.setParameter(key, absValue);
		}
	}

	@Override
	public void setInput(String key, Object value) {
		setValue(key, value, wsInputData);
	}

	@Override
	public void setOutput(String key, Object value) {
		setValue(key, value, wsOutputData);
	}

	@Override
	public boolean isInputEmpty(String key) {
		int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
		if (sepPos > 0) {
			// Header
			return wsInputData.isHeaderEmpty(key.substring(sepPos+1));
		} else {
			// Parameter
			return wsInputData.isParameterEmpty(key);
		}
	}

	private Parameter getFormalDescriptor(String key, int mode) {

		try {
			OperationInfo oi = getDynamicClient().getService(serviceName).getPort(portName).getOperation(operationName);
			ParameterInfo pi;
			Parameter p = null;

			int sepPos = key.indexOf(Constants.WS_PREFIX_SEPARATOR);
			if (sepPos > 0) {
				// Header
				if (mode == Constants.IN_MODE) {
					pi = oi.getInputHeader(key.substring(sepPos+1));	
				} else {
					pi = oi.getOutputHeader(key.substring(sepPos+1));
				}
			} else {
				// Parameter
				if (mode == Constants.IN_MODE) {
					pi = oi.getInputParameter(key);	
				} else {
					pi = oi.getOutputParameter(key);
				}
			}

			if (pi != null) {
				p = new Parameter();
				p.setName(pi.getName());
				p.setMode(pi.getMode());
				p.setMandatory(pi.isMandatory());
				p.setDefaultValue(pi.getDefaultValue());
				p.setRegex(pi.getRegex());
				p.setDocumentation(pi.getDocumentation());
				p.setPermittedValues(pi.getPermittedValues());
				p.setSchema(pi.getSchema());

				Integer cardMin = pi.getCardMin();
				if (cardMin != null) {
					p.setCardMin(cardMin);
				}
				Integer cardMax = pi.getCardMax();
				if (cardMax != null) {
					p.setCardMax(cardMax);
				}

				Class primitiveTypeClass = pi.getPrimitiveTypeClass();
				if (primitiveTypeClass != null) {
					p.setType(primitiveTypeClass.getName());
					p.setTypeClass(primitiveTypeClass);
				}
			}
			return p;

		} catch (DynamicClientException e) {
			throw new RuntimeException(e);
		}
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
