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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jade.core.Agent.Interrupted;

//#MIDP_EXCLUDE_FILE


/**
 * The behaviour implementing activities of type DYNAMIC-WEB-SERVICE in a workflow. 
 */
public class DynamicWebServiceInvocationBehaviour extends ActivityBehaviour {
	
	private MethodInvocator invocator;
	private DynamicWebService dynamicWebService;

	public DynamicWebServiceInvocationBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}
	
	/**
	 * Create a DynamicWebServiceInvocationBehaviour specifying whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method (being Nnn the
	 * name of the DynamicWebService activity) or directly by means of performDynamicWebService() method 
	 * of the WorkflowBehaviour class. This second case occurs when the workflow
	 * does not have a class associated to it, but is built instructing a WorkflowBehaviour
	 * object on the fly. 
	 * @param name The name of the activity
	 * @param owner The workflow this activity belongs to
	 * @param callExecuteMethod Whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method 
	 */
	public DynamicWebServiceInvocationBehaviour(String name, WorkflowBehaviour owner, boolean callExecuteMethod) {
		super(name, owner);
		requireSave = true;
		
		dynamicWebService = new DynamicWebService(owner, this);
		
		String methodName;
		Class paramClass;
		if (callExecuteMethod) {
			methodName = EngineHelper.activityName2Method(getBehaviourName());
			paramClass = DynamicWebService.class;
		} else {
			methodName = "performBuildingBlock";
			paramClass = InvocableBuildingBlock.class;
		}
		
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, dynamicWebService, paramClass);
	}

	public void setWsdl(String wsdl) {
		try {
			dynamicWebService.setWsdl(new URI(wsdl));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Malformed wsdl address "+wsdl, e);		
		}
	}
	
	public void setService(String service) {
		dynamicWebService.setServiceName(service);
	}
	
	public void setPort(String port) {
		dynamicWebService.setPortName(port);
	}
	
	public void setOperation(String operation) {
		dynamicWebService.setOperationName(operation);
	}
	
	public void setTimeout(int timeout) {
		dynamicWebService.setTimeout(timeout);
	}

	public void setSecurityContext(WebServiceSecurityContext securityContext) {
		dynamicWebService.setSecurityContext(securityContext);
	}

	public void setAddressingContext(WebServiceAddressingContext addressingContext) {
		dynamicWebService.setAddressingContext(addressingContext);
	}
	
	public void setNoWrapTypes(boolean noWrap) {
		dynamicWebService.setNoWrap(noWrap);
	}

	public void setPackageName(String packageName) {
		dynamicWebService.setPackageName(packageName);
	}	
	
	public void setEndpointAddress(String endpointAddress) {
		try {
			dynamicWebService.setEndpoint(new URL(endpointAddress));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Malformed endpoint address "+endpointAddress, e);
		}
	}

	public void action() {
		try {
			owner.enterInterruptableSection();
			invocator.invoke();
		}
		catch (InterruptedException ie) {
		}
		catch (Interrupted i) {
		}
		catch (ThreadDeath td) {
		}
		catch (Throwable t) {
			handleException(t);
			if (!EngineHelper.logIfUncaughtOnly(this, t)) {
				t.printStackTrace();
			}
		}
		finally {
			owner.exitInterruptableSection(this);
		}
	}

	public void reset() {
		super.reset();

		// Reset specific building block
		dynamicWebService.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		return dynamicWebService;
	}
}
