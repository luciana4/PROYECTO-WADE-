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
package com.tilab.wade.performer.interactivity;

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tilab.wade.performer.BuildingBlock;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.utils.OntologyUtils;


public class OutputInteraction extends BuildingBlock {

	private Map<String, Object> params = new HashMap<String, Object>();
	private String message;
	private WorkflowBehaviour owner;
	
	public OutputInteraction(WorkflowBehaviour owner, OutputActivityBehaviour activity) {
		super(activity);
		
		this.owner = owner;
	}
	
	public void reset() {
	}

	public Interaction getOutputInteraction() throws OntologyException {
		InteractiveWorkflowBehaviour iwb = (InteractiveWorkflowBehaviour)owner;
		
		Interaction interaction = new Interaction(iwb.getInteractionTitle(this));

		// Prepare main panel
		ListPanel mainPanel = new ListPanel();
		interaction.setMainPanel(mainPanel);
		
		// Add message
		mainPanel.addComponent(new Label(message));
		
		// Prepare parameters panel
		ListPanel paramPanel = new ListPanel();
		mainPanel.addComponent(paramPanel);
		
		for (String paramName : params.keySet()) {
			Object paramValue = params.get(paramName);
			
			// The convertParameter method ensure that the value is an AbsObject
			AbsObject paramAbsValue = null;
			if (paramValue != null && paramValue instanceof AbsObject) {
				paramAbsValue = (AbsObject)paramValue;
			} else {
				paramAbsValue = AbsPrimitive.wrap(((InteractiveWorkflowBehaviour)owner).getUnknownValueLabel());
			}
			
			StructuredDataElement paramElement = new StructuredDataElement(paramName);
			paramElement.setEditable(false);
			paramElement.setLabel(paramName);
			paramElement.setValue(paramAbsValue);

			// Add additional infos to data-element
			((InteractiveWorkflowBehaviour)owner).manageAdditionalInfoConstraint(paramElement, getActivity().getBehaviourName(), paramName, Constants.IN_MODE);
			
			paramPanel.addComponent(paramElement);
		}
		
		// Prepare actions
		Action ok = new Action(iwb.getOkButtonLabel());
		interaction.addAction(ok);
		
		return interaction;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public AbsObject createAbsTemplate(String key) throws Exception {
		AbsObject absValue = null;
		Object value = params.get(key);
		if (value != null) {
			absValue = AbsHelper.createAbsTemplate(value.getClass(), getOntology());
		}
		return absValue;
	}

	@Override
	protected Ontology createOntology() throws Exception {
		BeanOntology onto = new BeanOntology("OutputActivityOnto");
		for (Object value : params.values()) {
			OntologyUtils.addObjectToOntology(onto, value);
		}
		return onto;
	}

	@Override
	public Object getInput(String key) {
		return params.get(key);
	}

	@Override
	public List<String> getInputParameterNames() {
		List<String> inputParameterNames = new ArrayList<String>();
		inputParameterNames.addAll(params.keySet());
		return inputParameterNames;
	}

	@Override
	public Object getOutput(String key) {
		// OutputActivity not have output parameters
		return null;
	}

	@Override
	public List<String> getOutputParameterNames() {
		// OutputActivity not have output parameters
		return new ArrayList<String>();
	}

	@Override
	public boolean isInputEmpty(String key) {
		return params.containsKey(key);
	}

	@Override
	public boolean requireAbsParameters() {
		return true;
	}

	@Override
	public void setInput(String key, Object value) {
		params.put(key, value);
	}

	@Override
	public void setOutput(String key, Object value) {
		// OutputActivity not have output parameters
	}

	@Override
	public Parameter getInputParameter(String key) {
		if (!params.containsKey(key)) {
			return null;
		}
		Parameter p = new Parameter(key, null, Constants.IN_MODE);
		return p;
	}

	@Override
	public Parameter getOutputParameter(String key) {
		// OutputActivity not have output parameters
		return null;
	}
}
