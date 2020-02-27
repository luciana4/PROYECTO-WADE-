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

import jade.util.Logger;

import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowResult;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;

/**
 * Workflow Status Manager Storage interface
 */
public interface Storage {

	public final Logger myLogger = Logger.getMyLogger(Storage.class.getName());;
	
	/**
	 * Initialize the storage. 
	 * This method is called in the agent setup and if throw a StorageException the agent is terminated.
	 * The map passed as parameter provides access to the agent configuration arguments.
	 */
	public void init(Map<String, Object> arguments) throws StorageException;
	
	/**
	 * Close the storage.
	 * This method is called in the agent take down. 
	 */
	public void close() throws StorageException;
	
	/**
	 * Insert a new WorkflowExecutionInfo with key executionId.
	 */
	public void started(WorkflowExecutionInfo wei) throws StorageException;
	
	/**
	 * Update a WorkflowExecutionInfo with a new workflow serialization.  
	 */
	public void serializedStateChanged(String executionId, String currentActivity, byte[] serializedState, long timestamp) throws StorageException;

	/**
	 * Update a WorkflowExecutionInfo with a new workflow status.  
	 */
	public void statusChanged(String executionId, WorkflowStatus status, long timestamp) throws StorageException;
	
	/**
	 * Mark as terminated a workflow 
	 */
	public void terminated(String executionId, WorkflowResult result, List<Parameter> parameters, String errorMessage, long timestamp) throws StorageException;
	
	/**
	 * Mark as thawed a workflow, replace executor 
	 */
	public void thawed(String executionId, String executorName, long timestamp) throws StorageException;

	/**
	 * Remove a WorkflowExecutionInfo with specified key.
	 */
	public void removeExecution(String executionId) throws StorageException;
	
	/**
	 * Delete all present WorkflowExecutionInfos.
	 */
	public void cleanExecutions() throws StorageException;
	
	/**
	 * Delete the terminated WorkflowExecutionInfos older than retentionDays.
	 */
	public void cleanOlderExecutions(int retentionDays) throws StorageException;
	
	/**
	 * Return the WorkflowExecutionInfo associated to specific executionId
	 */
	public WorkflowExecutionInfo getExecution(String executionId) throws StorageException;

	/**
	 * Return the workflow serialized state associated to specific executionId
	 */
	public byte[] getSerializedState(String executionId) throws StorageException;

	/**
	 * Return the list of workflows assigned to specific requester and in status not TERMINATED
	 */
	public List<WorkflowExecutionInfo> getPendingExecutions(String requester) throws StorageException;
	
	/**
	 * Return the WorkflowExecutionInfos list associated to specific sessionId
	 */
	public List<WorkflowExecutionInfo> getSessionExecutions(String sessionId) throws StorageException;

	/**
	 * Return the database dialect usable in what, condition and order parameters of queryExecutions methods
	 */
	public String getQueryDialect() throws StorageException;
	
	/**
	 * Return the list associated to specific query
	 * select WHAT from table where CONDITION order by ORDER
	 * If maxResult is ALL_RESULTS (-1) return all elements
	 * The list elements can be:
	 * - Object with field value if only one field is required in WHAT 
	 * - WorkflowExecutionInfo object partially filled if more than one fields (but not all) are required in WHAT
	 * - WorkflowExecutionInfo object if all fields are required (WHAT=null or *)
	 */
	public List queryExecutions(String what, String condition, String order, int firstResult, int maxResult) throws StorageException;
}
