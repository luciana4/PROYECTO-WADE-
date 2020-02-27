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

import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.logging.Logger;

import com.tilab.wade.commons.TypeManager;

import jade.core.behaviours.TickerBehaviour;
import jade.core.Agent;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 22-set-2006
 * Time: 18.21.07
 *Ticker del controllo sul numero di thread e CPU
 */
public class PerformanceMonitorBehaviour extends TickerBehaviour {

	private long interval;
	
	// threshold values
	private long cpuUsageThreshold;
	private long cpuUsageReentrantTh;
	private long memoryUsageThreshold;
	private long memoryUsageReentrantTh;
	private int threadNumberThreshold; 
	private int threadNumberReentrantTh; 

	//used to check cpuUsage 
	private boolean isCpuUsageThresholdCrossed = false;

	private long maxHeapMemory;

	private MemoryMonitor memoryMonitor = null;
	private ThreadMonitor threadMonitor = null;

	private Map configProps;
	private ControllerAgent myCa;
	private MemoryThCrossingListener memoryThCrossingListener;
	
	private long lastTime = System.currentTimeMillis();
	private long traceInterval = 60000; // 1 min
	
	private Logger myLogger = Logger.getLogger(getClass().getName());


	public PerformanceMonitorBehaviour(Agent agent, long i, Map props) {
		super(agent, i);
		interval = i;
		configProps = props;
		myCa = (ControllerAgent) agent;
		memoryThCrossingListener = new MemoryThCrossingListener(this);
	}

	@Override
	public void onStart() {
		//get Threshold from properties
		cpuUsageThreshold = TypeManager.getLong(configProps, ControllerAgent.CPU_USAGE_THRESHOLD_KEY, ControllerAgent.CPU_USAGE_THRESHOLD_DEFAULT);
		cpuUsageReentrantTh = TypeManager.getLong(configProps, ControllerAgent.CPU_USAGE_REENTRANT_TH_KEY, cpuUsageThreshold);
		memoryUsageThreshold = TypeManager.getLong(configProps, ControllerAgent.MEMORY_USAGE_THRESHOLD_KEY, ControllerAgent.MEMORY_USAGE_THRESHOLD_DEFAULT);
		memoryUsageReentrantTh = TypeManager.getLong(configProps, ControllerAgent.MEMORY_USAGE_REENTRANT_TH_KEY, memoryUsageThreshold);
		threadNumberThreshold = TypeManager.getInt(configProps, ControllerAgent.THREAD_NUMBER_THRESHOLD_KEY, ControllerAgent.THREAD_NUMBER_THRESHOLD_DEFAULT);
		threadNumberReentrantTh = TypeManager.getInt(configProps, ControllerAgent.THREAD_NUMBER_REENTRANT_TH_KEY, threadNumberThreshold);

		//Configuration of the ThreadMonitor
		threadMonitor = ThreadMonitor.getInstance();
		threadMonitor.setThreadCountThreshold(threadNumberThreshold);
		threadMonitor.setThreadCountReentrantTh(threadNumberReentrantTh);

		// Configuration of the memoryMonitor with memoryUsageThreshold and Listener
		memoryMonitor = MemoryMonitor.getInstance();
		maxHeapMemory = memoryMonitor.getHeapMemoryUsage().getMax();
		memoryMonitor.setUsageThreshold((maxHeapMemory * memoryUsageThreshold) / 100);
		memoryMonitor.setUsageReentrantThreshold((maxHeapMemory * memoryUsageReentrantTh) / 100);
		memoryMonitor.addListener(memoryThCrossingListener,memoryMonitor);
		
		super.onStart();
	}

	protected void onTick() {
		checkThreadUsage();
		checkCpuUsage();
		checkMemoryUsage();
	}
	
	public void stop() {
		if (memoryMonitor != null) {
			memoryMonitor.removeListener(memoryThCrossingListener);
		}
		super.stop();
	}

	public long getMemoryUsageThreshold(boolean direction) {
		if(direction)
			return memoryUsageThreshold;
		else
			return memoryUsageReentrantTh;	
	}
	
	public int getThreadNumberThreshold(boolean direction) {
		if(direction)
			return threadMonitor.getThreadCountThreshold();
		else
			return threadMonitor.getThreadCountReentrantTh();	
	}
	
	public long getCpuUsageThreshold(boolean direction) {
		if(direction)
			return cpuUsageThreshold;
		else
			return cpuUsageReentrantTh;	
	}


	public MemoryUsage  getMemoryUsage() {
		return memoryMonitor.getHeapMemoryUsage();
	}
	
	public int  getThreadNumber() {
		return threadMonitor.getThreadCount();
	}
	
	public long getCpuUsage() {
		return myCa.getCpuUsage();
	}

	public boolean isCpuBelowThreshold() {
		return false;
	}
	
	/**
	 * Used by MemoryThCrossingListener when the memory threshold is exceeded 
	 *
	 */
	public void memoryThresholdExceeded(){
		myCa.memoryThresholdCrossed(true);
	}
	
	
//         |     *           |
//         | *       *       |
//---------*-----------*-----|-----------------
//        *|            *    |
//       * |             *   |
//      *  |              *  |
//     *   |               * |
//    *    |                *|
//---*-----|-----------------*-----------------
//  *      |                 |*     
// *       |                 | * 
//         |                 |  *
//    
// TRUE            FALSE            TRUE	
	
	public boolean isMemoryBelowThreshold() {
		return memoryMonitor.isBelowThreshold();
	}

	public boolean isThreadNumberBelowThreshold() {
		return threadMonitor.isBelowThreshold();
	}
	
	public boolean isCpuUsageBelowThreshold() {
		if(!isCpuUsageThresholdCrossed)
			return !isCpuThresholdCrossed().isCrossed();
		else
			return isCpuThresholdCrossed().isCrossed();
	}
	
	//private methods
	private void  checkThreadUsage() {
		Threshold th = threadMonitor.isThresholdCrossed(); 
		if(th.isCrossed()) {
			myCa.threadCountThresholdCrossed(th.isUp());
		}
	}

	private void  checkMemoryUsage() {
		// Keep track of the memory status evolution 
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastTime > traceInterval) {
			myLogger.fine("Agent "+myAgent.getName()+": Used heap = "+(memoryMonitor.getHeapMemoryUsage().getUsed()/1024/1024)+"Mb, Committed heap = "+(memoryMonitor.getHeapMemoryUsage().getCommitted()/1024/1024)+"Mb");
			lastTime = currentTime;
		}
		
		// Check the threshold crossing condition only in the down direction since
		// the up direction is already managed as an event within the MemoryMonitor
		if(memoryMonitor.isLowMemory()) {
			if(memoryMonitor.isThresholdReentred()) {
				memoryMonitor.setLowMemory(false);
				myCa.memoryThresholdCrossed(false);
			}
		}
	}

	private void  checkCpuUsage() {
		Threshold th = isCpuThresholdCrossed(); 
		if(th.isCrossed()) {
			myCa.cpuUsageThresholdCrossed(th.isUp());
		}
	}

	private Threshold isCpuThresholdCrossed() {

		if(!isCpuUsageThresholdCrossed) {
			if(getCpuUsage() > cpuUsageThreshold) {
				isCpuUsageThresholdCrossed = true;
				return new Threshold(true,true);
			}
			else {
				return new Threshold(false);
			}
		}
		else {
			if(getCpuUsage() < cpuUsageReentrantTh) {
				isCpuUsageThresholdCrossed = false;
				return new Threshold(true, false);
			}
			else {
				return new Threshold(false);
			}
		}
	}

	public long getMonitoringInterval() {
		return interval;
	}
}
