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
package com.tilab.wade.performer.interactivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.onto.BasicOntology;
import jade.content.schema.TermSchema;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class TypeConstraint extends ConstraintImpl {

	private static final long serialVersionUID = -1589645067599127330L;
	
	private static Map<String,Class> typesMap = new HashMap<String,Class>();
	static {
		typesMap.put(BasicOntology.STRING, String.class);
		typesMap.put(BasicOntology.FLOAT, Double.class);
		typesMap.put(BasicOntology.INTEGER, Long.class);
		typesMap.put(BasicOntology.BOOLEAN, Boolean.class);
		typesMap.put(BasicOntology.DATE, Date.class);
		typesMap.put(BasicOntology.BYTE_SEQUENCE, byte[].class);
	}
	
	private String type;

	protected TypeConstraint() {
		// Do not remove, used by JAXB
	}
	
	public TypeConstraint(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	public void validate(Object value) throws ConstraintException {
		
		// Nel caso di TypeConstraint su aggregato significa che gli elementi dell'aggregato 
		// sono primitivi -> scorro tutti gli elementi e verifico il tipo 
		if (value instanceof AbsAggregate) {
			AbsAggregate absAggregate = (AbsAggregate)value;
			for (String aggSlotName : absAggregate.getNames()) {
				validate(absAggregate.getAbsObject(aggSlotName));	
			}
			return;
		}
		
		
		// Se il type constraint è di tipo Term (Object) --> qualsiasi cosa va bene
		if (TermSchema.BASE_NAME.equalsIgnoreCase(type)) {
			return;
		}
		
		// If value is a primitive check the type 
		if (value instanceof AbsPrimitive) {
			AbsPrimitive absPrimitive = (AbsPrimitive)value;
			if (!absPrimitive.getTypeName().equalsIgnoreCase(type)) {
				throw new ConstraintException("Type violated, value type="+absPrimitive.getTypeName()+", expected type="+type, this);
			}
			return;
		}
		
		// Check generic object (not AbsXXX)
		if (value != null && !AbsObject.class.isAssignableFrom(value.getClass())) {
			Class clazz = typesMap.get(type);
			Object adjustedvalue = BasicOntology.adjustPrimitiveValue(value, clazz);
			if (adjustedvalue.getClass() != clazz) {
				throw new ConstraintException("Type violated, value class="+clazz.getName()+", expected type="+type, this);
			}
			return;
		}
	}
}
