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
package com.tilab.wade.event.beans;

import java.util.List;
import java.util.ArrayList;

public class Receiver {
    private String name;
    private String threaded;
    private List<Handler> handlers = new ArrayList<Handler>();
    private List<Ontology> ontologies = new ArrayList<Ontology>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThreaded() {
        return threaded;
    }

    public void setThreaded(String threaded) {
        this.threaded = threaded;
    }

    public List<Handler> getHandlers() {
        return handlers;
    }

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }
    public List<Ontology> getOntologies() {
        return ontologies;
    }

    public void addOntology(Ontology ontology) {
        this.ontologies.add(ontology);
    }

	@Override
	public String toString() {
		return "Receiver [name=" + name + "]";
	}
}
