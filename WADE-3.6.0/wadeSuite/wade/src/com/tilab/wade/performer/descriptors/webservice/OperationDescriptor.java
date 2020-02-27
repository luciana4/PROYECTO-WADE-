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

import java.io.Serializable;

import com.tilab.wade.performer.descriptors.Parameter;

import jade.util.leap.ArrayList;
import jade.util.leap.List;

public class OperationDescriptor implements Serializable {

	List formalParams = new ArrayList();
	List formalHeaders = new ArrayList();
	String returnValueType;
	String returnValueName;

	public OperationDescriptor() {
	}

	@Deprecated
	public OperationDescriptor(List formalParams, String returnValueType) {
		this.formalParams = formalParams;
		this.returnValueType = returnValueType;
	}
	
	public List getFormalParams() {
		return formalParams;
	}
	
	public void setFormalParams(List formalParams) {
		this.formalParams = formalParams;
	}

	public void addFormalParam(Parameter param) {
		formalParams.add(param);
	}

	public List getFormalHeaders() {
		return formalHeaders;
	}
	
	public void setFormalHeaders(List formalHeaders) {
		this.formalHeaders = formalHeaders;
	}

	public void addFormalHeader(Header header) {
		formalHeaders.add(header);
	}
	
	public String getReturnValueType() {
		return returnValueType;
	}
	
	@Deprecated
	public void setReturnValueType(String returnValueType) {
		this.returnValueType = returnValueType;
	}

	public String getReturnValueName() {
		return returnValueName;
	}
	
	public void setReturnValueName(String returnValueName) {
		this.returnValueName = returnValueName;
	}
}
