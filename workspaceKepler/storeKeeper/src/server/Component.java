package server;

import server.storekeeper.Point;



class Component {
	private int quantity;
	private Point coordinates;
	
	
	public Component(Point coordinates, int quantity) {
		super();
		this.coordinates = coordinates;
		this.quantity = quantity;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public Point getCoordinates() {
		return coordinates;
	}
	public void setCoordinates(Point coordinates) {
		this.coordinates = coordinates;
	}
	public boolean isAvailable() {
		return quantity>0;
	}	
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		sb.append("x=");
		sb.append(coordinates.getX());
		sb.append(" y=");
		sb.append(coordinates.getY());
		sb.append(" quantity=");
		sb.append(quantity);
		sb.append('}');
		return sb.toString();
	}
}

