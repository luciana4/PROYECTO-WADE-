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

//#MIDP_EXCLUDE_FILE

import jade.util.leap.List;

import java.io.IOException;
import java.util.jar.JarFile;

/**
 */
class MultipleJarClassLoader extends ClassLoader {
  
  /**
   */
  public MultipleJarClassLoader(List packages, ClassLoader parent) {
  	super(parent);
    /*File file = new File(filename);
    if(file.exists()){
      _file = new JarFile(new File(filename));
    }*/
  }
  
  /**
   * Method to close file descriptor for the JAR used by this ClassLoader.
   * If this method is invoked, no more classes from the JAR file will be loaded
   * using this ClassLoader.
   * @throws IOException File cannot be closed
   */
  public void close() throws IOException{
    //if(_file != null) _file.close();
  }
  
  protected Class findClass(String className) throws ClassNotFoundException{
    /*if (_file != null) {
	  	ZipEntry zEntry = _file.getEntry(className.replace('.', '/') + ".class");
	    try{
	      InputStream is = _file.getInputStream(zEntry);
	      int length = is.available();
	      byte[] rawClass = new byte[length];
	      is.read(rawClass);
	      is.close();
	      return defineClass(className, rawClass, 0, length);
	    }
	    catch(IOException ioe){
	      throw new ClassNotFoundException("IOError while reading jar file for class "+className+". "+ioe);
	    }
    }
    else {
    	throw new ClassNotFoundException(className);
    }*/
  	throw new ClassNotFoundException(className);
  }
  
  private JarFile _file = null;

}
