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
package com.tilab.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;

public class CopyFilteredFiles extends Task {

	private String sourceDir;
	private String destDir;
	private String excludes;
	
	public String getSourceDir() {
		return sourceDir;
	}
	public void setSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}
	public String getDestDir() {
		return destDir;
	}
	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}
	public String getExcludes() {
		return excludes;
	}
	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}
	
	public void execute() throws BuildException {
		try {
			List<String> excludedFiles = new ArrayList<String>();
			String[] excludesArray = excludes.split(",");
			for (String exclude : excludesArray) {
				// Scan exclude jar file and build list with all files
				JarFile jarFile = new JarFile(exclude);
		        Enumeration<JarEntry> entries = jarFile.entries();
		        while (entries.hasMoreElements()) {
		            JarEntry entry = entries.nextElement();
		            if (!entry.isDirectory()) {
		            	excludedFiles.add(entry.getName());
		            }
		        }
			}

			// Scan source files excluding files contained in jar
			File sd = new File(sourceDir);
			if (sd.isDirectory()) {
				DirectoryScanner scanner = new DirectoryScanner();
				scanner.setBasedir(sd);
				scanner.setExcludes(excludedFiles.toArray(new String[0]));
				scanner.scan();
				String[] files = scanner.getIncludedFiles();
				for (String file : files) {
					File sourceFile = new File(sourceDir, file);
					File destFile = new File(destDir, file);
					
					Copy antCopy = new Copy();
					antCopy.setOverwrite(true);
					antCopy.setFile(sourceFile);
					antCopy.setTofile(destFile);
					antCopy.execute();
				}
			}
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
