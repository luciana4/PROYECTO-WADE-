package com.tilab.wade.tools.launcher.test;

import com.tilab.wade.performer.layout.ActivityLayout;
import com.tilab.wade.performer.layout.WorkflowLayout;
import com.tilab.wade.performer.CodeExecutionBehaviour;
import com.tilab.wade.performer.WorkflowBehaviour;

@WorkflowLayout(activities={@ActivityLayout(label = "HelloWord", position="(453,169)", name="HelloWordActivity")})
public class HelloWord extends WorkflowBehaviour {

	public static final String HELLOWORDACTIVITY_ACTIVITY = "HelloWordActivity";

	private void defineActivities() {
		CodeExecutionBehaviour helloWordActivityActivity = new CodeExecutionBehaviour(HELLOWORDACTIVITY_ACTIVITY, this);
		registerActivity(helloWordActivityActivity, INITIAL_AND_FINAL);
	}

	protected void executeHelloWordActivity() throws Exception {
		System.out.println("Hello Word!");
	}

}
