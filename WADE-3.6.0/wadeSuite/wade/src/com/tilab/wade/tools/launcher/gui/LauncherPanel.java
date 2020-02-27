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

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class LauncherPanel extends JPanel {

	private Font smallFont = new Font("Monospaced", Font.BOLD, 10);
	
	private WorkflowPanel workflowPanel;
	private ParametersPanel parametersPanel;
	private StatusPanel statusPanel;
	private JTextField failureReason;
	
	private LauncherGUI launcherGUI;
	
 
	public LauncherPanel(LauncherGUI launcherGUI) {
		super();
		this.launcherGUI = launcherGUI; 
		initialize();
	}

	private void initialize() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Workflow Selection
		workflowPanel = new WorkflowPanel(launcherGUI);
		add(workflowPanel);

		// Workflow Parameters
		JPanel workflowParametersPanel = new JPanel();
		workflowParametersPanel.add(new JLabel("Workflow Parameters"));
		add(workflowParametersPanel);
		parametersPanel = new ParametersPanel(launcherGUI);
		add(parametersPanel);

		// Workflow status
		JPanel workflowStatusPanel = new JPanel();
		workflowStatusPanel.add(new JLabel("Workflow Status"));
		add(workflowStatusPanel);
		statusPanel = new StatusPanel();
		add(statusPanel);
		
		// Failure reason
		JPanel failurePanel = new JPanel();
		JLabel failureReasonLabel = new JLabel("Failure Reason:");
		failureReasonLabel.setFont(smallFont);
		failurePanel.add(failureReasonLabel);
		failureReason = new JTextField();
		failureReason.setEditable(false);
		failureReason.setPreferredSize(new Dimension(350,20));
		failurePanel.add(failureReason);
		add(failurePanel);
	}

	ParametersPanel getParameterPanel(){
		return parametersPanel;
	}
	
	WorkflowPanel getWorkflowPanel(){
		return workflowPanel;
	}
	
	StatusPanel getStatusPanel(){
		return statusPanel;
	}

	public void setFailureReason(String msg) {
		failureReason.setText(msg);
	}
	
	void reset() {
		workflowPanel.reset();
		parametersPanel.reset();
		statusPanel.reset();
		failureReason.setText(null);
	}
}

