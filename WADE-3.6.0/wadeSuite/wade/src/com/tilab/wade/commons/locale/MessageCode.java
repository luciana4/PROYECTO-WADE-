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
package com.tilab.wade.commons.locale;

/**
 * This interface includes basic constants to format messages that require localization.
 * Such messages must have the form "<code>MSGCODE_START_CHARACTER[code]ARGUMENT_SEPARATOR[arg0]ARGUMENT_SEPARATOR[arg1]...</code>"
 * and can be localized/formatted using the <code>localize()<code> method of the <code>MessageLocalizer</code> class.
 * In order for localization to work properly a suitable properties file must be supplied providing mapping
 * between message codes and localized messages. Localized message arguments must be encoded according to the default pattern
 * of the <code>java.text.MessageFormat</code> class e.g.<br>
 * ACTION_NOT_SUPPORTED = Action {0} not supported by Agents of type {1} *   
 */
public interface MessageCode {
	/**
	 * Start code for all messages that require localization. 
	 */
	public static final String MSGCODE_START_CHARACTER = "MSGCODE_";
	
	/**
	 * Argument separator for messages that require localization
	 */
	public static final String ARGUMENT_SEPARATOR = "_,_";
	
	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an unexpected error occurs
	 */
	public static final String UNEXPECTED_ERROR = MSGCODE_START_CHARACTER + "UNEXPECTED_ERROR";
		
	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an agent
	 * is requested to perform an action that is not supported.
	 */
	public static final String ACTION_NOT_SUPPORTED = MSGCODE_START_CHARACTER + "ACTION_NOT_SUPPORTED";
	
	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an agent
	 * does not understand an incoming request.
	 */
	public static final String REQUEST_NOT_UNDERSTOOD = MSGCODE_START_CHARACTER + "REQUEST_NOT_UNDERSTOOD";

	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an agent
	 * does not respond in time.
	 */
	public static final String TIMEOUT_EXPIRED = MSGCODE_START_CHARACTER + "TIMEOUT_EXPIRED";
	
	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an error occurs retrieving
	 * containers (or more precisely Control Agents) from the DF.
	 */
	public static final String ERROR_RETRIEVING_CONTAINERS_FROM_DF = MSGCODE_START_CHARACTER + "ERROR_RETRIEVING_CONTAINERS_FROM_DF";
	
	/**
	 * Code (already embedding the initial start code) identifying the message to be displayed when an error occurs retrieving
	 * agents living in a given container from the DF.
	 */
	public static final String ERROR_RETRIEVING_AGENTS_IN_CONTAINER_FROM_DF = MSGCODE_START_CHARACTER + "ERROR_RETRIEVING_AGENTS_IN_CONTAINER_FROM_DF";
	
}