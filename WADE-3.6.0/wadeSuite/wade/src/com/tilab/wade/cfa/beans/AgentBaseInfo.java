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

import jade.util.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.tilab.wade.utils.FileUtils;

public class AgentBaseInfo extends PlatformElement implements Cloneable {
	private static final long serialVersionUID = 5333949601628498610L;

	protected final Logger myLogger = Logger.getMyLogger(getClass().getName());;
	
	private String name;
    private String type;
	private String className;
    private String owner;
    private String group;
	private Collection<AgentArgumentInfo> parameters = new HashSet<AgentArgumentInfo>();

	public AgentBaseInfo() {
	}

	public AgentBaseInfo(String name, String type, String className, String owner, Collection parameters) {
		this.name = name;
		this.type = type;
		this.className = className;
		this.owner = owner;
		this.parameters = parameters;
	}

	public AgentBaseInfo(String name, String type, String className, String owner) {
		this.name = name;
		this.type = type;
		this.className = className;
		this.owner = owner;		
	}
	
	public AgentBaseInfo(String name, String type, String className, String owner, String group, Collection parameters) {
		this(name, type, className, owner, parameters);
		
		this.group = group;
	}

	public AgentBaseInfo(String name, String type, String className, String owner, String group) {
		this(name, type, className, owner);
		
		this.group = group;		
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public Collection<AgentArgumentInfo> getParameters() {
		return parameters;
	}

	public void setParameters(Collection<AgentArgumentInfo> arguments) {
		this.parameters = arguments;
	}

	// Required by Digester
	public void addParameter(AgentArgumentInfo agentArgumentInfo) {
		this.parameters.add(agentArgumentInfo);
	}

	public AgentArgumentInfo getParameter(String name) {
		AgentArgumentInfo result = null;
		Iterator<AgentArgumentInfo> it = parameters.iterator();
		while(it.hasNext()) {
			AgentArgumentInfo aai = it.next();
			if (aai.getKey().equals(name)) {
				result = aai;
				break;
			}
		}
		return result;
	}

	public String toString() {
		return toString("", false);
	}

	public String toString(String prefix, boolean onlyError) {
		StringBuffer sb = new StringBuffer(prefix+"AGENT ");
		sb.append(name);
		sb.append('\n');
		if (getErrorCode() != null) {
			sb.append(prefix+"- error-code: ");
			sb.append(getErrorCode());
			sb.append('\n');
		}
		if (!onlyError && parameters.size() > 0) {
			sb.append(prefix+"- arguments: ");
			for (AgentArgumentInfo argument : parameters) {
				sb.append(argument.toString(prefix+"  "));
			}			
			sb.append('\n');
		}

		return sb.toString();
	}

	public boolean equals(Object obj) {
		if (obj instanceof AgentBaseInfo) {
			return name.equals(((AgentBaseInfo) obj).getName());
		}
		return false;
	}

	public boolean isEquivalent(AgentBaseInfo ai) {
		if (ai == null) {
			return false;
		}

		return FileUtils.compareObject(getName(), ai.getName()) && FileUtils.compareObject(getType(), ai.getType());
	}

	public int hashCode() {
		return name.hashCode();
	}

	public boolean skipArgument(String argumentKey) {
		return false;
	}

	public SortedMap<String, String> getParametersForComparison() {
		SortedMap<String, String> result = new TreeMap<String, String>();
//		Iterator<AgentArgumentInfo> paramsIter = getParameters().iterator();
//		while (paramsIter.hasNext()) {
//			AgentArgumentInfo raai = paramsIter.next();
//			if (!skipArgument(raai.getKey())) {
//				result.put(raai.getKey(), raai.getValue().toString());
//			}
//		}
		return result;
	}
}
