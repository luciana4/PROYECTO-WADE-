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

import jade.core.AID;
import jade.content.Concept;

/**
 * Bean-like class embedding relevant information about a SubflowEntry
 * @author Giovanni Caire - TILAB
 */
public class SubflowInfo implements Concept {
	private String id;
	private AID delegatedPerformer;
	private String executionId;
	private int status;
	
	public SubflowInfo() {
	}
	
	public SubflowInfo(String id) {
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setDelegatedPerformer(AID delegatedPerformer) {
		this.delegatedPerformer = delegatedPerformer;
	}
	
	public AID getDelegatedPerformer() {
		return delegatedPerformer;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("(Subflow ");
		sb.append(":id ");
		sb.append(id);
		sb.append(' ');
		sb.append(":delegatedPerformer ");
		sb.append(delegatedPerformer.getName());
		sb.append(' ');
		sb.append(":executionId ");
		sb.append((executionId != null ? executionId : "null"));
		sb.append(' ');
		sb.append(":status ");
		sb.append(status);
		sb.append(')');
		return sb.toString();
	}
	
	private String value(String v) {
		return (v != null ? v : "null");
	}
}
