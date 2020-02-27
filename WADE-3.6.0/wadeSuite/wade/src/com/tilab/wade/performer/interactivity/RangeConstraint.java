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
import jade.content.onto.BasicOntology;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class RangeConstraint extends ConstraintImpl {

	private static final long serialVersionUID = 6851395147830189834L;

	private Double max;
	private Double min;
	
	protected RangeConstraint() {
		// Do not remove, used by JAXB
	}
	
	public RangeConstraint(Double min, Double max) {
		this.max = max;
		this.min = min;
	}

	public Double getMin() {
		return min;
	}

	public Double getMax() {
		return max;
	}

	public void validate(Object value) throws ConstraintException {
		// If the value is an AbsPrimitive -> extract the value
		if (value instanceof AbsPrimitive) {
			value = ((AbsPrimitive)value).getObject();
		}
		
		// Try to convert into double
		value = BasicOntology.adjustPrimitiveValue(value, Double.class);
		if (value instanceof Double) {
			double dValue = (Double)value;
			if ((dValue < min && min != null) || (dValue > max && max != null)) {
				throw new ConstraintException("Range violated, value="+dValue+", expected range=["+min+","+max+"]", this);
			}
		}
	}
}
