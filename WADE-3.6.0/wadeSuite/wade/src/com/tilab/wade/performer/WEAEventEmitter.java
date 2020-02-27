/*****************************************************************
 JADE - Java Agent DEvelopment Framework is a framework to develop
 multi-agent systems in compliance with the FIPA specifications.
 Copyright (C) 2000 CSELT S.p.A.
 
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

import jade.core.Agent;
import jade.util.leap.List;

import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.EventEmitter;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.ontology.ControlInfo;

class WEAEventEmitter extends EventEmitter {

	private WorkflowDescriptor myDescriptor;
	private WorkflowBehaviour myWorkflow;
	
	public WEAEventEmitter(Agent agent, String id, String onto, String lang, WorkflowDescriptor myDescriptor, WorkflowBehaviour myWorkflow) {
		super(agent, id, onto, lang);
		
		this.myDescriptor = myDescriptor;
		this.myWorkflow = myWorkflow;
	}
	
	void setWorkflow(WorkflowBehaviour wb) {
		myWorkflow = wb;
	}

	protected void handleEvent(String id, String type, Object ev) {	
		((WorkflowEngineAgent)getAgent()).handleEvent(id, type, ev);
	}	
	
	protected void adjustControlInfo(ControlInfo cInfo) {
		((WorkflowEngineAgent)getAgent()).adjustControlInfo(cInfo, myDescriptor);
	}
	
	protected WorkflowEvent customizeEvent(String id, long time, String type, WorkflowEvent ev, List controllers) {
		return myWorkflow.customizeEvent(id, time, type, ev, controllers);
	}
}
