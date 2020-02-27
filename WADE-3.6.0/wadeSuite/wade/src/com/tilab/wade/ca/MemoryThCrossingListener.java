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

import java.lang.management.MemoryNotificationInfo;

import javax.management.Notification;
import javax.management.NotificationListener;

public class MemoryThCrossingListener implements NotificationListener {
	private PerformanceMonitorBehaviour perf;
	
	
	public MemoryThCrossingListener(PerformanceMonitorBehaviour perf) {
		this.perf = perf;
	}

	public void handleNotification(Notification notification, Object handback) {
		String notifType = notification.getType();
        if (notifType.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
    		((MemoryMonitor) handback).setLowMemory(true);
    		perf.memoryThresholdExceeded();
        }
	}
}
