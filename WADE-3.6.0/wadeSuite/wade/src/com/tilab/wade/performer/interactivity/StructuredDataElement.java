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

import java.util.ArrayList;
import java.util.List;
import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.descriptors.Parameter;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsConcept;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.abs.AbsTerm;
import jade.content.abs.AbsVariable;
import jade.content.onto.OntologyException;
import jade.content.schema.AggregateSchema;
import jade.content.schema.Facet;
import jade.content.schema.ObjectSchema;
import jade.content.schema.TermSchema;
import jade.content.schema.facets.CardinalityFacet;
import jade.content.schema.facets.DefaultValueFacet;
import jade.content.schema.facets.DocumentationFacet;
import jade.content.schema.facets.PermittedValuesFacet;
import jade.content.schema.facets.RegexFacet;

public class StructuredDataElement extends InformationElement {

	private static final long serialVersionUID = 2279945410133843228L;

	private AbsObject value;
	private boolean rootRemoved = false;
	private boolean editable = true;
	
	protected StructuredDataElement() {
	}
		
	/**
	 * Create an empty StructuredDataElement
	 * @param id element identifier
	 */
	public StructuredDataElement(String id) {
		super(id);
	}
	
	/**
	 * Create a StructuredDataElement with specific value
	 * @param id element identifier
	 * @param value abstract object
	 * @throws OntologyException
	 */
	public StructuredDataElement(String id, AbsObject value) throws OntologyException {
		this(id);
		
		this.value = value;
	}

	/**
	 * Create a StructuredDataElement with a template abstract value derived from schema.
	 * The node constraints are automatically setted.
	 * @param id element identifier
	 * @param schema value schema
	 * @throws OntologyException
	 */
	public StructuredDataElement(String id, ObjectSchema schema) throws OntologyException {
		this(id);
		
		// Create abs template
		value = AbsHelper.createAbsTemplate(schema);
		
		// Make abs constrained and apply schema facets as constrains
		value = getConstrainedValue(value, schema, true);
	}

	/**
	 * Create a StructuredDataElement with a template abstract value derived from formal parameter.
	 * The node constraints are automatically setted.
	 * The root constraints are automatically setted.
	 * @param id element identifier
	 * @param parameter formal parameter descriptor
	 * @throws OntologyException
	 */
	public StructuredDataElement(String id, Parameter parameter) throws OntologyException {
		this(id, parameter, null);
	}
	
	/**
	 * Create a StructuredDataElement with a template abstract value derived from formal parameter and set the actual values.
	 * The node constraints are automatically setted.
	 * The root constraints are automatically setted.
	 * @param id element identifier
	 * @param parameter formal parameter descriptor
	 * @param actualValue actual value that are converted in ActualValueConstraints
	 * @throws OntologyException
	 */	
	public StructuredDataElement(String id, Parameter parameter, AbsObject actualValue) throws OntologyException {
		this(id);

		ObjectSchema schema = parameter.getSchema();
		if (schema == null) {
			throw new OntologyException("Schema not definited in parameter "+parameter.getName());
		}
		
		// Create abs template
		value = AbsHelper.createAbsTemplate(schema);
		
		// Make abs constrained and apply schema facets as constrains
		value = getConstrainedValue(value, schema, true);
		
		// Apply constraints derived from parameter 
		addConstraints(parameter);
		
		// Apply actual value as ActualValueConstraint
		if (actualValue != null) {
			manageCachedValues(null, value, actualValue, false);
		}
	}
	
	/**
	 * Get editable flag
	 * If true only the display type constraints are managed
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Set the editable flag
	 * If true only the display type constraints are managed  
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	/**
	 * Get abstract value
	 * @return abstract value
	 */
	public AbsObject getValue() {
		return value;
	}

	/**
	 * Set abstract value and reset all 
	 */
	public void setValue(AbsObject value) {
		this.value = value;
		
		// Reset root constraints 
		constraints.clear();
	}
	
	/**
	 * Add the constraints to all nodes of abstract value.
	 * Remember to set the value before add constraints. 
	 * @param schema of value
	 * @throws OntologyException 
	 */
	public void addNodeConstraints(ObjectSchema schema) throws OntologyException {
		value = getConstrainedValue(value, schema, editable);
	}
	
	
	/**
	 * Add all root constraints derived from wade parameter.
	 * Remember to set the value before add constraints. 
	 * @param parameter parameterDescriptor wade parameter
	 */
	public void addConstraints(Parameter parameter) {
		// Reset previous constraints 
		constraints.clear();
		
		// Mandatory/Optionality
		boolean mandatory = parameter.getMandatory();
		if (editable && value != null) {
			if (value instanceof AbsVariable) {
				// Se il parametro è una foglia variabile aggiunge se necessario il MandatoryConstraint
				if (mandatory) {
					constraints.add(new MandatoryConstraint());
				}
			} else {
				// Se il parametro è un concept aggiunge se necessario il OptionalityConstraints
				if (!mandatory) {
					constraints.add(new OptionalityConstraint());
				}
			}
		}

		// Type
		if (value != null) {
			if (value instanceof AbsVariable) {
				AbsVariable varValue = (AbsVariable)value;
				constraints.add(new TypeConstraint(varValue.getType()));
			} 
			else if (value instanceof AbsAggregate) {
				// Nel caso di aggregato come root deve verificare se il template dell'element
				// è AbsVariable in tal caso imposta il relativo TypeConstraint
				AbsAggregate aggValue = (AbsAggregate)value;
				AbsTerm aggElementValue = aggValue.getElementTemplate();
				if (aggElementValue instanceof AbsVariable) {
					constraints.add(new TypeConstraint(((AbsVariable)aggElementValue).getType()));
				}
			}
		}

		// Default value
		Object defaultValue = parameter.getDefaultValue();
		if (editable && defaultValue != null) {
			constraints.add(new DefaultConstraint(defaultValue));
		}
		
		// Regex
		String regex = parameter.getRegex();
		if (editable && regex != null) {
			constraints.add(new RegexConstraint(regex));
		}

		// Documentation
		String documentation = parameter.getDocumentation();
		if (documentation != null) {
			constraints.add(new DocumentationConstraint(documentation));
		}
		
		// Permitted values
		Object[] permittedValues = parameter.getPermittedValues();
		if (editable && permittedValues != null && permittedValues.length > 0) {
			constraints.add(new PermittedValuesConstraint(permittedValues));
		}
		
		// Cardinality
		Integer cardMin = parameter.getCardMin();
		Integer cardMax = parameter.getCardMax();
		if (editable && cardMin != null && cardMax != null) {
			constraints.add(new CardinalityConstraint(cardMin.intValue(), cardMax.intValue()));
		}
	}
	
	/**
	 * Mark if the root element must be removed
	 * Change take effect only if component is editable
	 * @param remove
	 */
	public void markToBeRemove(boolean remove) {
		if (editable) {
			rootRemoved = remove;
		}
	}

	/**
	 * Mark if the element must be removed,
	 * the slot is expressed as part (eg. person.address.city)
	 * If part is null add constraint to root element
	 */
	public void markToBeRemove(String part, boolean remove) throws OntologyException {
		
		// If part is not specified add constraint to root
		if (part == null || "".equals(part)) {
			markToBeRemove(remove);
		}
		
		// If part is specified add constraint to proper slot (only if value is present) 
		else {
			if (value != null) {
				AbsObject internalAbs = convertIntoConstrainedAbsObject(part);
				if (internalAbs instanceof ConstrainedAbsConcept) {
					// Set constraint
					ConstrainedAbsConcept cac = (ConstrainedAbsConcept)internalAbs;
					cac.markToBeRemove(getSlot(part), remove);
				}
			}
		}
	}
	
	/**
	 * Return true if value is mark to-be-remove 
	 */
	public boolean isToBeRemove() {
		return rootRemoved;
	}
	
	@Override
	public void stamp() {
		if (editable) {
			if (rootRemoved) {
				value = null;
			} else {
				stamp(value);
			}
		}
	}
	
	private void stamp(AbsObject value) {
		// Remove all nodes marked as toBeRemoved

		if (value == null || value instanceof AbsVariable) {
			return;
		}
		
		String[] slotNames = value.getNames();
		if (slotNames == null) {
			return;
		}

		ConstrainedAbsConcept cac = null;
		if (value instanceof ConstrainedAbsConcept) {
			cac = (ConstrainedAbsConcept)value;
		}
		
		for (String slotName : value.getNames()) {
			if (cac != null && cac.isToBeRemove(slotName)) {
				cac.set(slotName, (AbsTerm)null);
			} else {
				AbsObject slotValue = value.getAbsObject(slotName);
				stamp(slotValue);
			}
		}
	}
	
	@Override
	public void doValidate() throws ConstraintException {

		// Check if root node is removed
		AbsObject valueToValidate = value;
		if (rootRemoved) {
			valueToValidate = null;
		}

		// Check info-element constraints (root level)
		validateConstraints(valueToValidate);
			
		// Check nodes constraints (only nodes not removed)
		validate(valueToValidate);
	}

	private void validate(AbsObject value) throws ConstraintException {
		if (value == null || value instanceof AbsVariable) {
			return;
		}
		
		String[] slotNames = value.getNames();
		if (slotNames == null) {
			return;
		}

		ConstrainedAbsConcept cac = null;
		if (value instanceof ConstrainedAbsConcept) {
			cac = (ConstrainedAbsConcept)value;
		}
		
		for (String slotName : value.getNames()) {
			AbsObject slotValue = value.getAbsObject(slotName);
			boolean slotRemoved = false;
			
			if (cac != null) {
				slotRemoved = cac.isToBeRemove(slotName);
				if (!slotRemoved) {
					List<Constraint> nodeConstraints = cac.getConstraints(slotName);
					if (nodeConstraints != null) {
						for (Constraint nodeConstraint : nodeConstraints) {
							nodeConstraint.validate(slotValue);
						}
					}
				}
			} 
			
			if (!slotRemoved) {
				validate(slotValue);
			}
		}
	}
	
	private static AbsObject getConstrainedValue(AbsObject value, ObjectSchema schema, boolean editable) throws OntologyException {
		// 1 per i nodi di tipo AbsAggregate elabora l'elementTemplate (che contiene info sul tipo di dato)
		// 2) rimpiazza tutti i nodi di tipo AbsConcept in ConstrainedAbsConcept
		// 3) per tutti i nodi ConstrainedAbsConcept aggiunge i constraints ricavati dallo schema ontologico
		//  3.1) - se slot è AbsVariable -> constraints di: Type, Mandatory, DefaultValue, PermittedValues
		//  3.2) - altriment ->: constraints di: Optionality, Cardinality (solo se AbsAggregate)
		
		AbsObject constrainedValue = value;
		if (value != null) {
			if (value instanceof AbsAggregate) {
				AbsAggregate aggValue = (AbsAggregate)value;
				AbsTerm aggElementValue = aggValue.getElementTemplate();
				AggregateSchema aggSchema = (AggregateSchema)schema;
				TermSchema aggElementSchema = aggSchema.getElementsSchema();
				if (aggElementValue != null && aggElementSchema != null) {
					aggValue.setElementTemplate((AbsTerm)getConstrainedValue(aggElementValue, aggElementSchema, editable));
				}
			}
			else if (value instanceof AbsConcept) {
				String typeName = value.getTypeName();
				constrainedValue = new ConstrainedAbsConcept(typeName);
				String[] slotNames = value.getNames();
				if (slotNames != null) {
					for (String slotName : value.getNames()) {
						// Recursively get constrained value
						AbsObject slotValue = value.getAbsObject(slotName);
						ObjectSchema slotSchema = schema.getSchema(slotName);
						AbsObject slotConstrainedValue = getConstrainedValue(slotValue, slotSchema, editable);
						boolean slotMandatory = schema.isMandatory(slotName);
						
						if (slotValue instanceof AbsVariable) {
							// Set constraints on variable

							// TypeConstraint
							String slotTypeName = schema.getSchema(slotName).getTypeName();
							addSlotConstraint(constrainedValue, slotName, new TypeConstraint(slotTypeName));

							// MandatoryConstraint
							if (editable && slotMandatory) {
								addSlotConstraint(constrainedValue, slotName, new MandatoryConstraint());
							}

							// DefaultConstraint
							DefaultValueFacet defaultValueFacet = (DefaultValueFacet)getFacet(schema, slotName, DefaultValueFacet.class);
							if (editable && defaultValueFacet != null) {
								addSlotConstraint(constrainedValue, slotName, new DefaultConstraint(defaultValueFacet.getDefaultValue()));
							}
							
							// RegexConstraint
							RegexFacet regexFacet = (RegexFacet)getFacet(schema, slotName, RegexFacet.class);
							if (editable && regexFacet != null) {
								addSlotConstraint(constrainedValue, slotName, new RegexConstraint(regexFacet.getRegex()));
							}

							// DocumentationConstraint
							DocumentationFacet documentationFacet = (DocumentationFacet)getFacet(schema, slotName, DocumentationFacet.class);
							if (documentationFacet != null) {
								addSlotConstraint(constrainedValue, slotName, new DocumentationConstraint(documentationFacet.getDocumentation()));
							}
							
							// PermittedValuesConstraint
							PermittedValuesFacet permittedValuesFacet = (PermittedValuesFacet)getFacet(schema, slotName, PermittedValuesFacet.class);
							if (editable && permittedValuesFacet != null) {
								addSlotConstraint(constrainedValue, slotName, new PermittedValuesConstraint(permittedValuesFacet.getPermittedValues()));
							}
							
						} else {
							// Set constraints on node
							
							// OptionalityConstraint
							if (editable && !slotMandatory) {
								addSlotConstraint(constrainedValue, slotName, new OptionalityConstraint());
							}
							
							// CardinalityConstraint / TypeConstraint (only for primitive aggregate elements)
							if (slotValue instanceof AbsAggregate) {
								CardinalityFacet cardinalityFacet = (CardinalityFacet)getFacet(schema, slotName, CardinalityFacet.class);
								if (editable && cardinalityFacet != null) {
									addSlotConstraint(constrainedValue, slotName, new CardinalityConstraint(cardinalityFacet.getCardMin(), cardinalityFacet.getCardMax()));
								}
								
								AbsAggregate aggValue = (AbsAggregate)slotValue;
								AbsTerm aggElementValue = aggValue.getElementTemplate();
								if (aggElementValue instanceof AbsVariable) {
									addSlotConstraint(constrainedValue, slotName, new TypeConstraint(((AbsVariable) aggElementValue).getType()));
								}
							}
						}
						
						// Set value in the slot
						((ConstrainedAbsConcept)constrainedValue).set(slotName, slotConstrainedValue);
					}
				}
			}
		}

		return constrainedValue;
	}

	private static void addSlotConstraint(AbsObject constrainedValue, String slotName, Constraint constraint) {
		((ConstrainedAbsConcept)constrainedValue).addConstraint(slotName, constraint);
	}
	
	/**
	 * Add the constraint to a specific slot value,
	 * the slot is expressed as part (eg. person.address.city)
	 * If part is null add constraint to root element
	 *   
	 * @param constraint to add
	 * @param part slot part
	 */
	public void addConstraint(Constraint constraint, String part) throws OntologyException {
		addConstraint(constraint, part, false);
	}
	
	public void addConstraint(Constraint constraint, String part, boolean override) throws OntologyException {
		
		// If part is not specified add constraint to root
		if (part == null || "".equals(part)) {
			addConstraint(constraint, override);
		}
		
		// If part is specified add constraint to proper slot (only if value is present) 
		else {
			if (value != null) {
				AbsObject internalAbs = convertIntoConstrainedAbsObject(part);
				if (internalAbs instanceof ConstrainedAbsConcept) {
					// Set constraint
					ConstrainedAbsConcept cac = (ConstrainedAbsConcept)internalAbs;
					cac.addConstraint(getSlot(part), constraint, override);
				}
			}
		}
	}
	
	private String getSlot(String part) {
		String[] slots = splitPart(part);
		return slots[slots.length-1];
	}
	
	private String[] splitPart(String part) {
		part = part.replaceAll("_JADE.UNNAMED", "_JADE-UNNAMED");
		String[] slots = part.split("\\.");
		for (int i=0; i<slots.length; i++) {
			slots[i] = slots[i].replaceAll("_JADE-UNNAMED", "_JADE.UNNAMED");
		}
		return slots;
	}
	
	private AbsObject convertIntoConstrainedAbsObject(String part) throws OntologyException {
		String[] slots = splitPart(part);
		
		AbsObject internalSuperAbs = null;
		String superSlotName = null;
		AbsObject internalAbs = value;
		for (int i=0; i<(slots.length-1); i++) {
			internalSuperAbs = internalAbs;
			superSlotName = slots[i];
			internalAbs = internalAbs.getAbsObject(slots[i]);
			if (internalAbs == null) {
				// slot defined in part is not present
				throw new OntologyException("Slot "+slots[i]+" is not present in abs value "+value);
			}
		}

		if (!(internalAbs instanceof ConstrainedAbsConcept) && internalAbs instanceof AbsConcept) {
			// Convert AbsConcept into ConstrainedAbsConcept
			String typeName = internalAbs.getTypeName();
			ConstrainedAbsConcept constrainedInternalAbs = new ConstrainedAbsConcept(typeName);
			String[] slotNames = internalAbs.getNames();
			if (slotNames != null) {
				for (String slotName : internalAbs.getNames()) {
					constrainedInternalAbs.set(slotName, internalAbs.getAbsObject(slotName));
				}
			}
			internalAbs = constrainedInternalAbs;
			if (internalSuperAbs != null) {
				((AbsConcept)internalSuperAbs).set(superSlotName, constrainedInternalAbs);
			} else {
				value = constrainedInternalAbs;
			}
		}
		return internalAbs;
	}
	
	private static Facet getFacet(ObjectSchema schema, String slotName, Class facetClass) {
		Facet[] slotFacets = schema.getFacets(slotName);
		if (slotFacets != null) {
			for (Facet f : slotFacets) {
				if (f.getClass() == facetClass) {
					return f;
				}
			}
		}
		return null;
	}
	
	public void showValue() {
		showValue(this, getValue(), label!=null?label:id, 0);
	}
	
	private static void showValue(Object parent, AbsObject value, String name, int currentLevel) {
		if (value instanceof AbsVariable) {
			writeLine("-"+name+" = ? "+getConstaints(parent, name), currentLevel);
		}
		else if (value instanceof AbsPrimitive) {
			writeLine("-"+name+" = "+value, currentLevel);
		}
		else if (value instanceof AbsAggregate) {
			AbsAggregate agg = (AbsAggregate)value;
			writeLine("[]"+name+" "+getConstaints(parent, name), currentLevel);
			showValue(value, agg.getElementTemplate(), "elementType ("+agg.getElementTemplate().getTypeName()+")", currentLevel+1);
			for (int i=0; i<agg.size(); i++) {
				showValue(value, agg.get(i), "#"+i, currentLevel+1);
			}
		}
		else if (value instanceof AbsConcept) {
			writeLine("+"+name+" "+getConstaints(parent, name), currentLevel);
			for (String slot : value.getNames()) {
				showValue(value, value.getAbsObject(slot), slot, currentLevel+1);
			}
		}
	}

	private static String getConstaints(Object parent, String name) {
		List<Constraint> constraints = new ArrayList<Constraint>(); 
		if (parent instanceof StructuredDataElement) {
			constraints = ((StructuredDataElement)parent).getConstraints();
		} else if (parent instanceof ConstrainedAbsConcept) {
			constraints = ((ConstrainedAbsConcept)parent).getConstraints(name);
		}

		StringBuilder sb = new StringBuilder(); 
		if (constraints != null) {
			for (Constraint c : constraints) {
				if (c instanceof TypeConstraint) {
					addToConstaintsList(sb, ((TypeConstraint)c).getType());
				}
				else if (c instanceof MandatoryConstraint) {
					addToConstaintsList(sb, "MAN");
				}
				else if (c instanceof OptionalityConstraint) {
					addToConstaintsList(sb, "OPT");
				}
				else if (c instanceof DefaultConstraint) {
					addToConstaintsList(sb, "def="+((DefaultConstraint)c).getValue());							
				}
				else if (c instanceof PermittedValuesConstraint) {
					addToConstaintsList(sb, "values={"+((PermittedValuesConstraint)c).getPermittedValuesString()+"}");
				}
				else if (c instanceof RegexConstraint) {
					addToConstaintsList(sb, "rgx="+((RegexConstraint)c).getRegex());
				}
				else if (c instanceof DocumentationConstraint) {
					addToConstaintsList(sb, "doc="+((DocumentationConstraint)c).getDocumentation());
				}
				else if (c instanceof CardinalityConstraint) {
					int min = ((CardinalityConstraint)c).getMin();
					int max = ((CardinalityConstraint)c).getMax();
					addToConstaintsList(sb, "["+min+","+(max==-1?"unbounded":max)+"]");	
				}
			}
		}
		String consts = sb.toString();
		if (consts.length() > 0) {
			consts = "("+consts+")"; 
		}
		return consts;
	}

	private static void addToConstaintsList(StringBuilder sb, String text) {
		if (sb.length() != 0) {
			sb.append(", ");
		}
		sb.append(text);
	}
	
	private static void writeLine(String text, int indentLevel) {
		if (text != null) {
			StringBuilder sb = new StringBuilder(); 
			for(int i = 0; i < indentLevel; i++) {
				sb.append("\t");
			}
			sb.append(text);
			System.out.println(sb.toString());
		}
	}

	@Override
	protected Object getCacheValue() {
		return getValue();
	}

	@Override
	protected void setCacheValue(Object cacheValueObj) {
		try {
			manageCachedValues(null, getValue(), (AbsObject)cacheValueObj, true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void manageCachedValues(String part, AbsObject currentValue, AbsObject cacheValue, boolean removeEnabled) throws OntologyException {
		if (currentValue == null) {
			return;
		}
		
		if (removeEnabled && cacheValue == null) {
			markToBeRemove(part, true);
			return;
		}
		
		if (currentValue instanceof AbsVariable) {
			if (cacheValue instanceof AbsPrimitive) {
				addConstraint(new DefaultConstraint(((AbsPrimitive)cacheValue).getObject()), part, true);
			}
		}
		else if (currentValue instanceof AbsAggregate) {
			AbsAggregate currentAggregate = (AbsAggregate)currentValue;
			
			for (String slotName : cacheValue.getNames()) {
				String newPart = slotName;
				if (part != null) {
					newPart = part+Constants.BB_PART_SEPARATOR+newPart; 
				}
				
				AbsObject slotAbsObject;
				try {
					slotAbsObject = currentValue.getAbsObject(slotName);
				} catch(IndexOutOfBoundsException e) {
					currentAggregate.add(currentAggregate.getElementTemplate());
					slotAbsObject = currentValue.getAbsObject(slotName);
				}
				
				manageCachedValues(newPart, slotAbsObject, cacheValue.getAbsObject(slotName), removeEnabled);
			}
		}
		else if (currentValue instanceof AbsConcept) {
			for (String slotName : currentValue.getNames()) {
				String newPart = slotName;
				if (part != null) {
					newPart = part+Constants.BB_PART_SEPARATOR+newPart; 
				}
				manageCachedValues(newPart, currentValue.getAbsObject(slotName), cacheValue.getAbsObject(slotName), removeEnabled);
			}
		}
	}
}
