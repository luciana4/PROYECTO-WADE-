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
package com.tilab.wade.wseg.ontology;

import java.util.logging.Level;

import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.util.Logger;

public class WebServicesExposureOntology extends BeanOntology {
	
	private static final long serialVersionUID = -4288397928505966460L;

	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	public static final String ONTOLOGY_NAME = "WebServicesExposure-ontology";

    // The singleton instance of this ontology
    private final static Ontology theInstance = new WebServicesExposureOntology();

    public final static Ontology getInstance() {
        return theInstance;
    }

    /**
     * Constructor
     */
    private WebServicesExposureOntology() {
        super(ONTOLOGY_NAME);

        try {
        	add(Expose.class);
        	add(Unexpose.class);
        	add(GetExposed.class);
        	add(GetServicesArchive.class);
        	add(GetServicesArchiveInfo.class);
        	
        	add(ServicesArchiveInfo.class);
        	add(ServiceInfo.class);
        }
        catch (Exception e){
        	myLogger.log(Level.SEVERE, "Error creating WebServicesExposure-ontology", e);
        }
    }
}
