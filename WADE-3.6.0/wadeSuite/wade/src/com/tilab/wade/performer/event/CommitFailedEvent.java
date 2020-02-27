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
package com.tilab.wade.performer.event;
//#MIDP_EXCLUDE_BEGIN

public class CommitFailedEvent extends WorkflowEvent {
	private String executionId;
	private String entryId;
	private String entryType;
	private Throwable error;
	
	public CommitFailedEvent() {
	}
	
	public CommitFailedEvent(String executionId, String entryId, String entryType, Throwable t) {
		this.executionId = executionId;
		this.entryId = entryId;
		this.entryType = entryType;
		error = t;
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public String getEntryId() {
		return entryId;
	}
	
	public String getEntryType() {
		return entryType;
	}
	
	public Throwable getError() {
		return error;
	}
	
	public String toString() {
		return getClass().getName()+": execution-id="+getExecutionId()+", entryId="+getEntryId()+" entryType="+getEntryType()+" error="+getError();		
	}
}
