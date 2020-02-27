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

import java.util.*;

import com.tilab.wade.utils.FileUtils;

public class HostInfo extends PlatformElement{

	private static final long serialVersionUID = 2060315148568397980L;

	private String name;
	private String ipAddress;
	private String cpuNumber;
	private String ram;
	private String adminState;
	private String benchmarkSpecInt;
	private String benchmarkWf;
	private boolean availability; //true se l'host e` raggiungibile ed il bootDaemon risponde correttamente
	private boolean reachability; //true se l'host e` raggiungibile
	private boolean backupAllowed;
    
	private Collection<ContainerInfo> containers = new HashSet<ContainerInfo>();

    public HostInfo() {
		backupAllowed = false;
	}
    
	public boolean getAvailability() {
        return availability;
    }
    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
	public Collection<ContainerInfo> getContainers() {
		return containers;
	}
	public void setContainers(Collection<ContainerInfo> containers) {
		this.containers = containers;
	}
	
	// Required by Digester
	public void addContainer(ContainerInfo containerInfo) {
		this.containers.add(containerInfo);
	}

	public void removeAllContainers() {
		containers.clear();
	}
	
	public String getCpuNumber() {
		return cpuNumber;
	}
	public void setCpuNumber(String cpuNumber) {
		this.cpuNumber = cpuNumber;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getBenchmarkSpecInt() {
		return benchmarkSpecInt;
	}
	public void setBenchmarkSpecInt(String benchmarkSpecInt) {
		this.benchmarkSpecInt = benchmarkSpecInt;
	}
	public String getBenchmarkWf() {
		return benchmarkWf;
	}
	public void setBenchmarkWf(String benchmarkWf) {
		this.benchmarkWf = benchmarkWf;
	}
	public String getRam() {
		return ram;
	}
	public void setRam(String ram) {
		this.ram = ram;
	}
	public String getAdminState() {
		return adminState;
	}
	public void setAdminState(String adminState) {
		this.adminState = adminState;
	}
	
    //required by console in order to show detailInfos
    public List getContainersAsList() {
        return new ArrayList(containers);
    }

	public ContainerInfo getContainer(String name) {
		Iterator it = containers.iterator();
		while (it.hasNext()) {
			ContainerInfo ci = (ContainerInfo) it.next();
			if (ci.getName().equals(name)) {
				return ci;
			}
		}
		return null;
	}
    
    public boolean getReachability() {
		return reachability;
	}
    
	public void setReachability(boolean reachability) {
		this.reachability = reachability;
	}
	   
	public boolean isBackupAllowed() {
		return backupAllowed;
	}

	//required by ontology
	public boolean getBackupAllowed() {
		return isBackupAllowed();
	}
	
	public void setBackupAllowed(boolean backupAllowed) {
		this.backupAllowed = backupAllowed;
	}
	
	@Override
	public boolean equals(Object obj) {
		return ( obj instanceof HostInfo &&  name.equalsIgnoreCase(((HostInfo)obj).getName()) );
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
		
	}

	public String toString() {
		return toString("", false);
	}
	
	public String toString(String prefix, boolean onlyError) {
		StringBuffer sb = new StringBuffer(prefix+"HOST ");
		sb.append(name);
		sb.append('\n');
		if (getErrorCode() != null) {
			sb.append(prefix+"- error-code: ");
			sb.append(getErrorCode());
			sb.append('\n');
		}
		if (!onlyError) {
			sb.append(prefix+"- reachability: ");
			sb.append(reachability);
			sb.append('\n');
			sb.append(prefix+"- availability: ");
			sb.append(availability);
			sb.append('\n');
		}
		
		if (containers.size() > 0) {
			sb.append(prefix+"- containers:\n");
			Iterator it = containers.iterator();
			while (it.hasNext()) {
				ContainerInfo container = (ContainerInfo) it.next();
				sb.append(container.toString(prefix+"  ", onlyError));
			}
		}
		
		return sb.toString();
	}

}
