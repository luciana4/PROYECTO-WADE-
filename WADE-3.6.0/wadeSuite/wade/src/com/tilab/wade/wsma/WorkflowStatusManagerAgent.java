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
package com.tilab.wade.wsma;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AttributeGetter;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.commons.WadeAgentImpl;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.ontology.ThawWorkflow;
import com.tilab.wade.performer.ontology.WorkflowManagementOntology;
import com.tilab.wade.utils.CFAUtils;
import com.tilab.wade.utils.GUIDGenerator;
import com.tilab.wade.wsma.ontology.CleanExecutions;
import com.tilab.wade.wsma.ontology.GetExecution;
import com.tilab.wade.wsma.ontology.GetPendingExecutions;
import com.tilab.wade.wsma.ontology.GetQueryDialect;
import com.tilab.wade.wsma.ontology.GetSerializedState;
import com.tilab.wade.wsma.ontology.GetSessionExecutions;
import com.tilab.wade.wsma.ontology.QueryExecutions;
import com.tilab.wade.wsma.ontology.RemoveExecution;
import com.tilab.wade.wsma.ontology.SerializedStateChanged;
import com.tilab.wade.wsma.ontology.Started;
import com.tilab.wade.wsma.ontology.StatusChanged;
import com.tilab.wade.wsma.ontology.Terminated;
import com.tilab.wade.wsma.ontology.Thawed;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowResult;
import com.tilab.wade.wsma.ontology.WorkflowExecutionInfo.WorkflowStatus;
import com.tilab.wade.wsma.ontology.WorkflowStatusOntology;

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.lang.leap.LEAPCodec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.OntologyServer;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

/**
 * Workflow Status Manager Agent
 * This agent manages the state and the persistence of workflows.
 * With <code>storageClassName</code> agent attribute is possible to customize the storage implementation class
 * The default storage is the MemoryStorage
 */
public class WorkflowStatusManagerAgent extends WadeAgentImpl {

	private static final long serialVersionUID = 4381731786126014084L;

	public static final String STORAGE_CLASS_NAME_KEY = "storageClassName";

	private static final String EXECUTIONS_CLEANUP_RETENTION_DAYS_KEY = "executionsCleanupRetentionDays";
	private static final int EXECUTIONS_CLEANUP_RETENTION_DAYS_DEFAULT = 0;
	private static final String EXECUTIONS_CLEANUP_TRIGGER_HOUR_KEY = "executionsCleanupTriggerHour";
	private static final int EXECUTIONS_CLEANUP_TRIGGER_HOUR_DEFAULT = 2;

	private Storage storage;
	
	///////////////////////////////////////////////////
	// Agent attributes
	///////////////////////////////////////////////////
	@AttributeGetter(name="Storage class-name")
	public String getStorageClassName() {
		return storage.getClass().getName();
	}

	@Override
	protected void agentSpecificSetup() throws AgentInitializationException {
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(new LEAPCodec());
		getContentManager().registerOntology(WorkflowStatusOntology.getInstance());
		getContentManager().registerOntology(WorkflowManagementOntology.getInstance());
		getContentManager().registerOntology(ConfigurationOntology.getInstance());

		// Check if is the master (one wsma or first wsma agent pool)
		String poolName = getArgument(WadeAgent.AGENT_POOL, null);
		boolean agentMaster = getBooleanArgument(WadeAgent.AGENT_MASTER, false);
		boolean isMaster = poolName == null || (poolName != null && agentMaster); 
		
		try {
			// Read storage-implementation-class from agent arguments
			String storageClassName = getArgument(STORAGE_CLASS_NAME_KEY, null);
			if (storageClassName != null) {
				// Storage class name explicitly specified --> load it
				myLogger.log(Level.CONFIG, "Agent "+getName()+" - Loading Storage: "+storageClassName);
				storage = (Storage)Class.forName(storageClassName).newInstance();
			}
			else {
				// Load default storage: HibernateStorage if available; MemoryStorage otherwise
				try {
					storage = (Storage)Class.forName("com.tilab.wade.wsma.HibernateStorage").newInstance();
				}
				catch (Exception e) {
					storage = new MemoryStorage();
				}
			}
			myLogger.log(Level.INFO, "Agent "+getName()+" - Using Storage "+storage.getClass().getName());
			
			// Init storage
			storage.init(arguments);
			
		} catch (Exception e) {
			myLogger.log(Logger.SEVERE, "Agent "+getName()+" - Error creating wsma storage", e);
			throw new AgentInitializationException("Error creating wsma storage", e);
		}
		
		// Add the behaviour listening to WorkflowStatus events (inform) and request
		addBehaviour(new OntologyServer(this, WorkflowStatusOntology.getInstance(), new int[] {ACLMessage.INFORM, ACLMessage.REQUEST}, this));
		
		// Unless this is a restart-after-crash, add the behaviour listening to platform life cycle notifications: 
		// As soon as the platform is UP manage pending workflow executions if any
		if (!getRestarted() && isMaster) {
			AID platformLifeCycleTopic = registerToTopic(this, ConfigurationOntology.PLATFORM_LIFE_CYCLE_TOPIC);
			addBehaviour(new PlatformStartupListener(platformLifeCycleTopic));
		}

		// Add the behaviour to cleanup old executions
		// The behaviour trigger every days at executionsCleanupTriggerHour (dafault 02.00 AM)
		// All the older than executionsCleanupRetentionDays days executions are cleaned (default 0 = disabled) 
		// To disable the mechanism set executionsCleanupRetentionDays to 0
		final int executionsCleanupRetentionDays = getIntArgument(EXECUTIONS_CLEANUP_RETENTION_DAYS_KEY, EXECUTIONS_CLEANUP_RETENTION_DAYS_DEFAULT);
		if (executionsCleanupRetentionDays > 0) {
			final int executionsCleanupTriggerHour = getIntArgument(EXECUTIONS_CLEANUP_TRIGGER_HOUR_KEY, EXECUTIONS_CLEANUP_TRIGGER_HOUR_DEFAULT);

			WakerBehaviour executionsCleanupBehaviour = new WakerBehaviour(this, getNextWakeupDate(executionsCleanupTriggerHour)) {
				private static final long serialVersionUID = -2815454304813078957L;

				@Override
				protected void onWake() {
					try {
						storage.cleanOlderExecutions(executionsCleanupRetentionDays);
					} catch (StorageException e) {
						myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error removing older executions", e);
					}
				}
				
				@Override
				public int onEnd() {
					reset(getNextWakeupDate(executionsCleanupTriggerHour));
					return super.onEnd();
				}
			};
			addBehaviour(executionsCleanupBehaviour);
		} else {
			myLogger.log(Logger.INFO, "Agent "+getName()+" - Executions cleanup mechanism disabled");
		}
	}	

	private static Date getNextWakeupDate(int hour) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.compareTo(Calendar.getInstance()) < 0) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		return calendar.getTime();
	}
	
	@Override
	protected void takeDown() {
		// Close the storage
		try {
			storage.close();
		} catch (StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error closing storage", e);
		}
	}

	@Override
	public int getCurrentLoad() {
		return getCurQueueSize();
	}

	private void thaw(WorkflowExecutionInfo wei) {
		try {
			ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
			message.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			message.setOntology(WorkflowManagementOntology.getInstance().getName());
			message.addReceiver(wei.getExecutor());
			message.setConversationId(GUIDGenerator.getGUID());
			
			byte[] workflowSerializedState = storage.getSerializedState(wei.getExecutionId());
			
			ThawWorkflow tw = new ThawWorkflow(workflowSerializedState, null, null);
			tw.setExecution(Constants.ASYNCH);
			
			Action action = new Action(wei.getExecutor(), tw);
			
			getContentManager().fillContent(message, action);
			
			send(message);
			MessageTemplate mt = MessageTemplate.MatchConversationId(message.getConversationId());
			ACLMessage reply = blockingReceive(mt, 10000);
	        if (reply != null) {
	        	if (reply.getPerformative() == ACLMessage.FAILURE){
		        	if (reply.getSender().equals(getAMS())) {
		        		// Received FAILURE from AMS -> executor not present
		        		handleMissingOriginalExecutor(wei);
		        	} else {
		        		throw new Exception(reply.toString());
		        	}
	        	}
	        }
	        else {
	        	throw new Exception("Timeout");
	        }
		} catch(Exception e) {
			handleThawError(wei, e);
		}
	}

	protected void handleThawError(WorkflowExecutionInfo wei, Exception e) {
		try {
			// Error reloading the workflow. Mark it as KO
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error thawing workflow, executionId=" + wei.getExecutionId(), e);
			storage.terminated(	wei.getExecutionId(), 
					WorkflowResult.KO, 
					null, 
					"Cannot thaw workflow after system restart: "+e.getMessage(), 
					System.currentTimeMillis());
		} catch (StorageException e1) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error updating StatusChanged, executionId=" + wei.getExecutionId(), e1);
		}

	}
	
	protected void handleMissingOriginalExecutor(WorkflowExecutionInfo wei) {
		try {
			// Freeze the workflow. An operator will be able to reassign it to a different executor
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Missing original executor for workflow, executionId=" + wei.getExecutionId() + ". Freeze it");
			storage.statusChanged(wei.getExecutionId(), WorkflowExecutionInfo.WorkflowStatus.FROZEN, System.currentTimeMillis());
		} catch (StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error updating StatusChanged, executionId=" + wei.getExecutionId(), e);
		}
	}
	
	public void serveStartedInform(Started started, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving Started predicate " + started);
		
		try {
			WorkflowExecutionInfo wi = started.getWorkflowExecutionInfo();
			wi.setStartTime(System.currentTimeMillis());
			wi.setLastUpdateTime(wi.getStartTime());
			
			storage.started(wi);
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving Started predicate " + started, e);
			throw e;
		}
	}

	public void serveTerminatedInform(Terminated terminated, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving Terminated predicate " + terminated);
		
		try {
			String errorMessage = terminated.getErrorMessage();
			WorkflowResult result;
			if (errorMessage != null && errorMessage.length()>0) {
				result = WorkflowResult.KO;
			} else {
				result = WorkflowResult.OK;
			}
			storage.terminated(terminated.getExecutionId(), 
					result,
					terminated.getParameters(),
					errorMessage, 
					System.currentTimeMillis());
			
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving Terminated predicate " + terminated, e);
			throw e;
		}
	}

	public void serveThawedInform(Thawed thawed, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving Thawed predicate " + thawed);
		
		try {
			storage.thawed(thawed.getExecutionId(), thawed.getExecutorName(), System.currentTimeMillis());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving Thawed predicate " + thawed, e);
			throw e;
		}
	}
	
	public void serveSerializedStateChangedInform(SerializedStateChanged store, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving SerializedStateChanged predicate " + store);

		try {
			storage.serializedStateChanged(store.getExecutionId(), store.getCurrentActivity(), store.getSerializedState(), System.currentTimeMillis());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving SerializedStateChanged predicate " + store, e);
			throw e;
		}
	}

	public void serveStatusChangedInform(StatusChanged changeState, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving StatusChanged predicate " + changeState);

		try {
			storage.statusChanged(changeState.getExecutionId(), changeState.getStatus(), System.currentTimeMillis());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving StatusChanged predicate " + changeState, e);
			throw e;
		}
	}
	
	public void serveRemoveExecutionRequest(RemoveExecution remove, ACLMessage msg) throws Exception {
		myLogger.log(Logger.FINE, "Agent "+getName()+" - Serving Remove action " + remove);
		
		try {
			storage.removeExecution(remove.getExecutionId());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving Remove action " + remove, e);
			throw e;
		}
		
		sendResponse(msg, ACLMessage.INFORM, remove, null);
	}

	public void serveCleanExecutionsRequest(CleanExecutions clean, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving Clean action " + clean);
		
		try {
			storage.cleanExecutions();
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving Clean action " + clean, e);
			throw e;
		}
		
		sendResponse(msg, ACLMessage.INFORM, clean, null);
	}
	
	public void serveGetExecutionRequest(GetExecution getExecution, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetExecution action " + getExecution);
		
		String executionId = getExecution.getExecutionId();
		WorkflowExecutionInfo wei;
		try {
			wei = storage.getExecution(executionId);
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetExecution action " + getExecution, e);
			throw e;
		}
		
		if (wei != null) {
			sendResponse(msg, ACLMessage.INFORM, getExecution, wei);
		} else {
			sendResponse(msg, ACLMessage.FAILURE, getExecution, "Workflow with executionId="+executionId+" is not present");
		}
	}

	public void serveGetSerializedStateRequest(GetSerializedState getSerializedState, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetSerializedState action " + getSerializedState);
		
		String executionId = getSerializedState.getExecutionId();
		byte[] serializedState;
		try {
			serializedState = storage.getSerializedState(executionId);
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetSerializedState action " + getSerializedState, e);
			throw e;
		}
		
		if (serializedState != null) {
			sendResponse(msg, ACLMessage.INFORM, getSerializedState, serializedState);
		} else {
			sendResponse(msg, ACLMessage.FAILURE, getSerializedState, "Workflow serialized state with executionId="+executionId+" is not present");
		}
	}
	
	public void serveGetPendingExecutionsRequest(GetPendingExecutions getPendingExecutions, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetPendingExecutions action " + getPendingExecutions);
		
		List<WorkflowExecutionInfo> weis;
		try {
			weis = storage.getPendingExecutions(getPendingExecutions.getRequester());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetPendingExecutions action " + getPendingExecutions, e);
			throw e;
		}

		sendResponse(msg, ACLMessage.INFORM, getPendingExecutions, weis);
	}
	
	public void serveGetSessionExecutionsRequest(GetSessionExecutions getSessionExecutions, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetSessionExecutions action " + getSessionExecutions);
		
		List<WorkflowExecutionInfo> weis;
		try {
			weis = storage.getSessionExecutions(getSessionExecutions.getSessionId());
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetSessionExecutions action " + getSessionExecutions, e);
			throw e;
		}

		sendResponse(msg, ACLMessage.INFORM, getSessionExecutions, weis);
	}

	public void serveGetQueryDialectRequest(GetQueryDialect getQueryDialect, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving GetQueryDialect action");
		
		String queryDialect;
		try {
			queryDialect = storage.getQueryDialect();
		} catch (AbstractMethodError ame) {
			// getQueryDialect method not available (persistence add-on version 1.6.0 or previous and WADE version 3.2.0 or major)
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - WADE Persistence add-on (version 1.6.0 or previous) not totally compatible with WADE (version 3.2.1 or major)");
			queryDialect = HibernateDialectConstants.DIALECT;
			
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving GetQueryDialect action", e);
			throw e;
		}

		sendResponse(msg, ACLMessage.INFORM, getQueryDialect, queryDialect);
	}
	
	public void serveQueryExecutionsRequest(QueryExecutions query, ACLMessage msg) throws Exception {
		myLogger.log(Logger.INFO, "Agent "+getName()+" - Serving QueryExecutions action " + query);
		
		List results;
		try {
			results = storage.queryExecutions(query.getWhat(), query.getCondition(), query.getOrder(), query.getFirstResult(), query.getMaxResult());
			sendResponse(msg, ACLMessage.INFORM, query, results);
			
		} catch(UnsupportedOperationException uoe) {
			String failureMsg = "Querying workflow executions is not supported without the Persistence Add-On";
			myLogger.log(Logger.WARNING, failureMsg);
			sendResponse(msg, ACLMessage.FAILURE, query, failureMsg);
			
		} catch(StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error serving QueryExecutions action " + query, e);
			throw e;
		}
	}
	
	private void sendResponse(ACLMessage request, int performative, AgentAction agentAction, Object result) {
		ACLMessage reply = request.createReply();
		reply.setPerformative(performative);

		if (performative == ACLMessage.INFORM) {
			if (result != null) {
				if (result instanceof byte[]) {
					reply.setByteSequenceContent((byte[])result);
				} else {
					if (agentAction != null) {
						
						if (result instanceof ArrayList) {
							// Convert java list into jade list
							result = new jade.util.leap.ArrayList((ArrayList)result);
						}
				
						Action action = new Action(getAID(), agentAction);
						ContentElement ce = new Result(action, result);
						try {
							getContentManager().fillContent(reply, ce);
						} catch (Exception e) {
							// Should never happen
							myLogger.log(Level.SEVERE, "Agent "+getName()+" - Error encoding request", e);
							performative = ACLMessage.FAILURE;
							reply.setContent("Unexpected error: "+e.getMessage());
						}
					}
				}
			}
		} else {
			// FAILURE response
			if (result != null && result instanceof String) {
				reply.setContent((String)result);
			}
		}
		
		send(reply);
	}
	
	private static AID registerToTopic(Agent agent, String topicName) throws AgentInitializationException {
		try {
			TopicManagementHelper topicHelper = (TopicManagementHelper) agent.getHelper(TopicManagementHelper.SERVICE_NAME);
			AID topic = topicHelper.createTopic(topicName);
			topicHelper.register(topic);
			return topic;
		} catch (ServiceException se) {
			throw new AgentInitializationException("Error registering to topic "+topicName, se);
		}
	}

	private void checkWorkflowExecutions() throws StorageException {
		try {
			// Search workflows in state ACTIVE/SUSPENDED/ROLLBACK/WAIT_COMMIT
			String condition;
			String queryDialect = HibernateDialectConstants.DIALECT;
			try {
				queryDialect = storage.getQueryDialect();
			} catch (AbstractMethodError ame) {
				// getQueryDialect method not available (persistence add-on version 1.6.0 or previous and WADE version 3.2.0 or major)
				myLogger.log(Logger.WARNING, "Agent "+getName()+" - WADE Persistence add-on (version 1.6.0 or previous) not totally compatible with WADE (version 3.2.1 or major)");
			}
			if (HibernateDialectConstants.DIALECT.equals(queryDialect)) {
				condition = HibernateDialectConstants.STATUS_FIELD+"='"+WorkflowStatus.ACTIVE+"'" + 
					 " or "+HibernateDialectConstants.STATUS_FIELD+"='"+WorkflowStatus.SUSPENDED+"'" +
				     " or "+HibernateDialectConstants.STATUS_FIELD+"='"+WorkflowStatus.ROLLBACK+"'" + 
				     " or "+HibernateDialectConstants.STATUS_FIELD+"='"+WorkflowStatus.WAIT_COMMIT+"'";
			} else if (MongoDialectConstants.DIALECT.equals(queryDialect)) {
				condition = "{$or: [ " +
						"{\""+MongoDialectConstants.STATUS_KEY+"\" : \""+WorkflowStatus.ACTIVE+"\"}, " +
						"{\""+MongoDialectConstants.STATUS_KEY+"\" : \""+WorkflowStatus.SUSPENDED+"\"}, " +
						"{\""+MongoDialectConstants.STATUS_KEY+"\" : \""+WorkflowStatus.ROLLBACK+"\"}, " +
						"{\""+MongoDialectConstants.STATUS_KEY+"\" : \""+WorkflowStatus.WAIT_COMMIT+"\"} ]}";
			} else {
				throw new UnsupportedOperationException();
			}
			
			List<WorkflowExecutionInfo> weis = storage.queryExecutions(null, condition, null, 0, QueryExecutions.ALL_RESULTS);
			
			//             |     SR-TR    |    SR-NTR    |    LR
			// ------------+--------------+--------------+---------
			// ACTIVE      |  TERMINATED  |  TERMINATED  |   THAW
			//             |    TR-FAIL   |      KO      |
			// SUSPENDED   |  TERMINATED  |  TERMINATED  |   THAW
			//             |    TR-FAIL   |      KO      |
			// ROLLBACK	   |  TERMINATED  |      N/A     |   N/A
			//             |    TR-FAIL	  |		         |
			// WAIT_COMMIT |  TERMINATED  |      N/A     |   N/A
			//             |    TR-FAIL	  |		         |
			for (WorkflowExecutionInfo wei : weis) {
				if (wei.isLongRunning()) {
					// LongRunning
					if(wei.isInteractive()){
						// Interactive workflows cannot be restarted since we don't know who the 
						// interaction manager agent is --> mark them as FROZEN
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Workflow execution "+wei.getExecutionId()+" is interactive --> Mark it as FROZEN");
						storage.statusChanged(wei.getExecutionId(), WorkflowStatus.FROZEN, System.currentTimeMillis());
					}else{
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Workflow execution "+wei.getExecutionId()+" is long-running --> resume it");
						thaw(wei);
					}
				} else {
					if (wei.isTransactional()) {
						// ShortRunning - transactional
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Workflow execution "+wei.getExecutionId()+" is short-running & transactional --> Mark it as TERMINATED/TRANSACTION_FAIL");
						storage.terminated(	wei.getExecutionId(), 
											WorkflowResult.TRANSACTION_FAIL, 
											null, 
											"Workflow not correctly terminated before platform shutting down", 
											System.currentTimeMillis());
					} else {
						// ShortRunning - not transactional
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Workflow execution "+wei.getExecutionId()+" is short-running --> Mark it as TERMINATED/KO");
						storage.terminated(	wei.getExecutionId(), 
											WorkflowResult.KO, 
											null, 
											"Workflow not correctly terminated before platform shutting down", 
											System.currentTimeMillis());
					}
				}
			}
		} catch (UnsupportedOperationException uoe) {
			// QueryExecutions method not available...not a problem but is not possible check the workflow execution status
		} catch (StorageException e) {
			myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error checking storage consistency", e);
			throw e;
		}
	}

	
	/**
	 * Inner class PlatformStartupListener
	 * This behaviour is responsible for listening to platform startup completion notification
	 */
	private class PlatformStartupListener extends SimpleBehaviour {

		private static final long serialVersionUID = -5921795489489965046L;
		
		private boolean finished = false;
		private MessageTemplate template;

		private PlatformStartupListener(AID platformLifeCycleTopic) {
			template = MessageTemplate.MatchTopic(platformLifeCycleTopic);
		}

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				try {
					Result r = (Result) myAgent.getContentManager().extractContent(msg);
					String platformStatus = (String) r.getValue();
					if (CFAUtils.isPlatformActive(platformStatus)) {
						finished = true;

						checkWorkflowExecutions();
					}
				}
				catch (Exception e) {
					myLogger.log(Logger.WARNING, "Agent "+getName()+" - Error decoding platform life cycle notification", e);
				}
			}
			else {
				block();
			}
		}

		@Override
		public boolean done() {
			return finished;
		}
	}  // END of inner class PlatformStartupListener
}
