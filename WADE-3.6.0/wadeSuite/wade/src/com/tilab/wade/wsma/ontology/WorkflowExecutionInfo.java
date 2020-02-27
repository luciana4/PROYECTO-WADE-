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
package com.tilab.wade.wsma.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.descriptors.Parameter;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import jade.content.onto.annotations.SuppressSlot;
import jade.core.AID;

public class WorkflowExecutionInfo implements Concept {

	private static final long serialVersionUID = -3197445999119132533L;

	public static final String EXECUTION_ID_FIELD = "wi.executionId";
	public static final String NAME_FIELD = "wi.name";
	public static final String DOCUMENTATION_FIELD = "wi.documentation";
	public static final String PARENT_EXECUTION_ID_FIELD = "wi.parentExecutionid";
	public static final String WORKFLOW_ID_FIELD = "wi.workflowId";
	public static final String SESSION_ID_FIELD = "wi.sessionId";
	public static final String REQUESTER_FIELD = "wi.requester";
	public static final String EXECUTOR_FIELD = "wi.executorName";
	public static final String LONG_RUNNING_FIELD = "wi.longRunning";
	public static final String TRANSACTIONAL_FIELD = "wi.transactional";
	public static final String INTERACTIVE_FIELD = "wi.interactive";
	public static final String STATUS_FIELD = "wi.statusName";
	public static final String START_TIME_FIELD = "wi.startTime";
	public static final String LAST_UPDATE_TIME_FIELD = "wi.lastUpdateTime";
	public static final String RESULT_FIELD = "wi.resultName";
	public static final String ERROR_MESSAGE_FIELD = "wi.errorMessage";
	public static final String WF_CURRENT_ACTIVITY_FIELD = "wi.workflowCurrentActivity";
	public static final String PARAMETERS_FIELD = "wi.parameters";
	
	public enum WorkflowStatus { ACTIVE, FROZEN, SUSPENDED, WAIT_COMMIT, ROLLBACK, TERMINATED }
	public enum WorkflowResult { OK, KO, TRANSACTION_FAIL}
	
	private String executionId;
	private String name;
	private String documentation;
	private String parentExecutionid;
	private String workflowId;
	private String sessionId;
	private String requester;
	private String executorName;
	private boolean longRunning;
	private boolean transactional;
	private boolean interactive;
	private WorkflowStatus status;
	private long startTime;
	private long lastUpdateTime;
	private WorkflowResult result;
	private String errorMessage;
	private String workflowCurrentActivity;
	private List<WorkflowParameterInfo> parameters = new ArrayList<WorkflowParameterInfo>();
	
	public WorkflowExecutionInfo() {
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executorId) {
		this.executionId = executorId;
	}
	
	@Slot(mandatory=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Slot(mandatory=false)
	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Slot(mandatory=false)
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Slot(mandatory=false)
	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}
	
	@SuppressSlot
	public AID getExecutor() {
		return new AID(executorName, AID.ISGUID);
	}

	public WorkflowStatus getStatus() {
		return status;
	}

	public void setStatus(WorkflowStatus status) {
		this.status = status;
	}

	// Method used only for storage persistence 
	private String getStatusName() {
		return status.name();
	}

	// Method used only for storage persistence
	private void setStatusName(String statusName) {
		this.status = WorkflowStatus.valueOf(statusName);
	}
	
	@Slot(mandatory=false)
	public String getParentExecutionid() {
		return parentExecutionid;
	}

	public void setParentExecutionid(String parentExecutionid) {
		this.parentExecutionid = parentExecutionid;
	}

	public String getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}

	public void setLongRunning(boolean longRunning) {
		this.longRunning = longRunning;
	}

	public boolean isLongRunning() {
		return longRunning;
	}

	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	public boolean isTransactional() {
		return transactional;
	}

	public void setInteractive(boolean interactive){
		this.interactive = interactive;
	}
		
	public boolean isInteractive(){
		return this.interactive;
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	@Slot(mandatory=false)
	public WorkflowResult getResult() {
		return result;
	}

	public void setResult(WorkflowResult result) {
		this.result = result;
	}

	// Method used only for storage persistence 
	private String getResultName() {
		if (result != null) {
			return result.name();
		} else {
			return null;
		}
	}

	// Method used only for storage persistence
	private void setResultName(String resultName) {
		if (resultName != null) {
			this.result = WorkflowResult.valueOf(resultName);
		} else {
			this.result = null;
		}
	}
	
	@Slot(mandatory=false)
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Slot(mandatory=false)
	public String getWorkflowCurrentActivity() {
		return workflowCurrentActivity;
	}

	public void setWorkflowCurrentActivity(String workflowCurrentActivity) {
		this.workflowCurrentActivity = workflowCurrentActivity;
	}

	public List<WorkflowParameterInfo> getParameters() {
		return parameters;
	}

	public void setParameters(List<WorkflowParameterInfo> parameters) {
		this.parameters = parameters;
	}

	public void setWadeParameters(List<Parameter> wadeParameters) {
		if (wadeParameters != null) {
			parameters = new ArrayList<WorkflowParameterInfo>();
			for (int i=0; i<wadeParameters.size(); i++) {
				Parameter param = (Parameter)wadeParameters.get(i);
				WorkflowParameterInfo wpi = createWorkflowParameterInfo(param);
				parameters.add(wpi);
			}
		}
	}

	public void updateWadeParameters(List<Parameter> wadeParameters) {
		if (wadeParameters != null) {
			
			// Create map to optimize the process
			Map<String, WorkflowParameterInfo> wpisMap = new HashMap<String, WorkflowParameterInfo>();
			for (WorkflowParameterInfo wpi : parameters) {
				wpisMap.put(wpi.getName(), wpi);
			}
			
			// Update/Add values
			for (int i=0; i<wadeParameters.size(); i++) {
				Parameter param = (Parameter)wadeParameters.get(i);
				WorkflowParameterInfo wpi = wpisMap.get(param.getName());
				if (wpi != null) {
					wpi.setValue(formatValue(param.getValue()));
				} else {
					wpi = createWorkflowParameterInfo(param);
					if (wpi != null) {
						parameters.add(wpi);
					}
				}
			}		
		}
	}

	private WorkflowParameterInfo createWorkflowParameterInfo(Parameter parameter) {
		if (parameter.getType() == null && parameter.getValue() == null) {
			// The type of parameter is mandatory, if not present in parameter and 
			// if is not deducible from value return null 
			return null;
		}
		WorkflowParameterInfo wpi = new WorkflowParameterInfo();
		wpi.setExecutionId(executionId);
		wpi.setName(parameter.getName());
		wpi.setMode(parameter.getMode());
		wpi.setType(parameter.getType());
		if (wpi.getType() == null) {
			wpi.setType(parameter.getValue().getClass().getCanonicalName());
		}
		wpi.setValue(formatValue(parameter.getValue()));
		wpi.setDocumentation(parameter.getDocumentation());
		return wpi;
	}

	public static String formatValue(Object value) {
		return value!=null ? value.toString() : null;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if ( !(other instanceof WorkflowExecutionInfo) ) return false;

		final WorkflowExecutionInfo otherWEI = (WorkflowExecutionInfo) other;

        return getExecutionId().equals(otherWEI.getExecutionId());
	}
	
	@Override
	public int hashCode() {
        return getExecutionId().hashCode();
	}
	
	@Override
	public String toString() {
		return "WorkflowExecutionInfo [executionId=" + executionId + ", parentExecutionid=" + parentExecutionid + ", workflowId=" + workflowId
				+ ", sessionId=" + sessionId + ", requester=" + requester + ", executorName=" + executorName + ", status=" + status + ", startTime="
				+ startTime + ", lastUpdateTime=" + lastUpdateTime + ", result=" + result + ", errorMessage=" + errorMessage
				+ ", workflowCurrentActivity=" + workflowCurrentActivity + "]";
	}
}
