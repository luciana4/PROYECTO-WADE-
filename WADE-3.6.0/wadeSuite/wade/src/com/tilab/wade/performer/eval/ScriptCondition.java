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

import com.tilab.wade.performer.ComparisonHelper;

import jade.util.Logger;
import bsh.EvalError;
import bsh.Interpreter;

public class ScriptCondition extends Condition {

	protected Logger myLogger = Logger.getMyLogger(ScriptCondition.class.getName());

	private String conditionScript;
	private Interpreter interpreter = new Interpreter();

	
	public ScriptCondition(String conditionScript) {
		this.conditionScript = conditionScript;
		
		interpreter.getNameSpace().importClass(ComparisonHelper.class.getName());
	}
	
	public boolean evaluate() throws Exception {
		
		myLogger.log(Logger.INFO, "Evaluate script-condition "+conditionScript);
		
		// Resolve all bindings
		resolveBindings();
		
		// Evaluate condition
		boolean result = true;
		try {
			result = (Boolean)interpreter.eval(conditionScript);
		} catch(Exception e) {
			if (e instanceof EvalError) {
				throw (EvalError)e;
			} else {
				throw new EvalError("Condition <"+conditionScript+"> is not a valid logical expression", null, null);
			}
		}
		
		myLogger.log(Logger.INFO, "Script-condition "+conditionScript+" -> "+result);

		return result;
	}
	
	private void resolveBindings() throws Exception {
		for (Bind bind : getBindings()) {
			if (bind.getFormula() != null) {
				// Evaluate formula
				Object value = bind.getFormula().evaluate();

				// Set value to interpreter 
				interpreter.set(bind.getName(), value);
				
				myLogger.log(Logger.INFO, "Set "+bind.getName()+" = "+value);
			}
		}
	}
	
	public String toString() {
		return conditionScript;
	}
	
}
