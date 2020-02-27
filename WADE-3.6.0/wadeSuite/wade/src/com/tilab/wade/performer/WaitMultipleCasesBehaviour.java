package com.tilab.wade.performer;

import jade.content.ContentElement;
import jade.content.ContentElementList;
import jade.domain.FIPAAgentManagement.Property;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.Occurred;

/**
 * The behaviour representing a workflow "activity" whose execution corresponds to suspending   
 * until one out of a set of possible events happens.<br>
 * Each possible event corresponds to a Case that holds the information necessary to identify 
 * the event. There are different types of Cases for the different categories of possible 
 * events (custom, web-service...). Each Case has an ID that is used in the afterXXX() method
 * to determine which event has actually happened.<br>     
 * This activity holds different building blocks corresponding to the Event Template and Event
 * of each Case. These building blocks can be 
 * retrieved by means of the <code>getBuildingBlock(String id)</code> method passing the
 * "<case-id>#EVENT_TEMPLATE" and "<case-id>#EVENT" ids respectively.
 */
public class WaitMultipleCasesBehaviour extends AbstractWaitEventBehaviour {
	private static final long serialVersionUID = 57235736517L;

	public static final String BB_SEPARTOR = "#";
	
	private Map<String, EventCase> cases = new HashMap<String, EventCase>();
	private String occurredCaseId = null;

	public WaitMultipleCasesBehaviour(String name, WorkflowBehaviour owner) {
		this(name, owner, true);
	}

	public WaitMultipleCasesBehaviour(String name, WorkflowBehaviour owner, boolean hasDedicatedMethods) {
		super(name, owner, hasDedicatedMethods);
	}
	
	public void addCase(String caseId, EventCase ec) {
		ec.setId(caseId);
		ec.setActivity(this);
		cases.put(caseId, ec);
	}
	
	@Override
	public void init() throws Exception {
		Iterator<EventCase> it = cases.values().iterator();
		while(it.hasNext()) {
			EventCase ec = it.next();
			ec.init(myAgent);
		}
		
		super.init();
	}

	/**
	 * Create a MethodInvocator suitable to call the beforeXXX() method with a 
	 * Map parameter mapping each case-id to the EventTemplate identifying the event 
	 * to be received in that case. 
	 */
	@Override
	protected MethodInvocator createBeforeMethodInvocator(String beforeMethodName) {
		// At this time the templates map has not been initialized yet
		return new MethodInvocator(owner, beforeMethodName, null, Map.class) {
			private static final long serialVersionUID = 486345624L;
			
			@Override
			protected Object[] getMethodParams() {
				// Invoke the beforeXXX() method with a Map<caseId --> template> parameter
				Map<String, EventTemplate> templateMap = new HashMap<String, EventTemplate>(cases.size());
				for (String caseId : cases.keySet()) {
					templateMap.put(caseId, cases.get(caseId).getEventTemplate());
				}
				return new Object[]{templateMap};
			}
		};
	}

	/**
	 * Create a MethodInvocator suitable to call the afterXXX() method with the 
	 * occurred case ID and the received GenericEvent as parameters
	 * or null, null if the timeout expired
	 */
	@Override
	protected MethodInvocator createAfterMethodInvocator(String afterMethodName) {
		// At this time the occurred object (and therefore the event) has not been initialized yet
		return new MethodInvocator(owner, afterMethodName) {
			private static final long serialVersionUID = 98795874L;
			
			@Override
			protected Object[] getMethodParams() {
				if (occurredCaseId != null) {
					return new Object[]{occurredCaseId, cases.get(occurredCaseId).getEvent()};
				}
				else {
					// Timeout expired
					return new Object[]{null, null};
				}
			}
			
			@Override
			protected Class[] getMethodParamTypes() {
				return new Class[]{String.class, GenericEvent.class};
			}
		};
	}
	
	@Override
	protected void manageBindings() throws Exception {
		for (String caseId : cases.keySet()) {
			owner.manageBindings(cases.get(caseId).getBuildingBlock(EventTemplateBB.ID));
		}
	}

	@Override
	protected void manageOutputBindings() throws Exception {
		for (String caseId : cases.keySet()) {
			owner.manageOutputBindings(cases.get(caseId).getBuildingBlock(EventTemplateBB.ID));
		}
	}
	
	/**
	 * Retrieve the BuildingBlock corresponding to the given id.
	 * BuildingBlock ids have the form <case-id>#<type> where type can be 
	 * either "EVENT_TEMPLATE" or "EVENT".  
	 */
	@Override
	public BuildingBlock getBuildingBlock(String id) {
		int sepPos = id.indexOf(BB_SEPARTOR);
		if (sepPos > 0 && sepPos < (id.length()-1)) {
			String caseId = id.substring(0, sepPos);
			String type = id.substring(sepPos+1);
			EventCase ec = cases.get(caseId);
			if (ec != null) {
				return ec.getBuildingBlock(type);
			}
		}
		return null;
	}
	
	public boolean checkCompleted() throws Exception {
		return occurredCaseId != null || timeoutExpired;
	}

	protected ContentElement prepareRegistrationContent() {
		// Create a ContentElementList including as many Match predicates as the number 
		// of possible cases
		ContentElementList cel = new ContentElementList();
		for (String caseId : cases.keySet()) {
			cel.add(cases.get(caseId).getMatch());
		}
		return cel;
	}

	protected void handleOccurredEvent(Occurred occurred) {
		GenericEvent event = (GenericEvent) occurred.getEvent();
		List<Property> properties = event.getProperties();
		occurredCaseId = (String) getProperty(properties, EventTemplate.TAG_PROPERTY);
		EventCase occurredCase = cases.get(occurredCaseId);
		occurredCase.setEvent(event);
	}
	
	@Override
	public void reset() {
		super.reset();
		occurredCaseId = null;
		for (String caseId : cases.keySet()) {
			cases.get(caseId).reset();
		}
	}
	
	String getOccurredCase() {
		return occurredCaseId;
	}

	private static Object getProperty(List<Property> pp, String name) {
		for (Property p : pp) {
			if (p.getName().equals(name)) {
				return p.getValue();
			}
		}
		return null;
	}
}
