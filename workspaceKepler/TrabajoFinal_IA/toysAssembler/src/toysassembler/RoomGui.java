package toysassembler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class RoomGui extends JFrame {

	private static final int ELEMENT_SIZE = 10;

	public int guiWidth;
	public int guiHeight;
	private Thread t;
	private static int index=0;

	private JPanel mainPanel;
	private boolean stopped = false;


	public RoomGui(final Room room, final int viewSize, String title) {
		super(title);

		guiWidth = room.width;
		guiHeight = room.height;

		setSize(new Dimension(guiWidth, guiHeight));

		mainPanel = new JPanel() {
			public void paint(Graphics g) {
				g.clearRect(0, 0, getWidth(), getHeight());

				// Draw the robot
				Point currentPosition = room.getRobotPosition();
				g.setColor(Color.black);
				g.fillArc(currentPosition.x - ELEMENT_SIZE/2, currentPosition.y - ELEMENT_SIZE/2, ELEMENT_SIZE, ELEMENT_SIZE, 0, 360);

				// Draw the robot view area
				g.setColor(Color.blue);
				g.drawArc(currentPosition.x - viewSize/2, currentPosition.y - viewSize/2, viewSize, viewSize, 0, 360);

				// Draw the target if present
				Point targetPosition = room.getActualTargetPosition();
				if (targetPosition != null) {
					g.setColor(Color.red);
					g.drawLine(targetPosition.x, targetPosition.y - ELEMENT_SIZE/2, targetPosition.x, targetPosition.y + ELEMENT_SIZE/2);
					g.drawLine(targetPosition.x - ELEMENT_SIZE/2, targetPosition.y, targetPosition.x + ELEMENT_SIZE/2, targetPosition.y);
				}

				// Draw the robot current destination if present
				Point dest = room.getRobotDestination();
				if (dest != null) {
					if (dest.equals(targetPosition)) {
						g.setColor(Color.green);
					}
					else {
						g.setColor(Color.blue);
					}
					g.drawLine(dest.x, dest.y - ELEMENT_SIZE/2, dest.x, dest.y + ELEMENT_SIZE/2);
					g.drawLine(dest.x - ELEMENT_SIZE/2, dest.y, dest.x + ELEMENT_SIZE/2, dest.y);
				}
			}
		};
		getContentPane().add(mainPanel, BorderLayout.CENTER);
	}

	public void start() {
		incrementIndex();
		int i=getIndex();
		setLocation(((i+1)%2)*(this.getSize().width), ((i+1)/3)*(this.getSize().height));
		t = new Thread() {
			public void run() {
				while (!stopped) {
					mainPanel.repaint();

					try {
						Thread.sleep(50);
					}
					catch (InterruptedException ie) {
					}
				}
			}
		};
		t.start();

		setVisible(true);
	}

	public void stop() {
		stopped = true;
		try {
			t.join(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}	

	public static final void waitABit(long ms) throws InterruptedException {
		Thread.sleep(ms);
	}

	public void showMessage(String message,String boxTitle, int dialogType) {
		JOptionPane.showMessageDialog(this,message,boxTitle,dialogType);
		dispose();
	}
	public static synchronized int getIndex(){
		return index;
	}
	public static synchronized void incrementIndex(){
		index=(index+1)%4;


	}
}

