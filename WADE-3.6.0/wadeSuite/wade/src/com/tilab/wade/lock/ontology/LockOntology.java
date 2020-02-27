package com.tilab.wade.lock.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.JavaCollectionOntology;
import jade.content.onto.Ontology;

public class LockOntology extends BeanOntology {
	private static final long serialVersionUID = 324487978756L;

	public static final String ONTOLOGY_NAME = "Lock-Ontology";
		
	// The singleton instance of this ontology
	private final static Ontology theInstance = new LockOntology();

	public final static Ontology getInstance() {
		return theInstance;
	}

	private LockOntology() {
		super(ONTOLOGY_NAME, JavaCollectionOntology.getInstance());

		try {
			add(AcquireLock.class);
			add(ReleaseLock.class);
			add(RefreshLock.class);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
