package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import server.storekeeper.Point;

public class SimpleParser extends DefaultHandler {

	private static final String COMPONENT = "component";

	private static final String NAMESPACE = "";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_X = "x";
	private static final String ATTR_Y = "y";
	private static final String ATTR_QUANTITY = "quantity";

	private Map<String, Component> components;

	public SimpleParser() {
		components = new HashMap<String, Component>();
	}
	
	public Map<String, Component> getParserComponents(){
		return components;
	}

	public void parse(String resourcepath) throws IOException, SAXException, ParserConfigurationException {
		InputStream resourceAsStream = null;
		try {
			SAXParserFactory newInstance = SAXParserFactory.newInstance();
			SAXParser newSAXParser = newInstance.newSAXParser();
			XMLReader reader = newSAXParser.getXMLReader();
			reader.setContentHandler(this);
			resourceAsStream = getClass().getResourceAsStream(resourcepath);
			if (resourceAsStream == null) {
				throw new NullPointerException("resource "+resourcepath+" not found!");
			}
			InputStreamReader isr = new InputStreamReader(resourceAsStream);
			InputSource isource = new InputSource(isr);
			reader.parse(isource);
		} finally {
			if (resourceAsStream != null) {
				resourceAsStream.close();
			}
		}
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if (COMPONENT.equals(name)) {
			String componentName = attributes.getValue(NAMESPACE, ATTR_NAME);
			int x = Integer.parseInt(attributes.getValue(NAMESPACE, ATTR_X));
			int y = Integer.parseInt(attributes.getValue(NAMESPACE, ATTR_Y));
			Point coordinates=new Point(x,y);
			int quantity = Integer.parseInt(attributes.getValue(NAMESPACE, ATTR_QUANTITY));
			Component c = new Component(coordinates, quantity);
			components.put(componentName, c);
			System.out.println("added "+componentName+" "+c);
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
	}
}
