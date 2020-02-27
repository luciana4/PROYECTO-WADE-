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

import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.onto.AggregateHelper;
import jade.content.onto.BasicOntology;
import jade.content.onto.OntologyException;
import jade.core.Agent.Interrupted;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.tilab.wade.ca.WadeClassLoader;
import com.tilab.wade.ca.WadeClassLoaderManager;
import com.tilab.wade.ca.ontology.ModuleInfo;
import com.tilab.wade.ca.ontology.WorkflowDetails;
import com.tilab.wade.formats.WorkflowInterpreter;
import com.tilab.wade.performer.WorkflowEngineAgent.WorkflowExecutor;
import com.tilab.wade.performer.descriptors.Parameter;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.event.ExecutionErrorEvent;
import com.tilab.wade.performer.event.UnhandledExceptionEvent;
import com.tilab.wade.performer.layout.WorkflowDescription;
import com.tilab.wade.performer.ontology.Modifier;

/**
 Helper class providing utility static methods 
 @author Giovanni Caire - TILAB
 */
public class EngineHelper {
	public static final String ACTIVITY_METHOD_PREFIX = "execute";
	public static final String BEFORE_ACTIVITY_METHOD_PREFIX = "before";
	public static final String AFTER_ACTIVITY_METHOD_PREFIX = "after";
	public static final String CONDITION_METHOD_PREFIX = "check";
	
	public static final int EXECUTE_METHOD_TYPE = 0;
	public static final int BEFORE_METHOD_TYPE = 1;
	public static final int AFTER_METHOD_TYPE = 2;
	
	private static Logger myLogger = Logger.getMyLogger("com.tilab.wade.performer.EngineHelper");

	private static Map<Class, Object> defaultValues = new Hashtable<Class, Object>();
	
	static {
		defaultValues.put(Integer.class, new Integer(0));
		defaultValues.put(int.class, new Integer(0));
		defaultValues.put(Long.class, new Long(0L));
		defaultValues.put(long.class, new Long(0L));
		defaultValues.put(Short.class, new Short((short)0));
		defaultValues.put(short.class, new Short((short)0));
		defaultValues.put(Character.class, new Character('\u0000'));
		defaultValues.put(char.class, new Character('\u0000'));
		defaultValues.put(Byte.class, new Byte((byte)0));
		defaultValues.put(byte.class, new Byte((byte)0));
		defaultValues.put(Boolean.class, new Boolean(false));
		defaultValues.put(boolean.class, new Boolean(false));
		defaultValues.put(Float.class, new Float(0.0f));
		defaultValues.put(float.class, new Float(0.0f));
		defaultValues.put(Double.class, new Double(0.0d));
		defaultValues.put(double.class, new Double(0.0d));
	}
	
	public static void fillFormalParameters(Object obj, Class rootClass, List formalParams) {
		List unindexedParams = new ArrayList();
		recursiveFill(obj.getClass(), rootClass, formalParams, unindexedParams);
		
		// Sort by name the unindexed parameters
		Collections.sort(((ArrayList)unindexedParams).toList());
		
		// Finally insert/add un-indexed formal parameters and check that there are no null formal parameters
		Iterator unindexedIt = unindexedParams.iterator();
		for (int i = 0; i < formalParams.size(); ++i) {
			if (formalParams.get(i) == null) {
				// There is a hole in the formal parameters list --> fill it with the next unindexed formal 
				// parameter if there is one available (error otherwise) 
				if (unindexedIt.hasNext()) {
					formalParams.remove(i);
					formalParams.add(i, unindexedIt.next());
				} 
				else {
					throw new FormalParametersException("Class "+obj.getClass()+": Missing formal parameter at index "+i);
				}
			}
		}
		// Append remaining unindexed parameters at the end
		while (unindexedIt.hasNext()) {
			formalParams.add(unindexedIt.next());
		}
	}
	
	private static void recursiveFill(Class c, Class rootClass, List formalParams, List unindexedParams) {
		// First get FormalParameters defined in superclass if any
		Class superclass = c.getSuperclass();
		if (superclass != null && !superclass.equals(rootClass)) {
			recursiveFill(superclass, rootClass, formalParams, unindexedParams);
		}
		
		// Then get FormalParameters defined in the local class 
		Field[] fields = c.getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			FormalParameter fp = fields[i].getAnnotation(FormalParameter.class);
			if (fp != null) {
				Parameter p = new Parameter();
				p.setName(fields[i].getName());
				p.setMode(fp.mode());
				
				// Set parameter type and type-class
				p.setType(fields[i].getType().getName());
				p.setTypeClass(fields[i].getType());

				// Set parameter element type and type-class (only if parameter is an aggregate)
				// Warning: manage only first level (list of list of list are not managed)
				Class elementTypeClass = getElementTypeClass(fields[i]);
				if (fp.elementType() != Object.class) {
					elementTypeClass = fp.elementType();
				}
				if (elementTypeClass != null) {
					p.setElementTypeClass(elementTypeClass);
					p.setElementType(elementTypeClass.getName());
				}
				
				// Set constraints
				p.setMandatory(fp.mandatory());
				if (fp.cardMin() != FormalParameter.UNCARDINALIZED) {
					p.setCardMin(fp.cardMin());
				}
				if (fp.cardMax() != FormalParameter.UNCARDINALIZED) {
					p.setCardMax(fp.cardMax());
				}
				if (!fp.defaultValue().equals(FormalParameter.NULL)) {
					p.setDefaultValue(fp.defaultValue());
				}
				if (!fp.regex().equals(FormalParameter.NULL)) {
					p.setRegex(fp.regex());
				}
				if (!fp.documentation().equals(FormalParameter.NULL)) {
					p.setDocumentation(fp.documentation());	
				}
				if (fp.permittedValues().length > 0) {
					p.setPermittedValues(fp.permittedValues());	
				}
				
				// Manage parameter position
				int index = fp.index();
				if (index != FormalParameter.UNINDEXED) {
					// Note that fields are returned in random order --> We cannot simply add them 
					int size = formalParams.size();
					if (size > index) {
						Parameter p1 = (Parameter) formalParams.get(index);
						if (p1 == null) {
							formalParams.remove(index);
							formalParams.add(index, p);
						}
						else {
							throw new FormalParametersException("Duplicate formal parameter index ("+index+"): "+p.getName()+", "+p1.getName());
						}
					}
					else {
						for (int k = size; k < index; ++k) {
							formalParams.add(null);
						}
						formalParams.add(p);
					}
				}
				else {
					unindexedParams.add(p);
				}
			}
		}
	}

	/**
	 * Copy values from a List of Parameter-s to the corresponding fields of a target object.
	 */
	static final void copyInputParameters(Object target, List actualParams) throws WorkflowException {
		List initialActualParams = (List)((ArrayList)actualParams).clone();
		actualParams.clear();
		List formalParams = getFormalParameters(target);
		int size = (formalParams != null ? formalParams.size() : 0);
		for (int i = 0; i < size; ++i) {
			Parameter formal = (Parameter) formalParams.get(i);
			if (formal.getMode() == Constants.IN_MODE || formal.getMode() == Constants.INOUT_MODE) {
				// INPUT parameter: Get its value and set it to the target
				Parameter actual = getParameter(initialActualParams, formal, i);
				if (actual != null) {
					Object pValue = actual.getValue();
					setFieldValue(actual.getName(), pValue, target);
					actualParams.add(actual);
				}
				else {
					if (formal.getMandatory()) {
						throw new WorkflowException("Missing mandatory value for parameter "+formal);
					}
					if (formal.getMode() == Constants.INOUT_MODE) {
						actualParams.add(new Parameter(formal.getName(), formal.getType(), formal.getMode()));
					}
				}
			} else {
				// OUTPUT parameter: Add it (unless already present) so that it can be filled after the execution
				Parameter actual = getParameter(initialActualParams, formal, i);
				if (actual == null) {
					actualParams.add(new Parameter(formal.getName(), formal.getType(), formal.getMode()));
				} else {
					actualParams.add(actual);
				}
			}
		}
	}
	
	/**
	 * Extract from the fields of a target object the values of a list of OUT and INOUT Parameter-s
	 */
	static final void extractOutputParameters(Object target, List parameters) throws WorkflowException {
		if (parameters != null) {
			Iterator it = parameters.iterator();
			while (it.hasNext()) {
				Parameter p = (Parameter) it.next();
				if (p.getMode() == Constants.OUT_MODE || p.getMode() == Constants.INOUT_MODE) {
					String pName = p.getName();
					Object pValue = getFieldValue(pName, target);
					p.setValue(pValue);
				}
			}
		}
	}

	private static List getFormalParameters(Object obj) {
		if (obj instanceof WorkflowBehaviour) {
			return ((WorkflowBehaviour) obj).getFormalParameters();
		}
		else if (obj instanceof Application) {
			return ((Application) obj).getFormalParameters();
		}
		else {
			// FIXME: Check if obj declares a getFormalParameter() method and invoke it in that case
			List formalParams = new ArrayList();
			EngineHelper.fillFormalParameters(obj, Object.class, formalParams);
			return formalParams;
		}
	}
	
	static Set<String> initManagedFields(Object target, Class rootClass) {
		if (rootClass == null) {
			rootClass = Object.class;
		}
		Set<String> managedFieldNames = new HashSet<String>();
		
		recursiveFillManagedFields(target.getClass(), managedFieldNames, rootClass);
		
		return managedFieldNames;
	}
	
	private static void recursiveFillManagedFields(Class c, Set<String> ff, Class rootClass) {
		// First get managed fields defined in superclass if any
		Class superclass = c.getSuperclass();
		if (!superclass.equals(rootClass)) {
			recursiveFillManagedFields(superclass, ff, rootClass);
		}
		
		// Then get managed fields (non static, non final and not marked with the UnmanagedField annotation) 
		// defined in the local class 
		Field[] fields = c.getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			Field f = fields[i];
			int m = f.getModifiers();
			if (!java.lang.reflect.Modifier.isStatic(m) && !java.lang.reflect.Modifier.isFinal(m)) {
				UnmanagedField uf = fields[i].getAnnotation(UnmanagedField.class);
				if (uf == null) {
					ff.add(fields[i].getName());
				}
			}
		}
	}
	
	private static Object getFieldValue(String name, Object obj) {
		if (obj instanceof WorkflowBehaviour) {
			return ((WorkflowBehaviour) obj).getFieldValue(name);
		}
		else if (obj instanceof BaseApplication) {
			return ((BaseApplication) obj).getFieldValue(name);
		}
		else {
			return getFieldValue(name, obj, null);
		}
	}
	
	static Object getFieldValue(String name, Object obj, Map<String, Field> cachedFields) {
		Field field = null;
		try {
			field = EngineHelper.getField(name, obj, cachedFields);
			return field.get(obj);
		}
		catch (NoSuchFieldException nsfe) {
			throw new ReflectiveException("Connot find field "+name+" in class "+obj.getClass().getName(), nsfe);
		}
		catch (Exception e) {
			throw new ReflectiveException("Unexpected error retrieving value of field "+name+" of type "+field.getType().getName()+" of class "+obj.getClass().getName(), e);
		}
	}
	
	private static void setFieldValue(String name, Object value, Object obj) {
		if (obj instanceof WorkflowBehaviour) {
			((WorkflowBehaviour) obj).setFieldValue(name, value);
		}
		else if (obj instanceof BaseApplication) {
			((BaseApplication) obj).setFieldValue(name, value);
		}
		else {
			setFieldValue(name, value, obj, null);
		}
	}
	
	static void setFieldValue(String name, Object value, Object obj, Map<String, Field> cachedFields) {
		Field field = null;
		try {
			field = getField(name, obj, cachedFields);
			Class type = field.getType();
			if (value != null) {
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "Setting field "+name+" of type "+type.getName()+" to value "+value+" of class "+value.getClass().getName());
				}
	
				// If value is an abstract object, nullify variables (if any) and convert it into an object
				if (value instanceof AbsObject && !type.isAssignableFrom(AbsObject.class) ) {
					if (obj instanceof OntologyHolder) {
						value = AbsHelper.nullifyVariables((AbsObject)value, false);
						value = ((OntologyHolder)obj).getOntology().toObject((AbsObject)value);
					}
				}
				
				// If we are dealing with a "primitive" value adjust its type according to the destination-type
				// If this is not a "primitive" this call has no effect
				value = BasicOntology.adjustPrimitiveValue(value, type);
				
				// If we are dealing with an aggregate value, adjust its type (java array, java collection, jade collection) according to the destination-type
				// If this is not an aggregate this call has no effect
				value = AggregateHelper.adjustAggregateValue(value, type);
			}
			else {
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "Setting field "+name+" of type "+type.getName()+" to null");
				}
				// If we are trying to assign a null value to a primitive field, use the default value for the destination-type 
				if (type.isPrimitive()) {
					value = EngineHelper.getDefaultValue(type);
				}
			}
			
			field.set(obj, value);
		}
		catch (NoSuchFieldException nsfe) {
			throw new ReflectiveException("Cannot find field "+name+" in class "+obj.getClass().getName(), nsfe);
		}
		catch (IllegalArgumentException iae) {
			throw new ReflectiveException("Cannot assign value "+value+" of type "+value.getClass().getName()+" to field "+name+" of type "+field.getType().getName()+" of class "+obj.getClass().getName(), iae);
		}
		catch (OntologyException oe) {
			throw new ReflectiveException("Cannot convert Abs Descriptor "+value+" before assigning it to field "+name+" of type "+field.getType().getName()+" of class"+obj.getClass().getName(), oe);
		}
		catch (Exception e) {
			throw new ReflectiveException("Unexpected error assigning value "+value+" of type "+value.getClass().getName()+" to field "+name+" of type "+field.getType().getName()+" of class "+obj.getClass().getName(), e);
		}
	}
	
	/**
	 * Retrieve a field with a given name (possibly declared in a superclass)
	 * making it accessible if necessary and caching it for further usages if required.
	 */
	static Field getField(String fieldName, Object obj, Map<String, Field> cachedFields) throws NoSuchFieldException {
		Field f = (cachedFields != null ? cachedFields.get(fieldName) : null);
		if (f == null) {
			f = getField(fieldName, obj.getClass());
			if (f != null) {
				if (!f.isAccessible()) {
					try {
						f.setAccessible(true);
					}
					catch (SecurityException se) {
						throw new NoSuchFieldException("Field "+fieldName+" of class "+obj.getClass().getName()+" not accessible.");
					}
				}
				if (cachedFields != null) {
					cachedFields.put(fieldName, f);
				}
			}
			else {
				throw new NoSuchFieldException("Field "+fieldName+" not found in class "+obj.getClass().getName());
			}
		}
		return f;
	}
	
	private static Field getField(String fieldName, Class currentClass) {
		Field f = null;
		// Search first in the current class
		try {
			f = currentClass.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException nsfe) {
			// Field not declared in the current class --> Try in the superclasses
			Class superClass = currentClass.getSuperclass();
			if (!WorkflowBehaviour.class.equals(superClass) && !Application.class.equals(superClass)) {
				f = getField(fieldName, superClass);
			}
		}
		return f;
	}
	
	private static Parameter getParameter(List params, Parameter formalParameter, int index) {
		Parameter param = null;
		Iterator it = params.iterator();
		while (it.hasNext()) {
			Parameter p = (Parameter) it.next();
			if (p.getName() != null) {
				// Actual parameters passed by name
				if (p.getName().equals(formalParameter.getName())) {
					param = p;
					param.setMode(formalParameter.getMode());
					param.setType(formalParameter.getType());
					break;
				}
			}
			else {
				// Actual parameters passed by order (also set the name)
				if (index < params.size()) {
					param = (Parameter) params.get(index);
					param.setName(formalParameter.getName());
					param.setMode(formalParameter.getMode());
					param.setType(formalParameter.getType());
				}
				break;
			}
		}
		return param;
	}
	
	
	// FIXME: Likely to be removed together with AndTransition
	/**
	 Initialize the outgoing transitions and exception-transitions in
	 a given HierarchyNode.
	 */
	static final void initTransitions(List tt, List transitions, List exceptionTransitions) {
		if (tt != null) {
			int k = tt.size() - 1;
			for (int i = k; i >= 0; i--) {
				Transition t = (Transition) tt.get(i);
				List l = (t.isException() ? exceptionTransitions : transitions);
				// Default transitions must be evaluated only after normal transitions
				if (t.isDefault()) {
					l.add(t);
				} else {
					l.add(0, t);
				}
			}
		}
	}

	/**
	 Evaluate the outgoing transitions in a given HierarchyNode 
	 and produce an exit-value.
	 Also invoke the activity termination callback method
	 */
	static final int endActivity(HierarchyNode node) {
		OutgoingTransitions ot = node.getOutgoingTransitions();
		List transitions = ot.getTransitions();
		List exceptionTransitions = ot.getExceptionTransitions();
		int ret = Constants.DEFAULT_OK_EXIT_VALUE;
		Transition t = null; // This is declared here since we need it for logging purpose in case of condition evaluation error
		try {
			// If the execution of this node was interrupted due to a KillWorkflow from the outside, do not
			// even evaluate outgoing transitions since the execution will jump in the ERROR state.
			if (!node.isInterrupted()) {
				Throwable lastException = node.getLastException();
				List tt = (lastException == null ? transitions : exceptionTransitions);
				if (myLogger.isLoggable(Logger.FINE)) {
					myLogger.log(Logger.FINE, "Evaluating transitions from activity " + node.getBehaviourName() + ". Transitions list size = "+tt.size()+". Last-exception = " + lastException);
				}
				for (int i = 0; i < tt.size(); ++i) {
					t = (Transition) tt.get(i);
					if (myLogger.isLoggable(Logger.FINER)) {
						myLogger.log(Logger.FINER, "Evaluating " + t);
					}
					if (lastException != null) {
						// For sure t is an ExceptionTransition --> Pass it the occurred exception
						((ExceptionTransition) t).setException(lastException);
					}
					if (t.evaluateCondition()) {
						ret = t.getExitValue();
						if (myLogger.isLoggable(Logger.FINER)) {
							myLogger.log(Logger.FINER, "Exit value " + ret);
						}
						break;
					}
				}
				if (ret == Constants.DEFAULT_OK_EXIT_VALUE && lastException != null) {
					// Uncaught exception!
					if (logIfUncaughtOnly(node, lastException)) {
						lastException.printStackTrace();
					}
					ret = Constants.UNCAUGHT_EXCEPTION_EXIT_VALUE;
					if (node.hasJADEDefaultTransition()) {
						// The current node has a JADE outgoing default transition --> The exception will be ignored
						// --> Fire a warning event to keep track of it.
						((WorkflowEngineAgent.WorkflowExecutor) node.root()).getEventEmitter().fireEvent(Constants.WARNING_TYPE, new UnhandledExceptionEvent(node.getBehaviourName(), lastException), Constants.WARNING_LEVEL);
					}
				}
				node.getOwner().handleEndActivity(node);
			}
		} catch (Interrupted ie) {
			// Se si arriva qui significa che è stata interrotta la evaluateCondition della transizione.
			// (Caso facilmente riproducibile con una interact() nel metodo di valutazione della condizione che
			//  viene interrotta da una KillWorkflow.)
			// In questo caso (esattamente come nell'interruzione avvenuta nella execute() di una activity) 
			// il wf non deve essere ucciso immediatamente (quindi la Interrupted deve essere catchata)
			// e deve essere gestita la normale fine del wf (con il corretto aggiornamento dello stato sul WSMA)
			ret = Constants.DEFAULT_OK_EXIT_VALUE;
		} catch (Throwable thr) {
			thr.printStackTrace();
			fireExecutionErrorEvent(node, thr, Constants.SEVERE_LEVEL);
			
			// Cannot handle this exception --> propagate it to the parent node as a WorkflowException
			node.propagateException(new WorkflowException("Error evaluating condition "+t.getConditionName()+" "+t, thr));
		}
		node.mark();
		return ret;
	}
	
	static boolean logIfUncaughtOnly(HierarchyNode node, Throwable t) {
		// FIXME: Must call node.getOwner().logIfUncaughtOnly(node, t) 
		return true;
	}

	static final void fireExecutionErrorEvent(HierarchyNode node, Throwable t, int level) {
		//#MIDP_EXCLUDE_BEGIN
		WorkflowEngineAgent.WorkflowExecutor root = (WorkflowEngineAgent.WorkflowExecutor) node.root();
		String workflowName = root.getDescriptor().getId();
		ExecutionErrorEvent errorEvent = new ExecutionErrorEvent(workflowName, node.getBehaviourName(), t);
		root.getEventEmitter().fireEvent(Constants.WARNING_TYPE, errorEvent, level);
		root.setLastErrorEvent(errorEvent);
		//#MIDP_EXCLUDE_END
	}
	
	public static final MessageTemplate adjustReplyTemplate(MessageTemplate template, ACLMessage msg) {
		// If the agent sending the message is also one of the receivers,
		// avoid intercepting the msg as if it was the reply.
		return MessageTemplate.and(template, MessageTemplate.not(MessageTemplate.MatchCustom(msg, true)));
	}
	
	public static String lowerCaseFirst(String str) {
		char[] cc = str.toCharArray();
		cc[0] = Character.toLowerCase(cc[0]);
		return new String(cc);
	}

	public static String upperCaseFirst(String str) {
		char[] cc = str.toCharArray();
		cc[0] = Character.toUpperCase(cc[0]);
		return new String(cc);
	}
	
	public static final String activityName2Method(String activityName) {
		return activityName2Method(activityName, EXECUTE_METHOD_TYPE);
	}
	
	public static final String activityName2Method(String activityName, int type) {
		String prefix = ACTIVITY_METHOD_PREFIX;
		if (type == BEFORE_METHOD_TYPE) {
			prefix = BEFORE_ACTIVITY_METHOD_PREFIX;
		}
		else if (type == AFTER_METHOD_TYPE) {
			prefix = AFTER_ACTIVITY_METHOD_PREFIX;
		}
		return prefix+activityName;
	}
	
	public static final String conditionName2Method(String conditionName) {
		return CONDITION_METHOD_PREFIX+conditionName;
	}
	
	public static void checkMethodName(String methodName, String objectType, String objectName) throws IllegalArgumentException {
		char c = methodName.charAt(0);
		if (!Character.isJavaIdentifierStart(c)) {
			throw new IllegalArgumentException("Method name "+methodName+" for "+objectType+" "+objectName+" is not a valid Java method name");
		}
		for (int i = 1; i < methodName.length(); i++) {
			c = methodName.charAt(i);
			if (!Character.isJavaIdentifierPart(c)) {
				throw new IllegalArgumentException("Method name "+methodName+" for "+objectType+" "+objectName+" is not a valid Java method name");
			}
		} 
	}
	
	public static Object getDefaultValue(String type){
		Object value = null;
		Class cl;
		try {
			cl = Class.forName(type);
			value = getDefaultValue(cl);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return value;
	}
	
	static Object getDefaultValue(Class cl){
		return defaultValues.get(cl);
	}
	
	public static boolean isEventMessage(ACLMessage msg) {
		return "true".equals(msg.getUserDefinedParameter(Constants.EVENT_MESSAGE));
	}

	/**
	 * Get element aggregate class (java array or generic collection), 
	 * null if field not represent an aggregate or if is not deducible
	 * 
	 * @param field class field
	 * @return element aggregate class or null
	 */
	static Class getElementTypeClass(Field field) {
		Class elementTypeClass = null;
		Type fieldType = field.getGenericType();

		// If field-type is a Class check if is an array 
		if (fieldType instanceof Class) {
			Class fieldClass = (Class)fieldType;
			if (fieldClass.isArray() && fieldClass != byte[].class) {
				// extract the type of array elements
				elementTypeClass = fieldClass.getComponentType();
			}
		}
		
		// If field-type is a ParameterizedType get generic informations 
		else if (fieldType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)fieldType;
			Type[] actuals = pt.getActualTypeArguments();
			if (actuals.length > 0) {
				elementTypeClass = (Class)actuals[0];
			}
		}

		return elementTypeClass;
	}
	
	static DefaultParameterValues extractDefaultParameterValues(WorkflowBehaviour wb) {
		DefaultParameterValues dpv = null;
		
		Modifier dpvModifier = wb.getModifier(DefaultParameterValues.DEFAULT_PARAMETER_VALUES_MODIFIER);
		if (dpvModifier != null) {
			dpv = new DefaultParameterValues(dpvModifier);
		}
		
		return dpv;
	}

	static WebServiceSecurityContext extractWebServiceSecurityContext(WorkflowBehaviour wb, String activityId) {
		// Try to manage the activity with specific webservice security passed as modifier in workflow
		WebServiceSecurityContext wfSecurityContext = internalExtractWebServiceSecurityContext(wb, activityId);
		if (wfSecurityContext == null) {
	
			// Try to manage with default webservice security passed as modifier in workflow
			wfSecurityContext = internalExtractWebServiceSecurityContext(wb, null);
		}
		return wfSecurityContext;
	}

	private static WebServiceSecurityContext internalExtractWebServiceSecurityContext(WorkflowBehaviour wb, String activityId) {
		WebServiceSecurityContext securityContext = null;
		
		String modifierName;
		if (activityId == null) {
			modifierName = WebServiceSecurityContext.WEBSERVICE_SECURITY_MODIFIER;
		} else {
			modifierName = WebServiceSecurityContext.ACTIVITY_WEBSERVICE_SECURITY_MODIFIER+"_"+activityId;
		}
		
		Modifier securityModifier = wb.getModifier(modifierName);
		if (securityModifier != null) {
			securityContext = new WebServiceSecurityContext(securityModifier);
		}
		
		return securityContext;
	}
	
	static WebServiceAddressingContext extractWebServiceAddressingContext(WorkflowBehaviour wb, String activityId) {
		// Try to manage the activity with specific addressing passed as modifier in workflow
		WebServiceAddressingContext wfAddressingContext = internalExtractWebServiceAddressingContext(wb, activityId);
		if (wfAddressingContext == null) {
	
			// Try to manage with default addressing passed as modifier in workflow
			wfAddressingContext = internalExtractWebServiceAddressingContext(wb, null);
		}
		return wfAddressingContext;
	}
	
	private static WebServiceAddressingContext internalExtractWebServiceAddressingContext(WorkflowBehaviour wb, String activityId) {
		WebServiceAddressingContext addressingContext = null;
		
		String modifierName;
		if (activityId == null) {
			modifierName = WebServiceAddressingContext.WEBSERVICE_ADDRESSING_MODIFIER;
		} else {
			modifierName = WebServiceAddressingContext.ACTIVITY_WEBSERVICE_ADDRESSING_MODIFIER+"_"+activityId;
		}
		
		Modifier addressingModifier = wb.getModifier(modifierName);
		if (addressingModifier != null) {
			addressingContext = new WebServiceAddressingContext(addressingModifier);
		}
		
		return addressingContext;
	}
	
	public static WorkflowDetails buildWorkflowDetails(Class<?> clazz) {
		WorkflowDetails workflowDetails = new WorkflowDetails();
		workflowDetails.setClassName(clazz.getName());
		
		// Try to get information about the module containing this workflow
		ModuleInfo moduleInfo = WadeClassLoaderManager.getModuleInfo(clazz);
		workflowDetails.setModuleInfo(moduleInfo);
		
		// Get info from annotation
		WorkflowDescription workflowDescription = clazz.getAnnotation(WorkflowDescription.class);
		if (workflowDescription != null) {
			String wdName = workflowDescription.name();
			if (!wdName.equals(WorkflowDescription.NULL)) {
				workflowDetails.setName(wdName);
			}
			String wdDocumentation = workflowDescription.documentation();
			if (!wdDocumentation.equals(WorkflowDescription.NULL)) {
				workflowDetails.setDocumentation(wdDocumentation);
			}
			String wdCategory = workflowDescription.category();
			if (!wdCategory.equals(WorkflowDescription.NULL)) {
				workflowDetails.setCategory(wdCategory);
			}
			String wdIcon = workflowDescription.icon();
			if (!wdIcon.equals(WorkflowDescription.NULL)) {
				workflowDetails.setIcon(wdIcon);
			}	
			String wdColor = workflowDescription.color();
			if (!wdColor.equals(WorkflowDescription.DEFAULT_COLOR)) {
				workflowDetails.setColor(wdColor);
			}	
			workflowDetails.setComponent(workflowDescription.component());
		}
		
		if (workflowDetails.getName() == null) {
			workflowDetails.setName(clazz.getSimpleName());
		}
		
		if (workflowDetails.isComponent() == null) {
			workflowDetails.setComponent(false);
		}
		
		return workflowDetails;
	}
	
	public static boolean isInteractive(WorkflowExecutor we) {
		return Modifier.getModifier(Constants.INTERACTIVE_MODIFIER, we.getModifiers()) != null;
	}
	
	public static WorkflowBehaviour createWorkflowBehaviour(WorkflowDescriptor workflowDescriptor, ClassLoader classLoader) throws Exception {
		WorkflowBehaviour wb;
		
		if (workflowDescriptor.getFormat() == null || workflowDescriptor.getRepresentation() == null) {

			// No format specified -> standard workflow loading from class
			wb = (WorkflowBehaviour) Class.forName(workflowDescriptor.getId(), true, classLoader).newInstance();
			workflowDescriptor.setClassLoaderIdentifier(getClassLoaderIdentifier(classLoader));
		} else {
			// Format present -> use specific interpreter to load the workflow
			// Interpreter class is com.tilab.wade.converter.<format>.<FORMAT>Interpreter
			// and must extend WorkflowInterpreter interface  
			String wfiBasePkgName = "com.tilab.wade.formats";
			String wfiFormat = workflowDescriptor.getFormat();
			String wfiClassName = wfiBasePkgName+"."+wfiFormat.toLowerCase()+".interpreter."+wfiFormat.toUpperCase()+"Interpreter";
			Class wfiClass = Class.forName(wfiClassName);
			WorkflowInterpreter wfi;

			// Create and initialize interpreter instance
			wfi = (WorkflowInterpreter)wfiClass.newInstance();
			
			FileInputStream inputStream = null;
			try {
				String propertyFilename = wfiFormat.toLowerCase()+".properties";
	            URL input = ClassLoader.getSystemResource(propertyFilename);
	            if (input == null) {
	            	input = classLoader.getResource(propertyFilename);
	            }
	            
	            Properties properties = new Properties();
	            if (input != null) {
					properties.load(input.openStream());
	            }
	            
				wfi.init(properties);
			} finally {
				if (inputStream != null) {
					try { inputStream.close();} catch (IOException e) {}
				}
			}
			
			// Interpret workflow
			wb = wfi.interpret(workflowDescriptor.getRepresentation());
		}
		
		return wb;
	}
	
	private static String getClassLoaderIdentifier(ClassLoader cl) {
		if (cl instanceof WadeClassLoader){
			return ((WadeClassLoader) cl).getId();
		}
		return null;
	}
}
