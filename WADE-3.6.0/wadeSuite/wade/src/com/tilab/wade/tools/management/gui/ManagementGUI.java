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
package com.tilab.wade.tools.management.gui;

import jade.content.AgentAction;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import com.tilab.wade.ca.ModuleDeployer;
import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.cfa.ontology.ExportConfiguration;
import com.tilab.wade.cfa.ontology.GetConfigurations;
import com.tilab.wade.cfa.ontology.ImportConfiguration;
import com.tilab.wade.cfa.ontology.SaveConfiguration;
import com.tilab.wade.cfa.ontology.ShutdownPlatform;
import com.tilab.wade.cfa.ontology.StartupPlatform;
import com.tilab.wade.commons.TypeManager;
import com.tilab.wade.commons.WadeAgent;
import com.tilab.wade.utils.CFAUtils;
import com.tilab.wade.utils.DFUtils;

public class ManagementGUI extends JFrame {
	static final long CFA_TIMEOUT = 30000;

	private int cnt = 0;
	private StartupAction startupAction;
	private ShutdownAction shutdownAction;
	private SaveConfigurationAction saveConfigurationAction;
	private ImportConfigurationAction importConfigurationAction;
	private ExportConfigurationAction exportConfigurationAction;
	private DeployAction deployAction;
	private LauncherAction launcherAction;

	private JTextArea logArea;
	private StatusPanel statusPanel;

	// File choosers are declared as member variables to keep track of the current directory 
	JFileChooser xmlChooser;
	JFileChooser jarChooser;

	private Agent myAgent;
	private AID cfa;
	private DFAgentDescription caTemplate;
	private static Color DEFAULT_COLOR = new Color(251, 249, 249);
	private int launcherCounter = 0;

	public ManagementGUI(Agent a, AID cfa, DFAgentDescription caTemplate) {
		super();
		setPreferredSize(new Dimension(550, 450));
		setTitle("WADE");
		this.setBackground(DEFAULT_COLOR);

		myAgent = a;
		this.cfa = cfa;
		this.caTemplate = caTemplate;

		xmlChooser = new JFileChooser();
		xmlChooser.addChoosableFileFilter(new ExtensionFilter(".xml"));
		jarChooser = new JFileChooser();
		jarChooser.addChoosableFileFilter(new ExtensionFilter(".jar"));

		////Actions////
		URL url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/startup.png");
		ImageIcon icon = new ImageIcon(url);
		startupAction = new StartupAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/shutdown.png");
		icon = new ImageIcon(url);
		shutdownAction = new ShutdownAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/save.png");
		icon = new ImageIcon(url);
		saveConfigurationAction = new SaveConfigurationAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/import.png");
		icon = new ImageIcon(url);
		importConfigurationAction = new ImportConfigurationAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/export.png");
		icon = new ImageIcon(url);
		exportConfigurationAction = new ExportConfigurationAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/deploy.png");
		icon = new ImageIcon(url);
		deployAction = new DeployAction(this, icon);

		url = ManagementGUI.class.getClassLoader().getResource("com/tilab/wade/tools/management/gui/images/launcher.png");
		icon = new ImageIcon(url);
		launcherAction = new LauncherAction(this, icon);

		/////////////////////////////////////
		// Add main menu to the GUI window
		/////////////////////////////////////
		JMenuBar jmb = new JMenuBar();
		JMenu menu = null;
		JMenuItem item = null;

		// PLATFORM menu
		menu = new JMenu("Platform");
		item = menu.add(startupAction);
		item = menu.add(shutdownAction);
		jmb.add(menu);

		// CONFIGURATIONS menu
		menu = new JMenu("Configurations");
		item = menu.add(saveConfigurationAction);
		item = menu.add(importConfigurationAction);
		item = menu.add(exportConfigurationAction);
		jmb.add(menu);

		// CODE menu
		menu = new JMenu("Code");
		item = menu.add(deployAction);
		item = menu.add(launcherAction);
		jmb.add(menu);

		jmb.setBackground(DEFAULT_COLOR);
		setJMenuBar(jmb);

		/////////////////////////////////////////////////////
		// Add Toolbar to the NORTH part of the border layout 
		/////////////////////////////////////////////////////
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setBorder(null);
		bar.setBackground(DEFAULT_COLOR);
		bar.setBorderPainted(false);
		enrichButton(bar.add(startupAction), "Startup platform");
		enrichButton(bar.add(shutdownAction), "Shutdown platform");
		bar.addSeparator();
		enrichButton(bar.add(saveConfigurationAction), "Save running configuration");
		enrichButton(bar.add(importConfigurationAction), "Import a given configuration");
		enrichButton(bar.add(exportConfigurationAction), "Export the target configuration");
		bar.addSeparator();
		enrichButton(bar.add(deployAction), "Deploy a jar file");
		enrichButton(bar.add(launcherAction), "Workflow launcher");
		getContentPane().setBackground(DEFAULT_COLOR);
		getContentPane().add(bar, BorderLayout.NORTH);

		//////////////////////////////////////////////////////////////////
		// Add messages text area to the CENTER part of the border layout
		//////////////////////////////////////////////////////////////////
		logArea = new JTextArea();
		logArea.setEditable(false);
		JScrollPane sp = new JScrollPane(logArea);
		getContentPane().add(sp, BorderLayout.CENTER);

		//////////////////////////////////////////////////////////////////
		// Add Status Panel to the WEST part of the border layout
		//////////////////////////////////////////////////////////////////
		statusPanel = new StatusPanel();
		getContentPane().add(statusPanel, BorderLayout.WEST);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				handleExit();
			}
		});

		myAgent.addBehaviour(new StatusUpdater(this));
	}

	public void handleExit() {
		int answer = JOptionPane.showConfirmDialog(this, "Are you sure to exit?");
		if (answer == JOptionPane.YES_OPTION) {
			myAgent.doDelete();
		}
	}

	private void enrichButton(JButton button, String toolTipText) {
		button.setToolTipText(toolTipText);
		button.setBorderPainted(false);
		button.getAction().setEnabled(false);
		button.setBackground(DEFAULT_COLOR);

	}

	public void handleStartup() {
		try {
			ACLMessage request = createCfaRequest(new StartupPlatform());
			myAgent.send(request);

			MessageTemplate mt = MessageTemplate.MatchConversationId(request.getConversationId());
			ACLMessage reply = myAgent.blockingReceive(mt, CFA_TIMEOUT);
			if (reply != null) {
				if (reply.getPerformative() == ACLMessage.AGREE) {
					setStatus(ConfigurationOntology.STARTING_STATUS);
				} else {
					if (reply.getSender().equals(myAgent.getAMS())) {
						log("Error performing Startup-platform operation [Configuration agent " + cfa.getName() + " does not exist]");
					} else {
						log("Error performing Startup-platform operation [" + ACLMessage.getPerformative(reply.getPerformative()) + " reply received; content = " + reply.getContent() + "]");
					}
				}
			} else {
				log("Error performing Startup-platform operation [Timeout expired]");
			}
		}
		catch (Exception e) {
			handleException("Startup-platform", e);
		}
	}

	public void handleShutdown() {
		try {
			ShutdownPlatform sp = new ShutdownPlatform();
			int res = JOptionPane.showConfirmDialog(this, "Soft Shutdown?");
			if (res == JOptionPane.CANCEL_OPTION)
				return;
			if (res == JOptionPane.NO_OPTION){
				sp.setHardTermination(true);
			}

			ACLMessage request = createCfaRequest(sp);
			myAgent.send(request);

			MessageTemplate mt = MessageTemplate.MatchConversationId(request.getConversationId());
			ACLMessage reply = myAgent.blockingReceive(mt, CFA_TIMEOUT);
			if (reply != null) {
				if (reply.getPerformative() == ACLMessage.AGREE) {
					setStatus(ConfigurationOntology.SHUTDOWN_IN_PROGRESS);
				} else {
					if (reply.getSender().equals(myAgent.getAMS())) {
						log("Error performing Shutdown-platform operation [Configuration agent " + cfa.getName() + " does not exist]");
					} else {
						log("Error performing Shutdown-platform operation [" + ACLMessage.getPerformative(reply.getPerformative()) + " reply received; content = " + reply.getContent() + "]");
					}
				}
			} else {
				log("Error performing Shutdown-platform operation [Timeout expired]");
			}
		}
		catch (Exception e) {
			handleException("Shutdown-platform", e);
		}
	}

	public void handleSaveConfiguration() {
		try {
			ACLMessage request = createCfaRequest(new SaveConfiguration());
			ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, request, CFA_TIMEOUT);
			if (reply != null) {
				log("Configuration successfully saved");
			} else {
				log("Error performing Save-configuration operation [Timeout expired]");
			}

		}
		catch (Exception e) {
			handleException("Save configuration", e);
		}
	}

	public void handleImportConfiguration() {
		try {
			String name = selectConfiguration();
			if (name != null) {
				ImportConfiguration ic = new ImportConfiguration();
				ic.setName(name);
				ACLMessage message = createCfaRequest(ic);

				ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, message, CFA_TIMEOUT);
				if (reply != null) {
					log("Configuration " + name + " successfully imported");
				} else {
					log("Error performing Import-configuration operation [Timeout expired]");
				}
			}
		}
		catch (Exception e) {
			handleException("Import-configuration", e);
		}
	}

	private String selectConfiguration() throws Exception{
		String configurationName = null;

		GetConfigurations gc = new GetConfigurations();
		ACLMessage message = createCfaRequest(gc);

		ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, message, CFA_TIMEOUT);
		if (reply != null) {
			// Got configuration list
			if (reply.getPerformative() == ACLMessage.INFORM){
				Result result = (Result)myAgent.getContentManager().extractContent(reply);
				Collection configurations = (Collection)result.getValue();
				String [] confs = (String[])configurations.toArray(new String[0]);
				if (confs.length != 0){

					Object selectedValue = JOptionPane.showInputDialog(this, 
							"", "Select configuration name",
							JOptionPane.INFORMATION_MESSAGE, null,
							confs, confs[0]);
					configurationName = (String)selectedValue;
				}else{
					JOptionPane.showMessageDialog(this,"Sorry, not found configuration",
							"", JOptionPane.WARNING_MESSAGE);
				}
			}else{
				log("Received failure response performing get configuration operation");
			}

		} else {
			log("Error performing get configuration operation [Timeout expired]");
		}
		return configurationName;

	}

	public void handleExportConfiguration(String configurationName, String configurationDesc, boolean override) {
		try {
			ExportConfiguration ec = new ExportConfiguration();
			ec.setName(configurationName);
			ec.setDescription(configurationDesc);
			ec.setOverride(override);
			ACLMessage request = createCfaRequest(ec);
			ACLMessage reply = FIPAService.doFipaRequestClient(myAgent, request, CFA_TIMEOUT);
			if (reply != null) {
				log("Configuration successfully exported");
			} else {
				log("Error performing Export-configuration operation [Timeout expired]");
			}

		}
		catch (Exception e) {
			handleException("Export configuration", e);
		}
	}

	public void handleDeploy() throws Exception {
		int returnVal = jarChooser.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final String jarFilePathName = jarChooser.getSelectedFile().getAbsolutePath();
			jarChooser.setCurrentDirectory(jarChooser.getSelectedFile());
			myAgent.addBehaviour(new ModuleDeployer(jarFilePathName, true) {
				public int onEnd() {
					if (isSuccessfullyDeployed()) {
						log(jarFilePathName + " is been deployed corretly");
					} else {
						log(jarFilePathName + " isn't been deployed! "+getFailureReason());
					}
					return super.onEnd();
				}
			});
		}
	}

	public void handleMdb() {
		try {
			String name = myAgent.getLocalName() + "-MDBAdmin";
			AgentController ac = myAgent.getContainerController().createNewAgent(name, "com.tilab.wade.mdb.ProvisioningAgent", null);
			ac.start();
		}
		catch (Exception e) {
			handleException("Open MDB Administration", e);
		}
	}

	public void handleTracer() {
		try {
			String name = myAgent.getLocalName() + "-Tracer";
			AgentController ac = myAgent.getContainerController().createNewAgent(name, "com.tilab.wade.dispatcher.WorkflowDispatcherAgent", new String[]{"com.tilab.wade.debugger.controller.tracer.WfController"});
			ac.start();
		}
		catch (Exception e) {
			handleException("Start Tracer", e);
		}
	}

	public void handleLauncher() {
		try {
			String name = myAgent.getLocalName() + "-Launcher"+ launcherCounter++;
			AgentController ac = myAgent.getContainerController().createNewAgent(name, "com.tilab.wade.tools.launcher.LauncherAgent", null);
			ac.start();
		}
		catch (Exception e) {
			handleException("Start Launcher", e);
		}
	}

	/**
	 * Inner class ExtensionFilter
	 */
	private class ExtensionFilter extends FileFilter {
		private String extension;

		private ExtensionFilter(String ext) {
			super();
			extension = ext;
		}

		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			} else if (file.getName().endsWith(extension)) {
				return true;
			}
			return false;
		}

		public String getDescription() {
			return extension;
		}
	}

	synchronized void setStatus(String s) {
		if (!s.equals(statusPanel.getStatus())) {
			log("Platform status: " + s);
		}
		statusPanel.setStatus(s);
		enableAction();
	}

	synchronized void enableAction(){
		String s = statusPanel.getStatus();
		startupAction.setEnabled(ConfigurationOntology.DOWN_STATUS.equals(s));
		shutdownAction.setEnabled(CFAUtils.isPlatformActive(s));
		saveConfigurationAction.setEnabled(CFAUtils.isPlatformActive(s));
		importConfigurationAction.setEnabled(ConfigurationOntology.DOWN_STATUS.equals(s));
		exportConfigurationAction.setEnabled(true);
		deployAction.setEnabled(CFAUtils.isPlatformActive(s));
		launcherAction.setEnabled(true);
	}

	void log(String s) {
		logArea.append(s + "\n");
	}

	synchronized ACLMessage createCfaRequest(AgentAction action) throws OntologyException, Codec.CodecException {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.setOntology(ConfigurationOntology.getInstance().getName());
		request.addReceiver(cfa);
		request.setConversationId(myAgent.getLocalName() + "-" + System.currentTimeMillis() + "-" + cnt);
		cnt++;

		Action actExpr = new Action();
		actExpr.setActor(cfa);
		actExpr.setAction(action);
		myAgent.getContentManager().fillContent(request, actExpr);

		request.setReplyByDate(new Date(System.currentTimeMillis() + CFA_TIMEOUT));
		return request;
	}

	private void handleException(String op, Exception e) {
		e.printStackTrace();
		String msg = "Error performing " + op + " operation [" + e + "]";
		log(msg);
		JOptionPane.showMessageDialog(this, msg, "WARNING", JOptionPane.ERROR_MESSAGE);
	}

	private void handleException(String op) {
		log(op);
		JOptionPane.showMessageDialog(this, op, "WARNING", JOptionPane.ERROR_MESSAGE);
	}
}


