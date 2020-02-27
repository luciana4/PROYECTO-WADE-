package toysassembler;

/**
 * This class represent a toy component
 */
public class Component implements java.io.Serializable {
	private String type;
	private int serialNumber;
	
	private static int serialCnt = 0;
	
	public Component(String type) {
		this.type = type;
		serialNumber = serialCnt++;
	}

	public String getType() {
		return type;
	}
	
	public int getSerialNumber() {
		return serialNumber;
	}
	
	public String toString() {
		return type+"(serial-number = "+serialNumber+")";
	}
}
