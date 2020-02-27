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

import jade.util.Logger;

import com.tilab.wade.performer.BindingHolder;
import com.tilab.wade.performer.ComparisonHelper;

import bsh.EvalError;
import bsh.Interpreter;

public class Function extends BindingHolder implements Formula {
	
	protected Logger myLogger = Logger.getMyLogger(Function.class.getName());
	
	private String functionScript;
	private Interpreter interpreter = new Interpreter();

	
	public Function(String functionScript) {
		this.functionScript = functionScript;
		
		interpreter.getNameSpace().importClass(ComparisonHelper.class.getName());
	}

	public Object evaluate() throws Exception {
		
		myLogger.log(Logger.INFO, "Evaluate function "+functionScript);
		
		// Resolve all bindings
		resolveBindings();
		
		// Evaluate function
		Object result;
		try {
			result = interpreter.eval(functionScript);
		} catch(Exception e) {
			if (e instanceof EvalError) {
				throw (EvalError)e;
			} else {
				throw new EvalError("Function <"+functionScript+"> is not a valid expression", null, null);
			}
		}
		
		myLogger.log(Logger.INFO, "Function "+functionScript+" -> "+result);
		
		return result;
	}
	
	public String toString() {
		return "Function("+functionScript+")";
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
}
