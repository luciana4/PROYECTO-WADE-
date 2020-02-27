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
package com.tilab.wade.performer.descriptors.rest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResourceDescriptor implements Serializable {

	private static final long serialVersionUID = -5824068274786060353L;

	private String id;
	private String documentation;
	private String path;
	private int methodCnt;
	private Map<String, MethodDescriptor> methods = new HashMap<String, MethodDescriptor>();


	public ResourceDescriptor() {
		this(null, null);
	}
	
	public ResourceDescriptor(String path) {
		this(null, path);
	}
	
	public ResourceDescriptor(String id, String path) {
		this.id = id;
		this.path = path;
		this.methodCnt = 1;
	}

	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}
	
	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public String getPath() {
		return path;
	}
	
	public Set<String> getMethodIds() {
		return methods.keySet();
	}
	
	public MethodDescriptor getMethod(String id) {
		return methods.get(id);
	}
	
	public void addMethod(MethodDescriptor method) {
		String id = method.getId();
		if (id == null || id.isEmpty()) {
			id = method.getType().name()+getMethodCounter();
			method.setId(id);
		}
		
		methods.put(method.getId(), method);
	}
	
	private synchronized int getMethodCounter() {
		return methodCnt++;
	}
}
