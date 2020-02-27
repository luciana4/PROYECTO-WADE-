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

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class Image extends InformationElement {

	private static final long serialVersionUID = -7876999188459083649L;

	private byte[] data;
	private String url;
	private String description;
	
	protected Image() {
		// Do not remove, used by JAXB
	}

	public Image(String url) {
		this(null, url);
	}

	public Image(byte[] data) {
		this(null, data); 
	}
	
	public Image(String id, String url) {
		super(id);
		this.url = url;
	}

	public Image(String id, byte[] data) {
		super(id);
		this.data = data; 
	}

	public byte[] getImageData() {
		return data;
	}

	public void setImageData(byte[] data) {
		this.data = data;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	protected void doValidate() throws ConstraintException {
	}
	
	@Override
	public void stamp() {
		data = null;
	}

	@Override
	protected Object getCacheValue() {
		return new Object[] {data, url, description};
	}

	@Override
	protected void setCacheValue(Object value) {
		Object[] cache = (Object[])value;
		data = (byte[])cache[0];
		url = (String)cache[1];
		description = (String)cache[2];
	}
}
