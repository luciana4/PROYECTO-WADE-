package com.tilab.wade.utils;

import jade.content.ContentException;
import jade.content.onto.Ontology;

/**
 *  Class maintained for backward compatibility. 
 *  The XMLManager features are available on the JADE add-on XMLCodec
 */
public class XMLManager extends jade.content.lang.xml.XMLManager {
	
	public XMLManager() {
		super();
	}
	
	public XMLManager(String packageName) throws ContentException {
		super(packageName);
	}
	
	public XMLManager(Ontology onto) {
		super(onto);
	}
}
