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

import com.tilab.wade.performer.transaction.CodeEntry;
import com.tilab.wade.performer.transaction.TransactionManager;

import jade.core.Agent.Interrupted;

//#MIDP_EXCLUDE_FILE

/**
 * The behaviour implementing activities of type CODE in a workflow. 
 */
public class CodeExecutionBehaviour extends ActivityBehaviour {
	private MethodInvocator invocator;
	
	public CodeExecutionBehaviour(String name, WorkflowBehaviour owner) {
		super(name, owner);
		requireSave = true;
		String methodName = EngineHelper.activityName2Method(getBehaviourName());
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName);
	}
	
	public void action() {
		try {
			owner.enterInterruptableSection();
			invocator.invoke();
			
			TransactionManager tm = owner.getTransactionManager();
			if (tm != null) {
				CodeEntry myEntry = new CodeEntry(generateCodeEntryId(), owner, getBehaviourName());
				tm.addEntry(myEntry);
			}
		}
		catch (InterruptedException ie) {
		}
		catch (Interrupted i) {
		}
		catch (ThreadDeath td) {
		}
		catch (Throwable t) {
			handleException(t);
			if (!EngineHelper.logIfUncaughtOnly(this, t)) {
				t.printStackTrace();
			}
		}
		finally {
			owner.exitInterruptableSection(this);
		}
	}
	
	// Generates a unique id that identifies a code entry.
	private static long codeEntryCnt = 0;
	private synchronized static String generateCodeEntryId() {
		String id = "Code_"+codeEntryCnt;
		codeEntryCnt++;
		return id;
	}	
}
