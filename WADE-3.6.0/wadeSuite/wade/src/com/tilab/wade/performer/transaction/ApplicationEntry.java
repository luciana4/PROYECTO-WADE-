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

import com.tilab.wade.performer.Application;

import jade.core.behaviours.DataStore;

public class ApplicationEntry extends TransactionEntry {
	private Application myApplication;
	private DataStore myDataStore;
	
	public ApplicationEntry(String id, Application ap, DataStore ds) {
		setId(id);
		myApplication = ap;
		myDataStore = ds;
	}
	
	public boolean isSuccessful() {
		return true;
	}
	
	public void commit() throws Throwable {
		myApplication.setDataStore(myDataStore);
		myApplication.commit();
	}
	
	public void rollback() throws Throwable {
		myApplication.setDataStore(myDataStore);
		myApplication.rollback();
	}
}
