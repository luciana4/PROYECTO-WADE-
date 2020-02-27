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
package com.tilab.wade.performer;

import jade.core.AID;
import jade.core.Specifier;

import java.util.Vector;

/**
   This class represents an element in a delegation chain i.e. a couple (executor, executionId).
   @author Giovanni Caire - TILAB
 */
class DelegationChainElement {
	static final char DELEGATION_CHAIN_SEPARATOR = ';';
	static final char ELEMENT_SEPARATOR = ',';
	
	private AID executor;
	private String executionId;
	
	public DelegationChainElement() {
	}
	
	public DelegationChainElement(AID executor, String executionId) {
		this.executor = executor;
		this.executionId = executionId;
	}
	
	public String getExecutionId() {
		return executionId;
	}
	
	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
	
	public AID getExecutor() {
		return executor;
	}
	
	public void setExecutor(AID executor) {
		this.executor = executor;
	}
	
	public static final DelegationChainElement[] parseDelegationChain(String dc) {
		Vector v = Specifier.parseList(dc, DELEGATION_CHAIN_SEPARATOR);
		DelegationChainElement[] result = new DelegationChainElement[v.size()];
		for (int i = 0; i < result.length; ++i) {
			result[i] = parse((String) v.elementAt(i));
		}
		return result;
	}
	
	public static final DelegationChainElement parse(String element) {
		if (element == null || element.length() == 0) {
			return null;
		}
		else {
			Vector v = Specifier.parseList(element, ELEMENT_SEPARATOR);
			String executorName = (String)v.elementAt(0);
			String executionId = (String)v.elementAt(1);
			DelegationChainElement result = new DelegationChainElement(new AID(executorName, AID.ISGUID), executionId);
			return result;
		}
	}
	
	public static boolean shorter(String dc1, String dc2) {
		DelegationChainElement[] del1 = parseDelegationChain(dc1);
		DelegationChainElement[] del2 = parseDelegationChain(dc2);
		return del1.length < del2.length;
	}
}
			