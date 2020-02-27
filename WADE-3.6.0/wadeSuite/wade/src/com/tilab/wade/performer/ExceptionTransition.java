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

public class ExceptionTransition extends Transition {
	private Throwable lastException;
	
	public ExceptionTransition() {
		super(Constants.DEFAULT_EXCEPTION, null, null);
	}
	
	public ExceptionTransition(String conditionName, WorkflowBehaviour owner) {		
		super(Constants.EXCEPTION, conditionName, owner);
	}
	
	protected MethodInvocator createInvocator(WorkflowBehaviour owner, String methodName) {
		// Note that this method is invoked in the constructor and at that time lastException does not have a value yet
		// --> We cannot pass it to the MethodInvocator constructor
		return new MethodInvocator(owner, methodName) {
			protected Object[] getMethodParams() {
				return new Object[]{lastException};
			}
			
			protected Class[] getMethodParamTypes() {
				return new Class[]{Throwable.class};
			}		
		};
	}
	
	void setException(Throwable t) {
		lastException = t;
	}
}
