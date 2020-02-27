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
package com.tilab.wade.commons.ontology;

import jade.content.Concept;
import jade.util.leap.List;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 21-giu-2006
 * Time: 18.09.36
 * To change this template use File | Settings | File Templates.
 */
public class Attribute implements Concept {
	public static final int NO_TYPE = 0;
	public static final int STRING_TYPE = 1;
	public static final int INTEGER_TYPE = 2;
	public static final int BOOLEAN_TYPE = 3;
	public static final int DATE_TYPE = 4;
	public static final int FLOAT_TYPE = 5;
	public static final int SERIALIZABLE_TYPE = 6;

	private static final String[] typeNames = new String[]{
		"NONE",
		"STRING",
		"INTEGER",
		"BOOLEAN",
		"DATE",
		"FLOAT",
		"SERIALIZABLE"
	};

	private String id;
	private String name;
	private Object value;
	private boolean readOnly = true;
	private Object defaultValue;
	private List permittedValues;
	private int type = NO_TYPE;

	
	public static String getTypeName(int type) {
		return typeNames[type];
	}

	public static Object decode(String valueStr, int type) throws FormatException {
		try {
			switch (type) {
			case Attribute.STRING_TYPE:
				return valueStr;
			case Attribute.INTEGER_TYPE:
				return Long.parseLong(valueStr);
			case Attribute.BOOLEAN_TYPE:
				return Boolean.parseBoolean(valueStr);
			case Attribute.FLOAT_TYPE:
				return Double.parseDouble(valueStr);
			default:
				// FIXME: Handle DATE_TYPE
				throw new FormatException("Can't handle values of type "+Attribute.getTypeName(type));
			} 
		}
		catch (FormatException fe) {
			throw fe;
		}
		catch (Exception e) {
			throw new FormatException("Value "+valueStr+" cannot be converted into a "+Attribute.getTypeName(type));
		}
	}

	
	
	public Attribute() {
	}

	public Attribute(String id, Object value) {
		this.id = id;
		this.value = value;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return (id != null ? id : name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return (name != null ? name : id);
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean getReadOnly() {
		return readOnly;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setPermittedValues(List permittedValues) {
		this.permittedValues = permittedValues;
	}

	public List getPermittedValues() {
		return permittedValues;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
	

	// Inner class FormatException
	public static class FormatException extends Exception {
		public FormatException(String msg) {
			super(msg);
		}
	}
}
