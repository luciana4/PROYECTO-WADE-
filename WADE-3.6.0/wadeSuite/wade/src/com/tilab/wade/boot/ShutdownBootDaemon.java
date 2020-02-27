package com.tilab.wade.boot;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

import test.common.remote.TSDaemon;

import jade.cli.CLIManager;

/**
 * Command to shutdown a running BootDaemon via RMI.
 * All parameters can be null.
 */
public class ShutdownBootDaemon {

	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String NAME = "name";
	
	public ShutdownBootDaemon(String hostName, String bootPort, String bootName) throws Exception {
		if (hostName == null) {
			hostName = "localhost";
		}
		if (bootPort == null) {
			bootPort = Integer.toString(TSDaemon.DEFAULT_PORT);
		}
		if (bootName == null) {
			bootName = TSDaemon.DEFAULT_NAME;
		}

		RemoteManagerEx rme;
		String remoteManagerRMI = "rmi://"+hostName+":"+bootPort+"//"+bootName;
		try {
			rme = (RemoteManagerEx) Naming.lookup(remoteManagerRMI);
		}
		catch (Exception e) {
			throw new Exception("Error looking up remote manager "+remoteManagerRMI, e);
		}
		
		try {
			rme.exit();
		} catch(RemoteException rm) {
			// Normal exception, boot-daemon is death!
		}
	}
	
	public static void main(String[] args) {

		// Parse arguments:
		// -host <host-name>
		// -port <boot-daemon-port>
		// -name <boot-daemon-name>
		Properties properties = CLIManager.parseCommandLine(args);
		String hostName = properties.getProperty(HOST);
		String bootPort = properties.getProperty(PORT);
		String bootName = properties.getProperty(NAME);
		
		// Shutdown BootDaemon via rmi
		try {
			new ShutdownBootDaemon(hostName, bootPort, bootName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
