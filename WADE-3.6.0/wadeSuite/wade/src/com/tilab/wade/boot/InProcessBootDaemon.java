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

import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.ArrayList;
import jade.util.leap.List;
import jade.util.leap.Properties;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.tilab.wade.utils.logging.WadeHandler;

import test.common.TestException;
import test.common.remote.TSDaemon;

public class InProcessBootDaemon extends TSDaemon {
	
	private Hashtable containerControllers = new Hashtable();
	private int instanceCnt = 0;

	public InProcessBootDaemon() throws RemoteException {
		super();
	}
	
	public static void main(String args[]) {
		try {
			com.tilab.wade.Boot.setPreserveJavaTypes();
			
			// Manage Logging
			// FIXME: manage if possible the name of container
			Logger rootLogger = Logger.getLogger("");
			WadeHandler wadeHandler = null;
			Handler[] handlers = rootLogger.getHandlers();
			for (Handler h : handlers) {
				if (h instanceof WadeHandler) {
					wadeHandler = (WadeHandler)h;
				}
			}
			if (wadeHandler != null) {
				rootLogger.addHandler(wadeHandler.createConsoleHandler());
				rootLogger.addHandler(wadeHandler.createRollingFileHandler("containers"));
			} 

			// Start boot daemon
			InProcessBootDaemon daemon = new InProcessBootDaemon();
			daemon.start(args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void printWelcomeMessage(String port, String name) {
		System.out.println("In-process Boot Daemon ready on port: " + port + " using name: "+ name);
	}

	private String[] toArray(String cmdLineArgs) {
		StringTokenizer st = new StringTokenizer(cmdLineArgs, " \t\n");
		java.util.List l = new java.util.ArrayList(); 
		while (st.hasMoreTokens()) {
			l.add(st.nextToken());
		}
		return (String[]) l.toArray(new String[0]);
	}
	
	//////////////////////////////////////////
	// RemoteManager INTERFACE IMPLEMENTATION
	//////////////////////////////////////////
	public int launchJadeInstance(String instanceName, String classpath, String jvmArgs, String mainClass, String jadeArgs, String[] protoNames) throws TestException, RemoteException {
		instanceCnt++;
		try {
			System.out.println("Starting container with command line args = "+jadeArgs);
			String[] args = toArray(jadeArgs);
			Properties pp = Boot.parseCmdLineArgs(args);
			ProfileImpl p = new ProfileImpl(pp);
			ContainerController cc = null;
			if (p.getBooleanProperty(Profile.MAIN, false)) {
				cc = Runtime.instance().createMainContainer(p);
			}
			else {
				cc = Runtime.instance().createAgentContainer(p);
			}
			if (cc != null) {
				containerControllers.put(new Integer(instanceCnt), cc);
				return instanceCnt;
			}
			else {
				throw new TestException("JADE startup error");
			}
		}
		catch (IllegalArgumentException iae) {
			throw new TestException("Worng JADE startup arguments", iae);
		}
		catch (TestException te) {
			throw te;
		}
		catch (Exception e) {
			throw new TestException("Unexpected error", e);
		}
	}

	public List getJadeInstanceAddresses(int id) throws TestException, RemoteException {
		// FIXME: To be implemented
		return new ArrayList();
	}

	public String getJadeInstanceContainerName(int id) throws TestException, RemoteException {
		ContainerController cc = (ContainerController) containerControllers.get(new Integer(id));
		if (cc != null) {
			try {
				return cc.getContainerName();
			}
			catch (ControllerException ce) {
				throw new TestException("Invalid container", ce);
			}
		}
		else {
			throw new TestException("No JADE instance corresponding to ID "+id);
		}
	}

	public void killJadeInstance(int id) throws TestException, RemoteException {
		ContainerController cc = (ContainerController) containerControllers.remove(new Integer(id));
		if (cc != null) {
			try {
				cc.kill();
			}
			catch (ControllerException ce) {
				throw new TestException("Invalid container", ce);
			}
		}
		else {
			throw new TestException("No JADE instance corresponding to ID "+id);
		}
	}

	public int getJadeInstanceId(String containerName) throws TestException, RemoteException {
		synchronized (containerControllers) {
			Iterator it = containerControllers.keySet().iterator();
			while (it.hasNext()) {
				Integer id = (Integer) it.next();
				ContainerController cc = (ContainerController) containerControllers.get(id);
				try {
					if (cc.getContainerName().equalsIgnoreCase(containerName)) {
						return id.intValue();
					}
				}
				catch (ControllerException ce) {
					// Just print a stack trace and go on
					ce.printStackTrace();
				}
			}
			return -1;
		}
	}
}
