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

import jade.core.behaviours.DataStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ApplicationList implements java.io.Serializable {
	private WorkflowBehaviour owner;
	private List<ApplicationRecord> applications = new ArrayList<ApplicationRecord>();
	private String currentApplication;
	private int index = -1;

	ApplicationList(WorkflowBehaviour owner) {
		this.owner = owner;
	}
	
	public void add(String applicationId) {
		applications.add(new ApplicationRecord(applicationId));
	}
	
	public Application next() throws Exception {
		index++;
		ApplicationRecord ar = applications.get(index);
		currentApplication = ar.getApplicationId();
		return ar.getApplication();
	}
	
	void reset() {
		index = -1;
		// Reset the Data Store of all applications
		for (ApplicationRecord ar : applications) {
			try {
				ar.getApplication().setDataStore(new DataStore());
			}
			catch (Exception e) {
				// Should never happen as applications have been already loaded at this time
			}
		}
	}
	
	String getCurrentApplication() {
		return currentApplication;
	}
	
	private class ApplicationRecord implements Serializable {
		private Application application;
		private String applicationId;
		
		public ApplicationRecord(String id) {
			applicationId = id;
		}
		
		public Application getApplication() throws Exception {
			if (application == null) {
				application = (Application) Class.forName(applicationId, true, owner.getClass().getClassLoader()).newInstance();
				application.setAgent(owner.getAgent());
				application.setExecutor((WorkflowEngineAgent.WorkflowExecutor) owner.root());
			}
			return application;
		}
		public String getApplicationId() {
			return applicationId;
		}
	}
}
