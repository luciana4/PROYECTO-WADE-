package com.tilab.wade.wsma.ontology;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class WorkflowParameterInfo implements Concept {
	
	private static final long serialVersionUID = 6776254293820381470L;
	
	public static final String NAME_FIELD = "pi.name";
	public static final String TYPE_FIELD = "pi.type";
	public static final String MODE_FIELD = "pi.mode";
	public static final String VALUE_FIELD = "pi.value";
	public static final String DOCUMENTATION_FIELD = "pi.documentation";
	
	private String executionId;
	private String name;
	private String type;
	private int mode;
	private String value;
	private String documentation;
	
	public WorkflowParameterInfo() {
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executorId) {
		this.executionId = executorId;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	@Slot(mandatory=false)
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Slot(mandatory=false)
	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if ( !(other instanceof WorkflowParameterInfo) ) return false;

		final WorkflowParameterInfo otherWPI = (WorkflowParameterInfo) other;
		if (!getExecutionId().equals(otherWPI.getExecutionId())) {
			return false;
		}
		if (!getName().equals(otherWPI.getName())) {
			return false;
		}
		if (getMode() != otherWPI.getMode()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		 int hashCode = 17;
		 hashCode = hashCode * 37 + getExecutionId().hashCode();
		 hashCode = hashCode * 37 + getName().hashCode();
		 hashCode = hashCode * 37 + getMode();
		 return hashCode;
	}

	@Override
	public String toString() {
		return "ParameterInfo [name=" + name + ", type=" + type + ", mode=" + mode + ", value=" + value + "]";
	}
}
