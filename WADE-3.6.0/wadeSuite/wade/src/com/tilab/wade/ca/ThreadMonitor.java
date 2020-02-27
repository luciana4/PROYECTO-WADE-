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

//import com.tilab.wants.properties.PropertiesManager;

import java.lang.management.*;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import java.util.logging.*;

/**
 *  This class is a thread monitoring utility based on ThreadMXBean.
 *  It is used internally by WADE Control Agents. Developers are not expecetd to use this class directly 
 */
public class ThreadMonitor {

	//Logger
	private static Logger log = Logger.getLogger(ThreadMonitor.class.getName());

	private static ThreadMonitor instance = null;
	private ThreadMXBean mbean = null;
	private int threadCountThreshold;
	private int threadCountReentrantTh;
	private boolean isThresholdExceeded = false;

	private ThreadMonitor() {
		mbean = ManagementFactory.getThreadMXBean();
	}

	public static ThreadMonitor getInstance() {
		if(instance == null) {
			instance = new ThreadMonitor();
		}
		return instance;
	}

	public int getThreadCount() {
		return mbean.getThreadCount();
	}

	public ThreadInfo [] getThreadInfo() {
		long [] ids = mbean.getAllThreadIds();
		return mbean.getThreadInfo(ids);
	}
	
	public ThreadInfo getThreadInfo(long id) {
		return mbean.getThreadInfo(id);
	}

	public long getThreadCpuTime(long id) {
		return mbean.getThreadCpuTime(id);
	}

	public Map getThreadCpuTimeMap() {
		Map res = new HashMap();
		long [] ids = mbean.getAllThreadIds();
		for(int i=0;i<ids.length;i++) {
			res.put(new Long(ids[i]),new Long(mbean.getThreadCpuTime(ids[i])));
		}
		return res;

	}
	
	public long getTotalCpuTime(){
		long res=0;
		long [] ids = mbean.getAllThreadIds();
		for(int i=0;i<ids.length;i++) {
			res=res+mbean.getThreadCpuTime(ids[i]);
		}
		return res;

	}

	public void setThreadCountThreshold(int th) {
		threadCountThreshold = th;
	}

	public void setThreadCountReentrantTh(int reentrantTh) {
		threadCountReentrantTh = reentrantTh;
	}

	public int getThreadCountThreshold() {
		return threadCountThreshold;
	}

	public int getThreadCountReentrantTh() {
		return threadCountReentrantTh;
	}

	public boolean isThresholdExceeded() {
		return isThresholdExceeded;
	}

	/**
	 * This method returns a Threshold object containing the following infos: 
	 * a boolean value indicating if the threshold is crossed (th.isCrossed) and a boolean value indicating the direction of crossing (th.isUp)
	 * Note: This method produces a change of the state of the monitor (the flag isThresholdExceedeed is set)
	 * @return a Threshold object describing whether or not the threshold was crossed
	 */
	public synchronized Threshold isThresholdCrossed() {
		if(!isThresholdExceeded) {
			if(getThreadCount() > threadCountThreshold) {
				isThresholdExceeded = true;
				return new Threshold(true,true);
			}
			else {
				return new Threshold(false);
			}
		}
		else {
			if(getThreadCount() < threadCountReentrantTh) {
				isThresholdExceeded = false;
				return new Threshold(true, false);
			}
			else {
				return new Threshold(false);
			}
		}
	}
	
	/**
	 * This method can be used to detect whether or not the number of threads is below the threshold.
	 * It doesn't change the status of the monitor
	 * @return a boolean value indicating whether or not the number of threads is below the threshold
	 */
	public synchronized boolean isBelowThreshold() {
		if(!isThresholdExceeded)
			return getThreadCount() < threadCountThreshold;
		else
			return getThreadCount() < threadCountReentrantTh;
	}
	
	public boolean isValidThreadId(long id) {
		boolean res = true;
		try {
			mbean.getThreadInfo(id);
		}
		catch(IllegalArgumentException ie) {
			res =false;
		}
		catch(SecurityException se) {
			res = false;
			log.log(Level.SEVERE, se.getMessage(), se);
		}
		return res;
	}
	
	public static void main(String[] args) {
		ThreadMonitor instance = ThreadMonitor.getInstance();
		System.out.println("count "+instance.getThreadCount());
		Map m =instance.getThreadCpuTimeMap();
		Iterator it =m.keySet().iterator();
		while(it.hasNext()) {
			Object key =it.next();
			System.out.println("id "+key+" cpuTime "+m.get(key));
		}
		System.out.println("CPUTotal "+instance.getTotalCpuTime());
	}


}

