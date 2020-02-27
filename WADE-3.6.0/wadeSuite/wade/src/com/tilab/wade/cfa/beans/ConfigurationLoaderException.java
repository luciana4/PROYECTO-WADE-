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
package com.tilab.wade.cfa.beans;


/**
 *
 * @author Enrico Scagliotti
 * ConfigurationLoaderException
 */
public class ConfigurationLoaderException extends Exception {

	private static final long serialVersionUID = -7524362777042177496L;

	/**
     * Constructs a ConfigurationLoaderException with no detail message. A detail
     * message is a String that describes this particular exception.
     */
    public ConfigurationLoaderException() {
    	super();
    }

    /**
     * Constructs a ConfigurationLoaderException with the specified detail message.
     * A detail message is a String that describes this particular
     * exception.
     *
     * <p>
     *
     * @param msg the detail message.
     */
    public ConfigurationLoaderException(String msg) {
    	super(msg);
    }

    /**
     * Constructs a ConfigurationLoaderException with the specified detail message.
     * A detail message is a String that describes this particular
     * exception.
     *
     * <p>
     *
     * @param msg the detail message.
     * @param e the detail exception.
     */
    public ConfigurationLoaderException(String msg, Exception e) {
    	super(msg, e);
    }

    /**
     * Constructs a ConfigurationLoaderException with the specified detail exception.
     *
     * <p>
     *
     * @param e the detail exception.
     */
	public ConfigurationLoaderException(Exception e) {
		super(e);
	}

	@Override
	public String toString() {
		if (getMessage() == null) {
			return super.toString();
		}
		return getMessage();
	}
}

