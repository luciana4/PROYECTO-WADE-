package com.tilab.wade.cfa.beans;

import java.util.Collection;
import java.util.SortedMap;

import com.tilab.wade.commons.WadeAgent;

/**
 * This bean maps the '<virtualAgent>' tag that can be inserted in an application configuration file
 * as exemplified below<br>
 * <code>
 * <container name="C1">
 * 		<agents>
 * 			<virtualAgent name="va" type="My Type" replicationType="HOT" numberOfReplicas="2(C2,C3)"/>
 * 			...
 * 		</agents>
 * </container>
 * </code>
 * The above snippet means that the master replica will be started in container C1 and there will be
 * 2 additional replicas in container C2 and C3 respectively.<br>
 * It should be noticed that the virtual replicated agents mechanism requires the AgentReplicationService
 * to be active in all containers (including auxiliary containers) and the AgentMobilityService to be
 * active in the Main Container and in all containers where replicas must run.
 */
public class VirtualAgentInfo extends AgentInfo {

	private static final long serialVersionUID = -33857857284754L;

	private String numberOfReplicas;
	private String replicationType;

	public String getNumberOfReplicas() {
		return numberOfReplicas != null ? numberOfReplicas : "1";
	}

	public void setNumberOfReplicas(String numberOfReplicas) {
		this.numberOfReplicas = numberOfReplicas;
	}

	public String getReplicationType() {
		return replicationType != null ? replicationType : "HOT";
	}

	public void setReplicationType(String replicationType) {
		this.replicationType = replicationType;
	}

	@Override
	public SortedMap<String, String> getParametersForComparison() {
		SortedMap<String, String> result = super.getParametersForComparison();
		result.put("numberOfReplicas", getNumberOfReplicas());
		return result;
	}
	
	public AgentInfo asAgentInfo() {
		AgentInfo ai = new AgentInfo();
		ai.setName(getName()+"_R1"); // The agent name becomes that of the first replica
		ai.setClassName(getClassName());
		ai.setType(getType());
		ai.setOwner(getOwner());
		ai.setGroup(getGroup());
		Collection<AgentArgumentInfo> params = getParameters();
		params.add(new AgentArgumentInfo(WadeAgent.VIRTUAL_NAME, getName()));
		params.add(new AgentArgumentInfo(WadeAgent.NUMBER_OF_REPLICAS, getNumberOfReplicas()));
		params.add(new AgentArgumentInfo(WadeAgent.REPLICATION_TYPE, getReplicationType()));
		ai.setParameters(params);
		return ai;
	}
}
