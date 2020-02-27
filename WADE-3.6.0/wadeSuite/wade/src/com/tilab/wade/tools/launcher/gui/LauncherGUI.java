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
package com.tilab.wade.tools.launcher.gui;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPANames;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import com.tilab.wade.ca.ontology.DeploymentOntology;
import com.tilab.wade.ca.ontology.GetWorkflowList;
import com.tilab.wade.ca.ontology.GetWorkflowParameters;
import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.dispatcher.DefaultEventListener;
import com.tilab.wade.dispatcher.DispatchingCapabilities;
import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.WorkflowException;
import com.tilab.wade.performer.descriptors.ElementDescriptor;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.BeginActivity;
import com.tilab.wade.performer.event.BeginApplication;
import com.tilab.wade.performer.event.BeginWorkflow;
import com.tilab.wade.performer.event.DelegatedSubflow;
import com.tilab.wade.performer.event.EndActivity;
import com.tilab.wade.performer.event.EndApplication;
import com.tilab.wade.performer.event.EndWorkflow;
import com.tilab.wade.performer.ontology.ControlInfo;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.utils.DFUtils;

public class LauncherGUI extends JFrame  implements WorkflowResultListener{

	private static Color DEFAULT_COLOR = new Color(251, 249, 249);

	private Agent myAgent;

	private ExitAction exitAction;
	private LaunchAction launchAction;
	private KillAction killAction;
	private ResetAction resetAction;

	private LauncherPanel launcherPanel;
	private EventsPanel eventsPanel;
	private List<String> wadeWorkflowList = new ArrayList<String>();
	private DispatchingCapabilities dc = new DispatchingCapabilities();
	
	int cnt = 0;
	static final long CA_TIMEOUT = 30000;
	
	private String workflowExecutionId;

	public LauncherGUI(Agent a) {
		super();

		myAgent = a;

		this.setLocation(100, 100);
		setTitle("WORKFLOW LAUNCHER");
		setBackground(DEFAULT_COLOR);

		// Actions
		URL url = LauncherGUI.class.getClassLoader().getResource("com/tilab/wade/tools/launcher/gui/images/start.png");
		ImageIcon icon = new ImageIcon(url);
		launchAction = new LaunchAction(this, icon);
		url = LauncherGUI.class.getClassLoader().getResource("com/tilab/wade/tools/launcher/gui/images/stop.png");
		icon = new ImageIcon(url);
		killAction = new KillAction(this, icon);
		url = LauncherGUI.class.getClassLoader().getResource("com/tilab/wade/tools/launcher/gui/images/reset.png");
		icon = new ImageIcon(url);
		resetAction = new ResetAction(this, icon);
		url = LauncherGUI.class.getClassLoader().getResource("com/tilab/wade/tools/launcher/gui/images/exit.png");
		icon = new ImageIcon(url);
		exitAction = new ExitAction(this, icon);

		// Add main menu to the GUI window
		JMenuBar jmb = new JMenuBar();
		JMenu menu = null;
		JMenuItem item = null;

		menu = new JMenu("File");
		item = menu.add(exitAction);
		jmb.add(menu);

		menu = new JMenu("Workflow");
		item = menu.add(launchAction);
		item = menu.add(killAction);
		item = menu.add(resetAction);
		jmb.add(menu);

		jmb.setBackground(DEFAULT_COLOR);
		setJMenuBar(jmb);

		// Add Toolbar to the NORTH part of the border layout 
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setBorder(null);
		bar.setBackground(DEFAULT_COLOR);
		bar.setBorderPainted(false);
		enrichButton(bar.add(launchAction), "Launch workflow");
		enrichButton(bar.add(killAction), "Kill workflow");
		enrichButton(bar.add(resetAction), "Reset form");		
		bar.addSeparator();
		enrichButton(bar.add(exitAction), "Exit");
		getContentPane().setBackground(DEFAULT_COLOR);
		getContentPane().add(bar, BorderLayout.NORTH);		

		// Add Launcher Panel to the CENTER part of the border layout
		launcherPanel = new LauncherPanel(this);
		getContentPane().add(launcherPanel, BorderLayout.CENTER);

		// Add Events Panel to the EAST part of the border layout
		eventsPanel = new EventsPanel(this);
		getContentPane().add(eventsPanel, BorderLayout.EAST);
		
		// Add window closing listener
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				handleExit();
			}
		});

		populatePerformer();
		populateWadeWorkflows();
		refreshActionsStatus();

		dc.init(myAgent);
	}

	public void handleExit() {
		int answer = JOptionPane.showConfirmDialog(this, "Are you sure to exit?");
		if (answer == JOptionPane.YES_OPTION) {
			myAgent.doDelete();
		}
	}

	public void handleLaunch() {
		String performerName = launcherPanel.getWorkflowPanel().getPerformerName();
		String workflowName = launcherPanel.getWorkflowPanel().getWorkflowName();
		if (performerName == null){
			JOptionPane.showMessageDialog(this, "Performer can't be null", "WARNING", JOptionPane.ERROR_MESSAGE);
		}else if (workflowName == null){
			JOptionPane.showMessageDialog(this, "Workflow can't be null", "WARNING", JOptionPane.ERROR_MESSAGE);
		}else{
			WorkflowDescriptor wd = new WorkflowDescriptor(workflowName, 
					ElementDescriptor.paramListToMap(launcherPanel.getParameterPanel().getParameters()));
			wd.setTransactional(launcherPanel.getWorkflowPanel().isTransactional());
			wd.setSessionId(launcherPanel.getWorkflowPanel().getSessionId());
			jade.util.leap.List infos = new jade.util.leap.ArrayList();
			infos.add(new ControlInfo(Constants.FLOW_TYPE, myAgent.getAID(), launcherPanel.getWorkflowPanel().getFlowLevel()));
			
			AID performer = new AID(performerName,false);
			try {
				dc.launchWorkflow(performer, wd, this, infos);
			} catch (WorkflowException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Workflow launching error: "+e.getMessage(), "WARNING", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void handleKill() {
		if (workflowExecutionId != null) {
			String performerName = launcherPanel.getWorkflowPanel().getPerformerName();
			AID performer = new AID(performerName,false);
			try {
				dc.killWorkflow(performer, workflowExecutionId);
				refreshActionsStatus();
			} catch (WorkflowException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Workflow killing error: "+e.getMessage(), "WARNING", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void handleReset() {
		launcherPanel.reset();
		eventsPanel.reset();
		refreshActionsStatus();
	}
	
	private void enrichButton(JButton button, String toolTipText) {
		button.setToolTipText(toolTipText);
		button.setBorderPainted(false);
		button.getAction().setEnabled(false);
		button.setBackground(DEFAULT_COLOR);
		button.setFocusPainted(false);
	}

	void refreshActionsStatus(){
		String workflowName = launcherPanel.getWorkflowPanel().getWorkflowName();
		boolean worflowNameOk = workflowName != null && !"".equals(workflowName);
		
		String workflowStatus = launcherPanel.getStatusPanel().getStatus();
		boolean workflowRunning = workflowStatus != null && workflowStatus.equals(StatusPanel.RUNNING_STATUS);
		
		String performerName = launcherPanel.getWorkflowPanel().getPerformerName();
		boolean performerNameOk = performerName != null && !"".equals(performerName);
		
		boolean parametersOk = launcherPanel.getParameterPanel().checkInputParameters();
		
		launchAction.setEnabled(worflowNameOk && performerNameOk && parametersOk && !workflowRunning);
		killAction.setEnabled(workflowRunning);
		resetAction.setEnabled(!workflowRunning);
		exitAction.setEnabled(!workflowRunning);
		
		launcherPanel.getWorkflowPanel().setFieldsEnabled(!workflowRunning);
		launcherPanel.getParameterPanel().setFieldsEnabled(!workflowRunning);
	}

	void populateWadeWorkflows(){
		launcherPanel.getWorkflowPanel().setWadeWorkflowList(getWadeWorkflows());
	}
	
	List getWadeWorkflows() {
		AID ca = null;
		wadeWorkflowList = new ArrayList<String>();
		try {
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			AgentType caType = TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE);
			DFAgentDescription[] dfadList;
			dfadList = DFUtils.searchAllByType(myAgent, caType, null);
			AID [] aidList = DFUtils.getAIDs(dfadList);
			if (aidList.length > 0)
				ca = aidList[0];
			if (ca != null){

				request.addReceiver(ca);
				request.setOntology(DeploymentOntology.ONTOLOGY_NAME);
				request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
				Action action = new Action();
				action.setActor(ca);
				GetWorkflowList workflowList = new GetWorkflowList();
				action.setAction(workflowList);
				request.setConversationId(buildConversationalId());
				request.setReplyByDate(new Date(System.currentTimeMillis() + CA_TIMEOUT));

				myAgent.getContentManager().fillContent(request, action);
				ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, request);
				if (reply != null){
					if (reply.getPerformative() == ACLMessage.INFORM){
						Result result = (Result)myAgent.getContentManager().extractContent(reply);
						jade.util.leap.List workflows = (jade.util.leap.List)result.getValue();
						for (jade.util.leap.Iterator iter = workflows.iterator(); iter.hasNext();){
							wadeWorkflowList.add(((WorkflowDetails)iter.next()).getClassName());
						}
					}
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return wadeWorkflowList;

	}

	private void populatePerformer(){
		// Subscribe to the DF in order to receive performer agent registrations/deregistration
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.addProperties(new Property(WadeAgent.AGENT_ROLE, WadeAgent.WORKFLOW_EXECUTOR_ROLE));
		template.addServices(sd);
		ACLMessage subscriptionMsg = DFService.createSubscriptionMessage(myAgent, myAgent.getDefaultDF(), template, null);
		myAgent.addBehaviour(new SubscriptionInitiator(myAgent, subscriptionMsg) {

			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for (int i = 0; i < dfds.length; ++i) {
						AID agent = dfds[i].getName();
						if (dfds[i].getAllServices().hasNext()) {
							launcherPanel.getWorkflowPanel().addPerformer(agent.getLocalName());
						} else {
							launcherPanel.getWorkflowPanel().removePerformer(agent.getLocalName());
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private jade.util.leap.List getWorkflowParameters(String workflowName) throws Exception{
		jade.util.leap.List parameters = null;
		if (wadeWorkflowList.contains(workflowName)){
			AID ca = null;
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			AgentType caType = TypeManager.getInstance().getType(WadeAgent.CONTROL_AGENT_TYPE);
			DFAgentDescription[] dfadList;
			dfadList = DFUtils.searchAllByType(myAgent, caType, null);
			AID [] aidList = DFUtils.getAIDs(dfadList);
			if (aidList.length > 0)
				ca = aidList[0];
			if (ca != null){
				request.addReceiver(ca);
				request.setOntology(DeploymentOntology.ONTOLOGY_NAME);
				request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
				Action action = new Action();
				action.setActor(ca);
				GetWorkflowParameters wfParametersAction = new GetWorkflowParameters();
				wfParametersAction.setName(workflowName);
				action.setAction(wfParametersAction);
				request.setConversationId(buildConversationalId());
				request.setReplyByDate(new Date(System.currentTimeMillis() + CA_TIMEOUT));

				myAgent.getContentManager().fillContent(request, action);
				ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, request);
				if (reply != null){
					if (reply.getPerformative() == ACLMessage.INFORM){
						Result result = (Result)myAgent.getContentManager().extractContent(reply);
						Object o = result.getValue();
						parameters = (jade.util.leap.List)o;
					}else{
						throw new Exception(reply.getContent());
					}
				}
			}
		}else{
			WorkflowBehaviour myWorkflow = null;
			try{
				myWorkflow = (WorkflowBehaviour) Class.forName(workflowName).newInstance();
				parameters = myWorkflow.getFormalParameters();
			}catch(ClassNotFoundException e){
				throw new Exception("Workflow " + workflowName + " not found ");
			}
		}
		return parameters;
	}

	private synchronized String  buildConversationalId(){
		return myAgent.getLocalName() + "-" + System.currentTimeMillis() + "-" + cnt++;
	}
	
	void handleSelectedWorkflow(String workflowName){
		try{
			jade.util.leap.List workflowParameters = getWorkflowParameters(workflowName);
			launcherPanel.getParameterPanel().setParameters(workflowParameters);
		}catch (Exception e){
			JOptionPane.showMessageDialog(this, "Error: "+e.getMessage(), "WARNING", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void handleAssignedId(AID executor, String executionId) {
		WorkflowEventListener l = new DefaultEventListener(){
			public void handleBeginActivity(long time, BeginActivity event, AID executor, String executionId) {
				eventsPanel.addEvent("BeginActivity "+event.getName());
			}

			public void handleBeginApplication(long time, BeginApplication event, AID executor, String executionId) {
				eventsPanel.addEvent("BeginApplication "+event.getName());
			}

			public void handleDelegatedSubflow(long time, DelegatedSubflow event, AID executor, String executionId) {
				eventsPanel.addEvent("DelegatedSubflow "+event.getWorkflowId());
			}

			public void handleEndActivity(long time, EndActivity event, AID executor, String executionId) {
				eventsPanel.addEvent("EndActivity "+event.getName());
			}

			public void handleEndApplication(long time, EndApplication event, AID executor, String executionId) {
				eventsPanel.addEvent("EndApplication "+event.getName());
			}

			public void handleEndWorkflow(long time, EndWorkflow event, AID executor, String executionId) {
				eventsPanel.addEvent("EndWorkflow "+event.getName());
			}

			public void handleBeginWorkflow(long time, BeginWorkflow event, AID executor, String executionId) {
				launcherPanel.getStatusPanel().setStatus(StatusPanel.RUNNING_STATUS);
				launcherPanel.setFailureReason(null);
				refreshActionsStatus();
				eventsPanel.addEvent("");
				eventsPanel.addEvent("BeginWorkflow "+event.getName()+" (execId="+executionId+")");
			}
		};
		dc.setEventListener(l, Constants.FLOW_TYPE, executor, executionId);
		workflowExecutionId = executionId;
	}

	public void handleExecutionCompleted(jade.util.leap.List results,
			AID executor, String executionId) {
		launcherPanel.getParameterPanel().setResult(results);
		launcherPanel.getStatusPanel().setStatus(StatusPanel.COMPLETE_STATUS);
		refreshActionsStatus();
	}

	public void handleExecutionError(ExecutionError er, AID executor,
			String executionId) {
		launcherPanel.getStatusPanel().setStatus(StatusPanel.FAILED_STATUS);
		launcherPanel.setFailureReason(er.getReason());
		refreshActionsStatus();
	}

	public void handleLoadError(String reason) {
		launcherPanel.getStatusPanel().setStatus(StatusPanel.FAILED_STATUS);
		launcherPanel.setFailureReason(reason);
		refreshActionsStatus();
	}

	public void handleNotificationError(AID executor, String executionId) {
		launcherPanel.getStatusPanel().setStatus(StatusPanel.FAILED_STATUS);
		refreshActionsStatus();
	}
}
