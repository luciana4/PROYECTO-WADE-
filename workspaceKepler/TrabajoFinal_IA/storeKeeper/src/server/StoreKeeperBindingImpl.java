/**
 * StoreKeeperBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package server;

import server.Component;
import server.StoreKeeper;
import server.storekeeper.StoreKeeperPort;
import server.storekeeper.holders.PointHolder;

public class StoreKeeperBindingImpl implements StoreKeeperPort {
    public void getComponents(int quantity, java.lang.String type, javax.xml.rpc.holders.BooleanHolder availability, PointHolder location) throws java.rmi.RemoteException {
    	Component components = StoreKeeper.getInstance().getComponents(type, quantity);
		if (components==null){
			location.value=null;
			availability.value=false;
		}
		else{
			boolean av =components.isAvailable();
			availability.value = av;
		
			if (av){
				location.value = components.getCoordinates();
			}
			else {
				location.value = null;
			}
		}
    }
}
