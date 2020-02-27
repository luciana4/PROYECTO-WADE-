package pizza;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class PizzaProducer extends JPanel {

	private JPanel mainPanel;
	private JTextField clientNameText = new JTextField();
    private JTextField pizzaTypeText = new JTextField();

    
	public PizzaProducer() {
		setLayout(new BorderLayout());
	
		// Input panel
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
        
        // ClientName
        JLabel typeLabel = new JLabel("Client name:");
        c.gridx = 0;
        c.gridy = 1;
        inputPanel.add(typeLabel, c);
        clientNameText.setPreferredSize(new Dimension(180, 20));
        clientNameText.requestFocus();
        c.gridx = 1;        
        inputPanel.add(clientNameText, c);

        // PizzaType
        JLabel timeToLeaveLabel = new JLabel("Pizza type:");
        c.gridx = 0;
        c.gridy = 2;
        inputPanel.add(timeToLeaveLabel, c);
        pizzaTypeText.setPreferredSize(new Dimension(180, 20));
        pizzaTypeText.requestFocus();
        c.gridx = 1;        
        inputPanel.add(pizzaTypeText, c);
	
        // Button panel
        JPanel buttonPanel = new JPanel();
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;
        buttonPanel.setLayout(gridbag);

        JButton sendButton = new JButton("Delivery");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String clientName = clientNameText.getText();
            	if (clientName == null || clientName.length() == 0) {
            		JOptionPane.showMessageDialog(mainPanel, "Client name is mandatory");
            		return;
            	}
            	String pizzaType = pizzaTypeText.getText();
            	if (pizzaType == null || pizzaType.length() == 0) {
            		JOptionPane.showMessageDialog(mainPanel, "Pizza type is mandatory");
            		return;
            	}
    			
    			try {
    		        PizzaBindingStub binding = (PizzaBindingStub)new PizzaServiceLocator().getPizzaPort();
    		        binding.setTimeout(5000);
    		        binding.pizzaReady(clientName, pizzaType);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(mainPanel, "Error delivering pizza, "+e1.getMessage());
					e1.printStackTrace();
					return;
				}
            }
        });
        c.gridx = 0;
        c.gridy = 0;
        buttonPanel.add(sendButton, c);

        JButton clearButton = new JButton("Clear form");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	clientNameText.setText("");
            	pizzaTypeText.setText("");
            }
        });
        c.gridx = 2;
        c.gridy = 0;
        buttonPanel.add(clearButton, c);
        
        //Put everything together.
        mainPanel = new JPanel(gridbag);
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(inputPanel, c);
        c.gridx = 0;
        c.gridy = 1;
        mainPanel.add(buttonPanel, c);
        add(mainPanel, BorderLayout.LINE_START);
	}
        
	
	public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Make sure we have nice window decorations.
                JFrame.setDefaultLookAndFeelDecorated(true);

                //Create and set up the window.
                JFrame frame = new JFrame("Pizza Producer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                //Create and set up the content pane.
                JComponent newContentPane = new PizzaProducer();
                newContentPane.setOpaque(true); //content panes must be opaque
                frame.setContentPane(newContentPane);

                //Display the window.
                frame.pack();
                frame.setPreferredSize(new Dimension(300, 250));
                frame.setVisible(true);
            }
        });
	}
}
