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
package com.tilab.wade.cfa;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import test.common.remote.RemoteManager;

import com.tilab.wade.cfa.beans.ContainerInfo;
import com.tilab.wade.cfa.beans.ContainerProfileInfo;
import com.tilab.wade.cfa.beans.HostInfo;
import com.tilab.wade.cfa.beans.PlatformInfo;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.utils.UriUtils;

class PlatformStarter extends ConfigurationStarter {
	
	// FSM state names
	private static final String CHECK_HOST_AVAILABILITY_STATE = "__Check_Host_Availability__";
	private static final String SET_NEXT_CONTAINER_STATE = "__Set_Next_Container__";	
	private static final String MANAGE_CONTAINER_STATE = "__Manage_Container__";	
	private static final String CHECK_HOST_STATUS_STATE = "__Check_Host_Status__";		
	private static final String ERROR_STATE = "__Error__";
	private static final String SET_NEXT_BACKUP_MAIN_STATE = "__Set_Next_Backup_Main__";
	private static final String START_BACKUP_MAIN_STATE = "__Start_Backup_Main__";

	// FSM state extit values
	private static final int HOST_AVAILABLE = 0;
	private static final int HOST_NOT_AVAILABLE = 1;
	private static final int ALL_CONTAINERS_STARTED = 0;
	private static final int NOT_ALL_CONTAINERS_STARTED = 1;
	private static final int ALL_BACKUP_MAIN_STARTED = 2;
	private static final int NOT_ALL_BACKUP_MAIN_STARTED = 3;

	
	public PlatformStarter(ConfigurationAgent cfa, PlatformInfo platformInfo) {
		super(cfa, platformInfo);
	}

	@Override
	protected Behaviour getHostManager() {
		ParallelBehaviour hostsManager = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);

		// Save container profiles for later use in ContainerStarter
		Collection<ContainerProfileInfo> profiles = platformInfo.getContainerProfiles();

		// Number of hosts where it is possible to create a backup main (if necessary)
		int backupAllowedCounter=0;
		for (Iterator<HostInfo> it = platformInfo.getHosts().iterator(); it.hasNext(); ) {
			HostInfo host = it.next();
			if (host.isBackupAllowed()) {
				backupAllowedCounter++;
			}
		}

		// Number of backup main containers to be created in total
		int totalBackupsNumber = 0;
		if (platformInfo.getBackupsNumber() > 0){
			if (!((ConfigurationAgent)myAgent).getMainReplication()){
				logger.log(Logger.SEVERE, "PlatformStarter:: No backup main containers will be launched because MainReplication service is not active");
			}else {
				totalBackupsNumber = platformInfo.getBackupsNumber();
				if (backupAllowedCounter == 0){	
					logger.log(Logger.SEVERE, "PlatformStarter:: No backup main containers will be launched because there aren't any available hosts");
				}
			}
		}
		int backupsPerHost = (backupAllowedCounter == 0) ? 0 : (totalBackupsNumber / backupAllowedCounter);
		int remainingBackups = (backupAllowedCounter == 0 || totalBackupsNumber == 0) ? 0 : (totalBackupsNumber % backupAllowedCounter);
		
		HostInfo mainHostInfo = null;
		String localHostname = UriUtils.getLocalCanonicalHostname();

		Iterator hosts = platformInfo.getHosts().iterator();
		while (hosts.hasNext()) {	
			HostInfo h = (HostInfo) hosts.next();
			int backups = 0;
			if (UriUtils.compareHostNames(localHostname, h.getName())) {
				// h is the host where the Main Container is running
				mainHostInfo = h;
			}
			else {
				// h is NOT the host where the Main Container is running
				if (h.isBackupAllowed()) {
					backups = backupsPerHost;
					if (remainingBackups > 0) {
						remainingBackups--;
						backups++;
					}
				}
				HostManager hm = new HostManager(h, backups, profiles, this);
				hostsManager.addSubBehaviour(hm);
			}
		}
		
		// Manage the host where the Main Container is running at the end so that 
		// backup main containers are created preferentially on other hosts
		if (mainHostInfo != null) {
			int backups = 0;
			if (mainHostInfo.isBackupAllowed()){
				backups = backupsPerHost;
				if (remainingBackups > 0) {
					remainingBackups--;
					backups++;
				}
			}
			HostManager hm = new HostManager(mainHostInfo, backups, profiles, this);
			hostsManager.addSubBehaviour(hm);
		}
		
		return hostsManager;
	}

	@Override
	public int onEnd() {
		int code = super.onEnd();
		
		Object result = getResult();
		if (getExitCode() == ConfigurationOntology.OK) {
			if (containsWarnings((PlatformInfo) result)) {
				logger.log(Logger.WARNING, "Platform startup completed with warnings:\n" + result);

				cfa.setPlatformStatus(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS, result);
			} else {
				cfa.setPlatformStatus(ConfigurationOntology.ACTIVE_STATUS, ConfigurationAgent.NO_DETAIL);
			}
		} else {
			cfa.setPlatformStatus(ConfigurationOntology.ERROR_STATUS, result);
		}

		return code;
	}
	
	
	/**
	 * Inner class HostManager
	 * This behaviour manages all startup operations related to a given host, i.e.
	 * - Checking that the host is reachable and a BootDaemon is running on it
	 * - Starting all containers in it.
	 */
	private class HostManager extends FSMBehaviour {	
		private static final long serialVersionUID = 11111111L;
		private HostInfo host;
		private Iterator containers;
		private Iterator backupMains;

		int hostAvailabilityTimeout;

		public HostManager(HostInfo host, int backups, Collection<ContainerProfileInfo> profiles, PlatformStarter ps) {
			this.host = host;

			hostAvailabilityTimeout = Integer.parseInt(cfa.getProperty(ConfigurationAgent.HOST_REACHABLE_TIMEOUT_KEY, ConfigurationAgent.HOST_REACHABLE_TIMEOUT_DEFAULT));

			containers = host.getContainers().iterator();

			List bcl = new ArrayList();
			ContainerInfo ci;
			for (int i = 0; i < backups; i++) {
				ci = new ContainerInfo();
				bcl.add(ci);
			}
			
			backupMains = bcl.iterator();
			DataStore ds = getDataStore();

			Behaviour b = new HostAvailabilityChecker();
			b.setDataStore(ds);
			registerFirstState(b, CHECK_HOST_AVAILABILITY_STATE); 

			b = new NextContainerSetter();
			b.setDataStore(ds);
			registerState(b, SET_NEXT_CONTAINER_STATE);

			b = new ContainerManager(host.getName(), profiles, ps);
			b.setDataStore(ds);
			registerState(b, MANAGE_CONTAINER_STATE);

			b = new SetNextBackupMain();
			b.setDataStore(ds);
			registerState(b, SET_NEXT_BACKUP_MAIN_STATE);

			b = new BackupMainManager(host);
			b.setDataStore(ds);
			registerState(b, START_BACKUP_MAIN_STATE);

			registerLastState(new HostStatusChecker(), CHECK_HOST_STATUS_STATE);
			registerLastState(new Error(), ERROR_STATE);

			registerTransition(CHECK_HOST_AVAILABILITY_STATE, SET_NEXT_CONTAINER_STATE, HOST_AVAILABLE);
			registerDefaultTransition(CHECK_HOST_AVAILABILITY_STATE, ERROR_STATE);

			registerTransition(SET_NEXT_CONTAINER_STATE, MANAGE_CONTAINER_STATE, NOT_ALL_CONTAINERS_STARTED);
			registerDefaultTransition(MANAGE_CONTAINER_STATE, SET_NEXT_CONTAINER_STATE, new String[] {SET_NEXT_CONTAINER_STATE, MANAGE_CONTAINER_STATE});

			registerDefaultTransition(SET_NEXT_CONTAINER_STATE, SET_NEXT_BACKUP_MAIN_STATE);

			registerTransition(SET_NEXT_BACKUP_MAIN_STATE, START_BACKUP_MAIN_STATE, NOT_ALL_BACKUP_MAIN_STARTED);
			registerDefaultTransition(START_BACKUP_MAIN_STATE, SET_NEXT_BACKUP_MAIN_STATE, new String[] {SET_NEXT_BACKUP_MAIN_STATE, START_BACKUP_MAIN_STATE});
			registerDefaultTransition(SET_NEXT_BACKUP_MAIN_STATE, CHECK_HOST_STATUS_STATE);
		}

		/**
		 * Inner class HostAvailabilityChecker.
		 * This behaviour is responsible for checking if a host exists and the Boot Daemon is running on it.
		 * Moreover it initializes the RemoteManager to interact with that Boot Daemon
		 */
		private class HostAvailabilityChecker extends OneShotBehaviour {
			private static final long serialVersionUID = 11111112L;
			
			public void action(){
				RemoteManager rm = ((ConfigurationAgent) myAgent).checkHostAvailability(host, hostAvailabilityTimeout);
				getDataStore().put(ContainerManager.HOST_REMOTE_MANAGER_KEY, rm);
			}
			
			public int onEnd() {
				return (getDataStore().get(ContainerManager.HOST_REMOTE_MANAGER_KEY) != null ? HOST_AVAILABLE : HOST_NOT_AVAILABLE);
			}
		} // END of inner class HostAvailabilityChecker
		
		
		/**
		 * Inner class NextContainerSetter.
		 * This behaviour is responsible for setting the next container to be started
		 */
		private class NextContainerSetter extends OneShotBehaviour {
			private static final long serialVersionUID = 11111113L;
			
			public void action(){
				getDataStore().put(ContainerManager.CONTAINER_INFO_KEY, containers.hasNext() ? containers.next() : null);
			}
			
			public int onEnd() {
				return (getDataStore().get(ContainerManager.CONTAINER_INFO_KEY) != null ? NOT_ALL_CONTAINERS_STARTED : ALL_CONTAINERS_STARTED);
			}
		} // END of inner class NextContainerSetter

		private class SetNextBackupMain extends OneShotBehaviour {
			private static final long serialVersionUID = 111111135L;
			private int howMany = 0;
			
			public void action(){
				howMany++;
				DataStore dataStore = getDataStore();
				ContainerInfo ci = (ContainerInfo)(backupMains.hasNext() ? backupMains.next() : null);
				dataStore.put(ContainerManager.CONTAINER_INFO_KEY, ci);
			}
			
			public int onEnd() {
				int result = (getDataStore().get(ContainerManager.CONTAINER_INFO_KEY) != null ? NOT_ALL_BACKUP_MAIN_STARTED : ALL_BACKUP_MAIN_STARTED);
				if (result == ALL_BACKUP_MAIN_STARTED) {
					logger.log(Logger.FINE, "PlatformStarter::SetNextBackupMain::onEnd(): done starting "+howMany+" backup mains");
					backupMains = null;
				}
				return result;
			}
		} // END of inner class SetNextBackupMain


		/**
		 * Inner class HostStatusChecker
		 * This behaviour is responsible for cleaning the warnings related to all containers in the managed host
		 */
		private class HostStatusChecker extends OneShotBehaviour {
			private static final long serialVersionUID = 11111114L;
			
			public void action() {
				// Remove from host all containers started correctly. 
				Iterator it = host.getContainers().iterator();
				while (it.hasNext()) {
					ContainerInfo container = (ContainerInfo) it.next();
					if (container.getErrorCode() == null) {
						if (container.getAgents().size() == 0) {
							it.remove();
						}
					}
					else {
						// This container did not started correctly --> Clear all its agents
						container.getAgents().clear();
					}
				}
			}
		} // END of inner class HostStatusChecker
		
		/**
		 * Inner class Error.
		 * This behaviour is just a placeholder to make the HostManager behaviour abort.
		 */
		private class Error extends OneShotBehaviour {
			private static final long serialVersionUID = 11111115L;
			
			public void action() {
			}
		} // END of inner class Error
		
	} // END of inner class HostManager
	
}