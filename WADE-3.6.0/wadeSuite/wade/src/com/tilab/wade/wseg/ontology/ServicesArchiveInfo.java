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
package com.tilab.wade.wseg.ontology;

import java.util.List;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class ServicesArchiveInfo implements Concept {

	private static final long serialVersionUID = 6909032710467481981L;

	private String servicesArchiveName;
	private String purpose;
	private List<ServiceInfo> services;
	
	public ServicesArchiveInfo() {
	}
	
	@Slot(mandatory=true)
	public String getServicesArchiveName() {
		return servicesArchiveName;
	}

	public void setServicesArchiveName(String servicesArchiveName) {
		this.servicesArchiveName = servicesArchiveName;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setServices(List<ServiceInfo> services) {
		this.services = services;
	}

	public List<ServiceInfo> getServices() {
		return services;
	}
	
	@Override
	public String toString() {
		return "ServicesArchiveInfo [servicesArchiveName=" + servicesArchiveName + "]";
	}
}
