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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.eval.Constant;
import com.tilab.wade.performer.eval.Formula;
import com.tilab.wade.performer.eval.Reference;

public class BindingHolder implements Serializable {

	private List<Bind> bindings = new ArrayList<Bind>();
	private Map<String, Map<String, List<Bind>>> bindingsMap = null;
	
	/**
	 * Binds a given part of a given parameter to the value produced by a given Formula.
	 * This method allows to automatically assign at runtime a value produced by a previously 
	 * visited activity to a parameter of the activity this BindingHolder is associated to.
	 * For instance if activity A produces output parameter p1 and activity B requires 
	 * input parameter p2, the bind() method can be used to tell the WADE Workflow Engine to use p1 as 
	 * value for p2.
	 * @param name The name of the parameter that must be assigned. If the activity this 
	 * BindingHolder belongs to includes more than one BuildingBlock the <code>name</code>
	 * parameter will have the form <param-name>@<building-block-id>, where building-block-id 
	 * has a form that depends on the type of the activity this BindingHolder belongs to.
	 * @param part When not null this parameter indicates that the binding does not refer to 
	 * the whole parameter, but just to a part of it. For instance given the following classes<br>
	 * <code><br>
	 * class Person {<br>
	 *   String name;<br>
	 *   Address address;<br>
	 *   ...<br>
	 * }<br>
	 * <br>
	 * class Address {<br>
	 *   String street;<br>
	 *   int number;<br>
	 *   ...<br>
	 * }<br>
	 * </code>
	 * a binding to just the number of the address of a parameter of type Person is indicated as
	 * <code>address.number</code>
	 *   
	 * @param formula Defines where to get the value to be assigned to the indicated parameter or 
	 * parameter part.
	 * 
	 * @see BuildingBlock
	 */
	public void bind(String name, String part, Formula formula) {
		bindings.add(new Bind(name, part, formula));
	}
	public void bind(String name, String part, Object value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name, String part, boolean value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name, String part, int value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name, String part, long value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name, String part, float value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name, String part, double value) {
		bind(name, part, new Constant(value));
	}
	public void bind(String name) {
		bind(name, null, null);
	}

	protected List<Bind> getBindings() {
		return bindings;
	}

	// Organize binding in activityName -> targetName -> List of binds 
	// Note that only the Reference binding are managed
	protected Map<String, List<Bind>> getRelevantBindings(String activityName) {
		if (bindingsMap == null) {
			bindingsMap = createOutputBindingMap();
		}
		return bindingsMap.get(activityName);
	}
	
	private Map<String, Map<String, List<Bind>>> createOutputBindingMap() {
		Map<String, Map<String, List<Bind>>> bindingsMap = new HashMap<String, Map<String, List<Bind>>>();
		for (Bind bind : bindings) {
			Formula formula = bind.getFormula();
			if (formula instanceof Reference) {
				Reference reference = (Reference)formula;
				String activityName = reference.getActivityName();
				String targetName = bind.getName();

				Map<String, List<Bind>> bindings4Activity = bindingsMap.get(activityName);
				if (bindings4Activity == null) {
					bindings4Activity = new HashMap<String, List<Bind>>(); 
					bindingsMap.put(activityName, bindings4Activity);
				}

				List<Bind> bindings4Target = bindings4Activity.get(targetName);
				if (bindings4Target == null) {
					bindings4Target = new ArrayList<Bind>();
					bindings4Activity.put(targetName, bindings4Target);
				}
				
				bindings4Target.add(bind);
			}
		}
		
		return bindingsMap;
	}
	
	protected List<Bind> getBindings(String parameterName) {
		List<Bind> bindsByParam = new ArrayList<Bind>();
		for (Bind bind : bindings) {
			if (bind.name.equals(parameterName)) {
				bindsByParam.add(bind);
			}
		}
		return bindsByParam;
	}
	
	protected class Bind implements Serializable {
		private String name;
		private String part;
		private Formula formula;
		
		public Bind(String name, String part, Formula formula) {
			this.name = name;
			this.part = part;
			this.formula = formula;
		}

		public String getName() {
			return name;
		}

		public String getPart() {
			return part;
		}

		public Formula getFormula() {
			return formula;
		}
	}
	
}
