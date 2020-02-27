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


//#MIDP_EXCLUDE_BEGIN
import com.tilab.wade.performer.event.ExecutionErrorEvent;
//#MIDP_EXCLUDE_END


/**
   The behaviour implementing activities of type TOOL in a workflow. The execution of 
   an activity of type TOOL corresponds to the execution of one or more external tools
   called "applications". 
   @see BaseApplication 
   @author Giovanni Caire - TILAB
 */
public class ToolExecutionBehaviour extends ActivityBehaviour {
	private MethodInvocator invocator;
	private ApplicationList applications;
	
	public ToolExecutionBehaviour(String name, WorkflowBehaviour owner) {
		super(name, owner);
		requireSave = true;
		applications = new ApplicationList(owner);
		String methodName = EngineHelper.activityName2Method(getBehaviourName());
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, applications, ApplicationList.class);
	}
	
	public void addApplication(String applicationId) {
		applications.add(applicationId);
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
			// We use an ad hoc overloaded version of the handleException() method to properly notify the application name. 
			handleException(t, applications.getCurrentApplication());
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
		applications.reset();
	}
	
	/**
	 * Overloaded version of the handleException() method required to 
	 * handle the application name
	 */
	public void handleException(Throwable t, String currentApplication) {
		lastException = t;
		//#MIDP_EXCLUDE_BEGIN
		WorkflowEngineAgent.WorkflowExecutor root = (WorkflowEngineAgent.WorkflowExecutor) root();
		String workflowName = root.getDescriptor().getId();
		ExecutionErrorEvent errorEvent = new ExecutionErrorEvent(workflowName, getBehaviourName(), currentApplication, lastException);
		root.getEventEmitter().fireEvent(Constants.WARNING_TYPE, errorEvent, Constants.FINE_LEVEL);
		root.setLastErrorEvent(errorEvent);
		//#MIDP_EXCLUDE_END
	}
}
		