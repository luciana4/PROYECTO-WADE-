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
import java.util.Collection;

public interface ConfigurationLoader {

	public static final String CONFIGURATIONS_PATH = "configurationsPath";
	public static final String CONFIGURATIONS_PATH_DEFAULT = "cfg"+File.separator+"configuration";
	public static final String CONFIGURATIONS_RULES = File.separator+"platform-rules.xml";

    /**
     * Initialize the ConfigurationLoader by setting the cfaAgentProperties 
     */
	void init(Agent confAgent) throws ConfigurationLoaderException;
	
    /**
     * Returns the names of the .xml files included in the CFA
     * export directory.
     */
	Collection<String> listConfigurations() throws ConfigurationLoaderException;

    /**
     * Load the configuration from the DB.
     * This is used by the CFA to serve the StartupPlatform
     */
    PlatformInfo loadConfiguration() throws ConfigurationLoaderException;

    /**
     * Store a given configuration in the DB.
     * This is used by the CFA to serve the SaveConfiguration action
     */
	void storeConfiguration(PlatformInfo platformInfo) throws ConfigurationLoaderException;
    
    /**
     * Retrieve the list of hosts included into the DB configuration.
     * This method is provided to avoid checking all RP-device consistency
     * when only the list of hosts is needed.
     */
    Collection<HostInfo> getHosts() throws ConfigurationLoaderException;

    /**
     * Retrieve the list of main container agents included into the configuration.
     * This is used by the CFA to serve the GetMainAgents action
     */
    Collection<AgentInfo> getMainAgents() throws ConfigurationLoaderException;

    /**
     * Retrieve the list of agent pool included into the configuration.
     * This is used by the CFA to serve the GetAgentPools action
     */
    Collection<AgentPoolInfo> getAgentPools() throws ConfigurationLoaderException;
    
    /**
     * Retrieve the list of containerProfiles included into the DB configuration.
     * This method is provided in order to reconstruct the configuration
     *
     */
    Collection<ContainerProfileInfo> getContainerProfiles() throws ConfigurationLoaderException;

    /**
     * Import a platform configuration stored in the given XML file into the DB.
     * Network tables in the DB are never considered in this operation.
     * The CFA will perform this operation only when the WANTS platform is down.
     */
    void importConfiguration(String confName) throws ConfigurationLoaderException;

    /**
     * Export the platform configuration stored in the DB in a given XML file.
     * Network tables in the DB are never considered in this operation.
     */
    void exportConfiguration(String confName, String description, boolean overwrite) throws ConfigurationLoaderException;

    /**
     * Store a given configuration in the given XML file.
     * This is used by the CFA to serve the ExportRunningConfiguration action
     */
	void exportRunningConfiguration(PlatformInfo info, String confName, String description, boolean overwrite) throws ConfigurationLoaderException;
    
    /**
     * Delete a xml configuration
     */
    void deleteConfiguration(String confName) throws ConfigurationLoaderException;
    
    /**
     * Retrieve the list of group included into the configuration.
     */
    Collection<String> getGroups() throws ConfigurationLoaderException;
    
}
