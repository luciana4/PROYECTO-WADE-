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

import jade.util.leap.ArrayList;
import jade.util.leap.List;
import jade.util.leap.Serializable;

/**
 * This class groups the information related to the transitions exiting from an activity
 */
class OutgoingTransitions implements Serializable {
	private List transitions = new ArrayList();
	private List exceptionTransitions = new ArrayList();
	private int cnt = 0;
	
	OutgoingTransitions() {
	}
	
	/** 
	 * In order to facilitate Workflow extension there can be only one transition (and one exception transition) 
	 * from a given source activity to a given destination --> The algorithm is as follows:
	 * If t is NOT a default-transition --> 
	 *   If another transition to the same destination already exists, replace it.
	 *   Otherwise append the new transition just before the default-transition if any (this
	 *   ensures that transitions will be evaluated in the order they are added a part from the 
	 *   default transition that will always be evaluated at the end)
	 * Else
	 *   If another transition to the same destination already exists remove it.
	 *   In any case append the new transition at the end of the list
	 */
	Transition putTransition(Transition t) {
		t.setExitValue(cnt++);
		List tt = (t.isException() ? exceptionTransitions : transitions);
		String destination  = t.getDestination();
		Transition old = null;
		if (destination != null) {
			for (int i = 0; i < tt.size(); ++i) {
				Transition t1 = (Transition) tt.get(i);
				if (destination.equals(t1.getDestination())) {
					tt.remove(i);
					old = t1;
					if (!t.isDefault()) {
						tt.add(i, t);
						return old;
					}
				}
				else if (t1.isDefault()) {
					tt.add(i, t);
					return old;
				}
			}
			tt.add(t);
		}
		return old;
	}
	
	Transition removeTransition(String destination, boolean exception) {
		List tt = (exception ? exceptionTransitions : transitions);
		if (destination != null) {
			for (int i = 0; i < tt.size(); ++i) {
				Transition t = (Transition) tt.get(i);
				if (destination.equals(t.getDestination())) {
					tt.remove(i);
					return t;
				}
			}
		}
		return null;
	}
	
	List getTransitions() {
		return transitions;
	}

	List getExceptionTransitions() {
		return exceptionTransitions;
	}
	
	void init(OutgoingTransitions ot) {
		transitions = ot.transitions;
		exceptionTransitions = ot.exceptionTransitions;
		cnt = ot.cnt;
	}
}
