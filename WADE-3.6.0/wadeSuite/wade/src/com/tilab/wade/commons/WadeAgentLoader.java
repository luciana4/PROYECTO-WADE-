package com.tilab.wade.commons;

import com.tilab.wade.ca.CAServices;

import jade.core.Agent;
import jade.util.ObjectManager.Loader;
import jade.util.leap.Properties;

public class WadeAgentLoader implements Loader {

	private Agent agent;
	
	public WadeAgentLoader(Agent agent) {
		this.agent = agent;
	}
	
	@Override
	public Object load(String className, Properties pp) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		// Security check
		if (agent == null || agent.here() == null) {
			return null;
		}
		
		try {
			ClassLoader classLoader = CAServices.getInstance(agent).getDefaultClassLoader();
			return Class.forName(className, true, classLoader).newInstance();
		} catch(ClassNotFoundException cnfe) {
			// If the class is not present in WADE classloader return null to try with next loader
			return null;
		}
	}
}
