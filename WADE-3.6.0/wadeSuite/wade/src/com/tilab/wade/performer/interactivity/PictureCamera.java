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

import com.tilab.wade.performer.interactivity.ConstraintException;
import com.tilab.wade.performer.interactivity.InformationElement;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class PictureCamera extends InformationElement {
	private static final long serialVersionUID = 5273124022717109809L;

	protected byte[] data;
	
	protected PictureCamera() {
		// Do not remove, used by JAXB
	}
	
	public PictureCamera(String id) {
		super(id);
	}
	
	public byte[] getPictureData() {
		return data;
	}

	public void setPictureData(byte[] data) {
		this.data = data;
	}
	
	
	@Override
	public void stamp() {
	}

	@Override
	protected void doValidate() throws ConstraintException {
		validateConstraints(data);
	}

	@Override
	protected Object getCacheValue() {
		return getPictureData();
	}

	@Override
	protected void setCacheValue(Object value) {
		setPictureData((byte[])value);
	}
}
