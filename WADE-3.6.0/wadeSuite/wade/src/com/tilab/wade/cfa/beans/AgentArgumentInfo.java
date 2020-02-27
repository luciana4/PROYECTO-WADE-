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


public class AgentArgumentInfo extends PlatformElement {

	private static final long serialVersionUID = -700151438346406616L;

	private String id;
	private String key;
	private Object value;

    public AgentArgumentInfo() {
    	id = "";
    }

    public AgentArgumentInfo(String key, Object value) {
        this.key = key;
        this.value = value;
        updateId();
    }

    public String getId() {
		return id;
	}
    public void setId(String id) {
		this.id = id;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
        updateId();
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
        updateId();
	}
	
	public boolean equals(AgentArgumentInfo o){
		return id.endsWith(o.getId());
	}
	
	public int hashCode(){
		return id.hashCode();
	}

	
	public String toString() {
		return toString("");
	}
	
	public String toString(String prefix){
		StringBuffer buffer = new StringBuffer(prefix+"(ARGUMENT ");
		buffer.append(key);
		buffer.append("=");
		buffer.append(value);
		buffer.append(")");
		return buffer.toString();
	}
	
	private final void updateId() {
		id = (key != null ? key : "") + (value != null ? value.toString() : "");
	}
}
