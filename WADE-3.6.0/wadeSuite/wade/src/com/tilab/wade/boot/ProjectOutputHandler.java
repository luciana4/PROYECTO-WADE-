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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tilab.wade.utils.logging.RollingFileHandler;
import com.tilab.wade.utils.logging.WadeFormatter;
import com.tilab.wade.utils.logging.WadeHandler;

import test.common.OutputHandler;

class ProjectOutputHandler implements OutputHandler {
	private static Map<String, List<OutputHandler>> projectOutputHandlers = new HashMap<String, List<OutputHandler>>();
	private String projectName;
	private Logger logger;
	private OutputFilter outputFilter;
	
	ProjectOutputHandler(String projectName, String projectHome, String wadeHome, String projectloggingFile) {
		this.projectName = projectName;
		
		// Add new OutputHandler to project map
		synchronized (projectOutputHandlers) {
			List<OutputHandler> outputHandlers = projectOutputHandlers.get(projectName);
			if (outputHandlers == null) {
				outputHandlers = new ArrayList<OutputHandler>();
				projectOutputHandlers.put(projectName, outputHandlers);
			}
			outputHandlers.add(this);
		}
		
		// Verify if the project logger is already configured
		logger = Logger.getLogger(projectName);
		if (logger.getHandlers().length ==0) {
			try {
				if (projectloggingFile == null) {
					throw new Exception();
				}
				
				// Check log properties in project if not found read the default file in wade  
				String loggingFileName = projectHome+File.separator+projectloggingFile;
				if (!(new File(loggingFileName)).exists()) {
					loggingFileName = wadeHome+File.separator+projectloggingFile;
				}
				
				// Read project logging configuration
				Properties loggingConfig = BootDaemon.loadProperties(loggingFileName);
				
				if (loggingConfig != null) {
					
					// Check WadeHadler presence
					ConsoleHandler consoleHandler;
					RollingFileHandler rollingFileHandler;
					if (loggingConfig.getProperty("handlers", "").indexOf(WadeHandler.class.getName()) >= 0) {
						// Add handlers (ConsoleHandler and RollingFileHandler)
						// configured as described in WadeHandler of log.properties
						// replace formatter with WadeFormatter in short mode (DATE TIME MSG)
						WadeHandler projectContainersHandler = new WadeHandler(projectHome, loggingConfig);
						consoleHandler = projectContainersHandler.createConsoleHandler();
						rollingFileHandler = projectContainersHandler.createRollingFileHandler("containers");
						
					} else {
						consoleHandler = WadeHandler.createDefaultConsoleHandler();
						rollingFileHandler = WadeHandler.createDefaultRollingFileHandler("containers", projectHome);
					}
					
					consoleHandler.setFormatter(new WadeFormatter(true));
					logger.addHandler(consoleHandler);

					rollingFileHandler.setFormatter(new WadeFormatter(true));
					logger.addHandler(rollingFileHandler);
					
					logger.setUseParentHandlers(false);
				}
			} catch(Exception e) {
				// Error reading logger configuration file
				// Use default ConsoleHandler
				logger.addHandler(new ConsoleHandler());
				logger.setUseParentHandlers(false);
				logger.log(Level.WARNING, "Error reading logger configuration file "+projectloggingFile);
			}
		}
	}
	
	public void setFilter(OutputFilter outputFilter) {
		this.outputFilter = outputFilter;
	}
	
	public void handleOutput(String source, String msg) {
		// The logging level is not important as it is not considered by the WadeFormatter
		logger.log(Level.SEVERE, source+">> "+msg);
		
		// If preset call the specific OutputFilter to manage the message 
		if (outputFilter != null) {
			try {
				outputFilter.filter(msg);
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Error handling output-filter", e);
			}				
		}
	}

	public void handleTermination(int exitValue) {
		synchronized (projectOutputHandlers) {
			// Remove the current OutputHandler to project map
			List<OutputHandler> outputHandlers = projectOutputHandlers.get(projectName);
			outputHandlers.remove(this);
			
			// If all OutputHandler are removed delete the project entry in the map and
			// delete all handlers of the associated logger
			if (outputHandlers.size() == 0) {
				projectOutputHandlers.remove(projectName);
				
				for (Handler h : logger.getHandlers()) {
					h.close();
					logger.removeHandler(h);
				}
			}
		}
	}
}
