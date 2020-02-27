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
package com.tilab.wade.performer.ontology;

//#MIDP_EXCLUDE_FILE

import jade.content.Predicate;
import jade.util.leap.List;

/**
   Indicate that an error occurred during the execution of a 
   workflow.
   @author Giovanni Caire - TILAB
 */
public class ExecutionError implements Predicate {
	private String type;
	private String reason;
	private List parameters;
	
	public ExecutionError() {
	}
	
	public ExecutionError(String type, String reason, List parameters) {
		this.type = type;
		this.reason = reason;
		this.parameters = parameters;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}
	
	public final void setParameters(List parameters) {
		this.parameters = parameters;
	}
	
	public final List getParameters() {
		return parameters;
	}
	
	public String toString() {
		if (type != null) {
			return type+": "+reason;
		}
		else {
			return "no-type: "+reason;
		}
	}
}
