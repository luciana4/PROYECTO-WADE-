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
package com.tilab.wade.ca;

import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.ContentException;
import jade.content.Predicate;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.content.schema.ObjectSchema;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.OntologyServer;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import com.tilab.wade.ca.ontology.ChangeCurrentClassLoader;
import com.tilab.wade.ca.ontology.Deploy;
import com.tilab.wade.ca.ontology.Deployed;
import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.GetModules;
import com.tilab.wade.ca.ontology.GetWorkflowList;
import com.tilab.wade.ca.ontology.GetWorkflowParameters;
import com.tilab.wade.ca.ontology.ModuleInfo;
import com.tilab.wade.ca.ontology.Revert;
import com.tilab.wade.ca.ontology.Reverted;
import com.tilab.wade.ca.ontology.Undeploy;
import com.tilab.wade.ca.ontology.Undeployed;
import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.locale.MessageCode;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.utils.OntologyUtils;


public class DeploymentServer extends OntologyServer {

	private final Logger myLogger = Logger.getMyLogger(DeploymentServer.class.getName());

	private CAServices caServices;
	private TopicManagementHelper topicHelper;
	private AID deployTopic;


	public DeploymentServer(Agent myAgent) throws AgentInitializationException {
		// NOTE that we must NOT serve the replies (INFORM, FAILURE...) to propagated actions
		super(myAgent, DeploymentOntology.getInstance(), new int[]{ACLMessage.PROPAGATE, ACLMessage.REQUEST});

		caServices = CAServices.getInstance(myAgent);

		try {
			// get topic helper, used to notify jar deployment
			topicHelper = (TopicManagementHelper) myAgent.getHelper(TopicManagementHelper.SERVICE_NAME);
			deployTopic = topicHelper.createTopic(DeploymentOntology.DEPLOY_TOPIC);
		}
		catch (ServiceException se) {
			throw new AgentInitializationException("CA " + myAgent.getName() + ": Error connecting to the TopicManagementService", se);
		}
	}

	// Redefine the handleNotUnderstood method to print a meaningful message in case 
	// an old-style deployment request is received
	@Override
	protected void handleNotUnderstood(ContentException ce, ACLMessage msg) {
		ACLMessage reply = msg.createReply();
		reply.setPerformative(ACLMessage.FAILURE);
		if (msg.getUserDefinedParameter("WADE-Deployed-Name")!=null || msg.getUserDefinedParameter("Rebuild-ClassLoader")!=null) {
			myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Received a deployment action that is no longer compatible. Please update the client.");
			reply.setContent("Old-style deployment request.");
		}
		myAgent.send(reply);
	}
	

	////////////////////////////////////////////////
	// Deployment Ontology serving methods
	////////////////////////////////////////////////
	public void serveDeployPropagate(Deploy deploy, ACLMessage msg) {
		if (deploy.getModuleContent() != null) {
			// If we actually have a module to deploy (note that the Deploy action
			// can be used to rebuild the wade class loader only), adjust the module 
			// name  (if present in MANIFEST use it, otherwise use the name specified
			// in deploy action), and deploy-date manifest header. 
			try {
				enrichDeployInfo(deploy);
			} catch (Exception e) {
				myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Error enriching module-info for module" + deploy.getModuleName(), e);
			}
		}

		// If necessary generate a new classloader-id
		if (deploy.isRebuildClassLoader()) {
			deploy.setClassloaderId(String.valueOf(System.currentTimeMillis()));
		}

		// Propagate request to one CA per hosts
		propagateToHosts(msg, deploy, new Deployed(deploy.getModuleName(), deploy.isRebuildClassLoader()));
	}
	
	public void serveDeployRequest(Deploy deploy, ACLMessage msg) {
		ACLMessage reply = null;
		try {
			// Copy the module in deploy folder (if necessary) 
			if (deploy.getModuleContent() != null) {
				caServices.getClassLoaderManager().deploy(deploy.getModuleContent(), deploy.getModuleName());
				myLogger.log(Logger.INFO, "CA " + myAgent.getName() + " - Module " + deploy.getModuleName() + " successfully deployed");
			}

			// Manage classloader rebuilding (if necessary) 
			if (deploy.isRebuildClassLoader()) {
				rebuildClassLoader(msg, deploy.getClassloaderId());
				// The above operation is asynchronous. The reply will be sent on completion 
				return;
			}
			else {
				reply = createReply(msg, ACLMessage.INFORM);	
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Error deploying module " + deploy.getModuleName(), e);
			reply = createReply(msg, ACLMessage.FAILURE, null, "Error deploying module " + deploy.getModuleName()+": "+e.getMessage());
		}
		myAgent.send(reply);
	}
	
	public void serveUndeployPropagate(Undeploy undeploy, ACLMessage msg) {
		// If necessary generate a new classloader-id
		if (undeploy.isRebuildClassLoader()) {
			undeploy.setClassloaderId(String.valueOf(System.currentTimeMillis()));
		}

		// Propagate request to one CA per hosts
		propagateToHosts(msg, undeploy, new Undeployed(undeploy.getModuleName(), undeploy.isRebuildClassLoader()));
	}
	
	public void serveUndeployRequest(Undeploy undeploy, ACLMessage msg) {
		ACLMessage reply = null;
		try {
			// Delete the module in deploy folder 
			caServices.getClassLoaderManager().undeploy(undeploy.getModuleName());
			myLogger.log(Logger.INFO, "CA " + myAgent.getName() + " - Module " + undeploy.getModuleName() + " successfully undeployed");

			// Manage classloader rebuilding (if necessary) 
			if (undeploy.isRebuildClassLoader()) {
				rebuildClassLoader(msg, undeploy.getClassloaderId());
				// The above operation is asynchronous. The reply will be sent on completion 
				return;
			}
			else {
				reply = createReply(msg, ACLMessage.INFORM);	
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Error undeploying module " + undeploy.getModuleName(), e);
			reply = createReply(msg, ACLMessage.FAILURE, null, "Error undeploying module " + undeploy.getModuleName()+": "+e.getMessage());
		}
		myAgent.send(reply);
	}
	
	public void serveRevertPropagate(Revert revert, ACLMessage msg) {
		// If necessary generate a new classloader-id
		if (revert.isRebuildClassLoader()) {
			revert.setClassloaderId(String.valueOf(System.currentTimeMillis()));
		}

		// Propagate request to one CA per hosts
		propagateToHosts(msg, revert, new Reverted(revert.getModuleName(), revert.isRebuildClassLoader()));
	}
	
	public void serveRevertRequest(Revert revert, ACLMessage msg) {
		ACLMessage reply = null;
		try {
			// revert file from currentClassLoader to deploy folder 
			caServices.getClassLoaderManager().revert(revert.getModuleName());
			myLogger.log(Logger.INFO, "CA " + myAgent.getName() + " - Module " + revert.getModuleName() + " successfully reverted");

			// Manage classloader rebuilding (if necessary) 
			if (revert.isRebuildClassLoader()) {
				rebuildClassLoader(msg, revert.getClassloaderId());
				// The above operation is asynchronous. The reply will be sent on completion 
				return;
			}
			else {
				reply = createReply(msg, ACLMessage.INFORM);	
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Error reverting module " + revert.getModuleName(), e);
			reply = createReply(msg, ACLMessage.FAILURE, null, "Error reverting module " + revert.getModuleName()+": "+e.getMessage());
		}
		myAgent.send(reply);
	}
	
	public void serveChangeCurrentClassLoaderRequest(ChangeCurrentClassLoader changeCurrentClassLoader, ACLMessage msg) {
		// Set new current class loader
		caServices.getClassLoaderManager().changeCurrentClassLoader(changeCurrentClassLoader.getClassLoaderId());

		// Send confirm
		ACLMessage reply = msg.createReply();
		reply.setPerformative(ACLMessage.INFORM);
		myAgent.send(reply);
	}

	public void serveGetWorkflowListRequest(GetWorkflowList action, ACLMessage msg) {
		String clId = action.getClassloaderId();
		WadeClassLoader wcl = caServices.getClassLoaderManager().getClassLoader(clId);
		jade.util.leap.List wfList = wcl.getWorkflowList(action.getCategory(), action.getModuleName(), action.getComponentsOnly());
		ACLMessage reply = createReply(msg, ACLMessage.INFORM, (Action) getReceivedContentElement(), wfList);
		myAgent.send(reply);
	}

	public void serveGetWorkflowParametersRequest(GetWorkflowParameters action, ACLMessage msg) {
		String clId = action.getClassloaderId();
		String wfName = action.getName();
		ACLMessage reply = null;
		WadeClassLoader wcl = caServices.getClassLoaderManager().getClassLoader(clId);
		try {
			WorkflowBehaviour myWorkflow = (WorkflowBehaviour) Class.forName(wfName, true, wcl).newInstance();
			jade.util.leap.List params = myWorkflow.getFormalParameters();

			// For all formal parameters add the relative object-schema 
			if (params.size() > 0) {
				Ontology onto = myWorkflow.getOntology();

				Iterator it = params.iterator();
				while (it.hasNext()) {
					// Set schema to parameter 
					Parameter param = (Parameter)it.next();
					ObjectSchema paramSchema = OntologyUtils.getParameterSchema(param, onto);
					param.setSchema(paramSchema);
				}				
			}				

			reply = createReply(msg, ACLMessage.INFORM, (Action) getReceivedContentElement(), params);
		} catch (ClassNotFoundException cnfe) {
			reply = createReply(msg, ACLMessage.FAILURE, (Action) getReceivedContentElement(),
					MessageCode.UNEXPECTED_ERROR+MessageCode.ARGUMENT_SEPARATOR+"unexistent workflow "+wfName+MessageCode.ARGUMENT_SEPARATOR+"CA");
		} catch (Exception iae) {
			reply = createReply(msg, ACLMessage.FAILURE, (Action) getReceivedContentElement(),
					MessageCode.UNEXPECTED_ERROR+MessageCode.ARGUMENT_SEPARATOR+"cannot instantiate workflow "+wfName+MessageCode.ARGUMENT_SEPARATOR+"CA");
		} 
		myAgent.send(reply);
	}

	public void serveGetModulesRequest(GetModules action, ACLMessage msg) {
		ACLMessage reply;
		try {
			List<ModuleInfo> modules = caServices.getClassLoaderManager().getModules();
			reply = createReply(msg, ACLMessage.INFORM, (Action) getReceivedContentElement(), modules);
		} catch (Exception e) {
			reply = createReply(msg, ACLMessage.FAILURE, (Action) getReceivedContentElement(),
					MessageCode.UNEXPECTED_ERROR+MessageCode.ARGUMENT_SEPARATOR+"error get information of deployed modules");
		}
		myAgent.send(reply);
	}
	

	////////////////////////////////////////////////
	// Private utility methods
	////////////////////////////////////////////////
	private ACLMessage createReply(ACLMessage msg, int replyPerformative) {
		return createReply(msg, replyPerformative, null, null);
	}

	private ACLMessage createReply(ACLMessage msg, int replyPerformative, Action actExpr, Object result) {
		ACLMessage reply = msg.createReply();

		if (replyPerformative == ACLMessage.INFORM) {
			if (result != null && actExpr != null) {
				ContentElement ce = new Result(actExpr, result);
				try {
					myAgent.getContentManager().fillContent(reply, ce);
				} catch (Exception e) {
					myLogger.log(Logger.SEVERE, "CA " + myAgent.getName() + " - Error managing Deployment Ontology message.", e);

					replyPerformative = ACLMessage.FAILURE;
					reply.setContent(MessageCode.UNEXPECTED_ERROR+MessageCode.ARGUMENT_SEPARATOR+e.getMessage()+MessageCode.ARGUMENT_SEPARATOR+"CA");
				}
			}
		} else {
			if (result != null) {
				reply.setContent((String)result);
			}
		}
		reply.setPerformative(replyPerformative);
		return reply;
	}

	private void propagateToHosts(final ACLMessage msg, final AgentAction agentAction, final Predicate notification) {
		Collection<AID> caAIDs = ((ControllerAgent)myAgent).getOneCAByHosts();
		PropagatorBehaviour pb = new PropagatorBehaviour(caAIDs, agentAction) {
			public int onEnd() {
				ACLMessage reply = msg.createReply();
				if (propagationOk()) {
					reply.setPerformative(ACLMessage.INFORM);

					// Issue a proper notification on deploy topic 
					notifyDeployEvent(notification);
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent(getFailureMessage());
				}
				myAgent.send(reply);

				return super.onEnd();
			}
		};

		myAgent.addBehaviour(pb);
	}

	private void rebuildClassLoader(final ACLMessage msg, String clId) throws IOException {
		// Create folder and copy jars 
		caServices.getClassLoaderManager().createClassLoaderRepository(clId);

		// Propagate to other CA of this host
		Collection<AID> caAIDs = ((ControllerAgent)myAgent).getOtherCAsInThisHost();
		PropagatorBehaviour pb = new PropagatorBehaviour(caAIDs, new ChangeCurrentClassLoader(clId)) {
			public int onEnd() {
				ACLMessage reply = msg.createReply();
				if (propagationOk()) {
					reply.setPerformative(ACLMessage.INFORM);
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent(getFailureMessage());
				}
				myAgent.send(reply);

				return super.onEnd();
			}
		};
		myAgent.addBehaviour(pb);

		// Set new current class loader
		caServices.getClassLoaderManager().changeCurrentClassLoader(clId);
	}


	private void notifyDeployEvent(Predicate predicate) {
		try {
			myLogger.log(Logger.INFO, "CA " + myAgent.getName() + ": Send deploy notification");
			
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			inform.addReceiver(deployTopic);
			inform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			inform.setOntology(DeploymentOntology.ONTOLOGY_NAME);
			myAgent.getContentManager().fillContent(inform, predicate);
			myAgent.send(inform);
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "CA " + myAgent.getName() + ": Error encoding "+predicate.getClass().getSimpleName()+" event notification message. Exception message: "+ e.getMessage());
		}
	}

	private void enrichDeployInfo(Deploy deploy) throws Exception {
		JarInputStream jarInputStream = null;
		JarOutputStream jarOutputStream = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try { 
			ByteArrayInputStream inputStream = new ByteArrayInputStream(deploy.getModuleContent());
			jarInputStream = new JarInputStream(inputStream);

			// Get or create JAR manifest 
			Manifest manifest = jarInputStream.getManifest();
			if (manifest == null) {
				manifest = new Manifest();
			}
			if (manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION) == null) {
				manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			}
			// Set deploy date
			manifest.getMainAttributes().put(new Attributes.Name(ModuleInfo.BUNDLE_DATE), String.valueOf(System.currentTimeMillis()));

			// Manage module name
			String moduleName = manifest.getMainAttributes().getValue(ModuleInfo.BUNDLE_NAME);
			if (moduleName != null) {
				// Get name from manifest
				deploy.setModuleName(moduleName);
			} else {
				// Set name into manifest
				manifest.getMainAttributes().put(ModuleInfo.BUNDLE_NAME, deploy.getModuleName());
			}

			// Recreate jar content
			jarOutputStream = new JarOutputStream(outputStream, manifest);

			byte[] buf = new byte[10240];
			int n;
			JarEntry je;
			while((je = jarInputStream.getNextJarEntry()) != null) {
				jarOutputStream.putNextEntry((ZipEntry)je.clone());
				while ((n = jarInputStream.read(buf, 0, 10240)) > -1) {
					jarOutputStream.write(buf, 0, n);
				} 
				jarInputStream.closeEntry();
				jarOutputStream.closeEntry();
			}
		} 
		finally {
			if (jarInputStream != null) {
				try {jarInputStream.close();} catch (IOException ioe) {}
			}
			if (jarOutputStream != null) {
				try {jarOutputStream.close();} catch (IOException ioe) {}
			}
		}
		
		deploy.setModuleContent(outputStream.toByteArray());
	}
}
