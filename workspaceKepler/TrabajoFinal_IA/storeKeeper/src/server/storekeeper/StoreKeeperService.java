/**
 * StoreKeeperService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package server.storekeeper;

import server.storekeeper.StoreKeeperPort;

public interface StoreKeeperService extends javax.xml.rpc.Service {
    public java.lang.String getStoreKeeperPortAddress();

    public server.storekeeper.StoreKeeperPort getStoreKeeperPort() throws javax.xml.rpc.ServiceException;

    public server.storekeeper.StoreKeeperPort getStoreKeeperPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
