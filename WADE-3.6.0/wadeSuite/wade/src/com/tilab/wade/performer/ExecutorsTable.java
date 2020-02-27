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

import jade.core.behaviours.Behaviour;

import jade.util.leap.Map;
import jade.util.leap.HashMap;
import jade.util.leap.List;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;

import java.io.Serializable;
import java.util.Date;

import com.tilab.wade.performer.WorkflowEngineAgent.WorkflowExecutor;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.ExecutorInfo;
import com.tilab.wade.performer.ontology.SubflowInfo;
import com.tilab.wade.performer.transaction.*;

/**
 * This class stores the WorkflowExecutors active or enqueued in a given WorkflowEngineAgent 
 * and provides utility methods to inspect them
 * @author Giovanni Caire - TILAB
 */
public class ExecutorsTable implements Serializable {
	
	private Map executors = new HashMap();
	
	public ExecutorsTable() {
	}
	
	public synchronized void insert(WorkflowEngineAgent.WorkflowExecutor executor) {
		executors.put(executor.getId(), executor);
	}
	
	public synchronized WorkflowEngineAgent.WorkflowExecutor get(String id) {
		return (WorkflowEngineAgent.WorkflowExecutor) executors.get(id);
	}
	
	public synchronized WorkflowEngineAgent.WorkflowExecutor remove(String id) {
		return (WorkflowEngineAgent.WorkflowExecutor) executors.remove(id);
	}
	
	public synchronized boolean contains(String id) {
		return executors.containsKey(id);
	}
	
	public int size() {
		return executors.size();
	}
	
	public int suspendedCnt() {
	
		int cnt = 0;
		Iterator it = executors.values().iterator();
		while (it.hasNext()) {
			WorkflowEngineAgent.WorkflowExecutor we = (WorkflowEngineAgent.WorkflowExecutor) it.next();
			if (we.getStatus() == WorkflowEngineAgent.SUSPENDED_STATUS) {
				cnt++;;
			}
		}
		
		return cnt;
	}
	
	/**
	 * @return true if a WorkflowExecutor exists with the same sessionID of a given executor and that WorkflowExecutor
	 * is not waiting in the queue.
	 */
	boolean belongsToRunningSession(WorkflowEngineAgent.WorkflowExecutor executor) {
		String sessionId = executor.getDescriptor().getSessionId();
		if (sessionId != null) {
			Iterator it = executors.values().iterator();
			while (it.hasNext()) {
				WorkflowEngineAgent.WorkflowExecutor we = (WorkflowEngineAgent.WorkflowExecutor) it.next();
				WorkflowDescriptor wd = we.getDescriptor();
				if (sessionId.equals(wd.getSessionId())) {
					if (we.getStatus() != WorkflowEngineAgent.IDLE_STATUS &&
						we.getStatus() != WorkflowEngineAgent.SUSPENDED_STATUS) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @return All pending WorkflowExecutor belonging to a given session
	 * A "pending" workflow has the same session id, but a different session startup time 
	 */
	public synchronized List getPendingExecutors(String sessionId, Date sessionStartup) {
		List l = new ArrayList();
		Iterator it = executors.values().iterator();
		while (it.hasNext()) {
			WorkflowEngineAgent.WorkflowExecutor we = (WorkflowEngineAgent.WorkflowExecutor) it.next();
			WorkflowDescriptor wd = we.getDescriptor();
			if (sessionId.equals(wd.getSessionId())) {
				if (!sessionStartup.equals(wd.getSessionStartup())) {
					l.add(we);
				}
			}
		}
		return l;
	}
	
	public synchronized List getStatus() {
		List result = new ArrayList();
		Iterator it = executors.values().iterator();
		while (it.hasNext()) {
			WorkflowEngineAgent.WorkflowExecutor we = (WorkflowEngineAgent.WorkflowExecutor) it.next();
			ExecutorInfo info = getExecutorInfo(we);
			result.add(info);
		}
		return result;
	}

	public synchronized List getStatus(String sessionId) {
		List result = new ArrayList();
		Iterator it = executors.values().iterator();
		while (it.hasNext()) {
			WorkflowEngineAgent.WorkflowExecutor we = (WorkflowEngineAgent.WorkflowExecutor) it.next();
			if (sessionId.equals(we.getDescriptor().getSessionId())) {
				ExecutorInfo info = getExecutorInfo(we);
				result.add(info);
			}
		}
		return result;
	}

	private ExecutorInfo getExecutorInfo(WorkflowEngineAgent.WorkflowExecutor we) {
		ExecutorInfo info = new ExecutorInfo(we.getId());
		WorkflowDescriptor wd = we.getDescriptor();
		info.setSessionId(wd.getSessionId());
		info.setDelegationChain(wd.getDelegationChain());
		info.setExecutorStatus(we.getStatus());
		Behaviour b = we.getCurrent();
		if (b != null) {
			info.setExecutorFSMState(b.getBehaviourName());
		}
		b = we.getWorkflow().getCurrent();
		if (b != null) {
			info.setWorkflowFSMState(b.getBehaviourName());
		}
		info.setAbortCondition(we.getAbortCondition());
		WatchDog watchDog = we.getWatchDog();
		if (watchDog != null) {
			info.setWatchDogExpired(new Boolean(watchDog.isExpired()));
			info.setWatchDogStartTime(new Date(watchDog.getStartTime()));
			info.setWatchDogWakeupTime(new Date(watchDog.getWakeupTime()));
		}
		
		TransactionManager tm = we.getTransactionManager();
		if (tm != null) {
			java.util.List subflows = tm.getEntries(TransactionEntry.SUBFLOW_TYPE, null);
			List subflowInfos = new ArrayList();
			if (subflows != null) {
				for (int i = 0; i < subflows.size(); ++i) {
					subflowInfos.add(getSubflowInfo((SubflowEntry) subflows.get(i)));
				}
			}
			info.setSubflows(subflowInfos);
		}
		return info;
	}
	
	private SubflowInfo getSubflowInfo(SubflowEntry entry) {
		SubflowInfo info = new SubflowInfo(entry.getId());
		info.setDelegatedPerformer(entry.getDelegatedPerformer());
		info.setExecutionId(entry.getExecutionId());
		info.setStatus(entry.getStatus());
		return info;
	}
}
		