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
package com.tilab.wade.utils.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger formatter with:
 * - shortLine = false (default) 
 *   - DATE TIME SOURCE>> LEVEL: MESSAGE [CLASS-NAME METHOD-NAME] and if present the STACK-TRACE
 *     - DATE TIME are added only if the process is NOT launched by BootDaemon
 *     - SOURCE can be null
 * - shortLine = true
 *   - DATE TIME MESSAGE
 */
public class WadeFormatter extends Formatter {

	private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private final static String lineSeparator = System.getProperty("line.separator");

	private String sourceName;
	private boolean launchedByBootDaemon;
	private boolean shortLine;

	public WadeFormatter() {
		this(false, null);
	}
	
	public WadeFormatter(boolean shortLine) {
		this(shortLine, null);
	}

	private WadeFormatter(boolean shortLine, String sourceName) {
		launchedByBootDaemon = "true".equals(System.getProperty("TSDaemon"));
		this.shortLine = shortLine;
		this.sourceName = sourceName;
	}
	
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	
	/**
	 * Format the given LogRecord.
	 * @param record the log record to be formatted.
	 * @return a formatted log record
	 */
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		if (!launchedByBootDaemon || shortLine) {
			// Date-Time
			Date date = new Date(record.getMillis());
			synchronized (formatter) {
				sb.append(formatter.format(date));
			}
			sb.append(' ');
		}
			
		if (!shortLine) {
			// Source name
			if (!launchedByBootDaemon && sourceName != null) {
				sb.append(sourceName);
				sb.append(">>");
				sb.append(' ');
			}
				
			// Log LEVEL
			sb.append(record.getLevel());
			sb.append(": ");
		}
		
		// Message
		String message = formatMessage(record);
		sb.append(message);
		
		if (!shortLine) {
			// Class Method
			sb.append(" [");
			if (record.getSourceClassName() != null) {	
				sb.append(record.getSourceClassName());
			} else {
				sb.append('#');
				sb.append(record.getLoggerName());
			}
			if (record.getSourceMethodName() != null) {	
				sb.append(' ');
				sb.append(record.getSourceMethodName());
				sb.append("()");
			}
			sb.append(']');
		}
			
		sb.append(lineSeparator);
		
		if (!shortLine) {
			// Exception
			if (record.getThrown() != null) {
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					record.getThrown().printStackTrace(pw);
					pw.close();
					sb.append(sw.toString());
				} catch (Exception ex) {
					// ignore exception
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Set the source-name to WadeFormatter in all LoggerHandler 
	 */
	public static void manage(Logger logger, String sourceName) {
		Handler[] handlers = logger.getHandlers();
		for (Handler h : handlers) {
			if (h.getFormatter() instanceof WadeFormatter) {
				WadeFormatter plf = (WadeFormatter)h.getFormatter();
				plf.setSourceName(sourceName);
			}
		}
	}
}
