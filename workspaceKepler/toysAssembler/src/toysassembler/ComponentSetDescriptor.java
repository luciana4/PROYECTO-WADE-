package toysassembler;

/**
 * This class describes a set of toy components in terms of<br>
 * - type of components (e.g. head, leg, arm, wheel...)
 * - number of components 
 */
public class ComponentSetDescriptor {
	
	private String type;
	private int number;
	
	public ComponentSetDescriptor(int number, String type) {
		super();
		this.number = number;
		this.type = type;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	
	public String toString() {
		return toString(number, type);
	}
	
	public static final String toString(int n, String t) {
		return n+" "+t+(n > 1 ? "s" : "");
	}
}


