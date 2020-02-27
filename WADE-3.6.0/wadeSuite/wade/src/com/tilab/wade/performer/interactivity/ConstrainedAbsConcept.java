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
package com.tilab.wade.performer.interactivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jade.content.abs.AbsConcept;

/**
 * An abstract descriptor that extend the slot informations adding:
 * - list of constraints
 * - flag to indicate if the node was removed
 */
public class ConstrainedAbsConcept extends AbsConcept {

	private HashMap<String, List<Constraint>> constraintsMap = new HashMap<String, List<Constraint>>();
	private HashMap<String, Boolean> toBeRemovedMap = new HashMap<String, Boolean>();
	
	public ConstrainedAbsConcept(String typeName) {
		super(typeName);
	}

	public List<Constraint> getConstraints(String slotName) {
		return constraintsMap.get(slotName);
	}
	
	public void addConstraint(String slotName, Constraint constraint) {
		addConstraint(slotName, constraint, false);
	}
	
	public void addConstraint(String slotName, Constraint constraint, boolean override) {
		List<Constraint> constraints = getConstraints(slotName);
		if (constraints == null) {
			constraints = new ArrayList<Constraint>();
			constraintsMap.put(slotName, constraints);
		}
		
		// Check if already present
		Iterator<Constraint> it = constraints.iterator();
		while (it.hasNext()) {
			if (it.next().getClass() == constraint.getClass()) {
				if (override) {
					it.remove();
				} else {
					return;
				}
			}
		}
		
		constraints.add(constraint);
	}
	
	public void markToBeRemove(String slotName, boolean remove) {
		toBeRemovedMap.put(slotName, Boolean.valueOf(remove));
	}

	public boolean isToBeRemove(String slotName) {
		Boolean remove = toBeRemovedMap.get(slotName);
		if (remove != null && remove.booleanValue()) {
			return true;
		}
		return false;
	}
}
