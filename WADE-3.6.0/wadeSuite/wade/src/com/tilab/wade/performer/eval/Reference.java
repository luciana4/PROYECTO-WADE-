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
package com.tilab.wade.performer.eval;

import com.tilab.wade.performer.BindingException;
import com.tilab.wade.performer.BindingManager;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.HierarchyNode;
import com.tilab.wade.performer.WorkflowBehaviour;

import jade.content.abs.AbsObject;

public class Reference implements Formula {

	private String activityName;
	private String parameterName;
	private int parameterMode;
	private String parameterPart;
	private WorkflowBehaviour owner;
	
	/** 
	 * Constructs the reference to an output parameter of a given activity.
	 * @param activityName The name of the activity
	 * @param parameterName The name of the parameter
	 * @param owner The activity owner workflow 
	 */
	public Reference(String activityName, String parameterName, WorkflowBehaviour owner) {
		this(activityName, parameterName, Constants.OUT_MODE, null, owner);
	}
	
	/** 
	 * Constructs the reference to a part of a input or output parameter of a given activity.
	 * @param activityName The name of the activity
	 * @param parameterName The name of the parameter
	 * @param parameterMode One of <code>Constants.IN_MODE</code> or <code>Constants.OUT_MODE</code>
	 * @param parameterPart A string in the form "a.b.c..." identifying the referred part of the parameter  
	 * @param owner The activity owner workflow 
	 */
	public Reference(String activityName, String parameterName, int parameterMode, String parameterPart, WorkflowBehaviour owner) {
		this.activityName = activityName;
		this.parameterName = parameterName;
		this.parameterMode = parameterMode;
		this.parameterPart = parameterPart;
		this.owner = owner;
	}
	
	public Object evaluate() throws Exception {
		if (activityName != null) {
			HierarchyNode activity = getSourceActivity();
			return activity.getBindingManager().getValue(parameterName, parameterMode, parameterPart);
		} 
		else {
			Object value = owner.getFieldValue(parameterName);
			return BindingManager.getPartValue(value, parameterPart, owner);
		}
	}

	public AbsObject evaluateAbs() throws Exception {
		if (activityName != null) {
			HierarchyNode activity = getSourceActivity();
			return activity.getBindingManager().getAbsValue(parameterName, parameterMode, parameterPart);
		} 
		else {
			Object value = owner.getFieldValue(parameterName);
			return BindingManager.getAbsPartValue(value, parameterPart, owner);
		}			
	}

	private HierarchyNode getSourceActivity() throws BindingException {
		HierarchyNode activity = (HierarchyNode)owner.getState(activityName);
		if (activity == null) {
			throw new BindingException("Activity "+activityName+" not present in workflow");
		}
		return activity;
	}
	
	public String getActivityName() {
		return activityName;
	}
	
	public String toString() {
		return "Reference("+activityName+","+parameterName+","+parameterMode+","+parameterPart+")";
	}
	
}
