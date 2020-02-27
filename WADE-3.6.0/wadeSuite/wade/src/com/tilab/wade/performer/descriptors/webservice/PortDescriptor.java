package com.tilab.wade.performer.descriptors.webservice;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PortDescriptor implements Serializable {
	protected Map<String, OperationDescriptor> operationDescriptors = new HashMap<String, OperationDescriptor>();

	public OperationDescriptor getOperationDescriptor(String operName) {
		return operationDescriptors.get(operName);
	}

	public void addOperationDescriptor(String operName, OperationDescriptor operDesc) {
		operationDescriptors.put(operName, operDesc);
	}

	public Set<String> getOperationNames() {
		return operationDescriptors.keySet();
	}
}
