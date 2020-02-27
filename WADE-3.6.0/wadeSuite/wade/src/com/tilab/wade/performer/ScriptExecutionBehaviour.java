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

import jade.core.Agent.Interrupted;

//#MIDP_EXCLUDE_FILE

/**
 * The behaviour implementing activities of type SCIRPT in a workflow. 
 */
public class ScriptExecutionBehaviour extends ActivityBehaviour {
	
	private MethodInvocator invocator;
	private Script script;

	public ScriptExecutionBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	/**
	 * Create a ScriptExecutionBehaviour specifying whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method (being Nnn the
	 * name of the ScriptExecutionBehaviour activity) or directly by means of performScript() method 
	 * of the WorkflowBehaviour class. This second case occurs when the workflow
	 * does not have a class associated to it, but is built instructing a WorkflowBehaviour
	 * object on the fly. 
	 * @param name The name of the activity
	 * @param owner The workflow this activity belongs to
	 * @param callExecuteMethod Whether the activity 
	 * must be performed by means of an ad-hoc "executeNnn()" method 
	 */
	public ScriptExecutionBehaviour(String name, WorkflowBehaviour owner, boolean callExecuteMethod) {
		super(name, owner);
		requireSave = true;
		
		script = new Script(owner, this);
		
		String methodName;
		Class paramClass;
		if (callExecuteMethod) {
			methodName = EngineHelper.activityName2Method(getBehaviourName());
			paramClass = Script.class;
		} else {
			methodName = "performBuildingBlock";
			paramClass = InvocableBuildingBlock.class;
		}
		
		EngineHelper.checkMethodName(methodName, "activity", name);
		invocator = new MethodInvocator(owner, methodName, script, paramClass);
	}

	public void setScript(String script) {
		this.script.setScript(script);
	}

	public void setScriptType(Script.ScriptType scriptType) {
		this.script.setScriptType(scriptType);
	}
	
	public void importClass(String className) {
		script.importClass(className);
	}

	public void setClassLoader(ClassLoader cl) {
		script.setClassLoader(cl);
	}
	
	public void defineVariable(String name, String type) {
		script.defineVariable(name, type);
	}
	
	public void action() {
		try {
			owner.enterInterruptableSection();
			invocator.invoke();
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
	
	public void reset() {
		super.reset();

		// Reset specific building block
		script.reset();
	}
	
	public BuildingBlock getBuildingBlock(String id) {
		return script;
	}
	
}
