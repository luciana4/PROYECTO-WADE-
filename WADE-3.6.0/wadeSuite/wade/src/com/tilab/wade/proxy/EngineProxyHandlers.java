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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jade.content.Predicate;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.MessageTemplate.MatchExpression;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;
import jade.wrapper.gateway.GatewayAgent;

import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.event.Occurred;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.event.WorkflowEvent;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.performer.ontology.Frozen;
import com.tilab.wade.performer.ontology.GenericError;
import com.tilab.wade.performer.ontology.NotificationError;


public class EngineProxyHandlers {

	protected static Logger logger = Logger.getMyLogger(EngineProxyHandlers.class.getName());
	
	private EngineProxy engineProxy;
	private ThreadedBehaviourFactory tbf;
	private Behaviour wrappedNotificationBehaviour;
	private Behaviour wrappedResultBehaviour;
	private Map<String, WorkflowManagementBehaviour> wmbs = new HashMap<String, WorkflowManagementBehaviour>();

	
	EngineProxyHandlers(EngineProxy engineProxy) {
		this.engineProxy = engineProxy;
		tbf = new ThreadedBehaviourFactory();
	}

	
	////////////////////////////////////////////////////////////////////////////////////
	// NotificationHandler
	//
	void startNotificationHandler() throws Exception {

		// Create the notification behaviour
		CyclicBehaviour notificationBehaviour = new CyclicBehaviour() {

			private MessageTemplate template;
			
			@Override
			public void onStart() {
				super.onStart();
				
				// Prapare a template to match all message with a user defined parameter 
				// called WADE-event-message with true value
				// All message managed with EventEmitter arrive with this parameter
				template = new MessageTemplate(new MatchExpression() {
					public boolean match(ACLMessage msg) {
						
						String eventMessage = msg.getUserDefinedParameter(Constants.EVENT_MESSAGE);
						if ("true".equals(eventMessage)) {
							return true;
						}
						return false;
					}
				});
				
				((GatewayAgent)myAgent).releaseCommand(wrappedNotificationBehaviour);
			}
			
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive(template);
				if (msg != null) {
					try {
						if (msg.getPerformative() == ACLMessage.INFORM) {
							Occurred occurred = (Occurred) myAgent.getContentManager().extractContent(msg);
							WorkflowEvent ev = (WorkflowEvent)occurred.getEvent();
							if (logger.isLoggable(Logger.FINE)) {
								logger.log(Logger.FINE,"EngineProxy.NotificationHandler: Received notification, "+ev);
							}
							
							// All workflow events have the wf executionId as conversationId  
							String executionId =  msg.getConversationId();
							
							WorkflowController controller = engineProxy.getController(ev.getSessionId());
							if (controller != null) {
								WorkflowEventListener listener = controller.getNotificationListener(msg.getProtocol());
								if (listener != null) {
									try {
										listener.handleEvent(occurred.getTime(), ev, msg.getSender(), executionId);
									} catch(Exception e) {
										logger.log(Logger.WARNING, "EngineProxy.NotificationHandler: HandleEvent failed", e);
									}
								}
							}
						}
					} catch (Exception e){
						logger.log(Logger.SEVERE,"EngineProxy.NotificationHandler: Error decoding notification", e);
					}
				} else {
					block();
				}
			}
		};
		
		// Make threaded the notification behaviour and execute it 
		wrappedNotificationBehaviour = tbf.wrap(notificationBehaviour);
		engineProxy.dynamicJadeGateway.execute(wrappedNotificationBehaviour);	
	}

	
	
	////////////////////////////////////////////////////////////////////////////////////
	// ResultHandler
	//
	void startResultHandler() throws Exception {
		
		// Create the result behaviour
		CyclicBehaviour resultBehaviour = new CyclicBehaviour() {

			private MessageTemplate template;
			
			@Override
			public void onStart() {
				super.onStart();
				
				template = new MessageTemplate(new MatchExpression() {
					public boolean match(ACLMessage msg) {
						String conversationId = msg.getConversationId();
						return wmbs.containsKey(conversationId);
					}
				});
				
				((GatewayAgent)myAgent).releaseCommand(wrappedResultBehaviour);
			}
			
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive(template);
				if (msg != null) {
					try {
						String conversationId = msg.getConversationId();
						WorkflowManagementBehaviour wmb = wmbs.get(conversationId);
						if (wmb != null) {
							WorkflowResultListener resultListener = wmb.getResultListener();
							
							switch (msg.getPerformative()) {
							case ACLMessage.AGREE:
								handleAgree(msg, wmb, resultListener);
								break;

							case ACLMessage.INFORM:
								handleInform(msg, wmb, resultListener);
								break;
								
							case ACLMessage.REFUSE:
								handleRefuse(msg, wmb, resultListener);
								break;

							case ACLMessage.FAILURE:
								handleFailure(msg, wmb, resultListener);
								break;
								
							case ACLMessage.NOT_UNDERSTOOD:
								handleNotUnderstood(msg, wmb, resultListener);
								break;
								
							default:
								logger.log(Logger.WARNING,"EngineProxy.ResultHandler: Received message="+msg+" with unexpected performative");
								break;
							}
							
							// When WF terminate clean session
							if (msg.getPerformative() != ACLMessage.AGREE) {
								engineProxy.cleanSession(wmb, msg.getContent());
							}
						} else {
							logger.log(Logger.WARNING,"EngineProxy.ResultHandler: Received message="+msg+" but no one wmb waiting for it");
						}
					} catch (Exception e){
						logger.log(Logger.SEVERE,"EngineProxy.ResultHandler: Error decoding notification", e);
					}
				} else {
					block();
				}
			}
			
			private void handleAgree(final ACLMessage msg, WorkflowManagementBehaviour wmb, final WorkflowResultListener resultListener) {
				// Get executionId from AGREE message and set it to unblock the WMB
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE,"EngineProxy.ResultHandler: Received AGREE msg="+msg);
				}
				final String executionId = msg.getContent();
				wmb.setAgreeInfo(executionId, msg.getSender());
				
				if (resultListener != null) {
					// Uses a thread to prevent blocking behavior
					new Thread() {
						public void run() {
							try {
								resultListener.handleAssignedId(msg.getSender(), executionId);
							} catch (Exception e) {
								logger.log(Logger.WARNING, "EngineProxy.ResultHandler: HandleAssignedId failed, msg="+msg, e);
							}
						};
					}.start();
				}
			}
			
			private void handleInform(final ACLMessage msg, final WorkflowManagementBehaviour wmb, final WorkflowResultListener resultListener) {
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE,"EngineProxy.ResultHandler: Received INFORM msg="+msg);
				}
				if (resultListener != null) {
					// Uses a thread to prevent blocking behavior
					new Thread() {
						public void run() {
							try {
								Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
								if (p instanceof Result) {
									resultListener.handleExecutionCompleted(((Result) p).getItems(), msg.getSender(), wmb.getExecutionId());
								}
								else if (p instanceof Frozen) {
									resultListener.handleExecutionCompleted(null, msg.getSender(), wmb.getExecutionId());
								}
							}
							catch (Exception e) {
								logger.log(Logger.WARNING, "EngineProxy.ResultHandler: HandleExecutionCompleted failed, msg="+msg, e);
							}
						};
					}.start();
				}
			}
			
			private void handleRefuse(final ACLMessage msg, WorkflowManagementBehaviour wmb, final WorkflowResultListener resultListener) {
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE,"EngineProxy.ResultHandler: Received REFUSE msg="+msg);
				}
				
				if (resultListener != null) {
					// Uses a thread to prevent blocking behavior
					new Thread() {
						public void run() {
							try {
								Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
								if (p instanceof GenericError) {
									resultListener.handleLoadError(((GenericError)p).getReason());	
								}
							} catch (Exception e) {
								logger.log(Logger.WARNING, "EngineProxy.ResultHandler: HandleLoadError failed, msg="+msg, e);
							}
						};
					}.start();
				}
			}
			
			protected void handleNotUnderstood(final ACLMessage msg, WorkflowManagementBehaviour wmb, final WorkflowResultListener resultListener) {
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE,"EngineProxy.ResultHandler: Received NOT-UNDERSTOOD msg="+msg);
				}
				
				if (resultListener != null) {
					// Uses a thread to prevent blocking behavior
					new Thread() {
						public void run() {
							try {
								Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
								if (p instanceof GenericError) {
									resultListener.handleLoadError(((GenericError)p).getReason());	
								}
							} catch (Exception e) {
								logger.log(Logger.WARNING, "EngineProxy.ResultHandler: HandleLoadError failed, msg="+msg, e);
							}
						};
					}.start();
				}
			}
			
			private void handleFailure(final ACLMessage msg, final WorkflowManagementBehaviour wmb, final WorkflowResultListener resultListener) {
				if (logger.isLoggable(Logger.FINE)) {
					logger.log(Logger.FINE,"EngineProxy.ResultHandler: Received FAILURE msg="+msg);
				}
				
				if (resultListener != null) {
					// Uses a thread to prevent blocking behavior
					new Thread() {
						public void run() {
							if (msg.getSender().equals(myAgent.getAMS())) {
								resultListener.handleLoadError("EngineProxy.ResultHandler: Receved failure from AMS, msg="+msg);
							}
							else {
								try {
									Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
									if (p instanceof GenericError) {
										resultListener.handleLoadError(((GenericError) p).getReason());
									}
									else if (p instanceof NotificationError) {
										resultListener.handleNotificationError(msg.getSender(), wmb.getExecutionId());
									}
									else if (p instanceof ExecutionError) {
										resultListener.handleExecutionError((ExecutionError)p, msg.getSender(), wmb.getExecutionId());
									}
								} catch (Exception e) {
									e.printStackTrace();
									try {
										resultListener.handleNotificationError(msg.getSender(), wmb.getExecutionId());
									} catch (Exception e1) {
										logger.log(Logger.WARNING, "EngineProxy.ResultHandler: HandleNotificationError failed, msg="+msg, e);
									}
								}
							}
						};
					}.start();
				}
			}
		};
		
		// Make threaded the result behaviour and execute it 
		wrappedResultBehaviour = tbf.wrap(resultBehaviour);
		engineProxy.dynamicJadeGateway.execute(wrappedResultBehaviour);	
	}

	void addToResultHandler(String conversationId, WorkflowManagementBehaviour wmb) {
		wmbs.put(conversationId, wmb);
	}

	void removeFromResultHandler(String conversationId) {
		wmbs.remove(conversationId);
	}

	void cleanResultHanhler() {
		wmbs.clear();
	}

	
	////////////////////////////////////////////////////////////////////////////////////
	// ExecutorHandler
	//
	void startExecutorHandler() throws Exception {
		
		engineProxy.dynamicJadeGateway.execute(new SubscriptionInitiator(null, null) {
			
			@Override
			public void onStart() {
				super.onStart();
				
				((GatewayAgent)myAgent).releaseCommand(this);
			}

			@Override
			protected Vector prepareSubscriptions(ACLMessage subscription) {
				Vector l = new Vector(1);
				l.addElement(DFService.createSubscriptionMessage(myAgent, myAgent.getDefaultDF(), engineProxy.getExecutorTemplate(), null));
				return l;
			}

			@Override
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for (int i = 0; i < dfds.length; ++i) {
						AID agent = dfds[i].getName();
						if (dfds[i].getAllServices().hasNext()) {
							engineProxy.addExecutor(agent);
						} else {
							engineProxy.removeExecutor(agent);
						}
					}
				} catch (FIPAException e) {
					logger.log(Logger.SEVERE,"EngineProxy.ExecutorHandler: Error decoding df notification", e);
				}
			}
		});
	}

}
