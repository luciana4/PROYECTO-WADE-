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

import jade.gui.ClassSelectionDialog;
import jade.util.ClassFinder;
import jade.util.ClassFinderFilter;
import jade.util.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.performer.EngineHelper;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor;
import com.tilab.wade.performer.layout.WorkflowSkipped;
import com.tilab.wade.utils.FileUtils;


/**
   The ClassLoader to be used to load classes that must be deployed at runtime.
   WorkflowEngine agents use this class loader to load workflows, applications and service-descriptor classes  
 */
public class WadeClassLoader extends URLClassLoader {

	private static Logger myLogger = Logger.getMyLogger((WadeClassLoader.class).getName());

	private String id = null;
	private long lastUsage = -1;
	private ScanJars sj;
	private SortedSet<WorkflowDetails> workflows;
	private boolean classpathWfAdded = false;
	private SortedSet<String> webServiceDescriptors;
	private boolean classpathWSDAdded = false;
	private String[] classpathRelevantJars;

	/**
	 * Creates a new WadeClassLoader that load classes from the jar files included in a given root directory.
	 * @param root the directory where to get jar files from
	 * @param classpathRelevantJars Names of jar files where to search for workflows, applications and service-descriptor directly included in the classpath
	 */
	public WadeClassLoader(String id, ClassLoader parent, String root, String[] classpathRelevantJars) {
		super(getUrlList(root), parent);
		updateLastUsage();
		workflows = new TreeSet<WorkflowDetails>();
		webServiceDescriptors = new TreeSet<String>();
		this.classpathRelevantJars = classpathRelevantJars;
		this.id = id;
	}

	public void scanAllClasses() {
		sj = new ScanJars(getURLs());
	}

	public void loadAllClasses() {
		if (sj != null) {
			sj.loadAll(this);
		} else {
			myLogger.log(Logger.SEVERE, "loadAllClasses(): cannot load classes, call scanAllClasses() first");
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> result;
		if (myLogger.isLoggable(Logger.FINEST)) {
			myLogger.log(Logger.FINEST, "loading class "+name+(id == null ? " with anonymous classloader" : " with classloader "+id));
		}
		result = super.loadClass(name);

		// If is a valid WorkflowBehaviour add to workflows list
		if (WorkflowBehaviour.class.isAssignableFrom(result)) {
			if (myLogger.isLoggable(Logger.FINEST)) {
				myLogger.log(Logger.FINEST, "class "+name+" extends WorkflowBehaviour");
			}

			if (!isWorkflowExcluded(result)) {
				workflows.add(EngineHelper.buildWorkflowDetails(result));
			}
		}

		// If is a valid ServiceDescriptor add to webServiceDescriptors list
		if (ServiceDescriptor.class.isAssignableFrom(result)) {
			if (myLogger.isLoggable(Logger.FINEST)) {
				myLogger.log(Logger.FINEST, "class "+name+" extends ServiceDescriptor");
			}

			if (!isServiceDescriptorExcluded(result)) {
				webServiceDescriptors.add(result.getName());
			}
		}
		return result;
	}

	private static boolean isWorkflowExcluded(Class<?> clazz) {

		// Exclude classes not concrete (abstract or interfaces)
		int modifiers = clazz.getModifiers();
		if ((modifiers & (ClassSelectionDialog.ACC_ABSTRACT | ClassSelectionDialog.ACC_INTERFACE)) != 0) {
			return true;
		}

		// Check if is present the annotation WorkflowSkipped
		return clazz.getAnnotation(WorkflowSkipped.class) != null;
	}

	private static boolean isServiceDescriptorExcluded(Class<?> clazz) {

		// Exclude classes not concrete (abstract or interfaces)
		int modifiers = clazz.getModifiers();
		if ((modifiers & (ClassSelectionDialog.ACC_ABSTRACT | ClassSelectionDialog.ACC_INTERFACE)) != 0) {
			return true;
		}
		return false;
	}

	public jade.util.leap.List getWorkflowList() {
		return getWorkflowList(null, null, null);
	}
	
	public synchronized jade.util.leap.List getWorkflowList(String category, String moduleName, Boolean componentOnly) {
		// Get workflows in class-path (lazy initialization) 
		if (!classpathWfAdded) {
			if(classpathRelevantJars != null) {
				ClassFinder cf = new ClassFinder(classpathRelevantJars);
				Vector classpathWfs = cf.findSubclasses(WorkflowBehaviour.class.getName(), null, new WorkflowClassFilter());
				for (Object wfClass : classpathWfs) {
					workflows.add(EngineHelper.buildWorkflowDetails((Class) wfClass));
				}
			}

			classpathWfAdded = true;
		}

		// Add workflows in class-loader to workflows in class-path 
		jade.util.leap.List result = new jade.util.leap.ArrayList();
		for (WorkflowDetails wfDetails : workflows) {
			
			if ((category == null || category.equalsIgnoreCase(wfDetails.getCategory())) &&
                (moduleName == null || (wfDetails.getModuleInfo() != null && moduleName.equalsIgnoreCase(wfDetails.getModuleInfo().getName()))) &&
                (componentOnly == null || componentOnly.equals(wfDetails.isComponent()))) {
				result.add(wfDetails);		
			}
		}
		return result;
	}

	public synchronized jade.util.leap.List getWebServiceDescriptorList() {
		// Get service-descriptor in class-path (lazy initialization) 
		if (!classpathWSDAdded) {
			if(classpathRelevantJars != null) {
				ClassFinder cf = new ClassFinder(classpathRelevantJars);
				Vector classpathSDs = cf.findSubclasses(ServiceDescriptor.class.getName(), null, new ServiceDescriptorClassFilter());
				for (Object sdClass : classpathSDs) {
					webServiceDescriptors.add(((Class) sdClass).getName());
				}
			}

			classpathWSDAdded = true;
		}

		// Add service-descriptor in class-loader to service-descriptor in class-path 
		jade.util.leap.List result = new jade.util.leap.ArrayList(webServiceDescriptors.size());
		for (String sd : webServiceDescriptors) {
			result.add(sd);
		}
		return result;
	}

	private static URL [] getUrlList(String root) {
		String [] jarNames = FileUtils.getJarFilelist(root);
		URL [] urls = null;
		if (jarNames != null) {
			urls = new URL[jarNames.length];
			try {
				StringBuffer sb = new StringBuffer("Loading classes from:\n");
				for(int i=0;i < urls.length;i++) {
					urls[i] = new File(root+"/"+jarNames[i]).toURI().toURL();
					sb.append(urls[i]+"\n");
				}
				myLogger.log(Logger.INFO, sb.toString());
			}
			catch(MalformedURLException me) {
				me.printStackTrace();
			}
		}
		else {
			myLogger.log(Logger.WARNING, "WADE ClassLoader root directory not found");
			urls = new URL[0];
		}
		return urls;
	}

	public String getId() {
		return id;
	}

	public long getLastUsage() {
		return lastUsage;
	}

	public void updateLastUsage() {
		lastUsage = new Date().getTime();
	}

	public String getInfo() {
		return "id=\""+id+"\" lastUsage=<"+new Date(lastUsage)+">";
	}

	public String toString() {
		return "WadeClassLoader {"+getInfo()+"}";
	}

	private static class WorkflowClassFilter implements ClassFinderFilter {

		public boolean include(Class superClazz, Class clazz) {
			return !isWorkflowExcluded(clazz);
		}
	}

	private static class ServiceDescriptorClassFilter implements ClassFinderFilter {

		public boolean include(Class superClazz, Class clazz) {
			return !isServiceDescriptorExcluded(clazz);
		}
	}

	public void unlockResources() {
		try {
			Class clazz = java.net.URLClassLoader.class;
			java.lang.reflect.Field ucp = clazz.getDeclaredField("ucp");
			ucp.setAccessible(true);
			Object sun_misc_URLClassPath = ucp.get(this);
			java.lang.reflect.Field loaders = sun_misc_URLClassPath.getClass().getDeclaredField("loaders");
			loaders.setAccessible(true);
			Object java_util_Collection = loaders.get(sun_misc_URLClassPath);
			for (Object sun_misc_URLClassPath_JarLoader : ((java.util.Collection) java_util_Collection).toArray()) {
				try {
					java.lang.reflect.Field loader = sun_misc_URLClassPath_JarLoader.getClass().getDeclaredField("jar");
					loader.setAccessible(true);
					Object java_util_jar_JarFile = loader.get(sun_misc_URLClassPath_JarLoader);
					((java.util.jar.JarFile) java_util_jar_JarFile).close();
				} catch (Throwable t) {
					// if we got this far, this is probably not a JAR loader so skip it
				}
			}

			clazz = ClassLoader.class;
			java.lang.reflect.Field nativeLibraries = clazz.getDeclaredField("nativeLibraries");
			nativeLibraries.setAccessible(true);
			java.util.Vector java_lang_ClassLoader_NativeLibrary = (java.util.Vector) nativeLibraries.get(this);
			for (Object lib : java_lang_ClassLoader_NativeLibrary) {
				java.lang.reflect.Method finalize = lib.getClass().getDeclaredMethod("finalize", new Class[0]);
				finalize.setAccessible(true);
				finalize.invoke(lib, new Object[0]);
			}
		} catch (Throwable t) {
			// probably not a SUN VM
			myLogger.log(Logger.WARNING, "Unlocking classpath JAR not possible: probably not a SUN VM");
		}
		return;
	}	
}
