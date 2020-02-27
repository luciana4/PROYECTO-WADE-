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
package com.tilab.wade.cfa.beans;

import jade.content.Concept;

public class VersionsInfo implements Concept {

	private static final long serialVersionUID = -5291909067175407242L;
	
	private String jadeVersion;
	private String jadeRevision;
	private String jadeDate;
	private String wadeVersion;
	private String wadeRevision;
	private String wadeDate;
	private String projectVersion;
	private String projectRevision;
	private String projectDate;
	private String projectOriginalRevision;
	
	
	public VersionsInfo() {
		super();
	}

	public String getJadeVersion() {
		return jadeVersion;
	}

	public void setJadeVersion(String jadeVersion) {
		this.jadeVersion = jadeVersion;
	}

	public String getJadeRevision() {
		return jadeRevision;
	}

	public void setJadeRevision(String jadeRevision) {
		this.jadeRevision = jadeRevision;
	}

	public String getJadeDate() {
		return jadeDate;
	}

	public void setJadeDate(String jadeDate) {
		this.jadeDate = jadeDate;
	}

	public String getWadeVersion() {
		return wadeVersion;
	}

	public void setWadeVersion(String wadeVersion) {
		this.wadeVersion = wadeVersion;
	}

	public String getWadeRevision() {
		return wadeRevision;
	}

	public void setWadeRevision(String wadeRevision) {
		this.wadeRevision = wadeRevision;
	}

	public String getWadeDate() {
		return wadeDate;
	}

	public void setWadeDate(String wadeDate) {
		this.wadeDate = wadeDate;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public String getProjectRevision() {
		return projectRevision;
	}

	public void setProjectRevision(String projectRevision) {
		this.projectRevision = projectRevision;
	}
	
	public String getProjectDate() {
		return projectDate;
	}

	public void setProjectDate(String projectDate) {
		this.projectDate = projectDate;
	}


	public String getProjectOriginalRevision() {
		return projectOriginalRevision;
	}

	public void setProjectOriginalRevision(String projectOriginalRevision) {
		this.projectOriginalRevision = projectOriginalRevision;
	}

	@Override
	public String toString() {
		return "VersionsInfo [jadeVersion=" + jadeVersion + ", jadeRevision=" + jadeRevision + ", jadeDate=" + jadeDate + ", wadeVersion="
				+ wadeVersion + ", wadeRevision=" + wadeRevision + ", wadeDate=" + wadeDate + ", projectVersion=" + projectVersion
				+ ", projectRevision=" + projectRevision + ", projectDate=" + projectDate + ", projectOriginalRevision=" + projectOriginalRevision
				+ "]";
	}
}