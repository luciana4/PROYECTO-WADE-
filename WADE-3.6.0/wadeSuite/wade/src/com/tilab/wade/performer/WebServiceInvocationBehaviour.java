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

import jade.core.Agent.Interrupted;

//#MIDP_EXCLUDE_FILE


/**
 * The behaviour implementing activities of type WEB-SERVICE in a workflow. 
 */
public class WebServiceInvocationBehaviour extends ActivityBehaviour {
	
	private MethodInvocator invocator;
	private WebService webService;

	public WebServiceInvocationBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	/**
	 * Create a WebServiceInvocationBehaviour specifying whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method (being Nnn the
	 * name of the WebService activity) or directly by means of performWebService() method 
	 * of the WorkflowBehaviour class. This second case occurs when the workflow
	 * does not have a class associated to it, but is built instructing a WorkflowBehaviour
	 * object on the fly. 
	 * @param name The name of the activity
	 * @param owner The workflow this activity belongs to
	 * @param callExecuteMethod Whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method 
	 */
	public WebServiceInvocationBehaviour(String name, WorkflowBehaviour owner, boolean callExecuteMethod) {
		super(name, owner);
		
		webService = new WebService(owner, this);
		
		String methodName;
		Class paramClass;
		if (callExecuteMethod) {
			methodName = EngineHelper.activityName2Method(getBehaviourName());
			paramClass = WebService.class;
		} else {
			methodName = "performBuildingBlock";
			paramClass = InvocableBuildingBlock.class;
		}
		
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, webService, paramClass);
	}
	
	public void setOperation(String operation) {
		webService.setOperation(operation);
	}

	public void setDescriptorClassName(String descriptorClassName) {
		webService.setDescriptorClassName(descriptorClassName);
	}
	
	public void setTimeout(int timeout) {
		webService.setTimeout(timeout);
	}
	
	public void setSecurityContext(WebServiceSecurityContext securityContext) {
		webService.setSecurityContext(securityContext);
	}

	public void setAddressingContext(WebServiceAddressingContext addressingContext) {
		webService.setAddressingContext(addressingContext);
	}
	
	public void setEndpointAddress(String endpointAddress) {
		webService.setEndpointAddress(endpointAddress);
	}

	public void setPort(String port) {
		webService.setPort(port);
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
		webService.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		return webService;
	}
}
