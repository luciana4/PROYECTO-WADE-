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
package com.tilab.wade.boot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import test.common.TestException;
import test.common.remote.TSDaemon;

import com.tilab.wade.utils.logging.WadeFormatter;


/**
 * Demone che deve essere presente su tutti gli host di WADE. Implementa l'interfaccia RemoteManager e quindi consente di creare da remoto dei container
 * (JadeInstances).
 *
 * @author Giovanni Caire - TILAB 
 */

public class BootDaemon extends TSDaemon implements RemoteManagerEx {
	private static final long serialVersionUID = 317349564978029197L;

	private static jade.util.Logger logger = jade.util.Logger.getMyLogger(BootDaemon.class.getName());

	public static final String PROJECT_NAME = "project-name";
	public static final String PROJECT_NAME_DEF = "-D"+PROJECT_NAME+"=";
	public static final String PARENT_PROJECT_NAME_DEF = "-Dparent-project-name=";

	public static final String LOGGING_CONFIG_FILE = "java.util.logging.config.file";
	public static final String LOGGING_CONFIG_FILE_DEF = "-D"+LOGGING_CONFIG_FILE+"=";
	
	public static final String KILL_ON_OOM_DEF = "-kill-on-OOM ";
	
	private static String wadeHome = null;
	private static String wadeClasspath = null;

	public BootDaemon() throws RemoteException {
		super();
	}

	public static void main(String args[]) {
		
		// Add source-name to all WadeFormatter in configured root-logger handlers
		WadeFormatter.manage(Logger.getLogger(""), "Boot-Daemon");
		
		// Start BootDaemon
		String currentDir;
		try {
			currentDir = new File(".").getCanonicalPath();
		} catch (IOException e) {
			logger.log(Level.WARNING, ">>>>>>>>>>>>>>>> could not determine current directory", e);
			currentDir = ".";
		}
		
		wadeHome = System.getProperty("wade-home");
		if (wadeHome == null) {
			logger.log(Level.WARNING, ">>>>>>>>>>>>>>>> wade-home not set, using current directory");
			wadeHome = currentDir;
		}
		
		try {
			wadeHome = resolveSymbolicLink(wadeHome);
		} catch (IOException e) {
			logger.log(Level.WARNING, ">>>>>>>>>>>>>>>> could not resolve symbolic link", e);
		}
		
		wadeClasspath = System.getProperty("java.class.path");

		logger.log(Level.INFO, ">>>>>>>>>>>>>>>> current-directory = "+currentDir);
		logger.log(Level.INFO, ">>>>>>>>>>>>>>>> wade-home         = "+wadeHome);
		logger.log(Level.INFO, ">>>>>>>>>>>>>>>> classpath         = "+wadeClasspath);

		try {
			boolean daemonize = Boolean.getBoolean("daemon");
			if (daemonize) {
				System.in.close();
				String outFilename = System.getProperty("daemon.outfile");
				if (outFilename != null) {
					PrintStream outStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFilename, true)));
					System.out.close();
					System.err.close();
					System.setOut(outStream);
					System.setErr(outStream);
				}
				String pidFilename = System.getProperty("daemon.pidfile");
				if (pidFilename != null) {
					File f = new File(pidFilename);
					f.deleteOnExit();
				}
			}
			final BootDaemon daemon = new BootDaemon();
			if (daemonize) {
				final String[] myargs = args;
				Thread t = new Thread() {
					public void run() {
						try {
							daemon.start(myargs);
						} catch (Exception e) {
							logger.log(jade.util.Logger.SEVERE,"ERROR starting Boot Daemon", e);
						}
					}
				};
				t.setDaemon(true);
				t.start();
			} else {
				daemon.start(args);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE,"ERROR starting Boot Daemon", e);
		}
	}

	public void handleOutput(String source, String msg) {
		logger.log(Level.INFO, source+">> "+msg);
	}

	protected void printWelcomeMessage(String port, String name) {
		logger.log(Level.INFO, "Boot Daemon ready on port: " + port + " using name: "+ name);
	}

	@Override
	public int launchJadeInstance(String instanceName, String classpath, String jvmArgs, String mainClass, String jadeArgs, String[] protoNames) throws TestException, RemoteException {
		String projectName = getArgValue(jvmArgs, PROJECT_NAME_DEF);
		
		// If a project name is specified by means of the -Dproject-name=nnn system property
		// - Execute the new Java process in the project-home directory
		// - Manage the classpath as below:
		//   i)  If no classpath is specified, use the project classpath 
		//   ii) If the specified classpath contains ${projectName-classpath}, replace that with
		//       the project classpath
		// In any case prepare a suitable OutputHandler to manage the output of the Java process
		// to be launched
		//
		// See also comment in ConfigurationAgent.start()
		String projectHome = null;
		String loggingFile =  getArgValue(jvmArgs, LOGGING_CONFIG_FILE_DEF);
		ProjectOutputHandler oh;
		if (projectName != null) {
			Properties props = loadProjectProperties(projectName);
			projectHome = props.getProperty("project-home");
			jvmArgs += " -Dproject-home="+projectHome;

			String prjClasspath = getClasspathFromProject(projectName, props);
			
			// If this is a child project, add the classpath of the parent too
			String parentProjectName = getArgValue(jvmArgs, PARENT_PROJECT_NAME_DEF);
			if (parentProjectName != null) {
				Properties parentProps = loadProjectProperties(parentProjectName);
				String parentPrjClasspath = getClasspathFromProject(parentProjectName, parentProps);
				prjClasspath = prjClasspath + File.pathSeparatorChar + parentPrjClasspath;
				
				String parentProjectHome = parentProps.getProperty("project-home");
				jvmArgs += " -Dparent-project-home="+parentProjectHome;
			}
			
			if (classpath == null) {
				classpath = prjClasspath;
			} 
			else {
				String pncpKey = "${"+projectName+"-classpath}";
				classpath = classpath.replace(pncpKey, prjClasspath);
			}
			
			oh = new ProjectOutputHandler(projectName, projectHome, wadeHome, loggingFile);
		}
		else {
			oh = new ProjectOutputHandler("default", null, wadeHome, loggingFile);
		}
		
		jvmArgs += " -Dwade-home="+wadeHome;
		
		int instanceId = localLaunchJadeInstance(instanceName, classpath, jvmArgs, mainClass, jadeArgs, protoNames, oh, projectHome);
		
		// Manage output-filter
		String outputFilter = null;
		if (Boolean.parseBoolean(getArgValue(jadeArgs, KILL_ON_OOM_DEF))) {
			// KillOnOOOM filter specified in container profile
			outputFilter = KillOnOoMFilter.class.getName();
		}
		else {
			// Output filter specified in boot.properties
			outputFilter = System.getProperty("output-filter");
		}
		if (outputFilter != null) {
			try {
				// Get output-filter className and cfg parameter (optional)
				outputFilter = outputFilter.trim();
				String outputFilterClassName = outputFilter;
				String outputFilterCfg = null;
				int pos = outputFilter.indexOf("(");
				if (pos > 0) {
					outputFilterClassName = outputFilter.substring(0, pos);
					outputFilterCfg = outputFilter.substring(pos+1, outputFilter.length()-1);
				}
				
				// Create and initialize the output-filter
				OutputFilter of = (OutputFilter) Class.forName(outputFilterClassName).newInstance();
				of.init(outputFilterCfg, instanceId, instanceName, this);
				
				oh.setFilter(of);
			}
			catch (Exception e) {
				throw new TestException("Cannot instantiate output-filter "+outputFilter, e);
			}
		}
		
		return instanceId;
	}

	private static Properties loadProjectProperties(String projectName) throws TestException {
		String path = wadeHome+File.separator+"projects"+File.separator+projectName+".properties";
	
		return loadProperties(path);
	}
	
	static Properties loadProperties(String path) throws TestException {
		Properties props = new Properties();
		FileInputStream inputStream = null;
		try {
			logger.log(Level.FINE, "loading properties from "+path);
			inputStream = new FileInputStream(path);
			props.load(inputStream);
		} catch (IOException e) {
			props = null;
			throw new TestException("cannot read properties from "+path, e);
		} finally {
			if (inputStream != null) {
				try { inputStream.close();} catch (IOException e) {}
			}
		}
		return props;
	}
	
	private String getArgValue(String jvmArgs, String key) {
		int idx = jvmArgs.indexOf(key); 
		if (idx >= 0) {
			String tmp = jvmArgs.substring(idx+key.length());
			int ii = tmp.indexOf(' ');
			if (ii > 0) {
				return tmp.substring(0, ii);
			} else {
				return tmp;
			}
		}
		return null;
	}
	
	private String getClasspathFromProject(String projectName, Properties props) throws TestException {
		String projectHome = props.getProperty("project-home");
		if (projectHome == null) {
			throw new TestException("project-home for project "+projectName+" is not set");
		}
		String projectClasses = props.getProperty("project-classes");
		if (projectClasses == null) {
			projectClasses = projectHome+"/classes";
			projectClasses = getAbsolutePath(projectClasses);
		}
		String projectCfg = props.getProperty("project-cfg");
		if (projectCfg == null) {
			projectCfg = projectHome+"/cfg";
			projectCfg = getAbsolutePath(projectCfg);
		}
		String projectLib = props.getProperty("project-lib");
		if (projectLib == null) {
			projectLib = projectHome+"/lib";
			projectLib = getAbsolutePath(projectLib);
		}
		logger.log(Level.FINE, "project "+projectName+": project-home    = "+projectHome);
		logger.log(Level.FINE, "project "+projectName+": project-classes = "+projectClasses);
		logger.log(Level.FINE, "project "+projectName+": project-cfg     = "+projectCfg);
		logger.log(Level.FINE, "project "+projectName+": project-lib     = "+projectLib);

		String classpath = projectClasses+File.pathSeparatorChar;
		classpath += projectCfg+File.pathSeparatorChar;
		classpath += projectLib+"/*";
		return classpath;
	}

	private static String getAbsolutePath(String dir) {
		try {
			return new File(dir).getCanonicalPath();
		}
		catch (Exception e) {
			return dir;
		}
	}

	public static String resolveSymbolicLink(String file) throws IOException {
		File f = new File(file);
		if (f.getParent() != null) {
			File canonicalFile = f.getParentFile().getCanonicalFile();
			f = new File(canonicalFile, f.getName());
		}
		return f.getCanonicalFile().getPath();
	}

	public String getWadeHome() throws RemoteException {
		return wadeHome;
	}
	
	public void exit() throws RemoteException {
		logger.log(Level.INFO, "Goodbye!");
		System.exit(0);
	}
}
