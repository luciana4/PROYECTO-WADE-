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
package com.tilab.wade.performer;

import jade.util.leap.Serializable;

import com.tilab.wade.performer.eval.Condition;
import com.tilab.wade.performer.eval.MethodCondition;

/**
   This class provides the association between a condition and a
   behaviour exit value.
   @author Giovanni Caire - TILAB
 */
public class Transition implements Serializable {
	private boolean defaultType;
	private boolean exceptionType;
	private int exitValue;
	private String source;
	private String destination;
	private Condition condition;
	
	
	public Transition() {
		this(Constants.OTHERWISE, null, null);
	}
	
	public Transition(String conditionName, WorkflowBehaviour owner) {		
		this(Constants.CONDITION, conditionName, owner);
	}

	public Transition(Condition condition) {
		this(Constants.CONDITION, condition);
	}
	
	protected Transition(int type, Condition condition) {
		defaultType = (type == Constants.OTHERWISE || type == Constants.DEFAULT_EXCEPTION);
		exceptionType = (type == Constants.EXCEPTION || type == Constants.DEFAULT_EXCEPTION);
		if (!defaultType) {
			if (condition == null) {
				throw new IllegalArgumentException("Missing condition for transition of type "+type);
			}
			this.condition = condition;
		}
	}

	protected Transition(int type, String conditionName, WorkflowBehaviour owner) {
		defaultType = (type == Constants.OTHERWISE || type == Constants.DEFAULT_EXCEPTION);
		exceptionType = (type == Constants.EXCEPTION || type == Constants.DEFAULT_EXCEPTION);
		if (!defaultType) {
			if (conditionName == null) {
				throw new IllegalArgumentException("Missing condition name for transition of type "+type);
			}
			if (owner == null) {
				throw new IllegalArgumentException("Missing owner for transition of type "+type);
			}
			if (conditionName != null) {
				String methodName = EngineHelper.conditionName2Method(conditionName);
				EngineHelper.checkMethodName(methodName, "condition", conditionName);
				MethodInvocator invocator = createInvocator(owner, methodName);
				condition = new MethodCondition(conditionName, invocator);
			}
		}
	}
	
	protected MethodInvocator createInvocator(WorkflowBehaviour owner, String methodName) {
		return new MethodInvocator(owner, methodName);
	}
		
	public boolean evaluateCondition() throws Exception {
		boolean result = true;
		if (condition != null) {
			Boolean b = (Boolean) condition.evaluate();
			result = b.booleanValue();
		}
		return result;
	}
	
	final boolean isDefault() {
		return defaultType;
	}
	
	final boolean isException() {
		return exceptionType;
	}
	
	final int getExitValue() {
		return exitValue;
	}
	
	final void setExitValue(int v) {
		exitValue = v;
	}
	
	final void setDestination(String destination) {
		this.destination = destination;
	}
	
	final String getDestination() {
		return destination;
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public String getConditionName() {
		return condition.toString();
	}
	
	public String toString() {
		return "[Transition from "+(source != null ? source : "???")+" to "+(destination != null ? destination: "???")+"]";
	}
}
			