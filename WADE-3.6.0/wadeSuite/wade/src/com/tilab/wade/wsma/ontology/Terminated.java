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

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import java.util.List;

import com.tilab.wade.performer.descriptors.Parameter;

public class Terminated implements Predicate {

	private static final long serialVersionUID = 1598024042419395960L;

	private String executionId;
	private List<Parameter> parameters;
	private String errorMessage;
	
	
	public Terminated() {
	}

	public Terminated(String executionId, List<Parameter> parameters, String errorMessage) {
		this.executionId = executionId;
		this.parameters = parameters;
		this.errorMessage = errorMessage;
	}
	
	@Slot(mandatory=true, position=0)
	public String getExecutionId() {
		return executionId;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	@Slot(mandatory=true, position=1)
	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	@Slot(mandatory=false, position=2)
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return "Terminated [executionId=" + executionId + ", errorMessage=" + errorMessage + "]";
	}
}
