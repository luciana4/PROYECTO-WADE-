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

import jade.core.Agent;

import java.util.Iterator;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor;

public class WebServiceEventCase extends EventCase {

	private static final long serialVersionUID = 8768686315345L;
	
	private String port; 
	private String operation;
	private String descriptorClassName;
	private transient ServiceDescriptor descriptor; 
	
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

	public String getDescriptorClassName() {
		return descriptorClassName;
	}

	public void setDescriptorClassName(String descriptorClassName) {
		this.descriptorClassName = descriptorClassName;
		
		descriptor = null;
	}
	
	void init(Agent a) throws Exception {
		// Init and check service-descriptor
		if (descriptor == null) {
			if (descriptorClassName == null) {
				throw new WebServiceException("Missing ServiceDescriptor classname");
			}
			
			ClassLoader cl = CAServices.getInstance(a).getDefaultClassLoader();
			Class descriptorClass = Class.forName(descriptorClassName, true, cl);
			descriptor = (ServiceDescriptor)descriptorClass.newInstance();
		}

		// Check service
		String service = descriptor.getServiceName();
		if (service == null) {
			throw new WebServiceException("Missing webservice name in ServiceDescriptor "+descriptorClassName);
		}

		// Check port
		if (port == null) {
			// Get first available port
			Iterator<String> it = descriptor.getPortNames().iterator();
			if (it.hasNext()) {
				port = it.next();
			}
		}
		if (port == null) {
			throw new WebServiceException("Missing webservice-port name for webservice "+service);
		}
		
		// Check operation
		if (operation == null) {
			throw new WebServiceException("Missing webservice-operation name for webservice "+service);
		}
		
		setEventType(service+"."+port+"."+operation);
	}
}
