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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WadeHandler extends Handler {

	private LogManager logManager;
	private Properties fileProperties;
	private String projectHome;
	private Level level;
	private String location;
	private String namePrefix;
	private String dateFormat;
	private String hourFormat;
	private String nameSuffix;
	private int limit;
	private int preservedFreeDiskPerc;
	private String cycle = "day";

	public WadeHandler() {
		logManager = LogManager.getLogManager();

		configure(castToString(getLogManagerProperty("location")),
				  castToString(getLogManagerProperty("namePrefix")),
				  castToString(getLogManagerProperty("dateFormat")),
				  castToString(getLogManagerProperty("hourFormat")),
				  castToString(getLogManagerProperty("nameSuffix")),
				  castToInt(getLogManagerProperty("limit")),
				  castToInt(getLogManagerProperty("preservedFreeDiskPerc")),
                  castToString(getLogManagerProperty("cycle")),
                  castToLevel(getLogManagerProperty("level")));
	}

	public WadeHandler(String projectHome, Properties props) {
		this.fileProperties = props;
		this.projectHome = projectHome;
		
		configure(castToString(getFileProperty("location")),
				  castToString(getFileProperty("namePrefix")),
				  castToString(getFileProperty("dateFormat")),
				  castToString(getFileProperty("hourFormat")),
				  castToString(getFileProperty("nameSuffix")),
				  castToInt(getFileProperty("limit")),
				  castToInt(getFileProperty("preservedFreeDiskPerc")),
                  castToString(getFileProperty("cycle")),
                  castToLevel(getFileProperty("level")));
	}

	public WadeHandler(String location, String namePrefix, String dateFormat, String hourFormat, String nameSuffix, int limit, int preservedFreeDiskPerc, String cycle, Level level) {
		
		configure(location, namePrefix, dateFormat, hourFormat, nameSuffix, limit, preservedFreeDiskPerc, cycle, level);
	}
	
	@Override
	public void publish(LogRecord record) {
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

	public Level getLevel() {
		return level;
	}

	public String getLocation() {
		return location;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public String getHourFormat() {
		return hourFormat;
	}
	
	public String getNameSuffix() {
		return nameSuffix;
	}

	public int getLimit() {
		return limit;
	}

	public int getPreservedFreeDiskPerc() {
		return preservedFreeDiskPerc;
	}
	
	public String getCycle() {
		return cycle;
	}
	
	public ConsoleHandler createConsoleHandler() {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(level);
		consoleHandler.setFormatter(new WadeFormatter());
		return consoleHandler;
	}
	
	public RollingFileHandler createRollingFileHandler(String name) {
		String localNamePrefix = "";
		if (namePrefix != null) {
			localNamePrefix = localNamePrefix + namePrefix; 
		}
		if (name != null) {
			localNamePrefix = localNamePrefix + name; 
		}
		return new RollingFileHandler(location, localNamePrefix, dateFormat, hourFormat, nameSuffix, limit, preservedFreeDiskPerc, cycle, level, new WadeFormatter(), null, projectHome);
	}
	
	private void configure(String location, String namePrefix, String dateFormat, String hourFormat, String nameSuffix, int limit, int preservedFreeDiskPerc, String cycle, Level level) {
		if (location == null) {
			location = ".";
		}
		this.location = location; 
		
		if (namePrefix == null) {
			namePrefix = "";
		}
		this.namePrefix = namePrefix;
		
		if (dateFormat == null) {
			dateFormat = "yyyy_MM_dd";
		}
		this.dateFormat = dateFormat;

		if (hourFormat == null) {
			hourFormat = "HH_mm_ss";
		}
		this.hourFormat = hourFormat;
		
		if (nameSuffix == null) {
			nameSuffix = ".log";
		}
		this.nameSuffix = nameSuffix;
		
        if (limit < 0) {
            limit = 0;
        }
		this.limit = limit;
		
		if (preservedFreeDiskPerc < 0) {
			preservedFreeDiskPerc = 0;
		}
		this.preservedFreeDiskPerc = preservedFreeDiskPerc;
		
		if (cycle == null || (!cycle.equalsIgnoreCase("day") && !cycle.equalsIgnoreCase("week") && !cycle.equalsIgnoreCase("month") && !cycle.equalsIgnoreCase("year"))) {
			cycle = "day";
		}
		this.cycle = cycle;
		
		if (level == null) {
			level = Level.ALL;
		}
        this.level = level;
	}
	
	private String getLogManagerProperty(String name) {
		return logManager.getProperty(getClass().getName()+"."+name);
	}

	private String getFileProperty(String name) {
		return (String)fileProperties.get(getClass().getName()+"."+name);
	}
	
    private String castToString(String val) {
        if (val == null) {
            return null;
        }
        return val.trim();
    }

    private int castToInt(String val) {
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private Level castToLevel(String val) {
        if (val == null) {
            return null;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return null;
        }
    }
    
	/**
	 *	If WadeHandler is present there may be two scenarios:
	 *	- is a MAIN-CONTAINER:
	 *		- remove all ConsoleHandlers and RollingFileHandlers
	 *		 - add a ConsoleHandler configured with WadeHandler parameters and the WadeFormatter
	 *		 - add a RollingFileHandler configured with WadeHandler parameters and the WadeFormatter with name "main"
	 *	- is a PROJECT-CONTAINER:
	 *	     - remove all ConsoleHandlers
	 *	     - add a ConsoleHandler configured with WadeHandler parameters and the WadeFormatter without source-name
	 *         because it is already added by ProjectOutputHandler (necessary to bind all the lines of a stack-trace 
	 *         to the same source)
	 *         
	 *  If WadeHandler is not present:
	 *  - add default ConsoleHandler if not present
	 *  - if is a MAIN-CONTAINER add default RollingFileHandler
	 */
	public static void manage(Logger logger, boolean mainContainer) {
		WadeHandler wadeHandler = null;
		List<Handler> consoleHandlers = new ArrayList<Handler>();  
		List<Handler> rollingFileHandlers = new ArrayList<Handler>();
		Handler[] handlers = logger.getHandlers();
		for (Handler h : handlers) {
			if (h instanceof WadeHandler) {
				wadeHandler = (WadeHandler)h;
			}
			else if (h instanceof ConsoleHandler) {
				consoleHandlers.add(h);
			}
			else if (h instanceof RollingFileHandler) {
				rollingFileHandlers.add(h);
			}
		}

		for (Handler ch : consoleHandlers) {
			logger.removeHandler(ch);
		}
		if (mainContainer) {
			for (Handler rfh : rollingFileHandlers) {
				logger.removeHandler(rfh);
			}
		}
		
		if (wadeHandler != null) {
			logger.addHandler(wadeHandler.createConsoleHandler());
			if (mainContainer) {
				logger.addHandler(wadeHandler.createRollingFileHandler("main"));
			}
		} else {
			logger.addHandler(createDefaultConsoleHandler());
			if (mainContainer) {
				logger.addHandler(createDefaultRollingFileHandler("main", null));
			}
		}
	}
    
	public static ConsoleHandler createDefaultConsoleHandler() {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(new WadeFormatter());
		return consoleHandler;
	}
	
	public static RollingFileHandler createDefaultRollingFileHandler(String namePrefix, String projectHome) {
		return new RollingFileHandler("./log", namePrefix, "yyyy_MM_dd", "HH_mm_ss", ".log", 0, 10, "day", Level.ALL, new WadeFormatter(), null, projectHome);
	}
}
