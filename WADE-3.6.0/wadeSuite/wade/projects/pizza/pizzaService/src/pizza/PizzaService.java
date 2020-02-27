/**
 * PizzaService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Sep 27, 2010 (02:39:24 CEST) WSDL2Java emitter.
 */

package pizza;

public interface PizzaService extends javax.xml.rpc.Service {
    public java.lang.String getPizzaPortAddress();

    public pizza.PizzaPort getPizzaPort() throws javax.xml.rpc.ServiceException;

    public pizza.PizzaPort getPizzaPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
