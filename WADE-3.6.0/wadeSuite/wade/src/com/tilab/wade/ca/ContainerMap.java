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
package com.tilab.wade.ca;

import java.util.*;

import com.tilab.wade.cfa.beans.ContainerInfo;

/**
 * Created by IntelliJ IDEA.
 * User: wants
 * Date: 19-gen-2005
 * Time: 10.54.04
 * To change this template use File | Settings | File Templates.
 */
public class ContainerMap {
    private Map containers;

    public ContainerMap() {
        this.containers = new HashMap();
    }

    public List getContainers() {
        List result = new ArrayList();
        Iterator it = containers.values().iterator();
        while(it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    public boolean containsContainerInfo(String containerName) {
        return containers.containsKey(containerName);
    }

    public ContainerInfo getContainerInfo(String containerName) {
        return (ContainerInfo) containers.get(containerName);
    }

    public void putContainerInfo(String containerName, ContainerInfo info) {
        containers.put(containerName, info);
    }

    public ContainerInfo removeContainerInfo(String containerName) {
        return (ContainerInfo) containers.remove(containerName);
    }

}
