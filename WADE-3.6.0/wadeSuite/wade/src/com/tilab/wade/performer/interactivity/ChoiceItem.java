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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class ChoiceItem implements Serializable {
	private static final long serialVersionUID = -1153852364282462216L;

	protected Serializable value;
	protected String label;

	protected ChoiceItem() {
		// Do not remove, used by JAXB
	}
	
	public ChoiceItem(Serializable value) {
		this(value, value.toString());
	}
	
	public ChoiceItem(Serializable value, String label) {
		this.value = value;
		this.label = label;
	}

	@Deprecated
	public Serializable getKey() {
		return value;
	}

	@Deprecated
	public void setKey(Serializable key) {
		this.value = key;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}
	
	public String getLabel() {
		if (label != null) {
			return label;
		} else if (value != null) {
			return value.toString();
		} else {
			return null;
		}
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof ChoiceItem)) return false;
		
		ChoiceItem ci = (ChoiceItem)obj;
		return ((ci.getValue()==null && getValue()==null) || (ci.getValue()!=null && ci.getValue().equals(getValue()))) &&
		((ci.getLabel()==null && getLabel()==null) || (ci.getLabel()!=null && ci.getLabel().equals(getLabel()))); 
	}
	
	@Override
	public String toString() {
		return getLabel();
	}
}
