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

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: May 24, 2004
 * Time: 12:44:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileUtils {
	public static final String SEPARATOR = File.separator;
	private static final long LOCK_WAIT = 100;
	private static final long LOCK_TIMEOUT = 5000;


	public static String [] getJarFilelist(String dirname) {
		File dir = new File(dirname);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".jar") || name.endsWith(".zip"));
			}
		};
		return  dir.list(filter);

	}
	public static String [] getWfClassFilelist(String dirname,final String wfId) {
		File dir = new File(dirname);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith(wfId) && name.endsWith(".class"));
			}
		};
		return dir.list(filter);
	}

	public static String [] getAllFilelist(String dirname) {
		File dir = new File(dirname);
		return  dir.list();

	}
	public static File [] getDirlist(String dirname) {
		File dir = new File(dirname);
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		};
		return dir.listFiles(filter);
	}
	public static void printArray(String [] array) {
		for(int i=0;i < array.length;i++) {
			System.out.println(array[i]);
		}
	}
	public static void printFileArray(File [] array) {
		for(int i=0;i < array.length;i++) {
			System.out.println(array[i].getName());
		}
	}

	public static String getFileContent(File file) throws FileNotFoundException,IOException {
		return getReaderContent(new FileReader(file));
	}

	public static String getStreamContent(InputStream str) throws FileNotFoundException,IOException {
		return getReaderContent(new InputStreamReader(str));
	}

	public static String getReaderContent(Reader r) throws IOException {
		StringBuffer contents = new StringBuffer();
		String lineSeparator = System.getProperty("line.separator");

		BufferedReader input = null;
		try {
			input = new BufferedReader(r);
			String line = null;
			while (( line = input.readLine()) != null){
				contents.append(line);
				contents.append(lineSeparator);
			}
		} finally {
			if (input!= null) {
				try {input.close();} catch(Exception e) {}
			}
		}
		return contents.toString();
	}

	public static void setFileContents(File file, String contents) throws FileNotFoundException, IOException {
		if (file == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!file.exists()) {
			file.createNewFile();
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + file);
		}
		if (!file.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + file);
		}

		//declared here only to make visible to finally clause; generic reference
		Writer output = null;
		try {
			//use buffering
			output = new BufferedWriter( new FileWriter(file) );
			output.write( contents );
		}
		finally {
			//flush and close both "output" and its underlying FileWriter
			if (output != null) {
				try {output.close();} catch(Exception e) {}
			}
		}
	}

	public static void writeFile(File file, byte [] content) throws IOException {
		RandomAccessFile raf = null;
		FileLock lock = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			//needed if the old file length is greater than the new one
			raf.setLength(content.length);
			// Try to lock the file
			long startTime = System.currentTimeMillis();
			while (true) {
				try {
					lock = raf.getChannel().tryLock();
					if (lock != null) {
						break;
					}
					else {
						throw new  OverlappingFileLockException();
					}
				}
				catch (OverlappingFileLockException ofle)   {

					// File already locked
					if (System.currentTimeMillis() - startTime > LOCK_TIMEOUT) {
						throw new IOException("File "+ file.getName() +" locked");
					}
					waitABit(LOCK_WAIT);
				}
			}
			// Write file
			raf.write(content);
		}
		finally {
			if (raf != null) {
				try {raf.close();} catch(Exception e) {}
			}
			if (lock != null) {
				try {lock.release();} catch(Exception e) {}
			}
		}
	}

	private static void waitABit(long timeout) {
		try {
			Thread.sleep(timeout);
		}
		catch (Exception e) {}
	}

	public static void writeFile(File file, String content) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			fw.write(content);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new IOException("Error writing file");
		}
		finally {
			if (fw != null) {
				try {fw.close();} catch(Exception e) {}
			}
		}
	}

	// Copies src file to dst file.
	// If the dst file does not exist, it is created
	public static void copy(File src, File dst) throws IOException {
		// If dest is a directory use the same name of source
		if (dst.isDirectory()) {
			dst = new File(dst, src.getName());
		}

		FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } 
        finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
	}

	/* Copies all files under srcDir to dstDir. If dstDir does not exist, it will be created. */
	public static void copyDirectory(File srcDir, File dstDir) throws IOException {
		copyDirectory(srcDir, dstDir, true);
	}
	
	public static void copyDirectory(File srcDir, File dstDir, boolean recursive) throws IOException {
		if (srcDir.isDirectory()) {
			if (!dstDir.exists()) {
				dstDir.mkdirs();
			}
			String[] children = srcDir.list();
			for (int i=0; i<children.length; i++) {
				File srcFile = new File(srcDir, children[i]);
				if (recursive || srcFile.isFile()) {
					copyDirectory(srcFile, new File(dstDir, children[i]));
				}
			}
		} else {
			copy(srcDir, dstDir);
		}
	}

	public static byte[] file2byte(File file) throws IOException {
		FileChannel channel = null;
		byte [] fileBuffer;
		try {
			channel = new FileInputStream(file).getChannel();
			ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY,0,(int) channel.size());
			fileBuffer = new byte[buf.capacity()];
			buf.get(fileBuffer,0,fileBuffer.length);
		} finally {
			if (channel != null) {
				try {
					channel.force(false);
					channel.close();
					channel = null;
				} catch(Exception e) {}
			}
		}
		
		return fileBuffer;
	}

	public static void byte2File(File file, byte[] bytes) throws IOException {
		RandomAccessFile raf = null;
		FileChannel channel = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			//needed if the old file length is greater than the new one
			raf.setLength(bytes.length);
			channel = raf.getChannel();
			channel.write(ByteBuffer.wrap(bytes));
		}
		finally {
			if (channel != null) {
				try {
					channel.force(false);
					channel.close();
					channel = null;
					raf.close();
				} catch(Exception e) {}
			}
		}
	}
	
	/* Crea un file jar a partire da un file o una dir
	 */
	public static void makeJar(String classes, String jars, String filename) throws IOException, FileNotFoundException  {
		JarOutputStream jos = null;
		try {
			String jarname = filename.replace(SEPARATOR.charAt(0),'_');
			File file = new File(new File(classes), filename);
			File jarfile = new File(new File(jars), jarname);
			jos = new JarOutputStream(new FileOutputStream(jarfile + ".jar"));
			classes = new File(classes).toString() + SEPARATOR;
			recurseFiles(classes, file, jos);
		} finally {
			if (jos != null) {
				try {jos.close();} catch(Exception e) {}
			}
		}
	}

	public static void recursiveDelete(File path) throws IOException {
		File[] files = path.listFiles();
		for(int i=0; i<files.length; ++i) {
			if(files[i].isDirectory())
				recursiveDelete(files[i]);
			files[i].delete();
		}
		path.delete();
	}

	private static void recurseFiles(String root, File file, JarOutputStream jos) throws IOException, FileNotFoundException {

		if(file.isDirectory()) {
			//Create a jar entry

			// Crea il path relativo da inserire come nome della entry
			String entry = substringAfter(file.toString(),root);
			// INDEPENDENT-SEPARATOR
			//entry = entry.replace('\\','/');
			entry = entry.replace(SEPARATOR.charAt(0),'/');

			//System.out.println("entry dir "+entry);
			JarEntry jarEntry = new JarEntry(entry+"/");
			try {
				jos.putNextEntry(jarEntry);
			} finally {
				try {jos.closeEntry();} catch(Exception e) {}
			}

			String [] fileNames = file.list();
			if(fileNames != null) {
				for(int i=0; i<fileNames.length; i++) {
					recurseFiles(root,new File(file, fileNames[i]), jos);
				}
			}
		}
		else {
			byte[] buf = new byte[1024];
			int len;

			// Crea il path relativo da inserire come nome della entry
			String entry = substringAfter(file.toString(),root);
			// INDEPENDENT-SEPARATOR
			//entry = entry.replace('\\','/');
			entry = entry.replace(SEPARATOR.charAt(0),'/');
			//Create a jar entry
			JarEntry jarEntry = new JarEntry(entry);
			BufferedInputStream in = null;
			try {
				FileInputStream fin = new FileInputStream(file);
				in = new BufferedInputStream(fin);
				jos.putNextEntry(jarEntry);
				while((len = in.read(buf)) > 0) {
					jos.write(buf, 0, len);
				}
			} finally {
				if (in != null) {
					try {in.close();} catch(Exception e){}
				}
				try {jos.closeEntry();} catch(Exception e){}
			}
		}
	}

	private static String substringAfter(String str, String separator) {
		if (str == null || str.length() == 0) {
			return str;
		}
		if (separator == null) {
			return "";
		}
		int pos = str.indexOf(separator);
		if (pos == -1) {
			return "";
		}
		return str.substring(pos + separator.length());
	}

	/* not a file utility, but dunno where the hell to put it */
	public static boolean compareObject(Object o1, Object o2) {
		if ((o1 == null && o2 == null) ||
				(o1 != null && o2 != null && o1.toString().equals(o2.toString()))) {
			return true;
		}
		return false;
	}
	
	 public static void unzip(File zipFile, File destPath, List<String> filesToExtract) throws IOException {
		if (!destPath.exists()) {
			destPath.mkdirs();
		}

		byte[] buf = new byte[1024];
		int n;
		ZipInputStream zipInputStream = null;
		try {
	        zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
	
	        ZipEntry zipEntry = zipInputStream.getNextEntry();
	        while (zipEntry != null) { 
	            String entryName = zipEntry.getName();
	            if (filesToExtract == null || filesToExtract.contains(entryName)) {
		            File destFile = new File(destPath, entryName);
		            if(!zipEntry.isDirectory()) {
		            	
		            	File parentFile = destFile.getParentFile();
		            	if (parentFile !=null && !parentFile.exists()) {
		            		parentFile.mkdirs();
		            	}
		            	
		                FileOutputStream fileOutputStream = new FileOutputStream(destFile);
		                while ((n = zipInputStream.read(buf, 0, 1024)) > -1)
		                	fileOutputStream.write(buf, 0, n);
		
		                fileOutputStream.close(); 
		            }
	            }
	
	            zipInputStream.closeEntry();
	            zipEntry = zipInputStream.getNextEntry();
	        }
		} finally {
			if (zipInputStream != null) {
				try {zipInputStream.close();} catch(Exception e){}
			}
		}
    }
}
