/**
 * PizzaServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Sep 27, 2010 (02:39:24 CEST) WSDL2Java emitter.
 */

package pizza;

public class PizzaServiceLocator extends org.apache.axis.client.Service implements pizza.PizzaService {

    public PizzaServiceLocator() {
    }


    public PizzaServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public PizzaServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for PizzaPort
    private java.lang.String PizzaPort_address = "http://localhost:8080/wadeServices/services/PizzaPort";

    public java.lang.String getPizzaPortAddress() {
        return PizzaPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String PizzaPortWSDDServiceName = "PizzaPort";

    public java.lang.String getPizzaPortWSDDServiceName() {
        return PizzaPortWSDDServiceName;
    }

    public void setPizzaPortWSDDServiceName(java.lang.String name) {
        PizzaPortWSDDServiceName = name;
    }

    public pizza.PizzaPort getPizzaPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(PizzaPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getPizzaPort(endpoint);
    }

    public pizza.PizzaPort getPizzaPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            pizza.PizzaBindingStub _stub = new pizza.PizzaBindingStub(portAddress, this);
            _stub.setPortName(getPizzaPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setPizzaPortEndpointAddress(java.lang.String address) {
        PizzaPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (pizza.PizzaPort.class.isAssignableFrom(serviceEndpointInterface)) {
                pizza.PizzaBindingStub _stub = new pizza.PizzaBindingStub(new java.net.URL(PizzaPort_address), this);
                _stub.setPortName(getPizzaPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("PizzaPort".equals(inputPortName)) {
            return getPizzaPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("urn:pizza", "PizzaService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("urn:pizza", "PizzaPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("PizzaPort".equals(portName)) {
            setPizzaPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
