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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import com.tilab.wade.utils.FileUtils;

/**
   This handler supports the following properties in the log property files.
   <p>
   <table align="center" bgcolor="#ddddff" border=1 cellpadding="10" cellspacing="0"><tr><td><pre>
   com.tilab.wade.utils.logging.RollingFileHandler.level = FINEST
   com.tilab.wade.utils.logging.RollingFileHandler.location = ./log/
   com.tilab.wade.utils.logging.RollingFileHandler.namePrefix = project
   com.tilab.wade.utils.logging.RollingFileHandler.dateFormat = yyyy_MM_dd
   com.tilab.wade.utils.logging.RollingFileHandler.hourFormat = HH_mm_ss
   com.tilab.wade.utils.logging.RollingFileHandler.nameSuffix = .log
   com.tilab.wade.utils.logging.RollingFileHandler.cycle = day
   com.tilab.wade.utils.logging.RollingFileHandler.limit = 50000000
   com.tilab.wade.utils.logging.RollingFileHandler.preservedFreeDiskPerc = 10
   com.tilab.wade.utils.logging.RollingFileHandler.formatter = java.util.logging.SimpleFormatter
   </pre></td></tr></table>
 */
public class RollingFileHandler extends StreamHandler {

	private static final String CYCLE_NONE = "none";
	private static final String CYCLE_DAY = "day";
	private static final String CYCLE_WEEK = "week";
	private static final String CYCLE_MONTH = "month";
	private static final String CYCLE_YEAR = "year";
	
	private LogManager manager;
	private long cycleThreshold;
	private String baseLocation;
	private MeteredStream meteredStream;
	private File activeFile;
	private long firstLogDate;
	private long lastLogDate;
	private boolean limitExceededOnCycle;
	
	// The the directory where to put log files
	private String location;
	// The name prefix of log files
	private String namePrefix;
	// Date formatter to use in file name
	private SimpleDateFormat dateFormatter;
	// Hour formatter to use in file name
	private SimpleDateFormat hourFormatter;
	// File suffix (typically the extension)
	private String nameSuffix;
	// File limit in byte
	private int limit;
	// Percentage of space to be preserved on disk
	private int preservedFreeDiskPerc;
	// Time cycle (for file rolling)
	private String cycle;


	public RollingFileHandler() {
		manager = LogManager.getLogManager();

		configure(getStringProperty("location"),
                  getStringProperty("namePrefix"),
    	          getStringProperty("dateFormat"),
    	          getStringProperty("hourFormat"),
    	          getStringProperty("nameSuffix"),
                  getIntProperty("limit"),
                  getIntProperty("preservedFreeDiskPerc"),
                  getStringProperty("cycle"),
                  getLevelProperty("level"),
                  getFormatterProperty("formatter"),
                  getStringProperty("encoding"),
                  null);
		openActiveFile();
	}

	public RollingFileHandler(String location, String namePrefix, String dateFormat, String hourFormat, String nameSuffix, int limit, int preservedFreeDiskPerc, String cycle, Level level, Formatter formatter, String encoding, String baseLocation) {
		configure(location, namePrefix, dateFormat, hourFormat, nameSuffix, limit, preservedFreeDiskPerc, cycle, level, formatter, encoding, baseLocation);
		openActiveFile();
	}
	
	private void configure(String location, String namePrefix, String dateFormat, String hourFormat, String nameSuffix, int limit, int preservedFreeDiskPerc, String cycle, Level level, Formatter formatter, String encoding, String baseLocation) {
		this.baseLocation = baseLocation;
		
		if (location == null) {
			location = ".";
		}
		this.location = location;
		File locationFile = new File(location);
		if (!locationFile.exists()) {
			locationFile.mkdirs();
		}
		
		if (namePrefix == null) {
			namePrefix = "";
		}
		this.namePrefix = namePrefix;
		
		if (dateFormat == null) {
			dateFormat = "yyyy_MM_dd";
		}
		dateFormatter = new SimpleDateFormat(dateFormat, Locale.getDefault());

		if (hourFormat == null) {
			hourFormat = "HH_mm_ss";
		}
		hourFormatter = new SimpleDateFormat(hourFormat, Locale.getDefault());
		
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
		
		if (cycle == null || (!cycle.equalsIgnoreCase(CYCLE_NONE) && !cycle.equalsIgnoreCase(CYCLE_DAY) && !cycle.equalsIgnoreCase(CYCLE_WEEK) && !cycle.equalsIgnoreCase(CYCLE_MONTH) && !cycle.equalsIgnoreCase(CYCLE_YEAR))) {
			cycle = CYCLE_DAY;
		}
		this.cycle = cycle;
		
		if (level == null) {
			level = Level.ALL;
		}
        setLevel(level);
        
        if (formatter == null) {
        	formatter = new SimpleFormatter();
        }
        setFormatter(formatter);
        
        try {
            setEncoding(encoding);
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work.
            }
        }
        
        firstLogDate = -1;
        lastLogDate = -1;
        limitExceededOnCycle = false;
	}
    
	private synchronized void openActiveFile() {
		// Set first cycle threshold
		cycleThreshold = getNextCycleThreshold();
		
		// Open active file
		activeFile = getActiveFile();
		setOutputStream(activeFile);
	}

	private long getNextCycleThreshold() {
		// Compute next cycle
		Date nextDate;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(new Date());
		if (cycle.equalsIgnoreCase(CYCLE_NONE)) {
			nextDate = null;
		} else if (cycle.equalsIgnoreCase(CYCLE_WEEK)) {
			gc.add(Calendar.WEEK_OF_YEAR, 1);
			nextDate = gc.getTime();
		} else if (cycle.equalsIgnoreCase(CYCLE_MONTH)) {
			gc.add(Calendar.MONTH, 1);
			int month = gc.get(Calendar.MONTH);
			int year = gc.get(Calendar.YEAR);
			GregorianCalendar gc2 = new GregorianCalendar(year, month, 1);
			nextDate = gc2.getTime();
		} else if (cycle.equalsIgnoreCase(CYCLE_YEAR)) {
			gc.add(Calendar.YEAR, 1);
			int year = gc.get(Calendar.YEAR);
			GregorianCalendar gc2 = new GregorianCalendar(year, 0, 1);
			nextDate = gc2.getTime();
		} else { 
			// Day by default
			gc.add(Calendar.DAY_OF_MONTH, 1);
			nextDate = gc.getTime();
		}

		if (nextDate != null) {
			gc = new GregorianCalendar();
			gc.setTime(nextDate);
			gc.set(Calendar.HOUR, 0);
			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);
	
			nextDate = gc.getTime();
			return nextDate.getTime();
		} 
		else {
			return 0;
		}
	}
	
	private File getActiveFile() {
		File file = new File(location, namePrefix + nameSuffix);
		if (!file.isAbsolute() && baseLocation != null) {
			file = new File(baseLocation, file.getPath());
		}

		if (!file.exists()) {
			try {
				file.createNewFile();
			}
			catch (IOException ioe) {
				reportError(ioe.getMessage(), ioe, ErrorManager.WRITE_FAILURE);
			}
		}

		return file;
	}
	
	private void setOutputStream(File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file, true);
	        BufferedOutputStream bos = new BufferedOutputStream(fos);
	        meteredStream = new MeteredStream(bos, file.length());
			setOutputStream(meteredStream);
		} 
		catch (Exception fnfe) {
			reportError(fnfe.getMessage(), fnfe, ErrorManager.OPEN_FAILURE);
			fnfe.printStackTrace(System.err);
			setOutputStream(System.out);
		}
	}
	
	@Override
	public synchronized void publish(LogRecord record) {
		if (meteredStream != null) {
			// Check if we need to rotate
			boolean limitExceeded = limit > 0 && meteredStream.written >= limit;
			boolean cycleExceeded = cycleThreshold > 0 && System.currentTimeMillis() >= cycleThreshold;
			if (limitExceeded || cycleExceeded) {
				rotate(limitExceeded, cycleExceeded);
			}
	
			// Publish the record
			super.publish(record);
			super.flush();
	
			// Set dates
			if (firstLogDate == -1) {
				firstLogDate = System.currentTimeMillis();
			}
			lastLogDate = System.currentTimeMillis();
		}
	}

	private void rotate(boolean limitExceeded, boolean cycleExceeded) {
		try {
			backupActiveFile(limitExceeded, cycleExceeded);
		}
		catch (Throwable t) {
			t.printStackTrace();
			reportError(t.getMessage(), null, ErrorManager.GENERIC_FAILURE);
		}
		
		if (cycleExceeded) {
			// Set new cycle threshold
			cycleThreshold = getNextCycleThreshold();
		}
		
		firstLogDate = -1;
	}

	private void checkAndFreeSpace(long requiredSize) throws IOException {
		// Calculate min free disk size
		long totalDisk = (new File(activeFile.getParent())).getTotalSpace();
		long preservedFreeDisk = totalDisk * preservedFreeDiskPerc / 100;
		long minFreeDisk = requiredSize + preservedFreeDisk;
		
		// Checks if the current space is already sufficient
		while ((new File(activeFile.getParent())).getFreeSpace() < minFreeDisk) {
			// Get the backup log files ordered by date 
			File[] logFiles = getOrderedBackupLogFiles();
			if (logFiles.length == 0) {
				throw new IOException("Disk space is not enough and no backup log files to be deleted!");
			}

			// Delete the older backup log file
			logFiles[0].delete();
		};		
	}

	private File[] getOrderedBackupLogFiles() {
		// Get only log files (excluded active file) ordered by last modified date
		File dir = new File(activeFile.getParent());
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				String name = file.getName();
				return file.isFile() && name.startsWith(namePrefix) && name.endsWith(nameSuffix) && !name.equals(activeFile.getName());
			}
		};
		File[] files = dir.listFiles(filter);
		Arrays.sort(files, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
		    } 
		});
		return files;
	}
	
	private void backupActiveFile(boolean limitExceeded, boolean cycleExceeded) {
		// This is necessary to keep active the tail on active file
		File sourceFile = new File(activeFile.getPath());

		// Check and if necessary (and possible) free disk space to ensure the required disk space
		try {
			checkAndFreeSpace(sourceFile.length());
		} 
		catch (IOException e) {
			reportError(e.getMessage(), e, ErrorManager.GENERIC_FAILURE);
		}
		
		// Get backup file
		if (limitExceeded) {
			limitExceededOnCycle = true;
		}
		String targetFileName = getBackupFileName(!limitExceededOnCycle);
		File targetFile = new File(activeFile.getParent(), targetFileName);
		if (cycleExceeded) {
			limitExceededOnCycle = false;
		}
		
		// Copy file
		try {
			FileUtils.copy(sourceFile, targetFile);
		}
		catch (IOException e) {
			reportError(e.getMessage(), e, ErrorManager.WRITE_FAILURE);
		}
		
		// Clear active file
		try {
			PrintWriter writer = new PrintWriter(activeFile);
			writer.print("");
			writer.close();
		}
		catch (FileNotFoundException e) {
			reportError(e.getMessage(), e, ErrorManager.WRITE_FAILURE);
		}
		
		// Reset the file size meter
		meteredStream.reset();
	}
	
	private String getBackupFileName(boolean simpleFormat) {
		String startDate = getFormattedDate(firstLogDate);
		String subInterval = "";
		if (!simpleFormat) {
			String startHour = getFormattedHour(firstLogDate);
			String endHour = getFormattedHour(lastLogDate!=-1?lastLogDate:System.currentTimeMillis());

			if (cycle.equalsIgnoreCase(CYCLE_DAY)) {
				subInterval = "[" + startHour + "-" + endHour + "]";
			}
			else {
				String endDate = getFormattedDate(lastLogDate!=-1?lastLogDate:System.currentTimeMillis());
				subInterval = "[" + startDate + "@" + startHour + "-" + endDate + "@" + endHour + "]";
			}
		}
		
		return namePrefix + "-" + startDate + subInterval + nameSuffix;
	}
	
	private String getFormattedDate(long date) {
		String formattedDate = "???";
		if (date != -1) {
			try {
				formattedDate = dateFormatter.format(date);
			} catch (IllegalArgumentException iae) {
				// Ignore wrong date format
			}
		}
		return formattedDate;
	}

	private String getFormattedHour(long date) {
		String formattedHour = "???";
		if (date != -1) {
			try {
				formattedHour = hourFormatter.format(date);
			} catch (IllegalArgumentException iae) {
				// Ignore wrong date format
			}
		}
		return formattedHour;
	}
	
	private String getProperty(String name) {
		return manager.getProperty(getClass().getName()+"."+name);
	}
	
    private String getStringProperty(String name) {
        String val = getProperty(name);
        if (val == null) {
            return null;
        }
        return val.trim();
    }

    private int getIntProperty(String name) {
        String val = getProperty(name);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private Level getLevelProperty(String name) {
        String val = getProperty(name);
        if (val == null) {
            return null;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return null;
        }
    }
    
    private Formatter getFormatterProperty(String name) {
        String val = getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Formatter) clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return null;
    }

    
    // A metered stream is a subclass of OutputStream that
    //   (a) forwards all its output to a target stream
    //   (b) keeps track of how many bytes have been written
    private class MeteredStream extends OutputStream {
        OutputStream out;
        long written;

        MeteredStream(OutputStream out, long written) {
            this.out = out;
            this.written = written;
        }

        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff,off,len);
            written += len;
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
        
        public void reset() {
        	written = 0;
        }
    }

}
