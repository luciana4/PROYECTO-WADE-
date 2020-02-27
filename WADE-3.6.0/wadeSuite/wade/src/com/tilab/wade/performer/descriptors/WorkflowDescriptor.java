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
package com.tilab.wade.performer.descriptors;

import jade.util.leap.List;

import java.util.Date;
import java.util.Map;

import com.tilab.wade.performer.Constants;

/**
   Descriptor specifying a workflow in terms of process-id, name, 
   actual parameters and other workflow-specific attributes 
   @author Giovanni Caire - TILAB
 */
public class WorkflowDescriptor extends ElementDescriptor {
	private int execution = Constants.SYNCH;
	private int priority = Constants.NO_PRIORITY;
	private long timeout = Constants.INFINITE_TIMEOUT;
	private boolean transactional = false;
	private String requester = null;
	private String sessionId = null;
	private String delegationChain = null;
	private Date sessionStartup = null;
	private String classLoaderIdentifier = null;
	private String format = null;
	private String representation = null;

	public WorkflowDescriptor() {
		super();
	}
	
	public WorkflowDescriptor(String id) {
		super(id);
	}
	
	public WorkflowDescriptor(String id, List parameters) {
		super(id, parameters);
	}
	
	public WorkflowDescriptor(String id, Map<String, Object> paramsMap) {
		super(id, paramsMap);
	}
	
	/**
	 * @deprecated Use WorkflowDescriptor(String id, List parameters) or WorkflowDescriptor(String id, Map paramsMap) instead
	 */
	public WorkflowDescriptor(String id, List packages, List parameters) {
		super(id, packages, parameters);
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getRepresentation() {
		return representation;
	}

	public void setRepresentation(String representation) {
		this.representation = representation;
	}
	
	public void setExecution(int execution) {
		this.execution = execution;
	}
	
	public int getExecution() {
		return execution;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}
	
	/**
	   @return <code>true</code> if the described workflow must 
	   be cosidered as a transaction unit, i.e. all delegated subflows
	   must be executed in a transaction scope, and if the described
	   workflow fails the roll-back must be requested for all 
	   delegated subflows and the RB-workflow must be activated. 
	 */
	public boolean getTransactional() {
		return transactional;
	}		
	
	public void setRequester(String requester) {
		this.requester = requester;
	}
	
	public String getRequester() {
		return requester;
	}	
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getSessionId() {
		return sessionId;
	}	
	
	public void setDelegationChain(String delegationChain) {
		this.delegationChain = delegationChain;
	}
	
	public String getDelegationChain() {
		return delegationChain;
	}	
	
	public void setSessionStartup(Date sessionStartup) {
		this.sessionStartup = sessionStartup;
	}
	
	public Date getSessionStartup() {
		return sessionStartup;
	}

	public String getClassLoaderIdentifier() {
		return classLoaderIdentifier;
	}

	public void setClassLoaderIdentifier(String classLoaderIdentifier) {
		this.classLoaderIdentifier = classLoaderIdentifier;
	}	
	
	public void importInfo(WorkflowDescriptor dsc) {
		// Propagate the transactional attribute
		setTransactional(dsc.getTransactional());
		
		// Propagate the session-id attribute
		setSessionId(dsc.getSessionId());
		
		// Propagate the session-startup
		setSessionStartup(dsc.getSessionStartup());
		
		// Propagate the requester attribute
		setRequester(dsc.getRequester());
		
		// Propagate the classloader identifier
		setClassLoaderIdentifier(dsc.getClassLoaderIdentifier());
		
		// Update the delegation chain
		setDelegationChain(dsc.getDelegationChain());
		
		// Adjust the priority unless a priority is specifically set
		if (getPriority() == Constants.NO_PRIORITY) {
			setPriority(dsc.getPriority());
		}
	}
}
