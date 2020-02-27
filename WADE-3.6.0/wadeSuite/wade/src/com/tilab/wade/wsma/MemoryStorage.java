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
package com.tilab.wade.wsma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowResult;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;

import jade.util.Logger;

/**
 * Memory Workflow-Status-Manager-Storage implementation
 * Persist the workflows into memory. At the agent shout-down all data is lost.  
 */
public class MemoryStorage implements Storage {

	private static final int DEFAULT_MAX_STORAGE_ENTRY = 1000;
	private static final String MAX_STORAGE_ENTRY_KEY = "maxStorageEntry";
	
	private Map<String, WorkflowExecutionInfo> executions;
	private Map<String, byte[]> serializedStates;
	private int maxStorageEntry;
	
	public void init(Map<String, Object> arguments) throws StorageException {
		executions = new HashMap<String, WorkflowExecutionInfo>();
		serializedStates = new HashMap<String, byte[]>();
		maxStorageEntry = TypeManager.getInt(arguments, MAX_STORAGE_ENTRY_KEY, DEFAULT_MAX_STORAGE_ENTRY);
	}

	public void close() throws StorageException {
	}

	public void started(WorkflowExecutionInfo wei) throws StorageException {
		if (executions.size() >= maxStorageEntry) {
			removeOlderEntry();
		}
		
		executions.put(wei.getExecutionId(), wei);
	}

	private void removeOlderEntry() {
		String olderExecutionId = null;
		long olderUpdateTime = Long.MAX_VALUE;
		for (WorkflowExecutionInfo wei : executions.values()) {
			if (wei.getStatus() == WorkflowStatus.TERMINATED &&
					wei.getLastUpdateTime() < olderUpdateTime) {
				olderExecutionId = wei.getExecutionId();
				olderUpdateTime = wei.getLastUpdateTime();
			}
		}
		executions.remove(olderExecutionId);
		serializedStates.remove(olderExecutionId);
	}

	public void statusChanged(String executionId, WorkflowStatus status, long timestamp) throws StorageException {
		WorkflowExecutionInfo wei = executions.get(executionId);
		if (wei != null) {
			wei.setLastUpdateTime(timestamp);
			wei.setStatus(status);
		}
	}

	public void serializedStateChanged(String executionId, String currentActivity, byte[] serializedState, long timestamp) throws StorageException {
		WorkflowExecutionInfo wei = executions.get(executionId);
		if (wei != null) {
			wei.setLastUpdateTime(timestamp);
			wei.setWorkflowCurrentActivity(currentActivity);

			serializedStates.put(executionId, serializedState);
		}
	}

	public void terminated(String executionId, WorkflowResult result, List<Parameter> parameters, String errorMessage, long timestamp) throws StorageException {
		WorkflowExecutionInfo wei = executions.get(executionId);
		if (wei != null) {
			wei.setLastUpdateTime(timestamp);
			wei.setStatus(WorkflowStatus.TERMINATED);
			wei.setResult(result);
			wei.updateWadeParameters(parameters);
			wei.setErrorMessage(errorMessage);
			wei.setWorkflowCurrentActivity(null);

			serializedStates.remove(executionId);
		}
	}

	public void thawed(String executionId, String executorName, long timestamp) throws StorageException {
		WorkflowExecutionInfo wei = executions.get(executionId);
		if (wei != null) {
			wei.setLastUpdateTime(timestamp);
			wei.setStatus(WorkflowStatus.ACTIVE);
			wei.setExecutorName(executorName);
		}
	}

	public void removeExecution(String executionId) throws StorageException {
		executions.remove(executionId);
	}

	public void cleanExecutions() throws StorageException {
		executions.clear();
	}

	public void cleanOlderExecutions(int retentionDays) throws StorageException {
		long threshold = System.currentTimeMillis() - (retentionDays * 12 * 60 * 60 * 1000);
		Iterator<Map.Entry<String, WorkflowExecutionInfo>> it = executions.entrySet().iterator();
		while (it.hasNext()) {
		    Map.Entry<String, WorkflowExecutionInfo> entry = it.next();
		    WorkflowExecutionInfo wei = entry.getValue();
		    if (wei.getStatus() == WorkflowStatus.TERMINATED &&
				wei.getLastUpdateTime() <= threshold) {

		    	it.remove();
		        serializedStates.remove(wei.getExecutionId());
		    }
		}
	}
	
	public WorkflowExecutionInfo getExecution(String executionId) throws StorageException {
		return executions.get(executionId);
	}

	public byte[] getSerializedState(String executionId) throws StorageException {
		return serializedStates.get(executionId);
	}
	
	public List<WorkflowExecutionInfo> getSessionExecutions(String sessionId) throws StorageException {
		List<WorkflowExecutionInfo> weisBySession = new ArrayList<WorkflowExecutionInfo>();
		for (WorkflowExecutionInfo wei : executions.values()) {
			if (sessionId.equalsIgnoreCase(wei.getSessionId())) {
				weisBySession.add(wei);
			}
		}
		return weisBySession;
	}

	public List<WorkflowExecutionInfo> getPendingExecutions(String requester) throws StorageException {
		List<WorkflowExecutionInfo> weisByRequester = new ArrayList<WorkflowExecutionInfo>();
		for (WorkflowExecutionInfo wei : executions.values()) {
			if ((requester == null || requester.equalsIgnoreCase(wei.getRequester())) && wei.getStatus() != WorkflowExecutionInfo.WorkflowStatus.TERMINATED) {
				weisByRequester.add(wei);
			}
		}
		return weisByRequester;
	}

	public String getQueryDialect() throws StorageException {
		myLogger.log(Logger.WARNING, "QueryExecutions method not available in MemoryStorage");
		
		throw new UnsupportedOperationException("QueryExecutions method not available in MemoryStorage");
	}

	public List queryExecutions(String what, String condition, String order, int firstResult, int maxResult) throws StorageException {
		myLogger.log(Logger.WARNING, "QueryExecutions method not available in MemoryStorage");
		
		throw new UnsupportedOperationException("QueryExecutions method not available in MemoryStorage");
	}
}
