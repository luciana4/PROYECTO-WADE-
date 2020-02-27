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
package com.tilab.wade.wsma.ontology;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;

public class QueryExecutions implements AgentAction {
	
	private static final long serialVersionUID = -4951587110932570312L;
	
	public static final int ALL_RESULTS = -1;
	
	private String what;
	private String condition;
	private String order;
	private int firstResult = 0;
	private int maxResult = ALL_RESULTS;

	
	public QueryExecutions() {
	}

	public QueryExecutions(String what, String condition, String order) {
		this(what, condition, order, 0, ALL_RESULTS);
	}
	
	public QueryExecutions(String what, String condition, String order, int firstResult, int maxResult) {
		this.what = what;
		this.condition = condition;
		this.order = order;
		this.firstResult = firstResult;
		this.maxResult = maxResult;
	}
	
	@Slot(mandatory=false)
	public String getWhat() {
		return what;
	}
	
	public void setWhat(String what) {
		this.what = what;
	}
	
	@Slot(mandatory=false)
	public String getCondition() {
		return condition;
	}
	
	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	@Slot(mandatory=false)
	public String getOrder() {
		return order;
	}
	
	public void setOrder(String order) {
		this.order = order;
	}

	@Slot(mandatory=false)
	public int getFirstResult() {
		return firstResult;
	}

	public void setFirstResult(int firstResult) {
		this.firstResult = firstResult;
	}

	@Slot(mandatory=false)
	public int getMaxResult() {
		return maxResult;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}

	@Override
	public String toString() {
		return "Query [what=" + what + ", condition=" + condition + ", order=" + order + "]";
	}
}
