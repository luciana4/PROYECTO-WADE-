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

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class DisplayResource extends InformationElement {

	private static final long serialVersionUID = 8528083810368410330L;

	private String url;
	
	protected DisplayResource() {
		// Do not remove, used by JAXB
	}
	
	public DisplayResource(String id) {
		super(id);
	}
	
	@Override
	protected void doValidate() throws ConstraintException {
	}

	@Override
	public void stamp() {
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	protected Object getCacheValue() {
		return getUrl();
	}

	@Override
	protected void setCacheValue(Object value) {
		setUrl((String)value);
	}
}
