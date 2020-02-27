package com.tilab.wade.tools.launcher.gui;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class EventsPanel extends JPanel {

	private LauncherGUI launcherGUI;
	private JTextArea eventsArea;
	
	public EventsPanel(LauncherGUI launcherGUI) {
		super();
		this.launcherGUI = launcherGUI;
		initialize();
	}

	private void initialize() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JPanel workflowEventsPanel = new JPanel();
		workflowEventsPanel.add(new JLabel("Workflow Events"));
		add(workflowEventsPanel);
		
		eventsArea = new JTextArea(20,25);
		eventsArea.setEditable(false);
		JScrollPane eventsAreaPane = new JScrollPane(eventsArea);
		eventsAreaPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		eventsAreaPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		add(eventsAreaPane);
		
	}	
	
	void addEvent(String event) {
		eventsArea.append(event + "\n");
		eventsArea.setCaretPosition(eventsArea.getDocument().getLength());

	}
	
	void reset() {
		eventsArea.setText(null);
	}
}
