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
 * The behaviour implementing activities of type REST-SERVICE in a workflow. 
 */
public class RestServiceInvocationBehaviour extends ActivityBehaviour {
	
	private MethodInvocator invocator;
	private RestService restService;

	public RestServiceInvocationBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	/**
	 * Create a RestServiceInvocationBehaviour specifying whether the activity 
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
	public RestServiceInvocationBehaviour(String name, WorkflowBehaviour owner, boolean callExecuteMethod) {
		super(name, owner);
		
		restService = new RestService(owner, this);
		
		String methodName;
		Class paramClass;
		if (callExecuteMethod) {
			methodName = EngineHelper.activityName2Method(getBehaviourName());
			paramClass = RestService.class;
		} else {
			methodName = "performBuildingBlock";
			paramClass = InvocableBuildingBlock.class;
		}
		
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, restService, paramClass);
	}
	
	public void setDescriptorClassName(String descriptorClassName) {
		restService.setDescriptorClassName(descriptorClassName);
	}
	
	public void setBaseUri(String baseUri) {
		restService.setBaseUri(baseUri);
	}

	public void setResourceId(String resourceId) {
		restService.setResourceId(resourceId);
	}
	
	public void setMethodId(String methodId) {
		restService.setMethodId(methodId);
	}

	public void setRequestMediaTypeElement(String requestMediaTypeElement) {
		restService.setRequestMediaTypeElement(requestMediaTypeElement);
	}

	public void setResponseMediaTypeElement(String responseMediaTypeElement) {
		restService.setResponseMediaTypeElement(responseMediaTypeElement);
	}

	public void setFaultMediaTypeElement(String faultMediaTypeElement) {
		restService.setFaultMediaTypeElement(faultMediaTypeElement);
	}
	
	public void setTimeout(int timeout) {
		restService.setTimeout(timeout);
	}
	
	public void setEndpointAddress(String endpointAddress) {
		restService.setEndpointAddress(endpointAddress);
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
		restService.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		return restService;
	}
}
