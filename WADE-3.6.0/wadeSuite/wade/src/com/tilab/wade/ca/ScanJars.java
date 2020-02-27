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
package com.tilab.wade.ca;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jade.util.Logger;

class ScanJars {

	private final static Logger logger = Logger.getMyLogger(ScanJars.class.getName());

	public final static String JAR_EXT = ".jar";
	public static final String CLASS_EXT = ".class";

	private Set<ClasspathElement> classpathElements;

	private ClasspathElement rootElement;

	private class ClasspathElement {
		URL path;
		long id;
		Set<String> classes;

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (!(o instanceof ClasspathElement)) {
				return false;
			}
			return path.equals(((ClasspathElement)o).path);
		}

		@Override
		public int hashCode() {
			int hashcode = 41;
			if (path != null) {
				hashcode ^= path.hashCode();
			}
			return hashcode;
		}

		public ClasspathElement(URL path) {
			this.path = path;
			id = -1;
			classes = new HashSet<String>();
		}

		public String toString() {
			return "ClasspathElement{" +
					"path="+(path == null ? "null" : "\""+path+"\"")+
					" id="+Long.toHexString(id)+
					" classes="+classes+"}";
		}
	}

	private static String buildClassnameFromPathInJar(String name) {
		int i = name.indexOf(ScanJars.CLASS_EXT);
		name = name.substring(0, i);
		name = name.replace ('/', '.');
		return name; 
	}

	private void addJarClasses(ClasspathElement ce) throws IOException {
		long jarId = 0;
		boolean added;
		File f = new File(ce.path.getFile());
		JarFile jf = new JarFile(f);
		Enumeration<JarEntry> jes = jf.entries();
		JarEntry je;
		String pathOfClass;
		String nameOfClass;
		logger.log(Logger.FINE, "reading jar "+ce.path);
		while (jes.hasMoreElements()) {
			je = jes.nextElement();
			pathOfClass = je.getName();
			if (pathOfClass.endsWith(ScanJars.CLASS_EXT)) {
				jarId ^= je.getCrc();
				nameOfClass = buildClassnameFromPathInJar(pathOfClass);
				added = ce.classes.add(nameOfClass);
				if (logger.isLoggable(Logger.FINE)) {
					if (added) {
						logger.log(Logger.FINE, "added class "+nameOfClass+" found in "+jf.getName()+":"+je.getName()+" (crc="+Long.toHexString(je.getCrc())+")");
					} else {
						logger.log(Logger.WARNING, "skipped already present class "+nameOfClass);
					}
				}
			}
		}
		ce.id = jarId & 0x7fffffffffffffffL;
		jf.close();
	}

	public ScanJars(URL[] urls) {
		classpathElements = new HashSet<ClasspathElement>();
		for(int i = 0; i < urls.length; i++) {
			ClasspathElement ce = new ClasspathElement(urls[i]);
			try {
				addJarClasses(ce);
				classpathElements.add(ce);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		rootElement = buildRootElement();
	}

	private ClasspathElement buildRootElement() {
		ClasspathElement result = new ClasspathElement(null);
		if (classpathElements != null) {
			result.id = 0;
			for(ClasspathElement elem: classpathElements) {
				result.classes.addAll(elem.classes);
				result.id ^= elem.id;
			}
		}
		return result;
	}

	public long getId() {
		long id = -1;
		if (rootElement != null) {
			id = rootElement.id;
		}
		return id;
	}

	public void loadAll(ClassLoader cl) {
		logger.log(Logger.FINE, "id="+rootElement.id);
		Iterator<String> iter = rootElement.classes.iterator();
		String classname;
		while(iter.hasNext()) {
			classname = iter.next();
			try {
				logger.log(Logger.FINE, "loading class "+classname);
				cl.loadClass(classname);
			} catch (ClassNotFoundException e) {
				logger.log(Logger.WARNING, "Error loading class "+classname, e);
			}
		}
	}
	
}
