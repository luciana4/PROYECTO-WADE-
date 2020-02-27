package com.tilab.wade.tools.launcher.gui;

import jade.gui.ClassSelectionDialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.utils.GUIDGenerator;

public class WorkflowPanel extends JPanel {

	private JLabel workflowLabel;
	private JComboBox workflowComboBox;
	private JComboBox performerComboBox;
	private JLabel agentLabel;
	private JLabel sessionIdLabel;
	private JTextField sessionIdTextField;
	private JButton workflowButton;
	private JButton sessionIdButton;
	private JLabel transactionalLabel;
	private JCheckBox transactionalCheckBox;
	private JLabel verbosityLevelLabel;
	private JComboBox verbosityLevelBox;
	
	private LauncherGUI launcherGUI;
	private List workflowList;
	private ClassSelectionDialog csd;
	
	private static final String WORKFLOW = "WORKFLOW";
	private static final String ACTIVITY = "ACTIVITY";
	private static final String APPLICATION = "APPLICATION";
	
	

	public WorkflowPanel(LauncherGUI launcherGUI) {
		super();
		this.launcherGUI = launcherGUI;
		initialize();
	}

	private void initialize() {

		// Performer
		agentLabel = new JLabel("Performet agent:", JLabel.TRAILING);
		add(agentLabel);
		performerComboBox = new JComboBox();
		performerComboBox.setEditable(true);
		agentLabel.setLabelFor(performerComboBox);
		performerComboBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String performerName = (String)performerComboBox.getSelectedItem();
				if (performerName != null && !"".equals(performerName)) {
					launcherGUI.refreshActionsStatus();
				}
			}
		});
		add(performerComboBox);
		add(new JLabel());
		
		// Workflow
		workflowLabel = new JLabel("Workflow:", JLabel.TRAILING);
		add(workflowLabel);
		workflowComboBox = new JComboBox();
		workflowComboBox.setEditable(true);
		workflowLabel.setLabelFor(workflowComboBox);
		workflowComboBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if ("comboBoxChanged".equals(e.getActionCommand())) {
					String workflowName = (String)workflowComboBox.getSelectedItem();
					if (workflowName != null && !"".equals(workflowName)) {
						launcherGUI.handleSelectedWorkflow(workflowName);
						launcherGUI.refreshActionsStatus();
					}
				}
			}
		});
		add(workflowComboBox);
		workflowButton = new JButton("...");
		workflowButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (csd == null) {
					csd = new ClassSelectionDialog(new JDialog(), "Select Worfkflow", WorkflowBehaviour.class.getName());
				}
				if (csd.doShow(launcherGUI.getWadeWorkflows()) == ClassSelectionDialog.DLG_OK) {
					String workflowClassName = csd.getSelectedClassname();
					workflowComboBox.addItem(workflowClassName);
					workflowComboBox.setSelectedItem(workflowClassName);
					launcherGUI.refreshActionsStatus();
				}
			}
		});
		add(workflowButton);

		// SessionId
		sessionIdLabel = new JLabel("Session ID:", JLabel.TRAILING);
		add(sessionIdLabel);
		sessionIdTextField = new JTextField();
		sessionIdLabel.setLabelFor(sessionIdTextField);
		add(sessionIdTextField);
		sessionIdButton = new JButton("Generate");
		sessionIdButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				sessionIdTextField.setText(GUIDGenerator.getGUID());
			}
		});
		add(sessionIdButton);
		
		// Verbosity level
		verbosityLevelLabel = new JLabel("Verbosity Level:", JLabel.TRAILING);
		add(verbosityLevelLabel);
		verbosityLevelBox = new JComboBox();
		verbosityLevelBox.addItem(WORKFLOW);
		verbosityLevelBox.addItem(ACTIVITY);
		verbosityLevelBox.addItem(APPLICATION);
		
		verbosityLevelLabel.setLabelFor(verbosityLevelBox);
		add(verbosityLevelBox);
		add(new JLabel());
	
		// Transactional
		transactionalLabel = new JLabel("Transactional:", JLabel.TRAILING);
		add(transactionalLabel);
		transactionalCheckBox = new JCheckBox();
		transactionalCheckBox.setEnabled(true);
		transactionalCheckBox.setSelected(false);
		transactionalLabel.setLabelFor(transactionalCheckBox);
		add(transactionalCheckBox);
		add(new JLabel());
		
		setLayout(new SpringLayout());
		makeCompactGrid(this,
						5, 3, 	//rows, cols
						6, 6, 	//initX, initY
						6, 6);  //xPad, yPad
		
		setPreferredSize(new Dimension(500,160));
	}

	private static void makeCompactGrid(Container parent,
			int rows, int cols,
			int initialX, int initialY,
			int xPad, int yPad) {
		SpringLayout layout;
		try {
			layout = (SpringLayout)parent.getLayout();
		} catch (ClassCastException exc) {
			System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
			return;
		}

		//Align all cells in each column and make them the same width.
		Spring x = Spring.constant(initialX);
		for (int c = 0; c < cols; c++) {
			Spring width = Spring.constant(0);
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width,
						getConstraintsForCell(r, c, parent, cols).
						getWidth());
			}
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints =
					getConstraintsForCell(r, c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
		}

		//Align all cells in each row and make them the same height.
		Spring y = Spring.constant(initialY);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height,
						getConstraintsForCell(r, c, parent, cols).
						getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints =
					getConstraintsForCell(r, c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
		}

		//Set the parent's size.
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);
	}	

	private static SpringLayout.Constraints getConstraintsForCell(
			int row, int col,
			Container parent,
			int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component c = parent.getComponent(row * cols + col);
		return layout.getConstraints(c);
	}

	void addWorkflow(String workflowName){
		workflowComboBox.addItem(workflowName);
	}

	void removePerformer(String performerName) {
		performerComboBox.removeItem(performerName);
	}

	void addPerformer(String performerName) {
		performerComboBox.addItem(performerName);
	}
	
	void setWadeWorkflowList(List workflowList){
		this.workflowList = workflowList;
	}

	String getSessionId() {
		return sessionIdTextField.getText();
	}

	String getPerformerName() {
		return (String)performerComboBox.getSelectedItem();
	}

	String getWorkflowName() {
		return (String)workflowComboBox.getSelectedItem();
	}

	boolean isTransactional() {
		return transactionalCheckBox.isSelected();
	}

	void setFieldsEnabled(boolean enabled) {
		workflowComboBox.setEnabled(enabled);
		performerComboBox.setEnabled(enabled);
		sessionIdTextField.setEnabled(enabled);
		workflowButton.setEnabled(enabled);
		sessionIdButton.setEnabled(enabled);
		transactionalCheckBox.setEnabled(enabled);
		verbosityLevelBox.setEnabled(enabled);
	}
	
	void reset() {
		workflowComboBox.setSelectedItem(null);
		performerComboBox.setSelectedItem(null);
		sessionIdTextField.setText(null);
		transactionalCheckBox.setSelected(false);
		verbosityLevelBox.setSelectedItem(WORKFLOW);
	}

	int getFlowLevel() {
		if (verbosityLevelBox.getSelectedItem().equals(WORKFLOW)){
			return Constants.WORKFLOW_LEVEL;
		}if (verbosityLevelBox.getSelectedItem().equals(ACTIVITY)){
			return Constants.ACTIVITY_LEVEL;
		}else{
			return Constants.APPLICATION_LEVEL;
		}
	}	
}
