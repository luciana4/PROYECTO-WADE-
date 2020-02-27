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
package com.tilab.wade.raa;

import jade.util.Logger;

import com.tilab.wade.cfa.beans.AgentArgumentInfo;
import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.utils.condition.Getter;
import com.tilab.wade.utils.condition.GetterFactory;
import com.tilab.wade.utils.condition.GetterFactoryException;
import com.tilab.wade.utils.condition.UnexistentGetterException;
import com.tilab.wade.utils.condition.basic.StringGetter;

class AgentInfoGetterFactory implements GetterFactory, AgentInfoConstants {

	private final static Logger logger = Logger.getMyLogger(AgentInfoGetterFactory.class.getName());
	private final static Getter nameGetterInstance = new NameGetter();
	private final static Getter typeGetterInstance = new TypeGetter();
	private final static Getter classNameGetterInstance = new ClassNameGetter();
	private final static Getter ownerGetterInstance = new OwnerGetter();

	public static class NameGetter extends StringGetter {
		public String getValue(Object obj) {
			return ((AgentInfo)obj).getName();
		}
	}

	public static class TypeGetter extends StringGetter {
		public String getValue(Object obj) {
			return ((AgentInfo)obj).getType();
		}
	}

	public static class ClassNameGetter extends StringGetter {
		public String getValue(Object obj) {
			return ((AgentInfo)obj).getClassName();
		}
	}

	public static class OwnerGetter extends StringGetter {
		public String getValue(Object obj) {
			return ((AgentInfo)obj).getOwner();
		}
	}

	public class ArgumentGetter extends StringGetter {
		private String argumentName;

		public ArgumentGetter(String argumentName) {
			this.argumentName = argumentName;
		}

		public String getValue(Object obj) {
			String result = null;
			AgentArgumentInfo parameter = ((AgentInfo)obj).getParameter(argumentName);
			// TODO manage parameter not found and parameter is null (with throw Exception()?)
			if (parameter != null) {
				Object value = parameter.getValue();
				if(value != null){
					result = value.toString();
				} else {
					logger.log(Logger.WARNING, "Property "+argumentName+" is null");
				}
			} else {
				logger.log(Logger.WARNING, "Property "+argumentName+" not found!!");
			}
			return result;
		}
	}
	public Getter createGetter(String key) throws GetterFactoryException {
		Getter result = null;
		try {
			if (key.equals(AI_NAME)) {
				result = nameGetterInstance;
			} else if (key.equals(AI_TYPE)) {
				result = typeGetterInstance;
			} else if (key.equals(AI_CLASSNAME)) {
				result = classNameGetterInstance;
			} else if (key.equals(AI_OWNER)) {
				result = ownerGetterInstance;
			} else if (key.startsWith(AI_ARGUMENTS_PREFIX)) {
				String argumentName = key.substring(AI_ARGUMENTS_PREFIX.length());
				result = new ArgumentGetter(argumentName);
			}
		} catch (Exception e) {
			throw new GetterFactoryException(e);
		}
		if (result == null) {
			throw new UnexistentGetterException("Unexistent getter for key "+key);
		}
		return result;
	}
}
