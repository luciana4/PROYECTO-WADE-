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
 but WITHOUT ANY WARRANTY; without evken the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/
 
package com.tilab.wade;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import jade.util.Logger;

public class ProjectVersionManager {
	
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	
	public static final String VERSION_FILE_NAME = "version.properties";
	public static final String RELEASE_FILE_NAME = ".release.properties";
	public static final String WCVER = "Version";
	public static final String WCREV = "SVN-Revision";
	public static final String WCDATE = "SVN-Date";
	public static final String UNKNOWN = "UNKNOWN";
	
	private Properties props = new Properties();
	
	public ProjectVersionManager() {
		InputStream stream = null;
		try {
			// Try to read .release.properties
			ClassLoader cl = getClass().getClassLoader();
			stream = cl.getResourceAsStream(RELEASE_FILE_NAME);
			if (stream == null) {
				// Try to read version.properties
				stream = cl.getResourceAsStream(VERSION_FILE_NAME);
			}
			if (stream != null) {
				props.load(stream);
			}
		}
		catch (Exception e) {
			myLogger.log(Level.WARNING, "Error retrieving project version info", e);
		}
		finally {
			if (stream != null) {
				try { stream.close(); } catch (IOException e) {}
			}
		}
	}
	
	public String getVersion() {
		String version = (String) props.get(WCVER);
		if (version == null) {
			version = UNKNOWN;
		}
		return version;
	}
	
	public String getRevision() {
		String revision = (String) props.get(WCREV);
		if (revision == null) {
			revision = UNKNOWN;
		}
		return revision;
	}
	
	public String getDate() {
		String date = (String) props.get(WCDATE);
		if (date == null) {
			date = UNKNOWN;
		}
		return date;
	}
}
