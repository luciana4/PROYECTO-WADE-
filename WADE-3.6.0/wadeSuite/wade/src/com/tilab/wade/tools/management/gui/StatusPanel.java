/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

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

import java.awt.*;

import java.util.Map;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;


/**
 */
public class StatusPanel extends JPanel {

	private String status;

	private ButtonGroup leds;
	private JRadioButton downLed;
	private JRadioButton activeLed;
	private JRadioButton startingLed;
	private JRadioButton shutdownInProgressLed;
	private JRadioButton activeWithWarningsLed;
	private JRadioButton activeIncompleteLed;
	private JRadioButton errorLed;

	private Icon ledOff = new ImageIcon(getClass().getResource("images/disabled.png"));
	private Icon startupIcon = new ImageIcon(getClass().getResource("images/activePlatform.png"));
	private Icon notActiveIcon =  new ImageIcon(getClass().getResource("images/notActivePlatform.png"));
	private Icon warningIcon =  new ImageIcon(getClass().getResource("images/platformWarnings.png"));
	private Icon incompleteIcon =  new ImageIcon(getClass().getResource("images/platformIncomplete.png"));
	private Icon inProgressIcon =  new ImageIcon(getClass().getResource("images/platformInProgress.png"));

	private Font myFont = new Font("Monospaced", Font.BOLD, 10);

	private Map ledMap = new HashMap();

	public StatusPanel(){
		super();
		leds = new ButtonGroup();
		build();
	}

	public void build(){
		Border line = BorderFactory.createEtchedBorder();
		JPanel ledPanel = new JPanel();
		ledPanel.setLayout(new BoxLayout(ledPanel, BoxLayout.Y_AXIS));
		ledPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		ledPanel.setBorder(BorderFactory.createTitledBorder(line, "Current State", TitledBorder.CENTER, TitledBorder.TOP, new Font("Dialog", Font.BOLD, 10)));

		activeLed = new JRadioButton("Active", startupIcon);
		activeLed.setFont(myFont);
		activeLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		activeLed.setDisabledSelectedIcon(startupIcon);
		activeLed.setDisabledIcon(ledOff);
		activeLed.setEnabled(false);
		leds.add(activeLed);
		ledMap.put(ConfigurationOntology.ACTIVE_STATUS, activeLed);
		ledPanel.add(activeLed);

		activeIncompleteLed = new JRadioButton("Active incomplete", incompleteIcon);
		activeIncompleteLed.setFont(myFont);
		activeIncompleteLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		activeIncompleteLed.setDisabledSelectedIcon(incompleteIcon);
		activeIncompleteLed.setDisabledIcon(ledOff);
		activeIncompleteLed.setEnabled(false);
		leds.add(activeIncompleteLed);
		ledMap.put(ConfigurationOntology.ACTIVE_INCOMPLETE_STATUS, activeIncompleteLed);
		ledPanel.add(activeIncompleteLed);
		
		activeWithWarningsLed = new JRadioButton("Active with warnings", warningIcon);
		activeWithWarningsLed.setFont(myFont);
		activeWithWarningsLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		activeWithWarningsLed.setDisabledSelectedIcon(warningIcon);
		activeWithWarningsLed.setDisabledIcon(ledOff);
		activeWithWarningsLed.setEnabled(false);
		leds.add(activeWithWarningsLed);
		ledMap.put(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS, activeWithWarningsLed);
		ledPanel.add(activeWithWarningsLed);

		startingLed = new JRadioButton("Starting", inProgressIcon);
		startingLed.setFont(myFont);
		startingLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		startingLed.setDisabledSelectedIcon(inProgressIcon);
		startingLed.setDisabledIcon(ledOff);
		startingLed.setEnabled(false);
		leds.add(startingLed);
		ledMap.put(ConfigurationOntology.STARTING_STATUS, startingLed);
		ledPanel.add(startingLed);

		shutdownInProgressLed = new JRadioButton("Shutdown in progress", inProgressIcon);
		shutdownInProgressLed.setFont(myFont);
		shutdownInProgressLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		shutdownInProgressLed.setDisabledSelectedIcon(inProgressIcon);
		shutdownInProgressLed.setDisabledIcon(ledOff);
		shutdownInProgressLed.setEnabled(false);
		leds.add(shutdownInProgressLed);
		ledMap.put(ConfigurationOntology.SHUTDOWN_IN_PROGRESS, shutdownInProgressLed);
		ledPanel.add(shutdownInProgressLed);

		errorLed = new JRadioButton("Error", notActiveIcon);
		errorLed.setFont(myFont);
		errorLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		errorLed.setDisabledSelectedIcon(notActiveIcon);
		errorLed.setDisabledIcon(ledOff);
		errorLed.setEnabled(false);
		leds.add(errorLed);
		ledMap.put(ConfigurationOntology.ERROR_STATUS, errorLed);
		ledPanel.add(errorLed);

		downLed = new JRadioButton("Down", notActiveIcon);
		downLed.setFont(myFont);
		downLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		downLed.setDisabledSelectedIcon(notActiveIcon);
		downLed.setDisabledIcon(ledOff);
		downLed.setEnabled(false);
		leds.add(downLed);
		ledMap.put(ConfigurationOntology.DOWN_STATUS, downLed);
		ledPanel.add(downLed);


		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(15));
		add(ledPanel);
	}

	void setStatus(String status) {
		JRadioButton led = (JRadioButton)ledMap.get(status);
		if(led != null) {
			this.status = status;
			led.setSelected(true);
		}
	}
	
	String getStatus() {
		return status;
	}

}
