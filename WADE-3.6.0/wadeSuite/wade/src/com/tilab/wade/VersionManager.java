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
 
package com.tilab.wade;

import jade.util.Logger;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

public class VersionManager {
	
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	
	private static final String GROUP = "Wade Informations";
	private static final String WCVER = "Specification-Version";
	private static final String WCREV = "SVN-Revision";
	private static final String WCDATE = "SVN-Date";
	
	private Attributes attributes;
	
	public VersionManager() {
		try {
			Class clazz = this.getClass();
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			
			// Check if class is into jar 
			if (!classPath.startsWith("jar")) {
				myLogger.log(Level.WARNING, "VersionManager not from jar -> no version information available");
			  return;
			}

			// Get manifest attributes 
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
			InputStream is = new URL(manifestPath).openStream();
			Manifest manifest = new Manifest(is);			
			attributes = manifest.getAttributes(GROUP);
			is.close();
		}
		catch (Exception e) {
			myLogger.log(Level.WARNING, "Error retrieving versions info", e);
		}
	}
	
	public String getVersion() {
		if (attributes != null) {
			return attributes.getValue(WCVER);
		}
		else {
			return "UNKNOWN";
		}
	}
	
	public String getRevision() {
		if (attributes != null) {
			return attributes.getValue(WCREV);
		}
		else {
			return "UNKNOWN";
		}
	}
	
	public String getDate() {
		if (attributes != null) {
			return attributes.getValue(WCDATE);
		}
		else {
			return "UNKNOWN";
		}
	}
}
