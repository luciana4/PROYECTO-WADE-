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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.text.MessageFormat;

/**
 * This class provides a utility method to localize messages encoded according to the format described 
 * in <code>MessageCode</code>
 */
public class MessageLocalizer {
	/**
	 * The base name for all resource bundles containing localized WADE messages
	 */
	private static final String RESOURCE_BUNDLE_BASE_NAME = "locales.language";
	
	private static ResourceBundle myBundle;
	
	public static void setLocale(Locale locale) {
		myBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, locale);
	}
	
	public static String localize(String msg) {
		if (msg != null && msg.startsWith(MessageCode.MSGCODE_START_CHARACTER)) {
			// The message is encoded --> localize it
			int codeStartIndex = MessageCode.MSGCODE_START_CHARACTER.length();
			int codeEndIndex = msg.indexOf(MessageCode.ARGUMENT_SEPARATOR);
			String code = null;
			List params = null;
			if (codeEndIndex < 0) {
				code = msg.substring(codeStartIndex);
			}
			else {
				code = msg.substring(codeStartIndex, codeEndIndex);
				params = getParams(msg.substring(codeEndIndex+1));
			}
			
			try {
				String localizedMsg = myBundle.getString(code);
				if (params != null) {
					localizedMsg = MessageFormat.format(localizedMsg, params.toArray());
				}
				return localizedMsg;
			}
			catch (Exception e) {
				// Localized message not found or format error --> just return the non-localized message
				return msg;
			}
		}
		else {
			// The message is not encoded --> just do nothing
			return msg;
		}
	}
	
	private static List getParams(String paramsStr) {
		List l = new ArrayList();
		if (paramsStr != null) {
			StringTokenizer st = new StringTokenizer(paramsStr, MessageCode.ARGUMENT_SEPARATOR);
			while (st.hasMoreTokens()) {
				l.add(st.nextToken());
			}
		}
		return l;
	}
}
