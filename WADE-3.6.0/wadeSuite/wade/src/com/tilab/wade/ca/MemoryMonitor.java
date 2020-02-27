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

import javax.management.NotificationListener;
import javax.management.NotificationEmitter;
import javax.management.ListenerNotFoundException;
import java.lang.management.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: Mar 11, 2004
 * Time: 2:38:00 PM
 * To change this template use Options | File Templates.
 */
public class MemoryMonitor {
	
	private Logger myLogger = Logger.getLogger(getClass().getName());

	private static MemoryMonitor instance = null;
	private MemoryMXBean mbean = null;
	private MemoryPoolMXBean heap = null;
	private long usageReentrantThreshold;
	private boolean lowMemory = false;

	private MemoryMonitor() {
		mbean = ManagementFactory.getMemoryMXBean();
		heap = getHeapMonitor();
	}

	private MemoryPoolMXBean getHeapMonitor() {
		java.util.List pools = ManagementFactory.getMemoryPoolMXBeans();
		Iterator it = pools.iterator();
		MemoryPoolMXBean pool = null;
		MemoryPoolMXBean heapPool = null;
		while(it.hasNext()) {
			pool = (MemoryPoolMXBean) it.next();
			if(pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
				heapPool = pool;
				break;
			}
		}
		if (heapPool == null) {
			myLogger.log(Level.WARNING, "MemoryPoolMXBeans with threshold supported for heap memory not found");
		}
		
		return heapPool;
	}
	
	public static MemoryMonitor getInstance() {
		if(instance == null) {
			instance = new MemoryMonitor();
		}

		return instance;
	}

	public boolean isLowMemory() {
		return lowMemory;
	}


	public void setLowMemory(boolean lowMemory) {
		this.lowMemory = lowMemory;
	}

	public void setUsageThreshold(long th) {
		if (heap != null) {
			heap.setUsageThreshold(th);
		} else {
			myLogger.log(Level.WARNING, "setUsageThreshold not supported");
		}
	}

	public void setUsageReentrantThreshold(long threshold) {
		usageReentrantThreshold = threshold;
	}

	public long getUsageThreshold() {
		if (heap != null) {
			return heap.getUsageThreshold();
		}
		
		myLogger.log(Level.WARNING, "getUsageThreshold not supported");
		return 0;
	}

	public long getUsageReentrantThreshold() {
		return usageReentrantThreshold;
	}

	public boolean isThresholdCrossed() {
		if (heap != null) {
			return heap.isUsageThresholdExceeded();
		}
		
		myLogger.log(Level.WARNING, "isThresholdCrossed not supported");
		return false;
	}

	public boolean isThresholdReentred() {
		return (lowMemory && getHeapMemoryUsage().getUsed() < usageReentrantThreshold);
	}

	public boolean isBelowThreshold() {
		if(!lowMemory)
			return !isThresholdCrossed();
		else
			return isThresholdReentred();
	}

	public MemoryUsage getHeapMemoryUsage() {
		if (heap != null) {
			return heap.getUsage();
		}
		
		myLogger.log(Level.WARNING, "getHeapMemoryUsage not supported");
		return null;
	}

	public void addListener(NotificationListener nl, Object handBack) {
		((NotificationEmitter) mbean).addNotificationListener(nl,null,handBack);
	}
	
	public void removeListener(NotificationListener nl) {
		try {
			((NotificationEmitter) mbean).removeNotificationListener(nl);
		}
		catch(ListenerNotFoundException lnfe) {
			myLogger.log(Level.WARNING, "Lister not found in MemoryMXBean object");
		}
	}

	
	
	public static void main(String[] args) {
		MemoryMonitor instance = MemoryMonitor.getInstance();
		instance.getHeapMonitor();
	}
}
