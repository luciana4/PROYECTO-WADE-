package toysassembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Catalogue {
	private static Map toyComponents = new HashMap();
	
	static{
		// Required components to assemble a PUPPET
		List l = new ArrayList();
		l.add(new ComponentSetDescriptor(1, "head"));
		l.add(new ComponentSetDescriptor(1, "body"));
		l.add(new ComponentSetDescriptor(2, "arm"));
		l.add(new ComponentSetDescriptor(2, "leg"));
		toyComponents.put("PUPPET", l);
		
		// Required components to assemble a WAGON
		l = new ArrayList();
		l.add(new ComponentSetDescriptor(1, "cockpit"));
		l.add(new ComponentSetDescriptor(4, "wheel"));
		toyComponents.put("WAGON", l);
	}	

	public static Iterator getToyTypes() {
		return toyComponents.keySet().iterator();
	}
	
	public static List getRequiredComponents(String toyType) {
		return (List) toyComponents.get(toyType);
	}
}
