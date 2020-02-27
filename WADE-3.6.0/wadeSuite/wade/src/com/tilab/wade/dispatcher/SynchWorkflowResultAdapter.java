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
package com.tilab.wade.dispatcher;

import java.util.Map;

import jade.core.AID;
import jade.util.leap.List;

import com.tilab.wade.performer.descriptors.ElementDescriptor;
import com.tilab.wade.performer.ontology.ExecutionError;

/**
 * Ready made adapter for the WorkflowResultListener interface that allows waiting for the result 
 * of a workflow in a synchronous way
 */
public class SynchWorkflowResultAdapter implements WorkflowResultListener {
	private List results;
	private WorkflowExecutionException workflowExecutionException;
	private WorkflowLoadException workflowLoadException;
	private WorkflowNotificationException workflowNotificationException;
	private boolean terminated = false;

	public void handleAssignedId(AID executor, String executionId) {
	}

	public void handleExecutionCompleted(List results, AID executor, String executionId) {
		this.results = results;
		setTerminated();	
	}

	public void handleExecutionError(ExecutionError er, AID executor, String executionId) {
		workflowExecutionException = new WorkflowExecutionException(er, executor, executionId);
		setTerminated();
	}

	public void handleLoadError(String reason) {
		workflowLoadException = new WorkflowLoadException(reason);
		setTerminated();
	}

	public void handleNotificationError(AID executor, String executionId) {
		workflowNotificationException = new WorkflowNotificationException(executor, executionId);
		setTerminated();
	}
	
	private synchronized void setTerminated() {
		terminated = true;
		notifyAll();
	}
	
	public synchronized Map<String, Object> getWorkflowResult() throws WorkflowExecutionException, WorkflowLoadException, WorkflowNotificationException, InterruptedException {
		return getWorkflowResult(0);
	}
	
	public synchronized Map<String, Object> getWorkflowResult(long timeout) throws WorkflowExecutionException, WorkflowLoadException, WorkflowNotificationException, InterruptedException {
		return ElementDescriptor.paramListToMap(getResult(timeout));
	}
	
	public synchronized List getResult() throws WorkflowExecutionException, WorkflowLoadException, WorkflowNotificationException, InterruptedException {
		return getResult(0);
	}

	public synchronized List getResult(long timeout) throws WorkflowExecutionException, WorkflowLoadException, WorkflowNotificationException, InterruptedException {
		if (!terminated) {
			wait(timeout);
			if (!terminated) {
				return null;
			}
		}
		
		if (workflowExecutionException != null) {
			throw workflowExecutionException;
		}
		if (workflowLoadException != null) {
			throw workflowLoadException;
		}
		if (workflowNotificationException != null) {
			throw workflowNotificationException;
		}
		
		return results;
	}
}
