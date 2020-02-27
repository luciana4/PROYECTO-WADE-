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
package com.tilab.wade.cfa.beans;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import com.tilab.wade.cfa.ontology.ConfigurationOntology;
import com.tilab.wade.utils.FileUtils;
import com.tilab.wade.utils.XMLManager;

public class PlatformLoaderXML {

	private String configurationPath;
	
	// FIXME: Eliminare secondo parametro
	public PlatformLoaderXML(String path, String rules) {
		this.configurationPath = path;
	}

	/*
	 * Ritorna il file xml identificato da name
	 */
	private File getConfigurationFile(String name) {

		String configurationFile = configurationPath + FileUtils.SEPARATOR + name + ".xml";
		
		File xmlFile = new File(configurationFile);

		return xmlFile;
	}

	public boolean existConfiguration(String name) throws PlatformLoaderException {
		
		// Controllo correttezza nome
		if (name == null) {
			throw new PlatformLoaderException("Configuration name not inizialized");
		}

		// Costruisco il nome del file
		File input = getConfigurationFile(name);

		// Controllo se il file esiste
		return input.isFile();
	}

	/*
	 * Carica la configurazione
	 */
	public PlatformInfo loadConfiguration(String name) throws PlatformLoaderException {

		// Controllo correttezza nome
		if (name == null) {
			throw new PlatformLoaderException("Configuration name not inizialized");
		}

		// Costruisco il nome del file
		File input = getConfigurationFile(name);

		// Controllo se il file esiste
		if (!input.isFile()) {
			throw new PlatformLoaderException("Configuration file <" + name + "> not present");
		}

		try {
			XMLManager manager = new XMLManager(getOntology());
			return (PlatformInfo) manager.decode(input);
		}
		catch (Exception e) {
			throw new PlatformLoaderException("Configuration file <" + name + "> decoding error", e);
		}
	}

	/*
	 * Salva la configurazione
	 */
	public void storeConfiguration(String name, PlatformInfo platformInfo, boolean override) throws PlatformLoaderException {

		// Controllo correttezza contesto e nome file 
		if (name == null) {
			throw new PlatformLoaderException("Configuration name not inizialized");
		}

		if (platformInfo == null) {
			throw new PlatformLoaderException("Platform context not inizialized");
		}

		// Ottengo il file da scrivere
		File output = getConfigurationFile(name);

		// Controllo l'override 
		if (output.isFile() && !override) {
			throw new PlatformLoaderException("Configuration file <" + name + "> not rewritable");
		}

		// Codifico in XML e salvo su file
		try {
			XMLManager manager = new XMLManager(getOntology());
			manager.encodeToFile(platformInfo, output);
		} catch (Exception e) {
			throw new PlatformLoaderException("Error encoding configuration <" + name + ">", e);
		}
	}
	
	/*
	 * Elimina la configurazione specificata
	 */
	public void deleteConfiguration(String name) throws PlatformLoaderException {

		// Controllo correttezza contesto e nome file 
		if (name == null) {
			throw new PlatformLoaderException("Configuration name not inizialized");
		}

		// Ottengo il file da cancellare 
		File delete = getConfigurationFile(name);

		// Cancello il file 
		if (!delete.delete()) {
			throw new PlatformLoaderException("Problem deleting configuration file <" + name + ">");
		}
	}

	/*
	 * Ritorna la lista delle configurazioni presenti in configurationPath
	 */
	public Collection<String> getConfigurations() throws PlatformLoaderException {

		String[] fileList = FileUtils.getAllFilelist(configurationPath);
		if (fileList == null) {
			throw new PlatformLoaderException("Configuration path "+configurationPath+" doesn't exist or is not a directory");
		}

		Vector confList = new Vector();
		for (int i = 0; i < fileList.length; i++) {

			String fileName = fileList[i];
			if (fileName.toLowerCase().endsWith(".xml")) {
				confList.add(fileName.substring(0, fileName.lastIndexOf('.')));
			}
		}

		Collection<String> configurations = new HashSet<String>();
		configurations.addAll(confList);
		return configurations;
	}
	
    protected Ontology getOntology() throws OntologyException {
    	// Override on the fly the schema for the HostInfo concept to avoid writing to the configuration file 
    	// availability and reachability information
		Ontology dummy = new jade.content.onto.Ontology("dummy", ConfigurationOntology.getInstance(), new jade.content.onto.CFReflectiveIntrospector());
		ConceptSchema hostSchema = new ConceptSchema(ConfigurationOntology.HOST);		 
		hostSchema.addSuperSchema((ConceptSchema) dummy.getSchema(ConfigurationOntology.PLATFORM_ELEMENT));
		hostSchema.add(ConfigurationOntology.HOST_NAME, (PrimitiveSchema) dummy.getSchema(BasicOntology.STRING));            
		hostSchema.add(ConfigurationOntology.HOST_CONTAINERS, (ConceptSchema) dummy.getSchema(ConfigurationOntology.CONTAINER), 0, ObjectSchema.UNLIMITED, BasicOntology.SET);
		hostSchema.add(ConfigurationOntology.HOST_BACKUP_ALLOWED, (PrimitiveSchema) dummy.getSchema(BasicOntology.BOOLEAN), ObjectSchema.OPTIONAL);
		dummy.add(hostSchema, HostInfo.class);

		// FIXME: Should we do something to avoid writing the PlatformElement.errorCode?
		return dummy;
    }
}
