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
package com.tilab.wade.performer;

import jade.core.Agent;
import jade.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.WorkflowEngineAgent.WorkflowExecutor;
import com.tilab.wade.utils.FileUtils;

class WorkflowSerializationManager {
	private static WorkflowEngineAgent enclosingAgent;
	
	private static Logger myLogger = Logger.getJADELogger(WorkflowSerializationManager.class.getName());

    private static final HashMap primClasses = new HashMap(8, 1.0F);
    static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
    }
    
	static void save(WorkflowBehaviour wb) {
		try {
			WorkflowEngineAgent.WorkflowExecutor we = wb.rootExecutor;
			WorkflowEngineAgent wea = we.getWorkflow().getAgent();
			
			byte[] serializedState = serializeWorkflow(we);
			
			if (wb.supportTags()) {
				// Store the serialized state locally
				we.lastSerializedState = serializedState;
			}
			if (wb.isLongRunning()) {
				// Store the serialized state in the WSMA
				wea.getStatusManager().notifySerializedStateChanged(we, serializedState);
			}
		}
		catch (Exception e) {
			myLogger.log(Logger.WARNING, "Error serializing workflow "+wb.getExecutionId()+", exception message: "+e.getMessage());
		}
	}
	
	static WorkflowEngineAgent getEnclosingAgent() {
		return enclosingAgent;
	}

	static byte[] serializeWorkflow(WorkflowEngineAgent.WorkflowExecutor executor) throws Exception {
		long startTime = System.currentTimeMillis();
		WorkflowEngineAgent agent = executor.getWorkflow().getAgent();
		
		// Remove the reference to the agent before serializing to avoid serializing the whole agent
		executor.setAgent(null);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream encoder = new ObjectOutputStream(out);
			encoder.writeObject(executor);
		}
		finally {
			// Restore the reference to the agent
			executor.setAgent(agent);
		}
		
		// Get byte-array
		byte[] output = out.toByteArray();

		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Workflow serialize time="+(System.currentTimeMillis()-startTime)+", size="+output.length);
		}
		
		// Zip the content
		if (executor.getWorkflow().isCompressionActive()) {
			output = compress(output);
		}
		
		// Add a prefix (0xAA 0xBB 13-digit) with the class-loader-ID 
		String clId = executor.getDescriptor().getClassLoaderIdentifier();
		if (clId != null) {
			byte[] newOutput = new byte[output.length + 15];
			newOutput[0] = (byte) 0xAA;
			newOutput[1] = (byte) 0xBB;
			System.arraycopy(clId.getBytes(), 0, newOutput, 2, 13);
			System.arraycopy(output, 0, newOutput, 15, output.length);
			
			return newOutput; 
		} else {
			return output;	
		}
	}
	
	static synchronized WorkflowEngineAgent.WorkflowExecutor deserializeWorkflow(byte[] serializedWorkflow, WorkflowEngineAgent agent) throws Exception {
		// Store the agent that is deserializing the workflow so that WorkflowExecutorReplacer.readResolve() 
		// can find it by means of the getEnclosingAgent() method
		enclosingAgent = agent;
		
		// Check if serialized-workflow contains the class-loader-ID, in this case the
		// first two bytes are: 0xAA 0xBB
		String wclId = null;
		if (serializedWorkflow[0] == (byte) 0xAA && serializedWorkflow[1] == (byte) 0xBB) {
			wclId = new String(Arrays.copyOfRange(serializedWorkflow, 2, 15));
			serializedWorkflow = Arrays.copyOfRange(serializedWorkflow, 15, serializedWorkflow.length);
		}
		
		// Check if serialized-workflow is compressed, in this case the
		// first two bytes are: 0x78 0xDA
		if (serializedWorkflow.length>2 && serializedWorkflow[0]==(byte)0x78 && serializedWorkflow[1]==(byte)0xDA) {
			try {
				// Unzip the content
				serializedWorkflow = decompress(serializedWorkflow);
			} catch (DataFormatException dfe) {
				// Decompression failed, probably is not compressed but the content start with 0x78 0xDA 
			}
		}
		
		// Get appropriate wade-class-loader
		final ClassLoader wadeClassLoader = CAServices.getInstance(enclosingAgent).getClassLoader(wclId);
		
		// Deserialize using appropriate wade-class-loader
		long startTime = System.currentTimeMillis();
		ByteArrayInputStream inp = new ByteArrayInputStream(serializedWorkflow);
		ObjectInputStream decoder = new ObjectInputStream(inp) {

			@Override
			protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
				try {
					return Class.forName(desc.getName(), true, wadeClassLoader);
				} catch (ClassNotFoundException ex) {
				    Class cl = (Class) primClasses.get(desc.getName());
				    if (cl != null) {
				    	return cl;
				    } else {
				    	throw ex;
				    }
				}
			}
		};
		WorkflowEngineAgent.WorkflowExecutor executor = (WorkflowEngineAgent.WorkflowExecutor) decoder.readObject();
		executor.lastSerializedState = serializedWorkflow;
		
		executor.getWorkflow().initRootExecutor();
		
		enclosingAgent = null;
		
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Workflow deserialize time="+(System.currentTimeMillis()-startTime));
		}

		return executor;
	}
	
	static void tag(String tagName, WorkflowBehaviour wb) throws Exception {
		if (wb.supportTags()) {
			byte[] serializedState = wb.rootExecutor.lastSerializedState;
			String executionId = wb.getExecutionId();
			WorkflowEngineAgent agent =  wb.getAgent();
			
			// Be sure the TAG directory for this executionId exists. Create it otherwise
			String tagDirName = getTagDirName(executionId, agent);
			File tagDir = new File(tagDirName);
			if (!tagDir.exists()) {
				myLogger.log(Logger.CONFIG, "Creating TAG directory "+tagDirName+" ...");
				boolean success = tagDir.mkdirs();
				if (!success) {
					throw new IOException("Cannot create TAG directory "+tagDirName+".");
				}
			}
			else if (!tagDir.isDirectory()) {
				throw new IOException("TAG-directory-name "+tagDirName+" does not refer to a directory.");
			}
			
			String tagFileName = getTagFileName(tagDirName, tagName);
			
			if (myLogger.isLoggable(Logger.FINE)) { 
				myLogger.log(Logger.FINE, "Writing TAG file "+tagFileName+" ...");
			}
			
			FileUtils.byte2File(new File(tagFileName), serializedState);
			
			if (myLogger.isLoggable(Logger.FINE)) {
				myLogger.log(Logger.FINE, "TAG file "+tagFileName+" successfully created");
			}
		}
		else {
			throw new WorkflowException("TAG error: workflow does not support TAGS");
		}
	}
	
	static void reloadTag(String tagName, WorkflowBehaviour wb) throws Exception {
		// This method must always act on the top-most workflow
		wb = wb.rootExecutor.getWorkflow();
		WorkflowEngineAgent wea = wb.getAgent();
		myLogger.log(Logger.INFO, "Reloading TAG "+tagName+" ...");
		
		String tagDirName = getTagDirName(wb.getExecutionId(), wb.getAgent());
		String tagFileName = getTagFileName(tagDirName, tagName);
		File tagFile = new File(tagFileName);
		if (tagFile.exists()) {
			byte[] tagBytes = FileUtils.file2byte(tagFile);
			WorkflowExecutor we = WorkflowSerializationManager.deserializeWorkflow(tagBytes, wea);
			
			WorkflowEngineAgent.WorkflowExecutor rootExecutor = wb.rootExecutor;
			
			WorkflowBehaviour oldWb = (WorkflowBehaviour) we.getState(WorkflowEngineAgent.EXECUTE);
			rootExecutor.setDataStore(oldWb.getDataStore());
			rootExecutor.registerFirstState(oldWb, WorkflowEngineAgent.EXECUTE);
			// Re-initialize the rootExecutor internal variable of the old WF 
			oldWb.initRootExecutor();
			// Re-initialize the myWorkflow internal variable of the executor 
			rootExecutor.setWorkflow(oldWb);
			// Re-initialize the last serialized state
			rootExecutor.lastSerializedState = we.lastSerializedState;
			// If the WF is long-running notify the WSMA so that it is consistent with the reloaded TAG
			if (wb.isLongRunning()) {
				wea.getStatusManager().notifySerializedStateChanged(rootExecutor, tagBytes);
			}
			
			// Avoid evaluating transitions at the end of the current state by declaring it as interrupted
			((HierarchyNode) wb.getCurrent()).setInterrupted();
			
			myLogger.log(Logger.INFO, "TAG "+tagName+" successfully reloaded");
			
			// JUMP OUT!
			throw new Agent.Interrupted();
		}
		else {
			throw new IOException("TAG "+tagName+" not found");
		}
	}
	
	static void deleteTags(WorkflowBehaviour wb) throws Exception {
		wb = wb.rootExecutor.getWorkflow();
		WorkflowEngineAgent wea = wb.getAgent();
		String tagDirName = getTagDirName(wb.getExecutionId(), wb.getAgent());
		
		myLogger.log(Logger.INFO, "Deleting TAGS "+tagDirName+" ...");
		
		File tagsFile = new File(tagDirName);
		if (tagsFile.exists()) {
			FileUtils.recursiveDelete(tagsFile);
		}

		myLogger.log(Logger.INFO, "TAGS "+tagDirName+" successfully deleted");
	}
	
	
	private static String getTagDirName(String executionId, WorkflowEngineAgent agent) {
		String fileSeparator = System.getProperty("file.separator");
		String rootTagDirName = agent.getTypeProperty(WorkflowEngineAgent.TAG_DIR, "TAG");
		return rootTagDirName+fileSeparator+executionId.replace(':', '_');
	}
	
	private static String getTagFileName(String tagDirName, String tagName) {
		String fileSeparator = System.getProperty("file.separator");
		return tagDirName+fileSeparator+tagName+".tag";
	}
		
	private static byte[] compress(byte[] input) {
		long startTime = System.currentTimeMillis();
		
		// Create the compressor with highest level of compression
		Deflater compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);

		// Give the compressor the data to compress
		compressor.setInput(input);
		compressor.finish();
		
		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		// Compress the data
		byte[] buf = new byte[1024*100];
		while (!compressor.finished()) {
		    int count = compressor.deflate(buf);
		    bos.write(buf, 0, count);
		}
		try {
		    bos.close();
		} catch (IOException e) {
		}

		// Get the compressed data
		byte[] out = bos.toByteArray();
		
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Workflow compress time="+(System.currentTimeMillis()-startTime)+", initial size="+input.length+", final size"+out.length);
		}
		
		return out;
	}

	private static byte[] decompress(byte[] input) throws DataFormatException {
		long startTime = System.currentTimeMillis();
		
		// Create the decompressor and give it the data to compress
		Inflater decompressor = new Inflater();
		decompressor.setInput(input);
	
		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
	
		// Decompress the data
		byte[] buf = new byte[1024*100];
		while (!decompressor.finished()) {
	        int count = decompressor.inflate(buf);
	        bos.write(buf, 0, count);
		}
		try {
		    bos.close();
		} catch (IOException e) {
		}

		// Get the decompressed data
		byte[] out = bos.toByteArray();
		
		if (myLogger.isLoggable(Logger.FINE)) {
			myLogger.log(Logger.FINE, "Workflow decompress time="+(System.currentTimeMillis()-startTime)+", initial size="+input.length+", final size"+out.length);
		}
		
		return out;
	}
}