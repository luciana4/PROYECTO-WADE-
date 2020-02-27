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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class KillOnOoMFilter implements OutputFilter {

	private static jade.util.Logger logger = jade.util.Logger.getMyLogger(KillOnOoMFilter.class.getName());
	
	private int instanceId;
	private String instanceName;
	private BootDaemon bootDaemon;

	
	@Override
	public void init(String cfg, int instanceId, String instanceName, BootDaemon bootDaemon) throws Exception {
		logger.log(Level.INFO, "Activated KillOnOoMFilter on jade instance "+instanceName);
		
		this.instanceId = instanceId;
		this.instanceName = instanceName;
		this.bootDaemon = bootDaemon;
	}

	@Override
	public void filter(String message) {
		if (message != null && message.contains("OutOfMemoryError") && !message.contains("remote container")) {
			logger.log(Level.WARNING, "Detected OutOfMemoryError on jade instance "+instanceName+". Output line:\n"+message+"\n -> Activated container kill procedure...");
			
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						logger.log(Level.WARNING, "Kill jade instance "+instanceName);
						boolean success = killJadeInstance(instanceId, instanceName);
						if (success) {
							logger.log(Level.WARNING, "Jade instance "+instanceName+" successfully killed");
						}
						else {
							logger.log(Level.WARNING, "Jade instance "+instanceName+" kill command submitted");
						}
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error killing jade instance "+instanceName, e);
					}
				}
			};
			t.start();
		}
	}

	private boolean killJadeInstance(int instanceId, String instanceName) throws Exception {
		bootDaemon.killJadeInstance(instanceId);
		
		// Wait a bit
		try {
			Thread.sleep(5000);
		}
		catch (Exception e) {
		}

		// If the OS is NOT Windows check/kill process via script 
		if (System.getProperty("os.name").toLowerCase().indexOf("win") == -1) {
			try {
				logger.log(Level.INFO, "Check if jade instance "+instanceName+" is still alive");
				List<String> output = executeScript("."+File.separator+"bin"+File.separator+"getContainerPID.sh", instanceName, ".");
				if (!output.isEmpty()) {
					logger.log(Level.INFO, "jade instance "+instanceName+" (pid="+output.get(0)+") is still alive -> kill it");
					
					executeScript("."+File.separator+"bin"+File.separator+"killContainer.sh", instanceName, ".");					
				}
				else {
					logger.log(Level.INFO, "jade instance "+instanceName+" already terminated");
				}
				
				return true;
			}
			catch (Exception e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		return false;
	}
	
	private static List<String> executeScript(String scriptPath, String args, String workingPath) throws Exception {
		
		logger.log(Level.INFO, "Execute script="+scriptPath+", args="+args+", workingPath="+workingPath);
		
		// Check script and make it executable
		File scriptFile = new File(scriptPath);
		if (!scriptFile.exists()) {
			throw new IOException("Script "+scriptPath+" does not exist");
		}
		scriptFile.setExecutable(true);

		// Check working path
		File workingPathFile = null;
		if (workingPath != null) {
			workingPathFile = new File(workingPath);
			if (!workingPathFile.exists()) {
				throw new IOException("Working path "+workingPath+" does not exist");
			}
		}

		// Build command
		List<String> cmd = new ArrayList<String>();
		cmd.add(scriptFile.getPath());
		if (args != null) {
			String[] splitArgs = args.split(" ");
			for (String arg : splitArgs) {
				cmd.add(arg);
			}
		}

		Process process = null;
		List<String> output = new ArrayList<String>();
		try {
			// Build and execute the process 
			ProcessBuilder pb = new ProcessBuilder();
			pb.redirectErrorStream(true);
			pb.command(cmd);
			if (workingPathFile != null) {
				pb.directory(workingPathFile);
			}
			process = pb.start();
	
			// Get process output
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;  
			while ((line = br.readLine()) != null) {
				if (output != null) {
					output.add(line);
					logger.log(Level.INFO, "-- "+line);
				}
			}
	
			// Wait for the conclusion of the process
			process.waitFor();
	
		} catch (Exception e) {  
			if (process != null) {
				process.destroy();
				process = null;
			}
			throw new Exception("Error executing script "+scriptPath, e);
		} finally {
			if (process != null) {
				try {
					// Close all streams
					process.getInputStream().close();
					process.getOutputStream().close();
					process.getErrorStream().close();
	
					Thread.sleep(500);
				} catch(Exception e) {}
			}			
		}
		
		return output;
	}
}
