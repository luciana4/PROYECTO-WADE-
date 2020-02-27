package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import server.storekeeper.Point;




public class StoreKeeper {
	
	private static StoreKeeper anInstance;
	
	private Map <String, Component> store;
	
	public static StoreKeeper getInstance(){
		if (anInstance == null){
			anInstance = new StoreKeeper();
		}
		return anInstance;
	}

	public void init() throws IOException, SAXException, ParserConfigurationException {
		SimpleParser parser = new SimpleParser();
		parser.parse("/server/components.xml");
		store=parser.getParserComponents();
		
	}
	
	public Component getComponents(String type,int quantity) {
		Component component = store.get(type);
		if (component!=null){		
			int i = component.getQuantity()-quantity;
			component.setQuantity(i);
		}
		return component;	
	}
	
	public Map getMap(){
		return store;
	}
	
}
