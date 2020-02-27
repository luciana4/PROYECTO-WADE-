package com.tilab.wade.dispatcher;

import jade.core.AID;
import jade.util.leap.List;

import com.tilab.wade.performer.ontology.ExecutionError;

/**
 * Ready made adapter for the WorkflowResultListener interface that provides an empty implementation 
 * of all methods
 */
public class WorkflowResultAdapter implements WorkflowResultListener {

	@Override
	public void handleAssignedId(AID executor, String executionId) {
	}

	@Override
	public void handleLoadError(String reason) {
	}

	@Override
	public void handleNotificationError(AID executor, String executionId) {
	}

	@Override
	public void handleExecutionError(ExecutionError er, AID executor, String executionId) {
	}

	@Override
	public void handleExecutionCompleted(List results, AID executor, String executionId) {
	}

}
