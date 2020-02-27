package com.tilab.wade.utils;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;

public class CFAUtils {

	public static boolean isPlatformActive(String platformStatus) {
		return platformStatus.equals(ConfigurationOntology.ACTIVE_STATUS) || 
			   platformStatus.equals(ConfigurationOntology.ACTIVE_WITH_WARNINGS_STATUS) ||
			   platformStatus.equals(ConfigurationOntology.ACTIVE_INCOMPLETE_STATUS);
	}
}
