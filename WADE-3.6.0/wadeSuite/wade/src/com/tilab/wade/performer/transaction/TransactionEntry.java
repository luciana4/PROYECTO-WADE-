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
package com.tilab.wade.performer.transaction;

/**
 * Common base class for transaction entries related to applications and subflows
 */
public abstract class TransactionEntry {
	public static final Class APPLICATION_TYPE = com.tilab.wade.performer.transaction.ApplicationEntry.class; 
	public static final Class SUBFLOW_TYPE = com.tilab.wade.performer.transaction.SubflowEntry.class; 
	public static final Class TASK_TYPE = com.tilab.wade.performer.transaction.TaskEntry.class; 
	public static final Class ANY_TYPE = com.tilab.wade.performer.transaction.TransactionEntry.class; 
	
	private String id;
	private String label;
	private String activity;

	public String getId() {
		return id;
	}
	
	void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}
	
	void setLabel(String label) {
		this.label = label;
	}
	
	public String getActivity() {
		return activity;
	}
	
	void setActivity(String activity) {
		this.activity = activity;
	}
	
	public abstract boolean isSuccessful();
	public abstract void commit() throws Throwable;
	public abstract void rollback() throws Throwable;
}
