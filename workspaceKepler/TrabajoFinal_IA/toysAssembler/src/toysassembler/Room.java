package toysassembler;

import java.awt.Point;
import java.io.Serializable;

public class Room implements Serializable {

	public int width;
	public int height;

	private Point nominalTargetPosition;
	private Point actualTargetPosition;
	private Point currentPosition;
	private Point currentDestination;

	public Room (int nominalTargetX, int nominalTargetY){
		width = 500;
		height = 300;
		setRobotPosition(0, 0);
		
		nominalTargetPosition = new Point(nominalTargetX, nominalTargetY);
		if (componentMoved()) {
			// Select a new position randomly
			actualTargetPosition = getRandomPosition();
		}
		else {
			actualTargetPosition = nominalTargetPosition;
		}
	}

	public Point getNominalTargetPosition() {
		return nominalTargetPosition;
	}
	
	Point getActualTargetPosition(){
		return actualTargetPosition;
	}

	private void setRobotPosition(int x, int y) {
		currentPosition = new Point (x,y);
	}

	public Point getRobotPosition() {
		return currentPosition;
	}
	
	public Point getRobotDestination() {
		return currentDestination;
	}

	public void setRobotDestination(Point p) {
		currentDestination = p;
	}

	public Point look(int viewSize) {
		Point cp = getRobotPosition();
		Point tp = actualTargetPosition;

		int deltaX = tp.x - cp.x;
		int deltaY = tp.y - cp.y;
		double dist = Math.sqrt(deltaX*deltaX + deltaY*deltaY);

		if (dist <= viewSize/2) {
			return actualTargetPosition;
		}
		else {
			return null;
		}
	}

	public  void move(int speed) throws InterruptedException {
		Point cp =  getRobotPosition();
		Point dp = currentDestination;
		int deltaX = Math.abs(dp.x - cp.x);
		int deltaY = Math.abs(dp.y - cp.y);
		double dist = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
		
		if (dist < speed) {
			// Reach the destination
			setRobotPosition(dp.x,dp.y);
		}
		else {
			// Move towards the destination
			double signX = (deltaX != 0 ? (dp.x - cp.x) / (double) deltaX : 1);
			double signY = (deltaY != 0 ? (dp.y - cp.y) / (double) deltaY : 1);
			double sin = deltaY / dist;
			double cos = deltaX / dist;
			int x = (int) (speed * cos * signX);
			int y = (int) (speed * sin * signY);
			setRobotPosition(cp.x + x, cp.y + y);
		} 
		RoomGui.waitABit(50);
	}

	//////////////////////////////////////////////////////////////////////////////
	// The following methods are used to simulate the case where someone moved 
	// some components from their nominal position
	//////////////////////////////////////////////////////////////////////////////
	private boolean componentMoved() {
		double d = Math.random();
		return d > 0.7;
	}
	
	private Point getRandomPosition() {
		int x = (int) (Math.random() * width);
		int y = (int) (Math.random() * (height-20));
		return new Point(x, y);
	}
}
