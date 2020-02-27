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
import jade.core.behaviours.Behaviour;

/**
   This interface provides a common view of all nodes in the 
   workflow behaviours hierarchy. This is necessary since (as Java 
   does not support multiple inheritance) the classes in the
   workflow behaviours hierarchy do not descend from a unique
   common ancestor.
   @author Giovanni Caire - TILAB
 */
public interface HierarchyNode {
	WorkflowEngineAgent getAgent();
	
	Behaviour root();
	WorkflowBehaviour getOwner();
	
	DataStore getDataStore();
	
	void setDataStore(DataStore ds);
	
	String getBehaviourName();

	BindingManager getBindingManager();
	BuildingBlock getBuildingBlock(String id);
	
	OutgoingTransitions getOutgoingTransitions();
	boolean hasJADEDefaultTransition();
	
	/**
	 * Indicate whether the status of the enclosing workflow must be saved
	 * after the execution this node. The save operation occurs only 
	 * in case of long-running workflows.
	 * @return true if the status of the enclosing workflow must be saved
	 * after the execution this node; false otherwise
	 */
	boolean requireSave();
	void setRequireSave(boolean requireSave);
	
	/**
	   Mark this HierarchyNode as executed. This is used in conjunction
	   with reinit() to properly reset nodes that has already been visited
	   before executing them again.
	 */
	void mark();
	
	/**
	   Reset this HyerarchyNode only if it has already been executed.
	 */
	void reinit();
	
	////////////////////////////////////////////
	// Exception handling methods
	////////////////////////////////////////////
	/**
	   @return <code>true</code> if the activity implemented by this
	   <code>HierarchyNode</code> was marked in the wrokflow process 
	   definition as an error activity by means of the ERROR extended
	   attribute.
	 */
	boolean isError();
	
	
	/**
	   Mark the activity implemented by this HierarchyNode as an 
	   error activity.
	 */
	void setError(boolean b);
	
	/**
	   Retrieve the last exception that occurred in the local node or
	   in one of its childern
	 */
	Throwable getLastException();
	
	/**
	   Handle an exception that occurred in the local node or in one of 
	   its children.
	 */ 
	void handleException(Throwable t);
	
	/**
	   Propagate an exception to the parent node
	 */
	void propagateException(Throwable t);
	
	/**
	 * Check if the WorkflowBehaviour this node belongs to was interrupted
	 */
	boolean isInterrupted();
	
	/**
	 * Set the interrupted state of theis node
	 */
	void setInterrupted();
}
		