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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Calendar.class, 
	         DisplayResource.class, 
	         ExclusiveChoice.class,
	         Image.class,
	         Label.class, 
	         com.tilab.wade.performer.interactivity.Map.class, 
	         MultipleChoice.class, 
	         PictureCamera.class,
	         TextBox.class })
//#ANDROID_EXCLUDE_END
public abstract class InformationElement extends Component {

	protected abstract Object getCacheValue();
	protected abstract void setCacheValue(Object value);

	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint")
	//#ANDROID_EXCLUDE_END
	protected ArrayList<Constraint> constraints = new ArrayList<Constraint>();

	protected InformationElement() {
		// Do not remove, used by JAXB
	}

	public InformationElement(String id) {
		this.id = id;
	}
	
	@Override
	final void fillCacheData(Map<String, Object> cacheData) {
		if (getId() != null) {
			Object cacheValue = getCacheValue();
			cacheData.put(getId(), cacheValue);
		}
	}

	@Override
	final void setCacheData(Map<String, Object> cacheData) {
		if (getId() != null) {
			try {
				if (cacheData.containsKey(getId())) {
					setCacheValue(cacheData.get(getId()));
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Get constraints for the root element
	 */
	public List<Constraint> getConstraints() {
		return constraints;
	}

	/**
	 * Add constraints to root element
	 */
	public void addConstraint(Constraint constraint) {
		addConstraint(constraint, false);
	}
	
	public void addConstraint(Constraint constraint, boolean override) {
		if (!containConstraint(constraint.getClass())) {
			constraints.add(constraint);
		} else {
			if (override) {
				removeConstraint(constraint.getClass());
				constraints.add(constraint);
			}
		}
	}
	
	/**
	 * Check if a constraint is already present in root element  
	 */
	public boolean containConstraint(Class constraintClass) {
		for (Constraint constraint : constraints) {
			if (constraint.getClass() == constraintClass) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a constraint
	 */
	public void removeConstraint(Class constraintClass) {
		Iterator<Constraint> it = constraints.iterator();
		while (it.hasNext()) {
			Constraint constraint = it.next();
			if (constraint.getClass() == constraintClass) {
				it.remove();
			}
		}
	}
	
	protected void validateConstraints(Object value) throws ConstraintException {
		for (Constraint constraint : constraints) {
			constraint.validate(value);
		}
	}
}
