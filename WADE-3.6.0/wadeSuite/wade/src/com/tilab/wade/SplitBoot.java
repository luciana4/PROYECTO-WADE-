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

import jade.MicroBoot;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.imtp.leap.JICP.JICPProtocol;
import jade.util.leap.Properties;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SplitBoot {
	private static Logger logger = Logger.getLogger(SplitBoot.class.getName());
	private static final String UNKNOWN_CONTAINER_NAME = "(unknown)";
	
	/**
	 * This main class duplicates the jade.Boot.main code to properly manage 
	 * the formatter of logger adding the name of the container
	 */
	public static final void main(String[] args) {
		
		try {
			Properties pp = MicroBoot.parseCmdLineArgs(args);
			String containerName = pp.getProperty(Profile.CONTAINER_NAME, pp.getProperty(JICPProtocol.MSISDN_KEY, null));
			if (containerName == null) {
				containerName = UNKNOWN_CONTAINER_NAME; 
			}
			Boot.manageLoggerHandlers(false); // For sure this is not a Main Container
			Boot.manageLoggerFormatters(containerName);

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
			logger.log(Level.INFO, ">>>>>>>>>>>>>>>> project-home        = "+System.getProperty("project-home"));
			if (System.getProperty("parent-project-home") != null) {
				logger.log(Level.INFO, ">>>>>>>>>>>>>>>> parent-project-home = "+System.getProperty("parent-project-home"));
			}
			
			Boot.setPreserveJavaTypes();
			
			// Start the split container
			MicroRuntime.startJADE(pp, new Runnable() {
				public void run() {
					// Wait a bit before killing the JVM
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException ie) {
					}
					logger.log(Level.INFO, "Exiting now!");
					System.exit(0);
				} 
			});

			// If container-name is unknown get if from AgentContainer
			if (UNKNOWN_CONTAINER_NAME.equals(containerName)) {
				containerName = MicroRuntime.getContainerName();
				Boot.manageLoggerFormatters(containerName);
			}
		}
		catch (IllegalArgumentException iae) {
			System.err.println("Command line arguments format error. "+iae.getMessage());
			iae.printStackTrace();
			jade.Boot.printUsage();
			System.exit(-1);
		}
	}
}
