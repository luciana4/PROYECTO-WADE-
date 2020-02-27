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
package com.tilab.wade.utils.auxiliary;

import java.io.File;
import java.io.IOException;

import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.Specifier;
import jade.util.Logger;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.utils.CAUtils;
import com.tilab.wade.utils.FileUtils;

/**
 * @author Giovanni Caire
 */
public class StartAuxFE {
	private static Logger myLogger = Logger.getJADELogger(StartAuxFE.class.getName());
	
	public static void main(String[] args) {
		
		String cfgFileName = "cfg/auxIdl.xml";
		if (args != null && args.length > 0) {
			cfgFileName = args[0];
		}
		
		myLogger.log(Logger.INFO, "Parsing AUX FrontEnd configuration from file "+cfgFileName);
		try {
			String xmlConfiguration = FileUtils.getFileContent(new File(cfgFileName));
			AuxConfiguration cfg = AuxUtils.parse(xmlConfiguration);
			
			jade.util.leap.Properties pp = cfg.getJadeProperties();
			pp.setProperty(Profile.CONTAINER_NAME, "AUX-"+cfg.getName());
			myLogger.log(Logger.INFO, "Connecting to the platform with properties "+pp);
			MicroRuntime.startJADE(pp, new Runnable() {
				public void run() {
					System.exit(0);
				}
			});
			if (MicroRuntime.isRunning()) {
				myLogger.log(Logger.INFO, "Connecting to the platform with properties "+pp);
				for (AgentInfo info : cfg.getAgents()) {
					Specifier spc = CAUtils.agentInfoToSpecifier(info);
					myLogger.log(Logger.INFO, "Starting agent "+spc.getName()+" of class "+spc.getClassName());
					try {
						MicroRuntime.startAgent(spc.getName(), spc.getClassName(), spc.getArgs());
					} catch (Exception e) {
						myLogger.log(Logger.WARNING, "Error starting agent "+spc.getName()+" of class "+spc.getClassName(), e);
					}
				}
			}
			else {
				myLogger.log(Logger.SEVERE, "JADE Startup failed.");
			}
			
			myLogger.log(Logger.INFO, "AUX Front End startup completed");
		}
		catch (IOException ioe) {
			myLogger.log(Logger.SEVERE, "Cannot read Aux Front-End configuration file "+cfgFileName, ioe);
		}
	}

}
