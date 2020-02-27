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

import com.tilab.wade.performer.interactivity.InformationElement;
import com.tilab.wade.performer.interactivity.ConstraintException;

import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(Position.class)
//#ANDROID_EXCLUDE_END
public class Map extends InformationElement {
	private static final long serialVersionUID = 4467886152512749472L;

	//#ANDROID_EXCLUDE_BEGIN
	@XmlElementWrapper(name = "items")
	@XmlElement(name = "item")
	//#ANDROID_EXCLUDE_END
	private List<MapItem> items;
	private Image defaultMarker;

	protected Map() {
	}
	
	public Map(String id, Image defaultMarker) {
		super(id);

		this.defaultMarker = defaultMarker;
		this.items = new ArrayList();
	}

	public List<MapItem> getItems() {
		return items;
	}

	public void setItems(List<MapItem> items) {
		this.items = items;
	}
	
	public Image getDefaultMarker() {
		return defaultMarker;
	}

	public void setDefaultMarker(Image defaultMarker) {
		this.defaultMarker = defaultMarker;
	}

    @Override
    public void doValidate() throws ConstraintException {
    }

	@Override
	public void stamp() {
		items = null;
		defaultMarker = null;
	}

	@Override
	protected Object getCacheValue() {
		//items and defaultMarker are not to be cached since they are set to null in stamp() (and are not modifiable by user)
 		return new Object();
	}

	@Override
	protected void setCacheValue(Object value) {
	}
}
