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

import jade.util.Logger;
import jade.util.leap.Serializable;

import java.lang.reflect.*;

public class MethodInvocator implements Serializable {
	protected WorkflowBehaviour owner;
	private String methodName;
	private transient Method method;
	private Object param;
	private Class paramClass;
	
	private boolean optional = false;
	private boolean optionalMethodNotPresent = false;
	
	private Logger myLogger;

	public MethodInvocator(WorkflowBehaviour owner, String methodName) {
		this(owner, methodName, null, null);
	}
	
	public MethodInvocator(WorkflowBehaviour owner, String methodName, Object param, Class paramClass) {
		this.owner = owner;
		this.methodName = methodName;
		this.param = param;
		this.paramClass = paramClass;
		myLogger = Logger.getMyLogger(MethodInvocator.class.getName());
	}
	
	public void setOptional() {
		optional = true;
	}
	
	public Object invoke() throws Exception {
		if (optionalMethodNotPresent) {
			// Avoid loosing time when we already know that an optional method is not present
			return null;
		}
		
		if (method == null) {
			try {
				method = getMethod(methodName);
			}
			catch (NoSuchMethodException nsme) {
				if (optional) {
					optionalMethodNotPresent = true;
					return null;
				}
				else {
					throw nsme;
				}
			}
		}
		Object[] params = getMethodParams();
		if (myLogger.isLoggable(Logger.FINEST)) {
			myLogger.log(Logger.FINEST, "Invoking method "+method.getName());
		}
		try {
			return method.invoke(owner, params);
		}
		catch (InvocationTargetException ite) {
			Throwable t = ite.getTargetException();
			if (t instanceof Exception) {
				throw (Exception) t;
			}
			else {
				throw (Error) t;
			}
		}
	}
	
	protected Object[] getMethodParams() {
		return param != null ? new Object[]{param} : new Object[0];
	}
	
	protected Class[] getMethodParamTypes() {
		if (paramClass != null) {
			return new Class[]{paramClass};
		} else {
			if (param != null) {
				return new Class[]{param.getClass()};
			}
			else {
				return new Class[0];
			}
		}
	}
	
	private Method getMethod(String methodName) throws NoSuchMethodException {
		Class[] paramTypes = getMethodParamTypes();
		return owner.getMethod(methodName, paramTypes);
	}
	
}
