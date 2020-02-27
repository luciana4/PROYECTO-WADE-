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

//#MIDP_EXCLUDE_FILE

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.performer.ontology.GenericError;
import com.tilab.wade.performer.ontology.NotificationError;

import jade.content.Predicate;
import jade.core.Agent;
import jade.core.Agent.Interrupted;
import jade.domain.AMSService;
import jade.lang.acl.ACLMessage;

/**
 * The behaviour implementing activities of type SUBFLOW in a workflow. 
 */
public class SubflowDelegationBehaviour extends ActivityBehaviour {
	private MethodInvocator invocator;
	private Subflow subflow;
	private List<TerminationNotificationReceiver> asynchDelegations = new ArrayList<TerminationNotificationReceiver>();

	public SubflowDelegationBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}
	
	/**
	 * Create a SubflowDelegationBehaviour specifying whether the subflow delegation 
	 * must be performed by means of an ad-hoc "executeNnn()" method (being Nnn the
	 * name of the subflow activity) or directly by means of performSubflow() method 
	 * of the WorkflowBehaviour class. This second case occurs when the workflow
	 * does not have a class associated to it, but is built instructing a WorkflowBehaviour
	 * object on the fly. 
	 * @param name The name of the activity
	 * @param owner The workflow this subflow activity belongs to
	 * @param callExecuteMethod Whether the subflow delegation 
	 * must be performed by means of an ad-hoc "executeNnn()" method 
	 */
	public SubflowDelegationBehaviour(String name, WorkflowBehaviour owner, boolean callExecuteMethod) {
		super(name, owner);
		requireSave = true;
		
		subflow = new Subflow(this);
		
		String methodName;
		if (callExecuteMethod) {
			methodName = EngineHelper.activityName2Method(getBehaviourName());
		} else {
			methodName = "performSubflow";
		}
		
		EngineHelper.checkMethodName(methodName, "activity", name);
		// Note that at this time subflow does not have a value yet
		// --> We cannot pass it to the MethodInvocator constructor
		invocator = new MethodInvocator(owner, methodName, subflow, Subflow.class);
	}
	
	public void setSubflow(String subflowId) {
		subflow.setSubflowId(subflowId);
	}
	
	public void setFormat(String format) {
		subflow.setFormat(format);
	}

	public void setRepresentation(String representation) {
		subflow.setRepresentation(representation);
	}
	
	public void setAsynch() {
		subflow.setAsynch(true);
	}
	
	public void setIndependent() {
		subflow.setIndependent(true);
	}
	
	public void action() {
		try {
			owner.enterInterruptableSection();
			invocator.invoke();
		}
		catch (InterruptedException ie) {
		}
		catch (Interrupted i) {
		}
		catch (ThreadDeath td) {
		}
		catch (Throwable t) {
			handleException(t);
			if (!EngineHelper.logIfUncaughtOnly(this, t)) {
				t.printStackTrace();
			}
		}
		finally {
			owner.exitInterruptableSection(this);
		}
	}
	
	public void reset() {
		super.reset();

		// Reset specific building block
		subflow.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		return subflow;
	}
	
	void addAsynchronousDelegation(TerminationNotificationReceiver recv) {
		asynchDelegations.add(recv);
		recv.setDelegationBehaviour(this);
	}
	
	void removeAsynchronousDelegation(TerminationNotificationReceiver recv) {
		asynchDelegations.remove(recv);
	}
	
	Iterator<TerminationNotificationReceiver> getAllAsynchronousDelegations() {
		return asynchDelegations.iterator();
	}
	
	static void handleSubflowFailure(Agent agent, ACLMessage failure) throws Exception {
		if (failure.getSender().equals(agent.getAMS())) {
			// Performer does not exist
			throw new FailedSubflow("Performer "+AMSService.getFailedReceiver(agent, failure).getName()+" does not exist");
		}else if (failure.getSender().equals(CAServices.getInstance(agent).getLocalCA())){
			// Performer died during execution
			throw new FailedSubflow(failure.getContent());
		}
		else {
			// Execution failure
			Predicate p = (Predicate) agent.getContentManager().extractContent(failure);
			if (p instanceof NotificationError) {
				throw new FailedSubflow("Notification error received from delegated performer "+failure.getSender().getName());
			}
			else if (p instanceof GenericError) {
				throw new FailedSubflow("Error loading workflow: "+((GenericError) p).getReason());
			}
			else if (p instanceof ExecutionError) {
				// NOTE That unlike version 1, WADE 2 does not restore output parameters after a subflow execution error
				ExecutionError er = (ExecutionError) p;
				FailedSubflow fs = new FailedSubflow("|"+er.getReason());
				fs.setExecutionError(er);
				throw fs;
			}
			else {
				throw new FailedSubflow("Unknown error: "+p);
			}
		}
	}

	Subflow getSubflow() {
		return subflow;
	}
}
