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
package com.tilab.wade.cfa.ontology;

import jade.content.onto.*;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;

import com.tilab.wade.cfa.beans.*;

/**
 * The ontology defining concepts, predicates and actions used by WANTS agents to 
 * "talk about" platform elements such as hosts, containers, agents, agent arguments and container profiles
 * 
 * @author max
 *
 */
public class ConfigurationOntology extends Ontology implements ConfigurationVocabulary{	
    /**
     * The singleton instance of this ontology
     */
    private final static Ontology theInstance = new ConfigurationOntology();
    public static final String STARTING_STATUS = "starting-status";
    public static final String ACTIVE_STATUS = "active-status";
    public static final String SHUTDOWN_IN_PROGRESS = "shutdown-in_progress-status";
	public static final String DOWN_STATUS = "down-status";
	public static final String ACTIVE_WITH_WARNINGS_STATUS = "active-with-warnings";	
	public static final String ERROR_STATUS = "error-status";
	public static final String ACTIVE_INCOMPLETE_STATUS = "active-incomplete-status";
	
	public static final int OK = 0;
	public static final int KO = 1;
    
    public final static Ontology getInstance() {
        return theInstance;
    }
    
    /**
     * Constructor
     */
    private ConfigurationOntology() {
        super(ONTOLOGY_NAME, new Ontology[]{BasicOntology.getInstance(), SerializableOntology.getInstance()}, new CFReflectiveIntrospector());
        
        try {        	
        	// Concepts
        	createAllConcepts();
        	defineAllConcepts();            
            
            // Actions
        	createAllActions();            
        }
        catch (OntologyException oe) {
            oe.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    // Creation of all ConfigurationOntology actions
    private void createAllActions() throws OntologyException {
    	add(new AgentActionSchema(STARTUP_PLATFORM), StartupPlatform.class);            
        add(new AgentActionSchema(SHUTDOWN_PLATFORM), ShutdownPlatform.class);
        add(new AgentActionSchema(SAVE_CONFIGURATION), SaveConfiguration.class);	
        add(new AgentActionSchema(IMPORT_CONFIGURATION), ImportConfiguration.class);
        add(new AgentActionSchema(EXPORT_CONFIGURATION), ExportConfiguration.class);
        add(new AgentActionSchema(REMOVE_CONFIGURATION), RemoveConfiguration.class);
        add(new AgentActionSchema(COMPARE_RUNNING_WITH_TARGET_CONFIGURATION), CompareRunningWithTargetConfiguration.class);
        add(new AgentActionSchema(GET_CONFIGURATIONS), GetConfigurations.class);
        add(new AgentActionSchema(GET_PLATFORM_STATUS), GetPlatformStatus.class);
        add(new AgentActionSchema(GET_STATUS_DETAIL), GetStatusDetail.class);
        add(new AgentActionSchema(GET_HOSTS), GetHosts.class);
        add(new AgentActionSchema(GET_MAIN_AGENTS), GetMainAgents.class);
        add(new AgentActionSchema(GET_AGENT_POOLS), GetAgentPools.class);
        add(new AgentActionSchema(GET_CONTAINER_PROFILES), GetContainerProfiles.class);
        add(new AgentActionSchema(START_CONTAINER), StartContainer.class);
        add(new AgentActionSchema(START_AGENT), StartAgent.class);
        add(new AgentActionSchema(START_BACKUP_MAIN_CONTAINER), StartBackupMainContainer.class);
        add(new AgentActionSchema(KILL_CONTAINER), KillContainer.class);
        add(new AgentActionSchema(RESET_ERROR_STATUS), ResetErrorStatus.class);
        add(new AgentActionSchema(ADD_HOST), AddHost.class);
        add(new AgentActionSchema(REMOVE_HOST), RemoveHost.class);
        add(new AgentActionSchema(GET_VERSIONS_INFO), GetVersionsInfo.class);
    	add(new AgentActionSchema(STARTUP_GROUP), StartupGroup.class);            
        add(new AgentActionSchema(SHUTDOWN_GROUP), ShutdownGroup.class);
        add(new AgentActionSchema(GET_GROUPS_STATUS), GetGroupsStatus.class);
        
        // Actions that need parameter
        AgentActionSchema as = (AgentActionSchema) getSchema(IMPORT_CONFIGURATION);
        as.add(IMPORT_CONFIGURATION_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
        
        as = (AgentActionSchema) getSchema(EXPORT_CONFIGURATION);
        as.add(EXPORT_CONFIGURATION_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
        as.add(EXPORT_CONFIGURATION_DESCRIPTION, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(EXPORT_CONFIGURATION_OVERRIDE, (PrimitiveSchema)getSchema(BasicOntology.BOOLEAN));
        
        as = (AgentActionSchema) getSchema(REMOVE_CONFIGURATION);
        as.add(REMOVE_CONFIGURATION_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
        
        as = (AgentActionSchema) getSchema(START_CONTAINER);
        as.add(START_CONTAINER_PROJECT_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(START_CONTAINER_CONTAINER_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
        as.add(START_CONTAINER_JAVA_PROFILE, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(START_CONTAINER_JADE_PROFILE, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(START_CONTAINER_JADE_ADDITIONAL_ARGS, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);	
        as.add(START_CONTAINER_HOST_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(START_CONTAINER_AGENTS, (ConceptSchema) getSchema(AGENT), 0, ObjectSchema.UNLIMITED);

        as = (AgentActionSchema) getSchema(START_AGENT);
        as.add(START_AGENT_AGENT, (ConceptSchema) getSchema(AGENT));
        as.add(START_AGENT_CONTAINER_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        
        as = (AgentActionSchema) getSchema(START_BACKUP_MAIN_CONTAINER);
        as.add(START_BACKUP_MAIN_CONTAINER_CONTAINER_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        as.add(START_BACKUP_MAIN_CONTAINER_HOST_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        
        
        as = (AgentActionSchema) getSchema(KILL_CONTAINER);
        as.add(KILL_CONTAINER_CONTAINER_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
        as.add(KILL_CONTAINER_HARD_TERMINATION, (PrimitiveSchema)getSchema(BasicOntology.BOOLEAN));

        as = (AgentActionSchema) getSchema(SHUTDOWN_PLATFORM);
        as.add(SHUTDOWN_PLATFORM_HARD_TERMINATION, (PrimitiveSchema)getSchema(BasicOntology.BOOLEAN));

        as = (AgentActionSchema) getSchema(ADD_HOST);
        as.add(ADD_HOST_INFO, (ConceptSchema)getSchema(HOST));  

        as = (AgentActionSchema) getSchema(REMOVE_HOST);
        as.add(REMOVE_HOST_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));

        as = (AgentActionSchema) getSchema(STARTUP_GROUP);
        as.add(GROUP_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));

        as = (AgentActionSchema) getSchema(SHUTDOWN_GROUP);
        as.add(GROUP_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
	}

    
    // Creation of all ConfigurationOntology Concepts 
	private void createAllConcepts() throws OntologyException {
    	add(new ConceptSchema(PLATFORM_ELEMENT), PlatformElement.class);       
    	add(new ConceptSchema(PLATFORM), PlatformInfo.class);       
        add(new ConceptSchema(HOST), HostInfo.class);
        add(new ConceptSchema(CONTAINER), ContainerInfo.class);
        add(new ConceptSchema(AGENT_BASE), AgentBaseInfo.class);
        add(new ConceptSchema(AGENT), AgentInfo.class);
        add(new ConceptSchema(AGENT_POOL), AgentPoolInfo.class);
        add(new ConceptSchema(VIRTUAL_AGENT), VirtualAgentInfo.class);
        add(new ConceptSchema(AGENT_ARGUMENT), AgentArgumentInfo.class);
        add(new ConceptSchema(CONTAINER_PROFILE), ContainerProfileInfo.class);
        add(new ConceptSchema(CONTAINER_PROFILE_PROPERTY), ContainerProfilePropertyInfo.class);
        add(new ConceptSchema(VERSIONS), VersionsInfo.class);
        add(new ConceptSchema(GROUP_STATUS), GroupStatus.class);
	}

	// Define and specialize all concepts
    private void defineAllConcepts() throws OntologyException {
    	definePlatformElementConcept();
    	definePlatformConcept();
        defineHostConcept();
        defineContainerConcept();
        defineAgentBaseConcept();
        defineAgentConcept();            
        defineAgentPoolConcept();            
        defineVirtualAgentConcept();            
        defineAgentArgumentConcept();
        defineContainerProfileConcept();            
        defineContainerProfileProperty();
        defineVersionsConcept();
        defineGroupStatusConcept();
	}

	//  Platform conceptSchema specialization
    private void definePlatformElementConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(PLATFORM_ELEMENT);
        conceptSchema.add(PLATFORM_ELEMENT_ERROR_CODE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL); 
	}
    
	//  Platform conceptSchema specialization
    private void definePlatformConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(PLATFORM);
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(PLATFORM_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));            
        conceptSchema.add(PLATFORM_DESCRIPTION, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(BACKUP_NUMBER, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
        conceptSchema.add(MAIN_AGENTS, (ConceptSchema) getSchema(AGENT), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
        conceptSchema.add(HOSTS, (ConceptSchema) getSchema(HOST), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
        conceptSchema.add(AGENT_POOLS, (ConceptSchema) getSchema(AGENT_POOL), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
        conceptSchema.add(CONTAINER_PROFILES, (ConceptSchema) getSchema(CONTAINER_PROFILE), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);		
	}

    // Host conceptSchema specialization
    private void defineHostConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(HOST);		 
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(HOST_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));            
        conceptSchema.add(HOST_IPADDRESS, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(HOST_AVAILABILITY, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN));
        conceptSchema.add(HOST_REACHABILITY, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN));
        conceptSchema.add(HOST_CPU_NUMBER, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(HOST_RAM, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(HOST_ADMIN_STATE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(HOST_BENCHMARK_SPEC_INT, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(HOST_BENCHMAR_WF, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);		
        conceptSchema.add(HOST_CONTAINERS, (ConceptSchema) getSchema(CONTAINER), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
        conceptSchema.add(HOST_BACKUP_ALLOWED, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
	}
    
    //  Container conceptSchema specialization    
    private void defineContainerConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(CONTAINER);        
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(CONTAINER_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(CONTAINER_PROJECT_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(CONTAINER_JAVA_PROFILE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(CONTAINER_JADE_PROFILE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(CONTAINER_JADE_ADDITIONAL_ARGS, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);	
        conceptSchema.add(CONTAINER_SPLIT, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);	
        conceptSchema.add(CONTAINER_AGENTS, (ConceptSchema) getSchema(AGENT), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
	}
    
    //  AgentBase conceptSchema specialization
    private void defineAgentBaseConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(AGENT_BASE);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(AGENT_BASE_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(AGENT_BASE_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(AGENT_BASE_CLASSNAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(AGENT_BASE_OWNER, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(AGENT_BASE_GROUP, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(AGENT_BASE_PARAMETERS, (ConceptSchema) getSchema(AGENT_ARGUMENT), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
	}

    //  Agent conceptSchema specialization
    private void defineAgentConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(AGENT);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(AGENT_BASE));
    }
    
    //  AgentPool conceptSchema specialization
    private void defineAgentPoolConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(AGENT_POOL);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(AGENT_BASE));
        conceptSchema.add(AGENT_POOL_SIZE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
	}

    //  VirtualAgent conceptSchema specialization
    private void defineVirtualAgentConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(VIRTUAL_AGENT);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(AGENT));
        conceptSchema.add(VIRTUAL_AGENT_NUMBER_OF_REPLICAS, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
        conceptSchema.add(VIRTUAL_AGENT_REPLICATION_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
	}

    //  AgentArgument conceptSchema specialization
    private void defineAgentArgumentConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(AGENT_ARGUMENT);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(AGENT_ARGUMENT_KEY, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(AGENT_ARGUMENT_VALUE, (PrimitiveSchema) getSchema(BasicOntology.STRING));		
	}

    //  ContainerProfile conceptSchema specialization    
    private void defineContainerProfileConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(CONTAINER_PROFILE);        
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(CONTAINER_PROFILE_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(CONTAINER_PROFILE_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);		
        conceptSchema.add(CONTAINER_PROFILE_PROPERTIES, (ConceptSchema) getSchema(CONTAINER_PROFILE_PROPERTY), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
	}    
 
    //  ContainerProfileProperty conceptSchema specialization
	private void defineContainerProfileProperty() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(CONTAINER_PROFILE_PROPERTY);		
		conceptSchema.addSuperSchema((ConceptSchema) getSchema(PLATFORM_ELEMENT));
        conceptSchema.add(CONTAINER_PROFILE_PROPERTY_KEY, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(CONTAINER_PROFILE_PROPERTY_VALUE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);		
	}
	
	//  Versions conceptSchema specialization
	private void defineVersionsConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(VERSIONS);		
        conceptSchema.add(VERSIONS_JADE_VERSION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_JADE_REVISION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_JADE_DATE, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_WADE_VERSION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_WADE_REVISION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_WADE_DATE, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_PROJECT_VERSION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_PROJECT_REVISION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_PROJECT_ORIGINAL_REVISION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
        conceptSchema.add(VERSIONS_PROJECT_DATE, (PrimitiveSchema) getSchema(BasicOntology.STRING));
	}
	
	//  GroupStatus conceptSchema specialization
    private void defineGroupStatusConcept() throws OntologyException {
		ConceptSchema conceptSchema = (ConceptSchema) getSchema(GROUP_STATUS);
        conceptSchema.add(GROUP_STATUS_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING)); 
        conceptSchema.add(GROUP_STATUS_STATUS, (PrimitiveSchema) getSchema(BasicOntology.STRING));
	}
}
