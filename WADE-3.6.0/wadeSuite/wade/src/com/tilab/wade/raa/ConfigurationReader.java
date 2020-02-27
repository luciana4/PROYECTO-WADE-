/*****************************************************************
 WADE - Workflow and Agent Development Environment is a framework to develop 
 multi-agent systems able to execute tasks defined according to the workflow
 metaphor.
 Copyright (C) 2008 Telecom Italia S.p.A. 

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation, 
 version 2.1 of the License. 

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/
package com.tilab.wade.raa;

import jade.core.Agent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ConfigurationReader {

	private static final String ALLOCATION_RULE = "allocationRule";
	private static final String CONDITION = "condition";
	private static final String POLICY = "policy";
	private static final String PROPERTIES = "properties";
	private static final String NAME = "name";
	private static final Object PROPERTY = "property";
	private static final String VALUE = "value";

	private Document doc;
	private Agent myAgent;
	
	public ConfigurationReader (String fileName, Agent myAgent) throws ConfigurationException {
		doc = getDocument(fileName);
		this.myAgent = myAgent;
	}

	private  Document getDocument(String cfgPathName) throws ConfigurationException {
		Document doc= null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Try in the classpath
            URL input = ClassLoader.getSystemResource(cfgPathName);
            if (input == null) {
            	// If not found search from the package of the local class. In this way
            	// the raa configuration file is found both in the case it is specified as a/b.xml and in 
            	// the case it is specified as /a/b.xml
            	input = this.getClass().getResource(cfgPathName);
            }
            // If not found, try in the file system
            if (input == null) {
            	File f = new File(cfgPathName);
            	if (f.exists()) {
            		input = f.toURI().toURL();
            	}
            }
            if (input != null) {
				doc = builder.parse(input.openStream());
            }
            else {
            	throw new ConfigurationException("Configuration file "+cfgPathName+" not found");
            }
		} 
		catch (ConfigurationException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new ConfigurationException("Error reading configuration file "+cfgPathName, e);
		}
		return doc;
	}

	List<AllocationRule> getAllocationRules() throws ConfigurationException {
		List<AllocationRule> rules = new ArrayList<AllocationRule>();
		NodeList nodes = doc.getElementsByTagName(ALLOCATION_RULE);
		AllocationRule ar;
		for (int i = 0; i < nodes.getLength(); i++) {
			Element e = (Element)nodes.item(i);				
			ar = getAllocationRule(i+1, e);
			rules.add(ar);
		}		
		return rules;
	}

	private AllocationRule getAllocationRule(int ruleNumber, Element e) throws ConfigurationException	{
		NodeList children = e.getChildNodes();
		String key = Integer.toString(ruleNumber);
		AllocationRule ar = new AllocationRule(key);
		Node n;
		Element allocationRuleInnerElement;
		//System.out.println("NAME 2 "+e.getAttribute(NAME).trim());
		for (int i = 0; i < children.getLength(); ++i) {
			
			n = children.item(i);
			if (n instanceof Element) {
				allocationRuleInnerElement = (Element) n;
				String tag = allocationRuleInnerElement.getTagName();
				if (tag.equals(CONDITION)) {
					if (ar.getCondition() != null) {
						throw new ConfigurationException("too many conditions in element "+e);
					}
					ar.setCondition(new PolicyCondition(allocationRuleInnerElement.getTextContent()));
				} else if (tag.equals(POLICY)) {
					if (ar.getConfiguration() != null) {
						throw new ConfigurationException("too many policies in element "+e);
					}
					PolicyConfiguration pc = getPolicyConfiguration(key, allocationRuleInnerElement);
					ar.setConfiguration(pc);
				} else {
					throw new ConfigurationException("unrecognized tag in element "+e);
				}
			}
		}
		if (ar.getCondition() == null) {
			throw new ConfigurationException("missing condition in element "+e);
		}
		if (ar.getConfiguration() == null) {
			throw new ConfigurationException("missing policy in element "+e);
		}
		return ar;
	}

	private PolicyConfiguration getPolicyConfiguration(String ruleKey, Element e) throws ConfigurationException {
		NodeList children = e.getChildNodes();
		PolicyConfiguration pc = null;
		String className = null;
		Hashtable<String, String> properties = new Hashtable<String, String>();
		Node n;
		Element propertiesElement;
		className = e.getAttribute(NAME).trim();
		for (int i = 0; i < children.getLength(); ++i) {
			n = children.item(i);
			if (n instanceof Element) {
				propertiesElement = (Element)n;
				String tag = propertiesElement.getTagName();
				if (tag.equals(PROPERTIES)) {
					NodeList propertiesNodeList = propertiesElement.getChildNodes();
					Node n2;
					Element propertyElement;
					for (int j = 0; j < propertiesNodeList.getLength(); ++j) {
						n2 = propertiesNodeList.item(j);
						if (n2 instanceof Element) {
							propertyElement = (Element)n2;
							String tag2 = propertyElement.getTagName();
							if (tag2.equals(PROPERTY)) {
								String key = propertyElement.getAttribute(NAME).trim();
								if (properties.get(key) != null) {
									throw new ConfigurationException("duplicate property \""+key+"\" in allocationRule "+ruleKey);
								}
								properties.put(key, propertyElement.getAttribute(VALUE));
							} else {
								throw new ConfigurationException("(properties) unrecognized tag in element "+e);
							}
						}
					}
				} else {
					throw new ConfigurationException("(policy) unrecognized tag in elemnent "+e);
				}
			}
		}
		pc = new PolicyConfiguration(ruleKey, className, properties, myAgent);
		return pc;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String filename = "raa.xml";  
		ConfigurationReader cr = new ConfigurationReader(filename, null);
		List<AllocationRule> rules = cr.getAllocationRules();
		int i = 1;
		for (AllocationRule rule : rules) {
			System.out.println(rule);
		}
	}

}
