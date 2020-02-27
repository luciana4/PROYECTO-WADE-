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
public class RegexConstraint extends ConstraintImpl {

	private static final long serialVersionUID = 4563004233504522707L;

	private String regex; 

	protected RegexConstraint() {
		// Do not remove, used by JAXB
	}
	
	public RegexConstraint(String regex) {
		this.regex = regex;
	}

	public String getRegex() {
		return regex;
	}
	
	public void validate(Object value) throws ConstraintException {
		if (regex == null) {
			return;
		}
		
		String sValue = null;
		if (value instanceof AbsPrimitive) {
			AbsPrimitive absPrimitive = (AbsPrimitive)value;
			if (absPrimitive.getTypeName().equals(BasicOntology.STRING)) {
				sValue = absPrimitive.getString();
			}
		} else if (value instanceof String) {
			sValue = (String)value;
		}
		
		if (sValue != null && !sValue.matches(regex)) {
			throw new ConstraintException("Regex violated, value="+sValue+", regex="+regex, this);
		}
	}	
}
