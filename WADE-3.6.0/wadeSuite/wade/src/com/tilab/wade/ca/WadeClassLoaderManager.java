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

import jade.content.onto.SerializableOntology;
import jade.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.tilab.wade.ca.ontology.ModuleInfo;
import com.tilab.wade.ca.ontology.ModuleInfo.ModuleState;
import com.tilab.wade.utils.FileUtils;

public class WadeClassLoaderManager {
	private final static Logger logger = Logger.getMyLogger(WadeClassLoaderManager.class.getName());

	private final static String STARTUP_FILENAME = "startup"; 
	private final static String CLASSLOADER_FOLDER = "wcl";

	private String root = null;
	private String[] classpathRelevantJars;
	private Map<String, WadeClassLoader> classLoaderMap;
	private String currentClassLoaderId;


	WadeClassLoaderManager() {
		classLoaderMap = new HashMap<String, WadeClassLoader>();
	}

	void prepare(String root, String platformStartuptime) throws Exception {
		this.root = root;

		// Create wcl foder if not present
		File wclDir = new File(root, CLASSLOADER_FOLDER);
		if (!wclDir.exists()) {
			wclDir.mkdir();
		}

		// Check if startup file is already present
		// If it already exists and there is at least a classloader folder -> nothing to do
		// Otherwise creates a new classloader folder with platformStartuptime as id
		File startupFile = new File(root, STARTUP_FILENAME);
		boolean startupFileExist = startupFile.exists();
		boolean classloaderRepositoryExist = findClassLoaderIds().length > 0;
		if (!startupFileExist || !classloaderRepositoryExist) {

			// Create startup file
			startupFile.createNewFile();

			// Create new classloader repository
			createClassLoaderRepository(platformStartuptime);
		}
	}

	void init(String root, String[] classpathRelevantJars) throws Exception {
		if (this.root == null) {
			// Root folder not yet set
			this.root = root;
		}
		this.classpathRelevantJars = classpathRelevantJars;
		
		currentClassLoaderId = findCurrentClassLoaderId();
	}

	// Copy file in deploy folder
	synchronized void deploy(byte[] jarContent, String jarName) throws IOException {
		File jarFile = new File(root, jarName+".jar");
		FileUtils.byte2File(jarFile, jarContent);
	}

	// Delete file in deploy folder
	synchronized void undeploy(String jarName) throws IOException {
		File jarFile = new File(root, jarName+".jar");
		jarFile.delete();
	}

	// revert file from currentClassLoader to deploy folder
	synchronized void revert(String jarName) throws IOException {
		String currentClassLoaderRepository = getClassLoaderRepository(currentClassLoaderId);
		File srcFile = new File(currentClassLoaderRepository, jarName+".jar");
		if (srcFile.exists()) {
			File dstFile = new File(root, jarName+".jar");
	
			FileUtils.copy(srcFile, dstFile);
		}
	}
	
	// Create new folder with clId name
	// Copy all jar files from deploy folder to clId folder
	void createClassLoaderRepository(String clId) throws IOException {
		File clIdFolder = new File(getClassLoaderRepository(clId));
		if (clIdFolder.exists()) {
			logger.log(Logger.WARNING, "Repository folder for new classLoader ID "+clId+" already present");
		} else {
			clIdFolder.mkdir();
		}

		String[] jarFiles = FileUtils.getJarFilelist(root);
		for (String jarFile : jarFiles) {
			File srcFile = new File(root, jarFile);
			File dstFile = new File(clIdFolder, jarFile);

			FileUtils.copy(srcFile, dstFile);
		}
	}

	synchronized void changeCurrentClassLoader(String clId) {
		currentClassLoaderId = clId;

		// Instruct the SerializableOntology to use the new WADE ClassLoader
		((SerializableOntology) SerializableOntology.getInstance()).setClassLoader(getClassLoader(clId));
	}

	// Lazy loading of class loaders required
	synchronized WadeClassLoader getClassLoader(String clId) {
		// Check classloaderId
		if (clId == null) {
			// No id specified, take the current
			if (logger.isLoggable(Logger.FINER)) {
				logger.log(Logger.FINER, "No classloader specified, getting current");
			}
			clId = currentClassLoaderId;
		} else {
			if (logger.isLoggable(Logger.FINEST)) {
				logger.log(Logger.FINEST, "Searching for classloader "+clId);
			}
			if (!classLoaderMap.containsKey(clId) && !existClassLoaderRepository(clId)) {
				// FIXME: Could take the classloader with timestamp more like
				logger.log(Logger.WARNING, "Classloader "+clId+" not found, getting current");
				clId = currentClassLoaderId;
			}
		}

		// Try to get classloader from map
		WadeClassLoader wcl = classLoaderMap.get(clId);
		if (wcl == null) {
			// Classloader not used yet -> create it and store into map
			logger.log(Logger.FINEST, "Create classloader "+clId);
			wcl = new WadeClassLoader(clId, getClass().getClassLoader(), getClassLoaderRepository(clId), classpathRelevantJars);
			wcl.scanAllClasses();
			wcl.loadAllClasses();

			classLoaderMap.put(clId, wcl);
		}

		// Update last usage time
		wcl.updateLastUsage();

		return wcl;
	}

	synchronized String getClassLoadersInfo() {
		StringBuilder sb = new StringBuilder();
		Iterator<WadeClassLoader> iter = classLoaderMap.values().iterator();
		while (iter.hasNext()) {
			sb.append('(');
			sb.append(iter.next().getInfo());
			sb.append("), ");
		}
		sb.setLength(sb.length()-2);
		return sb.toString();
	}

	synchronized List<ModuleInfo> getModules() throws IOException {
		// Build map of deploy jars
		Map<String, ModuleInfo> moduleMap = new HashMap<String, ModuleInfo>();
		String[] deployJarFiles = FileUtils.getJarFilelist(root);
		for (String deployJarFile : deployJarFiles) {
			ModuleInfo deployMI = getModuleInfo(new File(root, deployJarFile));
			deployMI.setState(ModuleState.NEW);
			moduleMap.put(deployMI.getName(), deployMI);
		}		

		// Scan current classloader jars
		String classLoaderRepository = getClassLoaderRepository(currentClassLoaderId);
		String[] currentJarFiles = FileUtils.getJarFilelist(classLoaderRepository);
		for (String currentJarFile : currentJarFiles) {
			// Check differnces 
			ModuleInfo currentMI = getModuleInfo(new File(classLoaderRepository, currentJarFile));
			ModuleInfo deployMI = moduleMap.get(currentMI.getName());
			if (deployMI == null) {
				// Module DELETED
				currentMI.setState(ModuleState.DELETED);
				moduleMap.put(currentMI.getName(), currentMI);
			} else {
				if (deployMI.getDate() == currentMI.getDate()) {
					// Module equals
					deployMI.setState(ModuleState.ACTIVE);
				} else {
					// module modified
					deployMI.setState(ModuleState.MODIFIED);
				}
			}
		}

		List<ModuleInfo> modules = new ArrayList<ModuleInfo>(moduleMap.values());
		Collections.sort(modules);
		return modules;
	}

	private String getClassLoaderRepository(String clId) {
		return root+File.separator+CLASSLOADER_FOLDER+File.separator+clId;
	}

	private boolean existClassLoaderRepository(String clId) {
		File dir = new File(getClassLoaderRepository(clId));
		return dir.exists() && dir.isDirectory();
	}

	// Scan subfolders of the root (deploy folder) and look for 
	// the one with the maximum name (timestamp)
	private String findCurrentClassLoaderId() throws Exception {
		long maxValue = 0;
		String[] clIds = findClassLoaderIds();
		if (clIds != null) { 
			for (String clId : clIds) {
				long value = Long.parseLong(clId);
				if (value > maxValue) {
					maxValue = value;
				}
			}
		}
		if (maxValue == 0) {
			throw new Exception("Deploy folder does not contain any classloader");
		}
		return String.valueOf(maxValue);
	}


	// Find all subfolders of the root whose name is a long (timestamp)
	private String[] findClassLoaderIds() {
		File dir = new File(root, CLASSLOADER_FOLDER);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File file, String name) {
				boolean correctName = false;
				try {
					Long.parseLong(name);
					correctName = true;
				} catch(NumberFormatException nfe) {}

				return file.isDirectory() && correctName;
			}
		};
		return dir.list(filter);
	}

	private static ModuleInfo getModuleInfo(File jarFile) throws IOException {
		String jarName = jarFile.getName();
		FileInputStream inputStream = new FileInputStream(jarFile);
		JarInputStream jarStream = new JarInputStream(inputStream);
		Manifest manifest = jarStream.getManifest();
		inputStream.close();

		ModuleInfo moduleInfo = new ModuleInfo();
		if (manifest != null) {
			moduleInfo.setName(manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_NAME));
			moduleInfo.setVersion(manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_VERSION));
			moduleInfo.setCategory(manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_CATEGORY));
			moduleInfo.setDescription(manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_DESCRIPTION));
			String bundleDate = manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_DATE);
			if (bundleDate != null) {
				moduleInfo.setDate(Long.parseLong(bundleDate));
			}
		}

		String fileName = jarName.replaceFirst("[.][^.]+$", "");
		moduleInfo.setFileName(jarName);
		if (moduleInfo.getName() == null) {
			moduleInfo.setName(fileName);
		}

		if (moduleInfo.getCategory() == null) {
			if (fileName.endsWith("_C") || fileName.endsWith("_S")) {
				moduleInfo.setCategory(ModuleInfo.SERVICE_CATEGORY);
			} else {
				moduleInfo.setCategory(ModuleInfo.GENERIC_CATEGORY);
			}
		}

		return moduleInfo;
	}
	
	public static ModuleInfo getModuleInfo(Class<?> clazz) {
		try {
			// Try to get the jar file containing this class
			File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (f.isFile()) {
				return WadeClassLoaderManager.getModuleInfo(f);	
			}
		} catch(Exception e) {
			// Error accessing to class container
		}
		
		// No info about class container
		return null;
	}
}
