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

import jade.core.Agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.event.CommitFailedEvent;
import com.tilab.wade.performer.event.EventEmitter;
import com.tilab.wade.performer.event.RollbackFailedEvent;


/**
   This class manages the default commit and rollback processes of a transactional workflow. 
   As long as a transactional workflow is executed this class is filled with a collection of 
   <code>TransactionEntry</code> objects each one corresponding to a performed step of the workflow (an Application,
   a Subflow or a CodeActivity).
   The TransactionManager class provides method to commit/rollback the steps of the workflow (by invoking the 
   commit() and rollback() method on the related TransactionEntry objects) all together or separately.
   Committed/rolled-back entries are automatically removed from the TransactionManager.
   @author Giovanni Caire - TILAB
 */
public class TransactionManager implements Serializable {
	public static final String DEFAULT_LABEL = "__default__";
	
	private List entriesByOrder = new LinkedList();
	private Map entriesById = new HashMap();
	private String currentLabel = DEFAULT_LABEL;
	private List labels = new ArrayList();
	
	private String executionId;
	private EventEmitter myEventEmitter;

	public TransactionManager(String executionId, EventEmitter emitter) {
		this.executionId = executionId; 
		myEventEmitter = emitter;
	}
	
	/**
	 * Set the label that will be used to mark all entries that will be added from 
	 * now on 
	 */
	public void setLabel(String label) {
		currentLabel = label;
	}
	
	public String getLabel() {
		return currentLabel;
	}
	
	public List getLabels() {
		return labels;
	}
	
	/**
	 * Add a <code>TransactionEntry</code> to this TransactionManager.
	 */
	public synchronized void addEntry(TransactionEntry entry) {
		entry.setLabel(currentLabel);
		if (!labels.contains(currentLabel)) {
			labels.add(currentLabel);
		}
		labels.add(currentLabel);
		entriesByOrder.add(entry);
		entriesById.put(entry.getId(), entry);
	}
	
	/**
	 * Retrieve the <code>TransactionEntry</code> identified by a given ID or null if an entry
	 * with this ID is not found.
	 */
	public synchronized TransactionEntry getEntry(String id) {
		return (TransactionEntry) entriesById.get(id);
	}
	
	/**
	 * Remove the <code>TransactionEntry</code> that has a given ID.
	 */
	public synchronized TransactionEntry removeEntry(String id) {
		TransactionEntry entry = (TransactionEntry) entriesById.remove(id);
		if (entry != null) {
			entriesByOrder.remove(entry);
		}
		return entry;
	}
	
	/**
	 * Remove all <code>TransactionEntry</code> objects stored in this TransactionManager
	 */
	public synchronized void clear() {
		entriesByOrder.clear();
		entriesById.clear();
	}
	
	/**
	 * Retrieve all <code>TransactionEntry</code> objects of a given type that are marked with a given label.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE
	 * If <code>label</code> is null, all entries of the specified type are returned.
	 */
	public synchronized List getEntries(Class type, String label) {
		Iterator it = entriesByOrder.iterator();
		List l = new ArrayList();
		while (it.hasNext()) {
			TransactionEntry entry = (TransactionEntry) it.next();
			if (type.isInstance(entry)) {
				if (label == null || label.equals(entry.getLabel())) {
					l.add(entry);
				}
			}
		}
		return l;
	}
	
	/**
	 * Remove all <code>TransactionEntry</code> objects of a given type that are marked with a given label.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE
	 * If <code>label</code> is null, all entries of the specified type are removed.
	 * @return a List containing the removed entries
	 */
	public synchronized List removeEntries(Class type, String label) {
		List l = null;
		if (type.equals(TransactionEntry.ANY_TYPE) && label == null) {
			// All entries must be returned
			l = entriesByOrder;
			entriesByOrder = new LinkedList();
			entriesById.clear();
		}
		else {
			l = getEntries(type, label);
			Iterator it = l.iterator();
			while (it.hasNext()) {
				TransactionEntry entry = (TransactionEntry) it.next();
				removeEntry(entry.getId());
			}
		}
		return l;
	}
	
	/**
	 * Remove the last <code>TransactionEntry</code> of a given type.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE.
	 * @return The removed entry
	 */
	public synchronized TransactionEntry removeLastEntry(Class type) {
		TransactionEntry lastEntry = null;
		int size = entriesByOrder.size();
		for (int i = size; i > 0; --i) {
			lastEntry = (TransactionEntry) entriesByOrder.get(i-1);
			if (type.isInstance(lastEntry)) {
				return lastEntry;
			}
		}
		return null;
	}
	
	/**
	 * Commit all successful <code>TransactionEntry</code> objects stored in this TransactionManager.
	 */
	public boolean commit() {
		return commit(TransactionEntry.ANY_TYPE, null);
	}
	
	/**
	 * Commit all successful <code>TransactionEntry</code> objects of a given type that are marked with a given label.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE.
	 * If <code>label</code> is null, all entries of the specified type are committed.
	 * Committed entries are automatically removed.
	 */
	public boolean commit(Class type, String label) {
		boolean ret = true;
		List l = removeEntries(type, label);
		// Commit entries in reverse order
		for (int i = l.size(); i > 0; --i) {
			TransactionEntry entry = (TransactionEntry) l.get(i-1);
			if (!commit(entry)) {
				ret = false;
			}
		}
		return ret;
	}

	/**
	 * Commit the last <code>TransactionEntry</code> of a given type.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE.
	 * The committed entry is automatically removed.
	 */
	public boolean commitLast(Class type) {
		TransactionEntry lastEntry = removeLastEntry(type);
		if (lastEntry != null) {
			return commit(lastEntry);
		}
		else {
			// No such entry --> Nothing to commit
			return true;
		}
	}
	
	private boolean commit(TransactionEntry entry) {
		try {
			if (entry.isSuccessful()) {
				entry.commit();
				// FIXME Uncomment when the CommitEvent will be available
				//myEventEmitter.fireEvent(Constants.TRANSACTION_TYPE, new CommitEvent(executionId, entry), Constants.FINE_LEVEL);
			}
			return true;
		}
		catch (InterruptedException ie) {
			// Let it through as an Agent.Interrupted error
			throw new Agent.Interrupted();
		}
		catch (Agent.Interrupted ai) {
			// Let it through 
			throw ai;
		}
		catch (ThreadDeath td) {
			// Let it through 
			throw td;
		}
		catch (Throwable t) {
			myEventEmitter.fireEvent(Constants.WARNING_TYPE, new CommitFailedEvent(executionId, entry.getId(), entry.getClass().getName(), t), Constants.SEVERE_LEVEL);
			t.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Rollback all successful <code>TransactionEntry</code> objects stored in this TransactionManager.
	 */
	public boolean rollback() {
		return rollback(TransactionEntry.ANY_TYPE, null);
	}
	
	/**
	 * Rollback all successful <code>TransactionEntry</code> objects of a given type that are marked with a given label.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE.
	 * If <code>label</code> is null, all entries of the specified type are rolled back.
	 */
	public boolean rollback(Class type, String label) {
		boolean ret = true;
		List l = removeEntries(type, label);
		// Rollback entries in reverse order
		for (int i = l.size(); i > 0; --i) {
			TransactionEntry entry = (TransactionEntry) l.get(i-1);
			if (!rollback(entry)) {
				ret = false;
			}
		}
		return ret;
	}
	
	/**
	 * Rollback the last <code>TransactionEntry</code> of a given type.
	 * Valid types are TransactionEntry.APPLICATION_TYPE, TransactionEntry.SUBFLOW_TYPE and TransactionEntry.ANY_TYPE.
	 * The rolled back entry is automatically removed.
	 */
	public boolean rollbackLast(Class type) {
		TransactionEntry lastEntry = removeLastEntry(type);
		if (lastEntry != null) {
			return rollback(lastEntry);
		}
		else {
			// No such entry --> Nothing to rollback
			return true;
		}
	}
	
	private boolean rollback(TransactionEntry entry) {
		try {
			if (entry.isSuccessful()) {
				entry.rollback();
				// FIXME Uncomment when the RollbackEvent will be available
				//myEventEmitter.fireEvent(Constants.TRANSACTION_TYPE, new RollbackEvent(executionId, entry), Constants.FINE_LEVEL);
			}
			return true;
		}
		catch (InterruptedException ie) {
			// Let it through as an Agent.Interrupted error
			throw new Agent.Interrupted();
		}
		catch (Agent.Interrupted ai) {
			// Let it through 
			throw ai;
		}
		catch (ThreadDeath td) {
			// Let it through 
			throw td;
		}
		catch (Throwable t) {
			myEventEmitter.fireEvent(Constants.WARNING_TYPE, new RollbackFailedEvent(executionId, entry.getId(), entry.getClass().getName(), t), Constants.SEVERE_LEVEL);
			t.printStackTrace();
			return false;
		}
	}	
}
