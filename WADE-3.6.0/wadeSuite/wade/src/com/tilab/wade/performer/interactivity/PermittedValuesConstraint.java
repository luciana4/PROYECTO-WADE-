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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import jade.content.abs.AbsPrimitive;
import jade.content.abs.AbsVariable;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class PermittedValuesConstraint extends ConstraintImpl {

	private static final long serialVersionUID = -4955595263385438987L;
	
	private Object[] permittedValues;

	protected PermittedValuesConstraint() {
		// Do not remove, used by JAXB
	}
	
	public PermittedValuesConstraint(Object[] permittedValues) {
		this.permittedValues = permittedValues; 
	}
	
	public Object[] getPermittedValues() {
		return permittedValues;
	}

	public String getPermittedValuesString() {
		StringBuilder sb = new StringBuilder();
		if (permittedValues != null) {
			for (int i=0; i<permittedValues.length; i++) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(permittedValues[i]);
			}
		}
		return sb.toString();
	}
	
	public void validate(Object value) throws ConstraintException {
		if (permittedValues == null) {
			return;
		}
		
		Object oValue;
		if (value instanceof AbsPrimitive) {
			oValue = ((AbsPrimitive)value).getObject();
		} else if (value instanceof AbsVariable) {
			oValue = null;
		} else {
			oValue = value;
		}
		
		if (oValue != null) {
			for (int i=0; i<permittedValues.length; i++) {
				if (oValue.equals(permittedValues[i])) {
					return;
				}
			}
			throw new ConstraintException("Permitted values violated, value="+oValue+", expected values="+getPermittedValuesString(), this);
		}
	}
}
