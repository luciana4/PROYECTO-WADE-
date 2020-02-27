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
package com.tilab.wade.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *    Classe statica per la generazione di Globally Unique IDentifiers
 *    da usare come session id nell'esecuzione dei WF di Wants.
 *    
 * @author beppe
 */
public class GUIDGenerator {
	
	static AtomicLong incr = new AtomicLong(0);
	static String guidRoot;
    final static long startYear = 2000;
    static long startTime;
    static String hostName;
    static Calendar cal = new GregorianCalendar();



    static  {


        startTime = (cal.get(Calendar.YEAR)-startYear)
                    + cal.get(Calendar.DAY_OF_YEAR)
                    + cal.get(Calendar.HOUR_OF_DAY)
                    + cal.get(Calendar.MINUTE)
                    + cal.get(Calendar.SECOND);

		 try {
			 hostName = InetAddress.getLocalHost().getHostName();
             guidRoot = hostName + ":" + new UID().toString() + ":";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
	/**
	 * 
	 * @return A Globally unique identifier
	 *
	 */
	public static String getGUID ()  {
		return guidRoot + incr.incrementAndGet();
	}
    public static String getExecutionGUID (String agentName) {
        return hostName+"-"+agentName+"-"+startTime+"-"+incr.incrementAndGet();
    }

}
