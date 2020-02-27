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
package com.tilab.wade.performer.ontology;

//#MIDP_EXCLUDE_FILE

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.ReflectiveIntrospector;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;
import jade.content.schema.TermSchema;
import jade.domain.FIPAAgentManagement.FIPAManagementOntology;
import jade.domain.FIPAAgentManagement.FIPAManagementVocabulary;
import jade.domain.FIPAAgentManagement.Property;

import com.tilab.wade.event.EventOntology;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;


/**
 @author Giovanni Caire - TILAB
 */
public class WorkflowManagementOntology extends Ontology implements WorkflowManagementVocabulary {
	// The singleton instance of this ontology
	private final static Ontology theInstance = new WorkflowManagementOntology();
	
	public final static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Constructor
	 */
	private WorkflowManagementOntology() {
		super(ONTOLOGY_NAME, new Ontology[]{ErrorsOntology.getInstance(), EventOntology.getInstance()}, new ReflectiveIntrospector());
		
		try {
			add(new ConceptSchema(WORKFLOW), WorkflowDescriptor.class);
			add(new ConceptSchema(CONTROL_INFO), ControlInfo.class);
			add(new ConceptSchema(CONTROL_INFO_CHANGES), ControlInfoChanges.class);
			add(new ConceptSchema(PARAMETER), Parameter.class);
			add(new ConceptSchema(MODIFIER), Modifier.class);
			add(FIPAManagementOntology.getInstance().getSchema(FIPAManagementVocabulary.PROPERTY), Property.class);
			add(new ConceptSchema(EXECUTOR), ExecutorInfo.class);
			add(new ConceptSchema(SUBFLOW), SubflowInfo.class);
			
			add(new AgentActionSchema(EXECUTE_WORKFLOW), ExecuteWorkflow.class);
			add(new AgentActionSchema(THAW_WORKFLOW), ThawWorkflow.class);
			add(new AgentActionSchema(RECOVER_WORKFLOW), RecoverWorkflow.class);
			add(new AgentActionSchema(KILL_WORKFLOW), KillWorkflow.class);
			add(new AgentActionSchema(SET_CONTROL_INFO), SetControlInfo.class);
			add(new AgentActionSchema(UPDATE_CONTROL_INFO), UpdateControlInfo.class);
			add(new AgentActionSchema(GET_WRD), GetWRD.class);
			add(new AgentActionSchema(SET_WRD), SetWRD.class);
			add(new AgentActionSchema(GET_SESSION_STATUS), GetSessionStatus.class);
			add(new AgentActionSchema(RESET_CONTROL_INFOS), ResetControlInfos.class);
			add(new AgentActionSchema(RESET_MODIFIERS), ResetModifiers.class);
			add(new AgentActionSchema(GET_POOL_SIZE), GetPoolSize.class);
			
			add(new PredicateSchema(EXECUTION_ERROR), ExecutionError.class);
			add(new PredicateSchema(FROZEN), Frozen.class);
			
			// WORKFLOW
			ConceptSchema cs = (ConceptSchema) getSchema(WORKFLOW);
			cs.add(WORKFLOW_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);        
			cs.add(WORKFLOW_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);        
			cs.add(WORKFLOW_PACKAGES, (PrimitiveSchema) getSchema(BasicOntology.STRING), 0, ObjectSchema.UNLIMITED);
			cs.add(WORKFLOW_PARAMETERS, (ConceptSchema) getSchema(PARAMETER), 0, ObjectSchema.UNLIMITED);
			cs.add(WORKFLOW_EXECUTION, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_PRIORITY, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_TIMEOUT, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_TRANSACTIONAL, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_REQUESTER, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_SID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_DELEGATION_CHAIN, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_SESSION_STARTUP, (PrimitiveSchema) getSchema(BasicOntology.DATE), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_CLASSLOADER_IDENTIFIER, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_FORMAT, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(WORKFLOW_REPRESENTATION, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			
			// CONTROL_INFO
			cs = (ConceptSchema) getSchema(CONTROL_INFO);
			cs.add(CONTROL_INFO_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(CONTROL_INFO_SYNCH, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.MANDATORY);
			cs.add(CONTROL_INFO_VERBOSITY_LEVEL, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			cs.add(CONTROL_INFO_CONTROLLERS, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
			cs.add(CONTROL_INFO_SELF_CONFIG, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);

			// CONTROL_INFO_CHANGES
			cs = (ConceptSchema) getSchema(CONTROL_INFO_CHANGES);
			cs.add(CONTROL_INFO_CHANGES_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(CONTROL_INFO_CHANGES_SYNCH, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			cs.add(CONTROL_INFO_CHANGES_VERBOSITY_LEVEL, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(CONTROL_INFO_CHANGES_SELF_CONFIG, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			cs.add(CONTROL_INFO_CHANGES_CONTROLLERS, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
			cs.add(CONTROL_INFO_CHANGES_CONTROLLERS_TO_ADD, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
			cs.add(CONTROL_INFO_CHANGES_CONTROLLERS_TO_REMOVE, (ConceptSchema) getSchema(BasicOntology.AID), 0, ObjectSchema.UNLIMITED);
			
			// PARAMETER
			cs = (ConceptSchema) getSchema(PARAMETER);
			cs.add(PARAMETER_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);        
			cs.add(PARAMETER_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);        
			cs.add(PARAMETER_ELEMENT_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_MODE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);        
			cs.add(PARAMETER_WRAPPED_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);        
			cs.add(PARAMETER_DOCUMENTATION, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_SCHEMA, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_DEFAULT_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_MANDATORY, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_REGEX, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_CARD_MIN, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_CARD_MAX, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			cs.add(PARAMETER_PERMITTED_VALUES, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			
			// MODIFIER
			cs = (ConceptSchema) getSchema(MODIFIER);
			cs.add(MODIFIER_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING));        
			cs.add(MODIFIER_PROPERTIES, (ConceptSchema) getSchema(FIPAManagementVocabulary.PROPERTY), 0, ObjectSchema.UNLIMITED);
			
			// EXECUTOR
			cs = (ConceptSchema) getSchema(EXECUTOR);
			cs.add(EXECUTOR_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING));  
			cs.add(EXECUTOR_SESSION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_DELEGATION_CHAIN, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_EXECUTOR_STATUS, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));  
			cs.add(EXECUTOR_EXECUTOR_FSM_STATE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_WORKFLOW_FSM_STATE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_ABORT_CONDITION, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));  
			cs.add(EXECUTOR_WATCHDOG_EXPIRED, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_WATCHDOG_STARTTIME, (PrimitiveSchema) getSchema(BasicOntology.DATE), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_WATCHDOG_WAKEUPTIME, (PrimitiveSchema) getSchema(BasicOntology.DATE), ObjectSchema.OPTIONAL);  
			cs.add(EXECUTOR_SUBFLOWS, (ConceptSchema) getSchema(SUBFLOW), 0, ObjectSchema.UNLIMITED);
			
			// SUBFLOW
			cs = (ConceptSchema) getSchema(SUBFLOW);
			cs.add(SUBFLOW_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING));  
			cs.add(SUBFLOW_DELEGATED_PERFORMER, (ConceptSchema) getSchema(BasicOntology.AID));  
			cs.add(SUBFLOW_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);  
			cs.add(SUBFLOW_STATUS, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));  
			
			// EXECUTE_WORKFLOW
			AgentActionSchema as = (AgentActionSchema) getSchema(EXECUTE_WORKFLOW);
			as.add(EXECUTE_WORKFLOW_WHAT, (ConceptSchema) getSchema(WORKFLOW), ObjectSchema.MANDATORY);
			as.add(EXECUTE_WORKFLOW_HOW, (ConceptSchema) getSchema(CONTROL_INFO), 0, ObjectSchema.UNLIMITED);
			as.add(EXECUTE_WORKFLOW_MODIFIERS, (ConceptSchema) getSchema(MODIFIER), 0, ObjectSchema.UNLIMITED);

			// THAW_WORKFLOW
			as = (AgentActionSchema) getSchema(THAW_WORKFLOW);
			as.add(THAW_WORKFLOW_SERIALIZED_STATE, (PrimitiveSchema) getSchema(BasicOntology.BYTE_SEQUENCE), ObjectSchema.MANDATORY);
			as.add(THAW_WORKFLOW_EXECUTION, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			as.add(THAW_WORKFLOW_MODIFIERS, (ConceptSchema) getSchema(MODIFIER), 0, ObjectSchema.UNLIMITED);
			as.add(THAW_WORKFLOW_CONTROL_INFOS, (ConceptSchema) getSchema(CONTROL_INFO), 0, ObjectSchema.UNLIMITED);

			// RECOVER_WORKFLOW
			as = (AgentActionSchema) getSchema(RECOVER_WORKFLOW);
			as.add(RECOVER_WORKFLOW_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(RECOVER_WORKFLOW_EXECUTION, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			as.add(RECOVER_WORKFLOW_MODIFIERS, (ConceptSchema) getSchema(MODIFIER), 0, ObjectSchema.UNLIMITED);
			as.add(RECOVER_WORKFLOW_CONTROL_INFOS, (ConceptSchema) getSchema(CONTROL_INFO), 0, ObjectSchema.UNLIMITED);
			
			// RESET_MODIFIERS
			as = (AgentActionSchema) getSchema(RESET_MODIFIERS);
			as.add(RESET_MODIFIERS_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING));
			as.add(RESET_MODIFIERS_MODIFIERS, (ConceptSchema) getSchema(MODIFIER), 0, ObjectSchema.UNLIMITED);
			
			// RESET_CONTROL_INFOS
			as = (AgentActionSchema) getSchema(RESET_CONTROL_INFOS);
			as.add(RESET_CONTROL_INFOS_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING));
			as.add(RESET_CONTROL_INFOS_CONTROL_INFOS, (ConceptSchema) getSchema(CONTROL_INFO), 0, ObjectSchema.UNLIMITED);
			
			// KILL_WORKFLOW
			as = (AgentActionSchema) getSchema(KILL_WORKFLOW);
			as.add(KILL_WORKFLOW_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(KILL_WORKFLOW_SMOOTH, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			as.add(KILL_WORKFLOW_FREEZE, (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
			as.add(KILL_WORKFLOW_SCOPE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
			as.add(KILL_WORKFLOW_MESSAGE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);

			// SET_CONTROL_INFO
			as = (AgentActionSchema) getSchema(SET_CONTROL_INFO);
			as.add(SET_CONTROL_INFO_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(SET_CONTROL_INFO_INFO, (ConceptSchema) getSchema(CONTROL_INFO), ObjectSchema.MANDATORY);

			// UPDATE_CONTROL_INFO
			as = (AgentActionSchema) getSchema(UPDATE_CONTROL_INFO);
			as.add(UPDATE_CONTROL_INFO_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(UPDATE_CONTROL_INFO_INFO, (ConceptSchema) getSchema(CONTROL_INFO_CHANGES), ObjectSchema.MANDATORY);
			
			// GET_WRD
			as = (AgentActionSchema) getSchema(GET_WRD);
			as.add(GET_WRD_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(GET_WRD_WRD, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			
			// SET_WRD
			as = (AgentActionSchema) getSchema(SET_WRD);
			as.add(SET_WRD_EXECUTION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(SET_WRD_WRD, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(SET_WRD_VALUE, (TermSchema) TermSchema.getBaseSchema(), ObjectSchema.OPTIONAL);
			
			// GET_SESSION_STATUS
			as = (AgentActionSchema) getSchema(GET_SESSION_STATUS);
			as.add(GET_SESSION_STATUS_SESSION_ID, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			
			// EXECUTION_ERROR
			PredicateSchema ps = (PredicateSchema) getSchema(EXECUTION_ERROR);
			ps.add(EXECUTION_ERROR_REASON, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			ps.add(EXECUTION_ERROR_TYPE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			ps.add(EXECUTION_ERROR_PARAMETERS, (ConceptSchema) getSchema(PARAMETER), 0, ObjectSchema.UNLIMITED);			
		} 
		catch (OntologyException oe) {
			oe.printStackTrace();
		} 
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
