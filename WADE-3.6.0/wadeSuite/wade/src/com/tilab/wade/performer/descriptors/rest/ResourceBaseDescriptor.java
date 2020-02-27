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

public class ResourceBaseDescriptor implements Serializable {

	private static final long serialVersionUID = 6874598825428216169L;
	
	private String baseUri;
	private int resourceCnt;
	private Map<String, ResourceDescriptor> resources = new HashMap<String, ResourceDescriptor>();  
	
	public ResourceBaseDescriptor(String baseUri) {
		this.baseUri = baseUri;
	}

	public String getBaseURI() {
		return baseUri;
	}
	
	public Set<String> getResourceIds() {
		return resources.keySet();
	}
	
	public ResourceDescriptor getResource(String id) {
		return resources.get(id);
	}
	
	public void addResource(ResourceDescriptor resource) {
		String id = resource.getId();
		if (id == null || id.isEmpty()) {
			String path = resource.getPath();
			if (path != null && !path.isEmpty()) {
				resource.setId(path);
			} else {
				id = "Resource"+getResourceCounter();
				resource.setId(id);
			}
		}
		
		resources.put(resource.getId(), resource);
	}
	
	private synchronized int getResourceCounter() {
		return resourceCnt++;
	}
}
