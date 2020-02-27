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

import jade.content.Concept;
import jade.content.abs.AbsObject;
import jade.content.onto.annotations.Slot;
import jade.content.onto.annotations.SuppressSlot;
import jade.content.schema.ObjectSchema;

import com.tilab.wade.performer.Constants;
import com.tilab.wade.utils.OntologyUtils;


/**
   This class represents a parameter of a computational element
   such as a workflow or an application.
   It may be used to represent both a formal parameter (specifying
   the name, type and mode) and an actual parameter (specifying the 
   value).
   @author Giovanni Caire - TILAB
 */
public class Parameter implements Concept, Comparable {

	public static final int UNBOUNDED = -1;
	
	private String name;
	private String documentation;
	private String type;
	private transient Class typeClass;
	private String elementType;
	private transient Class elementTypeClass;
	private ObjectSchema schema;
	private int mode = Constants.NO_MODE;
	private Object value;
	private Object defaultValue;
	private boolean mandatory = true;
	private String regex;
	private Integer cardMin;
	private Integer cardMax;
	private Object[] permittedValues;
	
	
	public Parameter() {
	}
	
	/**
	 * Constructor suitable to create a Parameter object describing a formal parameter 
	 */
	public Parameter(String name, String type, int mode) {
		this.name = name;
		this.type = type;
		this.mode = mode;
	}
	
	/**
	 * Constructor suitable to create a Parameter object describing an actual parameter 
	 */
	public Parameter(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	public Parameter(Object value) {
		this.value = value;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

	/**
	 * Return the <code>Class</code> object for the class of this parameter.
	 * If null use <code>getTypeClass(ClassLoader cl, boolean overwrite)</code>.
	 * This slot is not present in the ontological schema for not to bring 
	 * (via coding/decoding) such information in a context where the class could 
	 * not be present. 
	 * 
	 * @return the <code>Class</code> object for the class of this parameter.
	 */
	@SuppressSlot
	public Class getTypeClass() {
		return typeClass;
	}
	
	public void setTypeClass(Class typeClass) {
		this.typeClass = typeClass;
	}

	/**
	 * Return the <code>Class</code> object for the class of this parameter.
	 * If the type-class is already present in parameter use it, else try to populate it
	 * on the fly loading the class associated to type name.
	 *  
	 * @param overwrite if true force the class reloading.
	 * @param cl class-loader from which the class must be loaded (Must be null).
	 * @return the <code>Class</code> object for the class of this parameter.
	 * @throws Exception if the typeClass is not present and associated class is not loadable.
	 */
	public Class getTypeClass(boolean overwrite, ClassLoader cl) throws Exception {
		if (typeClass == null || overwrite) {
			typeClass = OntologyUtils.getClassByName(type, cl);
		}
		return typeClass;
	}
	
	public void setElementType(String elementType) {
		this.elementType = elementType;
	}
	
	public String getElementType() {
		return elementType;
	}

	/**
	 * Return the <code>Class</code> object for the aggregate element class of this parameter.
	 * If null use <code>getElementTypeClass(ClassLoader cl, boolean overwrite)</code>.
	 * This slot is not present in the ontological schema for not to bring 
	 * (via coding/decoding) such information in a context where the class could 
	 * not be present. 
	 * 
	 * @return the <code>Class</code> object for the aggregate element class of this parameter.
	 */
	@SuppressSlot
	public Class getElementTypeClass() {
		return elementTypeClass;
	}
	
	public void setElementTypeClass(Class elementTypeClass) {
		this.elementTypeClass = elementTypeClass;
	}

	/**
	 * Return the <code>Class</code> object for the aggregate element class of this parameter.
	 * If the type-class is already present in parameter use it, else try to populate it
	 * on the fly loading the class associated to type name.
	 *  
	 * @param overwrite if true force the class reloading.
	 * @param cl class-loader from which the class must be loaded (Must be null).
	 * @return the <code>Class</code> object for the aggregate element class of this parameter.
	 * @throws Exception if the typeClass is not present and associated class is not loadable.
	 */
	public Class getElementTypeClass(boolean overwrite, ClassLoader cl) throws Exception {
		if ((elementTypeClass == null || overwrite) && elementType != null) {
			elementTypeClass = OntologyUtils.getClassByName(elementType, cl);
		}
		return elementTypeClass;
	}
	
	@Slot(manageAsSerializable=true)
	public ObjectSchema getSchema() {
		return schema;
	}

	public void setSchema(ObjectSchema schema) {
		this.schema = schema;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public int getMode() {
		return mode;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}
	
	/**
	 * Warning! 
	 * This method has an internal use only.
	 * Its uses is prohibited.
	 * To get the value must use the getValue() method.
	 * 	
	 * @see com.tilab.wade.performer.descriptors.Wrapper
	 */
	public Object getWrappedValue() {
		if (value instanceof AbsObject) {
			return new Wrapper(value);
		} else {
			return value;
		}
	}
	
	/**
	 * This method is for internal use only.
	 * To set the value of this Parameter use the <code>setValue()</code> method.
	 * 	
	 * @see com.tilab.wade.performer.descriptors.Wrapper
	 */
	public void setWrappedValue(Object obj) {
		if (obj instanceof Wrapper) {
			value = ((Wrapper) obj).getValue();
		} else {
			value = obj;
		}
	}
	
	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	// Is not possible call this method isMandatory() because 
	// Parameter schema is created with old style ontology
	public boolean getMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public Integer getCardMin() {
		return cardMin;
	}

	public void setCardMin(int cardMin) {
		this.cardMin = Integer.valueOf(cardMin);
	}

	public Integer getCardMax() {
		return cardMax;
	}

	public void setCardMax(int cardMax) {
		this.cardMax = Integer.valueOf(cardMax);
	}

	public Object[] getPermittedValues() {
		return permittedValues;
	}

	public void setPermittedValues(Object[] permittedValues) {
		this.permittedValues = permittedValues;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	/**
	 * Clone the current parameter only with name and value 
	 */
	public Parameter toActual() {
		return new Parameter(name, value);
	}
	
	public String toString() {
		return "Parameter "+(name != null ? ("name="+name+" ") : "")+(type != null ? ("type="+type+" ") : "")+"mode="+mode+" "+(value != null ? ("value="+value+" ") : "");
	}

	public int compareTo(Object obj) {
		return name.compareTo(((Parameter)obj).getName());
	}
}
