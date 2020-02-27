package toysassembler.searcher;

import com.tilab.wade.commons.AgentInitializationException;
import com.tilab.wade.commons.AgentType;
import com.tilab.wade.performer.WorkflowEngineAgent;

public class SearcherAgent extends WorkflowEngineAgent {
	private int viewSize = 100;
	private int speed = 5;

	/**
	 * Agent initialization
	 */
	public void agentSpecificSetup() throws AgentInitializationException {
		super.agentSpecificSetup();
		
		// A SearcherAgent can search for 1 component set at a time 
		// --> Set the pool-size to 1 so that we cannot execute workflows 
		// in parallel
		setPoolSize(1);
	}
	
	/**
	 * Return the type of this agent. This will be 
	 * inserted in the default DF description
	 */
	public AgentType getType() {
		AgentType type = new AgentType();
		type.setDescription("Searcher-Agent");
		return type;
	}
	
	public int getViewSize() {
		return viewSize;
	}
	public void setViewSize(int viewSize) {
		this.viewSize = viewSize;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}	
}
