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
package com.tilab.wade.proxy;

import jade.core.AID;
import jade.util.Logger;
import jade.util.leap.List;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.interactivity.Interaction;
import com.tilab.wade.performer.interactivity.InteractivitySnapshot;

/**
 * A <code>WorkflowController</code> object is returned by all the <code>launch()</code> methods
 * of the <code>EngineProxy</code> class. Such object allows controlling the execution of an interactive 
 * workflow and managing the related user interactions.
 */
public class WorkflowController {

	protected static Logger logger = Logger.getMyLogger(WorkflowController.class.getName());
	
	private EngineProxy engineProxy;
	private AID executor;
	private String executionId;
	private String sessionId;
	private boolean recovered;
	private boolean backEnabled;
	
	private Map<String, WorkflowEventListener> notificationListeners;
	
	private String interactionTerminatedMessage;
	private boolean interactionTerminated;
	private boolean terminated;
	
	private GoRequesterBehaviour gobackBehaviour;
	private GetSnapshotBehaviour getSnapshotBehaviour;
	private WorkflowManagementBehaviour wmBehaviour;
	
	private Map<String, Map<String, Object>> cacheDataMap = new HashMap<String, Map<String, Object>>();
	
	
	WorkflowController(EngineProxy engineProxy, AID executor, String executionId, String sessionId, EventGenerationConfiguration eventCfg, boolean recovered, WorkflowManagementBehaviour wmb) {
		this.engineProxy = engineProxy;
		this.sessionId = sessionId;
		this.executor = executor;
		this.executionId = executionId;
		this.recovered = recovered;
		this.interactionTerminated = false;
		this.terminated = false;
		this.wmBehaviour = wmb;
		this.backEnabled = false;
		
		setNotificationListener(eventCfg);
	}
	
	/**
	 * Make the controlled interactive workflow start and proceed until the first user interaction.
	 * @return The first user interaction
	 * @throws EngineProxyException
	 */
	public Interaction go() throws EngineProxyException {
		return go(null);
	}
	
	/**
	 * Make the controlled interactive workflow go ahead after the current user interaction and proceed until the 
	 * next user interaction.
	 * If the workflow is marked as interactivity-completed return a null interaction.
	 * If the workflow is suspended throw the SuspendedException with the message specified in wf method suspend(msg)
	 * @param interaction The current user interaction properly filled with user inputs (if any) and selected action
	 * @return The next user interaction
	 * @throws EngineProxyException 
	 */
	public Interaction go(Interaction interaction) throws EngineProxyException {
		if (logger.isLoggable(Logger.FINE)) {
			logger.log(Logger.FINE, "Begin go() for sessionId="+sessionId);
		}

		Interaction nextInteraction = goback(new GoRequesterBehaviour(executor, sessionId, interaction), interaction);
		
		if (logger.isLoggable(Logger.FINE)) {
			logger.log(Logger.FINE, "End go() for sessionId="+sessionId);
		}
		
		return nextInteraction;
	}

	/**
	 * Go back to previous workflow interaction
	 * @return The previous user interaction
	 * @throws EngineProxyException 
	 */
	public Interaction back() throws EngineProxyException {
		if (logger.isLoggable(Logger.FINE)) {
			logger.log(Logger.FINE, "Begin back() for sessionId="+sessionId);
		}

		// Verify that the action is supported
		if (!backEnabled) {
			throw new UnsupportedOperationException("Back not possible; workflow not support TAG or no previous step available");
		}
		
		Interaction prevInteraction = goback(new BackRequesterBehaviour(executor, sessionId), null);
		
		if (logger.isLoggable(Logger.FINE)) {
			logger.log(Logger.FINE, "End back() for sessionId="+sessionId);
		}
		return prevInteraction;
	}
	
	private Interaction goback(GoRequesterBehaviour b, Interaction interaction) throws EngineProxyException {
		
		// Verify that the interaction isn't already finished
		if (interactionTerminated) {
			throw new EngineProxyException(interactionTerminatedMessage);
		}

		synchronized (this) {
			// In case there is a GoBehaviour running, make it abort
			if (gobackBehaviour != null) {
				throw new ConcurrentModificationException("Action not possible; go/back action active");
			}
	
			gobackBehaviour = b;
		}
		
		// Store the current values of interaction in user-data
		storeCacheData(interaction);
		
		try {
			engineProxy.execute(b);

			if (b.getStatus() == GoRequesterBehaviour.FAILURE_STATUS) {
				String error = b.getError();
				Exception nestedException = b.getNestedException();
				throw new EngineProxyException(error, nestedException);
			}

			if (b.getStatus() == GoRequesterBehaviour.FROZEN_STATUS) {
				setInteractionTerminated("Controlled workflow frozen");
				throw new FrozenException();
			}
			
			if (b.getStatus() == GoRequesterBehaviour.SUSPENDED_STATUS) {
				setInteractionTerminated("Controlled workflow suspended");
				
				//Notifica il listener
				WorkflowResultListener resultListener = wmBehaviour.getResultListener();
				if (resultListener != null) {
					resultListener.handleExecutionCompleted(null, executor, wmBehaviour.getExecutionId());
				}

				// No INFORM/FAILURE received -> manually clean the session
				engineProxy.cleanSession(wmBehaviour, null);

				throw new SuspendedException(b.getError());
			}
			
			Interaction nextInteraction = b.getNextInteraction();
			if (nextInteraction == null || nextInteraction.isLast()) {
				setInteractionTerminated("Interaction terminated");
			}
			
			setBackEnabled(nextInteraction);
			applyCacheData(nextInteraction);
			
			return nextInteraction;

		} catch (Exception e) {
			throw new EngineProxyException("Error executing action "+b.getClass().getSimpleName(), e); 
		} finally {
			synchronized (this) {
				gobackBehaviour = null;
			}
		}
	}

	private void storeCacheData(Interaction interaction) {
		if (interaction != null && interaction.getId() != null) {
			Map<String, Object> interactionCacheData = interaction.getCacheData();
			cacheDataMap.put(interaction.getId(), interactionCacheData);
		}
	}

	private void applyCacheData(Interaction interaction) {
		if (interaction != null && interaction.getId() != null) {
			Map<String, Object> interactionCacheData = cacheDataMap.get(interaction.getId());
			interaction.setCacheData(interactionCacheData);
		}
	}
	
	private void setBackEnabled(Interaction interaction) {
		if (interaction == null || interaction.isLast()) {
			backEnabled = false;
		} else {
			backEnabled = interaction.isBackEnabled();
		}
	}
	
	/**
	 * Get the current interactivity snapshot
	 * @return Current interactivity snapshot
	 * @throws EngineProxyException
	 */
	public InteractivitySnapshot getSnapshot() throws EngineProxyException {
		if (logger.isLoggable(Logger.FINE)) {
			logger.log(Logger.FINE, "Begin getSnapshot() for sessionId="+sessionId);
		}
		
		// Verify that the interaction isn't already finished 
		if (interactionTerminated) {
			throw new EngineProxyException(interactionTerminatedMessage);
		}

		GetSnapshotBehaviour b;
		synchronized (this) {
			// In case there is a GetSnapshotBehaviour running, make it abort
			if (getSnapshotBehaviour != null) {
				getSnapshotBehaviour.abort();
			}
			
			// In case there is a Go/BackBehaviour running, make it abort
			if (gobackBehaviour != null) {
				gobackBehaviour.abort();
			}
			
			b = new GetSnapshotBehaviour(executor, sessionId);
			getSnapshotBehaviour = b;
		}
		
		try {
			engineProxy.execute(b);

			if (b.getStatus() != GoRequesterBehaviour.SUCCESS_STATUS) {
				throw new EngineProxyException(b.getError(), b.getNestedException());
			}
			
			InteractivitySnapshot snapshot = b.getSnapshot();
			setBackEnabled(snapshot.getInteraction());
			applyCacheData(snapshot.getInteraction());
			
			if (logger.isLoggable(Logger.FINE)) {
				logger.log(Logger.FINE, "End getSnapshot() for sessionId="+sessionId);
			}
			
			return snapshot;
		} catch (Exception e) {
			throw new EngineProxyException("Error executing action getSnapshot", e); 
		} finally {
			synchronized (this) {
				if (b == getSnapshotBehaviour) {
					getSnapshotBehaviour = null;
				}
			}
		}
	}

	/**
	 * Retrieves the <code>AID</code> of the agent executing the controlled workflow
	 * @return The <code>AID</code> of the agent executing the controlled workflow
	 */
	public AID getExecutor() {
		return executor;
	}

	/**
	 * Retrieves the session-id of the session (if any) the controlled workflow belongs to.
	 * @return The session-id of the session (if any) the controlled workflow belongs to
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Retrieves the execution-id of the controlled workflow
	 * @return The execution-id of the controlled workflow
	 */
	public String getExecutionId() {
		return executionId;
	}
	
	/**
	 * Retrieves if the controller is associated to a recover action
	 * @return True if the controller is associated to a recover action
	 */
	public boolean isRecovered() {
		return recovered;
	}

	/**
	 * Retrieves if the back action is enabled
	 * @return True if the back action is enabled
	 */
	public boolean isBackEnabled() {
		return backEnabled && gobackBehaviour == null;
	}
	
	void setInteractionTerminated(String message) {
		gobackBehaviour = null;
		interactionTerminated = true;
		interactionTerminatedMessage = message;
	}
	
	WorkflowEventListener getNotificationListener(String type) {
		return notificationListeners.get(type);
	}
	
	void setNotificationListener(EventGenerationConfiguration eventCfg) {
		if (eventCfg != null) {
			notificationListeners = eventCfg.getListeners();
		}
	}
	
	WorkflowManagementBehaviour getWMB() {
		return wmBehaviour;
	}
	
	void markAsRecovered() {
		recovered = true;
		backEnabled = false;
	}

	void setTerminated() {
		interactionTerminated = true;
		terminated = true;
	}

	boolean isTerminated() {
		return terminated;
	}

	void update(WorkflowResultListener resultListener, EventGenerationConfiguration eventCfg, WorkflowContext context, boolean interactiveMode) throws EngineProxyException {
		
		// Set notification listener in controller
		setNotificationListener(eventCfg);
		
		// Update WMB
		wmBehaviour.update(resultListener, eventCfg, context, interactiveMode);
		
		// Prepare and send new control-infos and modifiers to executor 
		List cInfos = wmBehaviour.prepareControlInfos();
		List modifiers = wmBehaviour.prepareModifiers();
		
		
		ResetControlInfosBehaviour rciBehaviour = new ResetControlInfosBehaviour(executor, executionId, cInfos);
		engineProxy.execute(rciBehaviour);
		
		ResetModifiersBehaviour rmBehaviour = new ResetModifiersBehaviour(executor, executionId, modifiers);
		engineProxy.execute(rmBehaviour);
	}
}
