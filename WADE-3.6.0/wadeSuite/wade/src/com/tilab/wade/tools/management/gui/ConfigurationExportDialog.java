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

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;

public class ConfigurationExportDialog extends JDialog {
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JTextField configurationName;
	private JTextField configurationDesc;
	private JCheckBox override;
	private boolean isCanceled=false;

	public ConfigurationExportDialog() {
		buildUI();
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}

		});

		// call onCancel() when cross is clicked
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onCancel();
			}
		});

		// call onCancel() on ESCAPE
		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		this.setTitle("Configuration Export");
		this.pack();
		this.setVisible(true);
	}

	private void onOK() {
		isCanceled=false;
		dispose();

	}

	private void onCancel() {
		isCanceled=true;
		dispose();

	}

	private void buildUI() {
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(gridbag);
        inputPanel.setBorder(
                		BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(""),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Configuration name
        JLabel typeLabel = new JLabel("Configuration name");
        c.gridx = 0;
        c.gridy = 1;
        inputPanel.add(typeLabel, c);
        configurationName = new JTextField();
        configurationName.setPreferredSize(new Dimension(180, 20));
        configurationName.requestFocus();
        c.gridx = 1;        
        inputPanel.add(configurationName, c);

        // Description
        JLabel timeToLeaveLabel = new JLabel("Description");
        c.gridx = 0;
        c.gridy = 2;
        inputPanel.add(timeToLeaveLabel, c);
        configurationDesc = new JTextField();
        configurationDesc.setPreferredSize(new Dimension(180, 20));
        configurationDesc.requestFocus();
        c.gridx = 1;        
        inputPanel.add(configurationDesc, c);
		
        // Override
        JLabel parametersLabel = new JLabel("Override");
        c.gridx = 0;
        c.gridy = 3;
        inputPanel.add(parametersLabel, c);
        override = new JCheckBox();
        override.requestFocus();
        c.gridx = 1;        
        inputPanel.add(override, c);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;
        buttonPanel.setLayout(gridbag);
        c.gridx = 0;
        c.gridy = 0;
        buttonOK = new JButton("OK");
        buttonPanel.add(buttonOK, c);
        c.gridx = 1;
        c.gridy = 0;
        buttonCancel = new JButton("Cancel");
        buttonPanel.add(buttonCancel, c);
        
        //Put everything together.
        contentPane = new JPanel(gridbag);
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        contentPane.add(inputPanel, c);
        c.gridx = 0;
        c.gridy = 1;
        contentPane.add(buttonPanel, c);
	}

	public String getConfigurationName() {
		return configurationName.getText();
	}

	public String getConfigurationDesc() {
		return configurationDesc.getText();
	}

	public boolean getOverride() {
		return override.isSelected();
	}

	public boolean isCanceled() {
		return isCanceled;
	}
}
