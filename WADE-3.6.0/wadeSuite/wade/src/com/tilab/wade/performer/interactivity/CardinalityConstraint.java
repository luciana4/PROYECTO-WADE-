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

import jade.content.abs.AbsAggregate;

import com.tilab.wade.performer.descriptors.Parameter;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class CardinalityConstraint extends ConstraintImpl {

	private static final long serialVersionUID = -5098432521089967730L;

	public static final int UNBOUNDED = Parameter.UNBOUNDED;
	
	private int max;
	private int min;
	
	protected CardinalityConstraint() {
		// Do not remove, used by JAXB
	}
	
	public CardinalityConstraint(int min, int max) {
		this.max = max;
		this.min = min;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public void validate(Object value) throws ConstraintException {
		Integer count = null;
		if (value instanceof AbsAggregate) {
			count = ((AbsAggregate)value).getCount();
		} else if (value instanceof java.util.Collection) {
			count = ((java.util.Collection)value).size();
		} else if (value instanceof jade.util.leap.Collection) {
			count = ((jade.util.leap.Collection)value).size();
		}
		
		if (count != null &&
			(count < min || (count > max && max != UNBOUNDED))) {
			throw new ConstraintException("Cardinality violated, size="+count+", expected cardinality="+min+","+max, this);
		}
	}
}
