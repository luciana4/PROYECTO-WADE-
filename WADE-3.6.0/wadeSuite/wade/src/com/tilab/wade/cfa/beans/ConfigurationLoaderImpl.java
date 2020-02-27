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
package com.tilab.wade.cfa.beans;

import jade.core.Agent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.tilab.wade.cfa.ConfigurationAgent;
import com.tilab.wade.cfa.GroupManager;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;

public class ConfigurationLoaderImpl implements ConfigurationLoader {
	
	private static final String TARGET_CONFIGURATION_NAME = "_target";
	
	private PlatformLoaderXML platformLoaderXML;
	
	public void init(Agent cfa) throws ConfigurationLoaderException {
		Map properties = TypeManager.getInstance().getProperties(TypeManager.getInstance().getType(WadeAgent.CONFIGURATION_AGENT_TYPE));
		String xmlCnfPath = TypeManager.getString(properties, CONFIGURATIONS_PATH, CONFIGURATIONS_PATH_DEFAULT);
		this.platformLoaderXML = new PlatformLoaderXML(xmlCnfPath, CONFIGURATIONS_RULES);
	}
	
	/**
	 * Returns the names of the available configurations. 
	 */
	public Collection<String> listConfigurations() throws ConfigurationLoaderException {
		try {
			Collection<String> confs = platformLoaderXML.getConfigurations();
			confs.remove(TARGET_CONFIGURATION_NAME);
			return confs;
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}
	
	/**
	 * Load the target configuration
	 */
	public PlatformInfo loadConfiguration() throws ConfigurationLoaderException {
		
		
		PlatformInfo pi;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		
		return pi;
	}
	
	public void storeConfiguration(PlatformInfo platformInfo) throws ConfigurationLoaderException {
		if (platformInfo == null) {
			throw new ConfigurationLoaderException("Platform info not inizialized");
		}
		
		try {
			platformLoaderXML.storeConfiguration(TARGET_CONFIGURATION_NAME, platformInfo, true);
		} catch(PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}		
	}
		
	/**
	 * Retrieve the list of hosts specified in the target configuration.
	 */
	public Collection<HostInfo> getHosts() throws ConfigurationLoaderException {
		Collection<HostInfo> hosts;
		PlatformInfo pi = null;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
			hosts = pi.getHosts();
			
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		return hosts;
	}
	
	/**
	 * Retrieve the list of main container agents specified in the target configuration.
	 */
    public Collection<AgentInfo> getMainAgents() throws ConfigurationLoaderException {
		Collection<AgentInfo> mainAgents;
		PlatformInfo pi = null;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
			mainAgents = pi.getMainAgents();
			
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		return mainAgents;
    }
	
	/**
	 * Retrieve the list of agent-pools specified in the target configuration.
	 */
    public Collection<AgentPoolInfo> getAgentPools() throws ConfigurationLoaderException {
		Collection<AgentPoolInfo> agentPools;
		PlatformInfo pi = null;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
			agentPools = pi.getAgentPools();
			
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		return agentPools;
    }
	
	/**
	 * Retrieve the list of container-profiles specified in the target configuration.
	 */
	public Collection<ContainerProfileInfo> getContainerProfiles() throws ConfigurationLoaderException {
		Collection<ContainerProfileInfo> containerProfiles = null;
		PlatformInfo pi = null;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
			containerProfiles = pi.getContainerProfiles();
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		return containerProfiles;
	}
	
	/**
	 * Import a given configuration as the target configuration
	 */
	public void importConfiguration(String confName) throws ConfigurationLoaderException {
		
		if (confName == null) {
			throw new ConfigurationLoaderException("Configuration name not specified");
		}
		
		PlatformInfo pi;
		try {
			pi = platformLoaderXML.loadConfiguration(confName);
			
			if(pi==null){
				throw new ConfigurationLoaderException("Error loading configuration: "+confName);
			}
			
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		
		try {
			platformLoaderXML.storeConfiguration(TARGET_CONFIGURATION_NAME, pi, true);
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}
	
	/**
	 * Export the target configuration with a given name and description
	 */
	public void exportConfiguration(String confName, String description, boolean overwrite) throws ConfigurationLoaderException {
		
		if (confName == null) {
			throw new ConfigurationLoaderException("Configuration name not specified");
		}
		
		PlatformInfo pi;
		try {
			pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
			
			if(pi==null){
				throw new ConfigurationLoaderException("Error loading current configuration: no platform present");
			}
			
			/* Setta il nome */
			pi.setName(confName);
			
			/* Setta la descrizione */
			if (description != null)
				pi.setDescription(description);
			
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
		
		try {
			/* Salvo configurazione su XML */
			platformLoaderXML.storeConfiguration(confName,pi,overwrite);
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}
	
	/**
	 * Export the running configuration with a given name and description
	 */
	public void exportRunningConfiguration(PlatformInfo pi, String confName, String description, boolean overwrite) throws ConfigurationLoaderException {
		
		/* Controllo correttezza nome */
		if (confName == null) {
			throw new ConfigurationLoaderException("Configuration name not inizialized");
		}
		
		/* Controllo PlatformInfo */
		if (pi == null) {
			throw new ConfigurationLoaderException("Platform info not inizialized");
		}
		
		/* Setta descrizione */
		if (description != null)
			pi.setDescription(description);
		
		/* Salvo configurazione su XML */
		try {
			platformLoaderXML.storeConfiguration(confName, pi, overwrite);
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}
		
	/**
	 * Delete a xml configuration
	 */
	public void deleteConfiguration(String confName) throws ConfigurationLoaderException {
		
		/* Controllo correttezza nome */
		if (confName == null) {
			throw new ConfigurationLoaderException("Configuration name not inizialized");
		}
		
		/* Cancello la configurazione */
		try {
			platformLoaderXML.deleteConfiguration(confName);
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}

    /**
     * Retrieve the list of groups included into the configuration.
     */
	public Collection<String> getGroups() throws ConfigurationLoaderException {
		try {
			if (platformLoaderXML.existConfiguration(TARGET_CONFIGURATION_NAME)) {
				PlatformInfo pi = platformLoaderXML.loadConfiguration(TARGET_CONFIGURATION_NAME);
				return GroupManager.getGroups(pi);
			}
			else {
				// No target configuration -> no groups 
				return new ArrayList<String>();
			}
		} catch (PlatformLoaderException e) {
			throw new ConfigurationLoaderException(e);
		}
	}
}
