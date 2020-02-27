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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import com.tilab.wade.ca.CAServices;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.eval.Formula;
import com.tilab.wade.performer.eval.Reference;
import com.tilab.wade.utils.OntologyUtils;

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.util.Logger;
import jade.util.leap.Iterator;

public class BindingManager extends BindingHolder {

	protected Logger myLogger = Logger.getMyLogger(BindingManager.class.getName());
	
	private HierarchyNode activity;

	
	public BindingManager(HierarchyNode activity) {
		this.activity = activity;
	}
	
	private String getBuildingBlockId(String key) {
		// Key is of type param[@bbId] where:
		// - 'param' is the name of parameter
		// - 'bbId' is the id of building block (optional)    
		String bbId = null;
		int sepPos = key.indexOf(Constants.BB_ID_SEPARATOR);
		if (sepPos >= 0) {
			bbId = key.substring(sepPos+1);
		}
		return bbId;
	}
	
	private BuildingBlock getBuildingBlock(String bbId) throws BindingException {
		BuildingBlock bb = activity.getBuildingBlock(bbId);
		if (bb == null) {
			throw new BindingException("Building block "+bbId+" not present in activity "+activity.getBehaviourName());
		}
		return bb;
	}

	private String getParamName(String key) {
		// Key is of type param[@bbId] where:
		// - 'param' is the name of parameter
		// - 'bbId' is the id of building block (optional)    
		String paramName = key;
		int sepPos = key.indexOf(Constants.BB_ID_SEPARATOR);
		if (sepPos >= 0) {
			paramName = key.substring(0, sepPos);
		}
		return paramName;
	}
	
	private Object getValue(BuildingBlock bb, String paramName, int mode) throws Exception {
		// Get parameter value
		Object value;
		if (mode == Constants.IN_MODE) {
			value = bb.getInput(paramName);
		} else {
			value = bb.getOutput(paramName);
		}
		return value;
	}
	
	public Object getValue(String key, int mode, String part) throws Exception {
		// Get BuildingBlock
		BuildingBlock bb = getBuildingBlock(getBuildingBlockId(key));
		
		// Get value
		Object value = getValue(bb, getParamName(key), mode);		

		// Get part of parameter
		return getPartValue(value, part, bb);
	}

	public static Object getPartValue(Object value, String part, OntologyHolder owner) throws Exception {
		if (part != null) {
		
			// Convert in abs
			AbsObject absValue = owner.getOntology().fromObject(value);
			
			// Extract part
			absValue = getAttribute(absValue, part);
			
			// Convert in obj
			value = owner.getOntology().toObject(absValue);
		}
		return value;
	}
	
	public AbsObject getAbsValue(String key, int mode, String part) throws Exception {
		// Get BuildingBlock
		BuildingBlock bb = getBuildingBlock(getBuildingBlockId(key));
		
		// Get value
		Object value = getValue(bb, getParamName(key), mode);		

		// Get abs part of parameter
		return getAbsPartValue(value, part, bb);
	}
	
	
	public static AbsObject getAbsPartValue(Object value, String part, OntologyHolder owner) throws Exception {
		AbsObject absValue;
		if (value instanceof AbsObject) {
			absValue = (AbsObject)value;
		} else {
			absValue = owner.getOntology().fromObject(value);
		}
		
		// Get part of parameter
		if (part != null && absValue != null) {
			absValue = getAttribute(absValue, part);
		}
		
		return absValue;
	}
	
	public void convertParameters(BuildingBlock bb) throws Exception {

		for (String parameterName : bb.getInputParameterNames()) {
			Object paramValue = bb.getInput(parameterName);
			if (paramValue != null) {
				if (bb.requireAbsParameters() && !(paramValue instanceof AbsObject)) {
					// Convert Obj in Abs
					AbsObject abs = bb.getOntology().fromObject(paramValue);
					bb.setInput(parameterName, abs);
				} 
				else if (!bb.requireAbsParameters() && paramValue instanceof AbsObject) {
					// Convert Abs in Obj
					Object obj = bb.getOntology().toObject((AbsObject)paramValue);
					bb.setInput(parameterName, obj);
				}
			}
		}
	}

	void resolveBindings(BuildingBlock bb) throws Exception {
		
		// If there are no bindings avoid doing extra effort 
		if (!getBindings().isEmpty()) {
			for (String parameterName : bb.getInputParameterNames()) {
				if (bb.isInputEmpty(parameterName)) {
					// Parameter not filled -> check bindings
					
					List<Bind> bindings = getBindings(parameterName);
					if (bindings.size() > 0) {
						AbsObject parameterAbs = null;
						Object parameterValue = null;
						
						for (Bind bind : bindings) {
							String part = bind.getPart();
							if ("".equals(part)) {
								part = null;
							}
							Formula formula = bind.getFormula();
							
							if (part == null && formula == null) {
								// Fictitious bindings
								parameterValue = bb.createAbsTemplate(parameterName);
								myLogger.log(Logger.INFO, "Resolve fictitious binding of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName);
								break;
								
							}
							else if (part == null && formula != null) {
								// Total binding
								if (bb.requireAbsParameters() && formula instanceof Reference) {
									parameterValue = ((Reference)formula).evaluateAbs();
								} else {
									parameterValue = formula.evaluate();
								}
								myLogger.log(Logger.INFO, "Resolve formula binding of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName+" = "+formula);
								break;
								
							} 
							else if (part != null && formula != null) {
								// Partial binding
								if (parameterAbs == null) {
									parameterAbs = bb.createAbsTemplate(parameterName);
									parameterValue = parameterAbs; 
								}
		
								AbsObject attrValue;
								if (formula instanceof Reference) {
									attrValue = ((Reference)formula).evaluateAbs();
								} else {
									Object attrObj = formula.evaluate();
									attrValue = bb.getOntology().fromObject(attrObj);
								}
								
								setAttribute(parameterAbs, part, attrValue);
								myLogger.log(Logger.INFO, "Resolve partial formula binding of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName+"."+part+" = "+formula);
								
							}
							else {
								// Not permitted
								throw new BindingException("Wrong bind of activity "+bb.getActivity().getBehaviourName()+", parameter "+parameterName+"."+part+" with NULL formula");
							}
						}
						
						// Fill parameter
						bb.setInput(parameterName, parameterValue);
					}
				}
			}
		}
	}

	/**
	 * Output bindings declare where (in which fields or parts of a field of the owner workflow) to store 
	 * information extracted from the just executed building block. Therefore output bindings are bindings 
	 * defined on the owner workflow in which the references included in the binding formula point to
	 * parameters of the just executed building block. As a consequence this method is always called
	 * on the owner workflow bindingManager.
	 */
	void resolveOutputBindings(BuildingBlock bb) throws Exception {
		// If there are no bindings avoid doing extra effort 
		if (!getBindings().isEmpty()) {
			WorkflowBehaviour wb = bb.getActivity().getOwner();
			// Loop on bindings of the owner workflow that reference parameters of building block bb.
			// Such bindings are organized as a map as that below
			// owner field1 --> {bindings relevant for field1}
			// owner field2 --> {bindings relevant for field2}
			// Remember that due to partial bindings, more that one bindings can be set on the same field.
			String activityName = bb.getActivity().getBehaviourName();
			Map<String, List<Bind>> bbBinds = getRelevantBindings(activityName);
			if (bbBinds != null) {
				for (Entry<String, List<Bind>> entry : bbBinds.entrySet()) {
					String fieldName = entry.getKey();
					AbsObject fieldAbs = null;
					Object fieldValue = null;
					
					List<Bind> bindings = entry.getValue();
					for (Bind bind : bindings) {
						String part = bind.getPart();
						if ("".equals(part)) {
							part = null;
						}
						Formula formula = bind.getFormula();
						
						if (part == null && formula != null) {
							fieldValue = formula.evaluate();
							myLogger.log(Logger.INFO, "Resolve formula binding of activity "+bb.getActivity().getBehaviourName()+", wf field "+fieldName+" = "+formula);
						} 
						else if (part != null && formula != null) {
							// Partial binding
							if (fieldAbs == null) {
								FieldInfo fi = wb.getFieldType(fieldName);
								if (fi == null) {
									throw new BindingException("Partial bind of activity "+bb.getActivity().getBehaviourName()+", wf field "+fieldName+"."+part+" not possible using data-store");
								}
	
								// Dynamically add fields type to workflow ontology
								BeanOntology onto = (BeanOntology)wb.getOntology();
								if (onto.getSchema(fi.getTypeClass()) == null) {
									OntologyUtils.addClassToOntology(onto, fi.getTypeClass());
								}
								if (fi.getElementTypeClass() != null && onto.getSchema(fi.getElementTypeClass()) == null) {
									OntologyUtils.addClassToOntology(onto, fi.getElementTypeClass());
								}
								
								fieldAbs = AbsHelper.createAbsTemplate(fi.getTypeClass(), onto);
								fieldValue = fieldAbs; 
							}
	
							AbsObject attrValue = ((Reference)formula).evaluateAbs();
							
							setAttribute(fieldAbs, part, attrValue);
							myLogger.log(Logger.INFO, "Resolve partial formula binding of activity "+bb.getActivity().getBehaviourName()+", wf field "+fieldName+"."+part+" = "+formula);
						}
						else {
							// Not permitted
							throw new BindingException("Wrong bind of activity "+bb.getActivity().getBehaviourName()+", wf field "+fieldName+"."+part+" with NULL formula");
						}
					}
					
					if (fieldValue instanceof AbsObject) {
						// Note that we get here only in case that fieldValue has been created by means of partial bindings
						// --> type information for this field has already been added to the ontology 
						fieldValue = wb.getOntology().toObject((AbsObject)fieldValue);
					}
					
					wb.setFieldValue(fieldName, fieldValue);
				}
			}
		}
	}
	
	static AbsObject getAttribute(AbsObject abs, String part) throws BindingException {
		if (abs == null) {
			return null;
		}

		if (part == null) {
			throw new BindingException("Part must be not null");
		}

		AbsObject internalAbs = abs;
		StringTokenizer st = new StringTokenizer(part, Constants.BB_PART_SEPARATOR);
		while (st.hasMoreTokens()) {
			String slotName = st.nextToken();
			internalAbs = internalAbs.getAbsObject(slotName);
			if (internalAbs == null) {
				break;
			}
		}
		return internalAbs;
	}
	
	static void setAttribute(AbsObject abs, String part, AbsObject partValue) throws BindingException {
		if (part == null) {
			throw new BindingException("Part must be not null");
		}

		String attrName;
		int lastSeparatorPos = part.lastIndexOf(Constants.BB_PART_SEPARATOR);
		if (lastSeparatorPos == -1) {
			attrName = part;
			part = "";
		} else {
			attrName = part.substring(lastSeparatorPos+1);
			part = part.substring(0, lastSeparatorPos);
		}

		AbsObject internalAbs = getAttribute(abs, part);
		if (internalAbs != null) {
			try {
				AbsHelper.setAttribute(internalAbs, attrName, partValue);
			} catch (OntologyException e) {
				throw new BindingException("Wrong part "+part+" for schema "+abs.getAbsType());
			}
		} else {
			throw new BindingException("Part "+part+"not present in abs object "+abs);
		}
	}
	
}
