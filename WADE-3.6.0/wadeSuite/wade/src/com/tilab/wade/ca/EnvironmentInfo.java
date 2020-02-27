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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.SystemUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 14-mar-2004
 * Time: 11.48.57
 * To change this template use Options | File Templates.
 */
public class EnvironmentInfo {
	/* EnvironmentInfo */
	protected static final String OS_NAME = "OS_NAME";
	protected static final String OS_ARCHITECTURE = "OS_ARCHITECTURE";
	protected static final String OS_VERSION = "OS_VERSION";
	protected static final String OS_AVAILABLE_PROCESSOR = "OS_AVAILABLE_PROCESSOR";
	protected static final String RUNTIME_NAME = "RUNTIME_NAME";
	protected static final String RUNTIME_SPEC_NAME = "RUNTIME_SPEC_NAME";
	protected static final String RUNTIME_VERSION = "RUNTIME_VERSION";
	protected static final String RUNTIME_SPEC_VERSION = "RUNTIME_SPEC_VERSION";
	protected static final String RUNTIME_VM_VERSION = "RUNTIME_VM_VERSION";
	protected static final String RUNTIME_VM_NAME = "RUNTIME_VM_NAME";
	protected static final String RUNTIME_UPTIME = "RUNTIME_UPTIME";

	private static EnvironmentInfo instance = null;
	private OperatingSystemMXBean opMbean = null;
	private RuntimeMXBean runMbean = null;


	private EnvironmentInfo() {
		opMbean = ManagementFactory.getOperatingSystemMXBean();
		runMbean = ManagementFactory.getRuntimeMXBean();
	}

	public static EnvironmentInfo getInstance() {
		if(instance == null) {
			instance = new EnvironmentInfo();
		}

		return instance;

	}
	public Map getEnvironmentInfo() {
		Map info =new HashMap();
		info.put(OS_NAME,opMbean.getName());
		info.put(OS_ARCHITECTURE,opMbean.getArch());
		info.put(OS_VERSION,opMbean.getVersion());
		info.put(OS_AVAILABLE_PROCESSOR,new Integer(opMbean.getAvailableProcessors()));
		info.put(RUNTIME_NAME, runMbean.getName());
		info.put(RUNTIME_SPEC_NAME, runMbean.getSpecName());
		info.put(RUNTIME_SPEC_VERSION,runMbean.getSpecVersion());
		info.put(RUNTIME_VM_NAME,runMbean.getVmName());
		info.put(RUNTIME_VM_VERSION,runMbean.getVmVersion());
		info.put(RUNTIME_UPTIME, new Long(runMbean.getUptime()));
		return info;

	}
	public long getRuntimeUpTime() {
		return runMbean.getUptime();
	}
	
	public static void main(String[] args) {
		EnvironmentInfo env =EnvironmentInfo.getInstance();
		Map info = env.getEnvironmentInfo();
		Iterator it=info.keySet().iterator();
		while(it.hasNext()) {
			Object key =it.next();
			System.out.println(key+ " "+ info.get(key));
		}
	}
}
