/**
 * StoreKeeperPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package server.storekeeper;

public interface StoreKeeperPort extends java.rmi.Remote {
    public void getComponents(int quantity, java.lang.String type, javax.xml.rpc.holders.BooleanHolder availability, server.storekeeper.holders.PointHolder location) throws java.rmi.RemoteException;
}
