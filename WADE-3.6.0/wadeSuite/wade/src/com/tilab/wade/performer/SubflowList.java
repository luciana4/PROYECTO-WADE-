package com.tilab.wade.performer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SubflowList implements java.io.Serializable {

	private Map <String, Object> subflowsMap = new HashMap <String, Object>();
	
	void put(String label, int policy) {
		if (policy == SubflowJoinBehaviour.ANY){
			subflowsMap.put(label,null);
		}else{
			subflowsMap.put(label, new ArrayList<Subflow>());
		}
	}
	
	void put(String label, int policy, List <Subflow> subflows) {
		if (subflows == null || subflows.size() == 0){
			return;
		}
		if (policy == SubflowJoinBehaviour.ANY){
			subflowsMap.put(label, subflows.get(0));
		}else{
			List<Subflow> sbfs = (List<Subflow>)subflowsMap.get(label);
			sbfs.addAll(subflows);
		}
	}
	
	public Object get(String label) {
		return subflowsMap.get(label);
	}

	List<Subflow> getAll() {
		List<Subflow> subflows = new ArrayList<Subflow>();
		for (Object value : subflowsMap.values()) {
			if (value != null) {
				if (value instanceof Subflow) {
					subflows.add((Subflow)value);
				} else {
					subflows.addAll((List<Subflow>)value);
				}
			}			
		}
		return subflows;
	}
	
	public void reset() {
		subflowsMap.clear();
	}
}
