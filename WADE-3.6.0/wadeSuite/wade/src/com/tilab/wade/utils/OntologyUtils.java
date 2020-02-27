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
package com.tilab.wade.utils;

import java.lang.reflect.Array;
import java.util.Map;

import jade.content.abs.AbsObject;
import jade.content.onto.AggregateHelper;
import jade.content.onto.BasicOntology;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.schema.ObjectSchema;
import jade.content.schema.TermSchema;

import com.tilab.wade.performer.descriptors.Parameter;

public class OntologyUtils {

	/**
 	 * Convert in schema the object value passed 
	 * and add it to passed bean-ontology.
	 * The ClassLoader must be null.
	 * 
	 * @param onto bean-ontology where to add new schema.
	 * @param value actualParam the actual parameter.
	 * @throws Exception
	 */
	public static void addObjectToOntology(BeanOntology onto, Object value) throws Exception {
		if (value != null && !(value instanceof AbsObject)) {
			Class valueClass = value.getClass();
			addClassToOntology(onto, valueClass);
			
			// In case of aggregate get the first element
			
			// Java array
			Object elementObj = null;
			if (valueClass.isArray() && Array.getLength(value) > 0) {
				elementObj = Array.get(value, 0);
			}
			
			// Java collection
			else if (java.util.List.class.isAssignableFrom(valueClass)) {
				java.util.List javaList = (java.util.List)value;
				if (javaList.size() > 0) {
					elementObj = javaList.get(0);
				}
			}
			
			// Jade collection
			else if (jade.util.leap.List.class.isAssignableFrom(valueClass)) {
				jade.util.leap.List jadeList = (jade.util.leap.List)value;
				if (jadeList.size() > 0) {
					elementObj = jadeList.get(0);
				}
			}
			
			// Add to ontology element class
			if (elementObj != null) {
				addClassToOntology(onto, elementObj.getClass());
			}
		}
	}

	/**
 	 * Convert in schema the object value representing the passed actual parameter 
	 * and add it to passed bean-ontology.
	 * The ClassLoader must be null.
	 * 
	 * @param onto bean-ontology where to add new schema.
	 * @param actualParam the actual parameter.
	 * @throws Exception
	 */
	public static void addActualParameterToOntology(BeanOntology onto, Parameter actualParam) throws Exception {
		Object value = actualParam.getValue();
		addObjectToOntology(onto, value);
	}
	
	/**
	 * Convert in schema the class representing the passed formal parameter 
	 * and add it to passed bean-ontology.
	 * The ClassLoader must be null.
	 * 
	 * @param onto bean-ontology where to add new schema.
	 * @param formalParam the formal parameter.
	 * @param cl class loader from which the class must be loaded.
	 * @throws Exception
	 */
	public static void addFormalParameterToOntology(BeanOntology onto, Parameter formalParam, ClassLoader cl) throws Exception {

		// Add parameter class
		Class paramClass = formalParam.getTypeClass(false, cl);
		addClassToOntology(onto, paramClass);


		// Add aggregate element parameter class
		Class elementParamClass = formalParam.getElementTypeClass(false, cl);
		if (elementParamClass != null) {
			addClassToOntology(onto, elementParamClass);
		}
	}
	
	/**
	 * Convert in schema the passed class and add it to passed bean-ontology.
	 * 
	 * @param onto bean-ontology where to add new schema.
	 * @param clazz class to add.
	 * @throws Exception
	 */
	public static void addClassToOntology(BeanOntology onto, Class clazz) throws Exception {

		// Skip java/jade collections and arrays (don't add class to onto)
		if ((clazz.isArray() && clazz != byte[].class) ||
			java.util.List.class.isAssignableFrom(clazz) ||
			jade.util.leap.List.class.isAssignableFrom(clazz)
			) {
			return;
		}
		
		// Check if schema is already present
		ObjectSchema schema = onto.getSchema(clazz);
		if (schema == null) {

			// Add new class
			onto.add(clazz);
		}
	}
	
	/**
	 * Get the schema of parameter
	 * 
	 * @param param Parameter
	 * @param onto Ontology
	 * @return Schema of parameter
	 * @throws Exception
	 */
	public static ObjectSchema getParameterSchema(Parameter param, Ontology onto) throws Exception {
		return getParameterSchema(param, onto, null);
	}
	
	/**
	 * Get the schema of parameter
	 * 
	 * @param param Parameter
	 * @param onto Ontology
	 * @param cl ClassLoader
	 * @return Schema of parameter
	 * @throws Exception
	 */
	public static ObjectSchema getParameterSchema(Parameter param, Ontology onto, ClassLoader cl) throws Exception {
		ObjectSchema paramSchema = null;
		
		// If the schema is already present in parameter --> use it
		if (param.getSchema() != null) {
			paramSchema = param.getSchema(); 
		} 
		// Get schema fom ontology
		else {
			//
			Class paramClass = param.getTypeClass(false, cl);
			
			// Try to get associated schema from ontology
			paramSchema = onto.getSchema(paramClass);
			
			// If no schema found is as aggregate 
			if (paramSchema == null) {
				Class paramElementClass = param.getElementTypeClass(false, cl);
				ObjectSchema paramElementSchema = null;
				if (paramElementClass != null) {
					paramElementSchema = onto.getSchema(paramElementClass);
				}
				paramSchema = AggregateHelper.getSchema(paramClass, (TermSchema)paramElementSchema);
			}
		}
		
		return paramSchema;
	}
	
	/**
	 * Returns the <code>Class</code> object associated with the class or
     * interface with the given string name.
     * It works also for the primitives type. 
     * 
	 * @param className the fully qualified name of the desired class.
	 * @param cl class loader from which the class must be loaded .
	 * @return the <code>Class</code> object for the class with the specified name.
	 * @throws ClassNotFoundException if the class cannot be loaded.
	 */
	public static final Class getClassByName(String className, ClassLoader cl) throws Exception {
		if (className == null) {
			throw new Exception("Class-name not specified");
		}
		
		Class clazz = getPrimitiveClassFromName(className);
		if (clazz == null) {
			if (cl == null) {
				clazz = Class.forName(className);
			} else {
				clazz = Class.forName(className, true, cl);
			}
		}
		return clazz;
	}

    private static Class getPrimitiveClassFromName(String primitive) {
        if (primitive.equals("int"))
            return int.class;
        else if (primitive.equals("short"))
            return short.class;
        else if (primitive.equals("boolean"))
            return boolean.class;
        else if (primitive.equals("byte"))
            return byte.class;
        else if (primitive.equals("long"))
            return long.class;
        else if (primitive.equals("double"))
            return double.class;
        else if (primitive.equals("float"))
            return float.class;
        else if (primitive.equals("char"))
            return char.class;
        
        return null;
    }
    
	public static long getLong(Map props, String key, long defaultVal) {
		long val = defaultVal;
		try {
			Object tmp = props.get(key);
			if (tmp != null) {
				val = ((Long) BasicOntology.adjustPrimitiveValue(tmp, Long.class)).longValue();
			}
		}
		catch (Exception e) {
			// Ignore and keep default
		}
		return val;
	}

	public static int getInt(Map props, String key, int defaultVal) {
		int val = defaultVal;
		try {
			Object tmp = props.get(key);
			if (tmp != null) {
				val = ((Integer) BasicOntology.adjustPrimitiveValue(tmp, Integer.class)).intValue();
			}
		}
		catch (Exception e) {
			// Ignore and keep default
		}
		return val;
	}	

	public static boolean getBoolean(Map props, String key, boolean defaultVal) {
		boolean val = defaultVal;
		try {
			Object tmp = props.get(key);
			if (tmp != null) {
				val = ((Boolean) BasicOntology.adjustPrimitiveValue(tmp, Boolean.class)).booleanValue();
			}
		}
		catch (Exception e) {
			// Ignore and keep default
		}
		return val;
	}	

	public static String getString(Map props, String key, String defaultVal) {
		String val = defaultVal;
		try {
			Object tmp = props.get(key);
			if (tmp != null) {
				val = (String) BasicOntology.adjustPrimitiveValue(tmp, String.class);
			}
		}
		catch (Exception e) {
			// Ignore and keep default
		}
		return val;
	}	
}
