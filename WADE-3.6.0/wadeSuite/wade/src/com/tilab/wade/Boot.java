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

import jade.content.lang.sl.SLCodec;
import jade.core.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.ControllerException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.utils.logging.WadeFormatter;
import com.tilab.wade.utils.logging.WadeHandler;


public class Boot {
	private static Logger logger = Logger.getLogger(Boot.class.getName());
	static final String UNKNOWN_CONTAINER_NAME = "(unknown)";
	
	/**
	 * This main class duplicates the jade.Boot.main code to properly manage 
	 * the formatter of logger adding the name of the container
	 */
	public static final void main(String[] args) {
		
		try {
			// Create the Profile 
			ProfileImpl p = null;
			if (args.length > 0) {
				if (args[0].startsWith("-")) {
					// Settings specified as command line arguments
					Properties pp = jade.Boot.parseCmdLineArgs(args);
					if (pp != null) {
						p = new ProfileImpl(pp);
					} else {
						// One of the "exit-immediately" options was specified!
						return;
					}
				} else {
					// Settings specified in a property file
					p = new ProfileImpl(args[0]);
				}
			} else {
				// Settings specified in the default property file
				p = new ProfileImpl(jade.Boot.DEFAULT_FILENAME);
			} 
			
			//#PJAVA_EXCLUDE_BEGIN
			// Add container-name to all ProjectLineFormatter
			// Search it in jade-profile or set Main-Container if the container is a master-main
			boolean mainContainer = p.getBooleanProperty(Profile.MAIN, true) && !p.getBooleanProperty(Profile.LOCAL_SERVICE_MANAGER, false);
			String containerName = p.getParameter(Profile.CONTAINER_NAME, null);
			if (containerName == null) {
				if (mainContainer) {
					containerName = AgentContainer.MAIN_CONTAINER_NAME;
				} else {
					containerName = UNKNOWN_CONTAINER_NAME; 
				}
			}
			manageLoggerHandlers(mainContainer);
			manageLoggerFormatters(containerName);
			//#PJAVA_EXCLUDE_END

			String currentDir;
			try {
				currentDir = new File(".").getCanonicalPath();
			} catch (IOException e) {
				logger.log(Level.WARNING, ">>>>>>>>>>>>>>>> could not determine current directory", e);
				currentDir = ".";
			}
			
			if (System.getProperty("project-home") == null) {
				System.setProperty("project-home", currentDir);
			}
			logger.log(Level.INFO, ">>>>>>>>>>>>>>>> current-directory   = "+currentDir);
			if (System.getProperty("wade-home") != null) {
				logger.log(Level.INFO, ">>>>>>>>>>>>>>>> wade-home           = "+System.getProperty("wade-home"));
			}
			if (System.getProperty("project-name") != null) {
				logger.log(Level.INFO, ">>>>>>>>>>>>>>>> project-name        = "+System.getProperty("project-name"));
			}
			logger.log(Level.INFO, ">>>>>>>>>>>>>>>> project-home        = "+System.getProperty("project-home"));
			if (System.getProperty("parent-project-home") != null) {
				logger.log(Level.INFO, ">>>>>>>>>>>>>>>> parent-project-home = "+System.getProperty("parent-project-home"));
			}
			
			setPreserveJavaTypes();
			
			// Start a new JADE runtime system
			Runtime.instance().setCloseVM(true);
			
			//#PJAVA_EXCLUDE_BEGIN
			// Check whether this is the Main Container or a peripheral container
			jade.wrapper.AgentContainer agentContainer;
			if (p.getBooleanProperty(Profile.MAIN, true)) {
				agentContainer = Runtime.instance().createMainContainer(p);
			} else {
				agentContainer = Runtime.instance().createAgentContainer(p);
			}

			// If container-name is unknown get if from AgentContainer
			if (UNKNOWN_CONTAINER_NAME.equals(containerName)) {
				try {
					containerName = agentContainer.getContainerName();
					manageLoggerFormatters(containerName);
				} catch (ControllerException e) {
					logger.log(Level.WARNING, "Error getting container-name", e);
				}
			}
			//#PJAVA_EXCLUDE_END
			/*#PJAVA_INCLUDE_BEGIN
			// Starts the container in SINGLE_MODE (Only one per JVM)
			Runtime.instance().startUp(p);
			#PJAVA_INCLUDE_END*/
		}
		catch (ProfileException pe) {
			System.err.println("Error creating the Profile ["+pe.getMessage()+"]");
			pe.printStackTrace();
			jade.Boot.printUsage();
			System.exit(-1);
		}
		catch (IllegalArgumentException iae) {
			System.err.println("Command line arguments format error. "+iae.getMessage());
			iae.printStackTrace();
			jade.Boot.printUsage();
			System.exit(-1);
		}
	}
		
	public static void setPreserveJavaTypes() {
		String preserveJavaTypesStr = System.getProperty(SLCodec.PRESERVE_JAVA_TYPES);
		if (preserveJavaTypesStr == null) {
			Map globalProperties = TypeManager.getInstance().getProperties();
			preserveJavaTypesStr = TypeManager.getString(globalProperties, SLCodec.PRESERVE_JAVA_TYPES, "true");
			System.setProperty(SLCodec.PRESERVE_JAVA_TYPES, preserveJavaTypesStr);
		}
	}

	static void manageLoggerHandlers(boolean mainContainer) {
		Logger rootLogger = Logger.getLogger("");
		WadeHandler.manage(rootLogger, mainContainer);
	}
		
	static void manageLoggerFormatters(String containerName) {
		Logger rootLogger = Logger.getLogger("");
		WadeFormatter.manage(rootLogger, containerName);
	}
}
