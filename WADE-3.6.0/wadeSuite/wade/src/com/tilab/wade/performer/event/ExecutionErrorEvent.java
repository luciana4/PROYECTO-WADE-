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
package com.tilab.wade.performer.event;

/**
   The event generated when an exception occurred during the 
   execution of an activity.
 */
public class ExecutionErrorEvent extends WorkflowEvent {
	private String workflow;
  	private String activity;
  	private String application;
	private Throwable exception;
	
	public ExecutionErrorEvent() {
	}
	
	
	public ExecutionErrorEvent(String workflow, String activity, Throwable exception) {
		this.workflow = workflow;
	  	this.activity = activity;
		this.exception = exception;
	}
	
	public ExecutionErrorEvent(String workflow, String activity, String application, Throwable exception) {
		this.workflow = workflow;
	  	this.activity = activity;
	  	this.application = application;
		this.exception = exception;
	}
	
	public String getActivity() {
		return activity;
	}
	
	public void setActivity(String activity) {
		this.activity = activity;
	}
	
	public Throwable getException() {
		return exception;
	}
	
	public void setException(Throwable exception) {
		this.exception = exception;
	}

    public String getApplication() {
      return application;
    }

    public void setApplication(String application) {
      this.application = application;
    }

	public String getWorkflow() {
	  return workflow;
	}

	public void setWorkflow(String workflow) {
	  this.workflow = workflow;
	}
	
	public String toString() {
	  if (application != null)
		  return getClass().getName()+" in workflow " +workflow+", activity "+activity+", application "+application+". Exception is "+exception;
	  else
		  return getClass().getName()+" in workflow " +workflow+", activity "+activity+". Exception is "+exception;
	}
}
