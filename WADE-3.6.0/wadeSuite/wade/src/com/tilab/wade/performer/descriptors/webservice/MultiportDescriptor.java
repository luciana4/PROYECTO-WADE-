package com.tilab.wade.performer.descriptors.webservice;

import java.rmi.Remote;

import javax.xml.rpc.ServiceException;

public interface MultiportDescriptor {

	public Remote getService(String portName) throws ServiceException;
}
