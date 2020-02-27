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
package com.tilab.wade.performer.descriptors.webservice;

import com.tilab.wade.performer.descriptors.Parameter;

public class Header extends Parameter {
	
	public static final int EXPLICIT_HEADER = -1;
	
	private int signaturePosition = EXPLICIT_HEADER;
	private String namespace;
	private String actor = null;
	private boolean mustUnderstand = false;
	private boolean relay = false;
	

	public Header() {
		super();
	}
	
	/**
	 * Constructor suitable to create a Header object describing a formal header 
	 */
	public Header(String name, String namespace, String type, int mode, int signaturePosition) {
		super(name, type, mode);
		this.namespace = namespace;
		this.signaturePosition = signaturePosition;
	}
	
	/**
	 * Constructor suitable to create a Header object describing an actual header 
	 */
	public Header(String name, Object value) {
		super(name, value);
	}
	
	public Header(Object value) {
		super(value);
	}
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public int getSignaturePosition() {
		return signaturePosition;
	}

	public void setSignaturePosition(int signaturePosition) {
		this.signaturePosition = signaturePosition;
	}
	
	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public boolean isMustUnderstand() {
		return mustUnderstand;
	}

	public void setMustUnderstand(boolean mustUnderstand) {
		this.mustUnderstand = mustUnderstand;
	}

	public boolean isRelay() {
		return relay;
	}

	public void setRelay(boolean relay) {
		this.relay = relay;
	}
	
	public String toString() {
		return "Header "+(getName() != null ? ("name="+getName()+" ") : "")+(namespace != null ? ("namespace="+namespace+" ") : "")+(getType() != null ? ("type="+getType()+" ") : "")+"mode="+getMode()+" "+(getValue() != null ? ("value="+getValue()+" ") : "")+("signaturePosition="+signaturePosition);
	}
}
