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
import jade.util.leap.ArrayList;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tilab.wade.performer.descriptors.Parameter;

/**
   Base class for all applications that can be executed in a Tool activity of a workflow.
   @see ToolExecutionBehaviour
   @author Giovanni Caire - TILAB
 */
public abstract class BaseApplication extends Application implements Serializable {
	
	private Set<String> assignedParameters = new HashSet<String>();
	private transient Map<String, Field> cachedFields = new HashMap<String, Field>();
	
	private Set<String> managedFieldNames;

	
	/** 
	 * Construct an Application with a default name
	 */
	public BaseApplication() {
		this(null);
	}

	/** 
	 * Construct an Application with a given name
	 */
	public BaseApplication(String n) {
		super(n, null);
		fillFormalParameters();
	}

	protected void fillFormalParameters() {
		formalParams  = new ArrayList();
		EngineHelper.fillFormalParameters(this, BaseApplication.class, formalParams);
	}
	
	/**
	 * Fill the actual value of an input parameter of this application
	 */
	public void fill(String key, Object value) {
		setFieldValue(key, value);
		assignedParameters.add(key);
	}

	/**
	 * Retrieve the actual value of an output parameter of this application 
	 * @param name The name of the parameter whose value is retrieved
	 * @return The actual value of output parameter <code>name</code>
	 */
	public Object extract(String name) {
		return getFieldValue(name);
	}

	final void setFieldValue(String key, Object value) {
		EngineHelper.setFieldValue(key, value, this, cachedFields);
	}

	final Object getFieldValue(String key) {
		return EngineHelper.getFieldValue(key, this, cachedFields);
	}

	/**
	 * This method is used internally by the engine. It must not be used by developers
	 */
	protected void checkParameters(){
		if (formalParams != null){
			try {
				for (int i=0; i<formalParams.size(); i++){
					Parameter param = (Parameter)formalParams.get(i);
					if (param.getMode() == Constants.IN_MODE || param.getMode() == Constants.INOUT_MODE) {
						if (param.getMandatory() && !assignedParameters.contains(param.getName())) {
							// INPUT parameter not assigned!
							throw new IllegalArgumentException("Application "+getClass().getName()+": parameter "+param.getName()+" not assigned in input");
						}
					}
				}
			}
			finally {
				assignedParameters.clear();
			}
		}
	}

	/**
	 * This method is called internally by the framework. It must not be used by developers.
	 */
	public void setDataStore(DataStore ds) {
		if (managedFieldNames == null) {
			managedFieldNames = EngineHelper.initManagedFields(this, BaseApplication.class);
		}
		for (String fieldName : managedFieldNames) {
			setFieldValue(fieldName, ds.get(fieldName));
		}
	}
	
	/**
	 * This method is called internally by the framework. It must not be used by developers.
	 */
	public DataStore getDataStore() {
		if (managedFieldNames == null) {
			managedFieldNames = EngineHelper.initManagedFields(this, BaseApplication.class);
		}
		DataStore ds = new DataStore(managedFieldNames.size());
		for (String fieldName : managedFieldNames) {
			ds.put(fieldName, getFieldValue(fieldName));
		}
		return ds;
	}

}
