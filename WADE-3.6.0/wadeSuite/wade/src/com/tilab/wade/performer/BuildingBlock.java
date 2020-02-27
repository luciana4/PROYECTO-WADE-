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

import java.util.List;

import com.tilab.wade.performer.descriptors.Parameter;

import jade.content.abs.AbsObject;
import jade.content.onto.Ontology;
import jade.util.leap.Serializable;

/**
 * Base class for building-blocks executed within workflow activities
 */
public abstract class BuildingBlock implements Serializable, OntologyHolder {

	protected HierarchyNode activity;
	private transient Ontology onto;
	protected int varIndex = 0;

	/**
	 * Get the list of input parameter names  
	 */
	public abstract List<String> getInputParameterNames();

	/**
	 * Get the list of output parameter names  
	 */
	public abstract List<String> getOutputParameterNames();

	/**
	 * Get the input parameter information   
	 */
	public abstract Parameter getInputParameter(String key);

	/**
	 * Get the output parameter information   
	 */
	public abstract Parameter getOutputParameter(String key);
	
	/**
	 * Get input parameter value
	 */
	public abstract Object getInput(String key);

	/**
	 * Get output parameter value
	 */
	public abstract Object getOutput(String key);
	
	/**
	 * Set input parameter value
	 */
	public abstract void setInput(String key, Object value);
	
	/**
	 * Set output parameter value
	 */
	public abstract void setOutput(String key, Object value);
	
	/**
	 * Return true if the input is not filled or binded (parameter is null)  
	 */
	public abstract boolean isInputEmpty(String key);

	/**
	 * Return true if the building block require that all the parameter must be abstract object 
	 */
	public abstract boolean requireAbsParameters();
	
	/**
	 * Create a full filled abstract object with variable on the leafs
	 */
	public abstract AbsObject createAbsTemplate(String key) throws Exception;
	
	/**
	 * Prepare the building block to another invocation
	 */
	public abstract void reset();

	protected abstract Ontology createOntology() throws Exception;

	/**
	 * Create a building block for a specific activity 
	 */
	public BuildingBlock(HierarchyNode activity) {
		this.activity = activity;
	}
	
	/**
	 * Get relative activity 
	 */
	public HierarchyNode getActivity() {
		return activity;
	}
	
	/**
	 * Get (create if not present) the ontology associated 
	 */
	public Ontology getOntology() throws Exception {
		if (onto == null) {
			onto = createOntology(); 
		}
		return onto;
	}
}
