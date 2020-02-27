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

package com.tilab.wade.tools.launcher.gui;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

public class StatusPanel extends JPanel {

	static final String RUNNING_STATUS = "RUNNING";
	static final String COMPLETE_STATUS = "COMPLETE";
	static final String FAILED_STATUS = "FAILED";

	private String status;

	private ButtonGroup leds;
	private JRadioButton runningLed;
	private JRadioButton completeLed;
	private JRadioButton failedLed;

	private Icon ledOff = new ImageIcon(getClass().getResource("images/disabled.png"));
	private Icon runningIcon = new ImageIcon(getClass().getResource("images/running.png"));
	private Icon completeIcon =  new ImageIcon(getClass().getResource("images/complete.png"));
	private Icon failedIcon =  new ImageIcon(getClass().getResource("images/failed.png"));

	private Font ledFont = new Font("Monospaced", Font.BOLD, 10);

	private Map<String, JRadioButton> ledMap = new HashMap<String, JRadioButton>();

	public StatusPanel(){
		super();
		initialize();
	}

	private void initialize(){
		leds = new ButtonGroup();
		
		Border line = BorderFactory.createEtchedBorder();
		JPanel ledPanel = new JPanel();
		ledPanel.setLayout(new BoxLayout(ledPanel, BoxLayout.X_AXIS));
		ledPanel.setAlignmentY(JPanel.CENTER_ALIGNMENT);

		runningLed = new JRadioButton("Running", runningIcon);
		runningLed.setFont(ledFont);
		runningLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		runningLed.setDisabledSelectedIcon(runningIcon);
		runningLed.setDisabledIcon(ledOff);
		runningLed.setEnabled(false);
		leds.add(runningLed);
		ledMap.put(RUNNING_STATUS, runningLed);
		ledPanel.add(runningLed);

		completeLed = new JRadioButton("Complete", completeIcon);
		completeLed.setFont(ledFont);
		completeLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		completeLed.setDisabledSelectedIcon(completeIcon);
		completeLed.setDisabledIcon(ledOff);
		completeLed.setEnabled(false);
		leds.add(completeLed);
		ledMap.put(COMPLETE_STATUS, completeLed);
		ledPanel.add(completeLed);

		failedLed = new JRadioButton("Failed", failedIcon);
		failedLed.setFont(ledFont);
		failedLed.setAlignmentX(JButton.LEFT_ALIGNMENT);
		failedLed.setDisabledSelectedIcon(failedIcon);
		failedLed.setDisabledIcon(ledOff);
		failedLed.setEnabled(false);
		leds.add(failedLed);
		ledMap.put(FAILED_STATUS, failedLed);
		ledPanel.add(failedLed);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(ledPanel);
	}

	void setStatus(String status) {
		JRadioButton led = ledMap.get(status);
		if(led != null) {
			this.status = status;
			led.setSelected(true);
		}
	}
	
	String getStatus() {
		return status;
	}
	
	void reset() {
		for(JRadioButton led : ledMap.values()) {
			led.setSelected(false);
		}
	}
}
