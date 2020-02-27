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
package com.tilab.wade.proxy;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsConcept;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.abs.AbsTerm;
import jade.content.abs.AbsVariable;
import jade.content.onto.BasicOntology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.util.leap.Properties;
import jade.wrapper.gateway.DynamicJadeGateway;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import com.tilab.wade.dispatcher.WorkflowEventListener;
import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.DefaultParameterValues;
import com.tilab.wade.performer.WebServiceSecurityContext;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.interactivity.Action;
import com.tilab.wade.performer.interactivity.CardinalityConstraint;
import com.tilab.wade.performer.interactivity.Component;
import com.tilab.wade.performer.interactivity.ConstrainedAbsConcept;
import com.tilab.wade.performer.interactivity.Constraint;
import com.tilab.wade.performer.interactivity.ConstraintException;
import com.tilab.wade.performer.interactivity.DocumentationConstraint;
import com.tilab.wade.performer.interactivity.Panel;
import com.tilab.wade.performer.interactivity.RegexConstraint;
import com.tilab.wade.performer.interactivity.StructuredDataElement;
import com.tilab.wade.performer.interactivity.DefaultConstraint;
import com.tilab.wade.performer.interactivity.Interaction;
import com.tilab.wade.performer.interactivity.MandatoryConstraint;
import com.tilab.wade.performer.interactivity.OptionalityConstraint;
import com.tilab.wade.performer.interactivity.PermittedValuesConstraint;
import com.tilab.wade.performer.interactivity.TypeConstraint;
import com.tilab.wade.performer.ontology.ExecutionError;

/**
 * This class provides a simple textual console to execute interactive workflows.
 */
public class EngineProxyShell implements WorkflowResultListener, WorkflowEventListener {

	private EngineProxy ep;
	private DynamicJadeGateway gateway; 
	private String host = "localhost";
	private int port = 1099;
	private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	private boolean inited = false;
	private WorkflowContext context = new WorkflowContext();
	private WorkflowController controller;
	private int varCounter;
	private int nodeCounter;
	private Vector<Variable> vars = new Vector<Variable>();
	private ArrayList<String> nots = new ArrayList<String>();
	
	public EngineProxyShell() {
		gateway = new DynamicJadeGateway();
		ep = EngineProxy.getEngineProxy(gateway);
		
		context.setDefaultParameterValues(new DefaultParameterValues());
		context.setWebServiceDefaultSecurityContext(new WebServiceSecurityContext());
		
		writeLine("Interactive Wade Console");
		writeLine("------------------------");
		writeLine();
		
		mainMenu();
	}
	
	private void mainMenu() {
		while(true) {
			writeLine();
			writeLine("1) Console configuration/status");
			writeLine("2) Execute workflow");
			writeLine("3) Exit");
			
			String choice = readLine("Choice? ");
			writeLine();
			if ("1".equalsIgnoreCase(choice)) {
				configureMenu();
			} else if ("2".equalsIgnoreCase(choice)) {
				executeMenu();
			} else if ("3".equalsIgnoreCase(choice)) {
				exit();
			}
		}
	}

	private void configureMenu() {
		boolean exit = false;
		while(!exit) {
			writeLine();
			writeLine("1) Show actual configuration/status");
			writeLine("2) Configure");
			writeLine("3) Connect platform");
			writeLine("4) Disconnect platform");
			writeLine("5) Return");
			
			String choice = readLine("Choice? ");
			writeLine();
			if ("1".equalsIgnoreCase(choice)) {
				showConfig();
			} else if ("2".equalsIgnoreCase(choice)) {
				config();
			} else if ("3".equalsIgnoreCase(choice)) {
				try {
					init(false);
					gateway.checkJADE();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if ("4".equalsIgnoreCase(choice)) {
				gateway.shutdown();
			} else if ("5".equalsIgnoreCase(choice)) {
				exit = true;
			}
		}
	}

	private void showConfig() {
		writeLine("Platform host: "+host);
		writeLine("Platform port: "+port);
		writeLine("Platform status: "+(gateway.isGatewayActive()?"connected":"disconnected"));
	}

	private void config() {
		host = readLine("Platform host? ");
		port = Integer.parseInt(readLine("Platform port? "));
		init(true);
	}
	
	private void executeMenu() {
		boolean exit = false;
		while(!exit) {
			writeLine();
			writeLine("1) Launch interpreted WF");
			writeLine("2) Launch deployed WF");
			writeLine("3) View wf events");
			writeLine("4) Security");
			writeLine("5) Default parameters");
			writeLine("6) Return");
			
			String choice = readLine("Choice? ");
			writeLine();
			if ("1".equalsIgnoreCase(choice)) {
				launchInterpretedWF();
			} else if ("2".equalsIgnoreCase(choice)) {
				launchDeployedWF();
			} else if ("3".equalsIgnoreCase(choice)) {
				viewEvents();
			} else if ("4".equalsIgnoreCase(choice)) {
				securityMenu();
			} else if ("5".equalsIgnoreCase(choice)) {
				dpMenu();
			} else if ("6".equalsIgnoreCase(choice)) {
				exit = true;
			}
		}
	}
	
	private void viewEvents() {
		writeLine("Last workflow events");
		for (String not : nots) {
			writeLine(not, 1);
		}
	}

	private void dpMenu() {
		boolean exit = false;
		while(!exit) {
			writeLine();
			writeLine("1) View");
			writeLine("2) Reset");
			writeLine("3) Set parameter");
			writeLine("4) Set header");
			writeLine("5) Return");
			
			String choice = readLine("Choice? ");
			writeLine();
			DefaultParameterValues dpv = context.getDefaultParameterValues();
			if ("1".equalsIgnoreCase(choice)) {
				writeLine("Not implemented...");
			} else if ("2".equalsIgnoreCase(choice)) {
				context.setDefaultParameterValues(new DefaultParameterValues());
			} else if ("3".equalsIgnoreCase(choice)) {
				String activity = readLine("Activity name? ");
				String name = readLine("Parameter name? ");
				String part = readLine("Parameter part? ");
				String value = readLine("Parameter value? ");
				dpv.setParameterValue(activity, name, part, value);
			} else if ("4".equalsIgnoreCase(choice)) {
				String activity = readLine("Activity name? ");
				String name = readLine("Header name? ");
				String part = readLine("Header part? ");
				String value = readLine("Header value? ");
				dpv.setHeaderValue(activity, name, part, value);
			} else if ("5".equalsIgnoreCase(choice)) {
				exit = true;
			}
		}
	}

	private void securityMenu() {
		boolean exit = false;
		while(!exit) {
			writeLine();
			writeLine("1) View");
			writeLine("2) Reset");
			writeLine("3) Set http security");
			writeLine("4) Set wss security");
			writeLine("5) Set ssl");
			writeLine("6) Return");
			
			String choice = readLine("Choice? ");
			writeLine();
			WebServiceSecurityContext wssc = context.getWebServiceDefaultSecurityContext();
			if ("1".equalsIgnoreCase(choice)) {
				writeLine("HttpUsername: "+wssc.getHttpUsername());
				writeLine("HttpPassword: "+wssc.getHttpPassword());
				writeLine("WSSUsername: "+wssc.getWSSUsername());
				writeLine("WSSPasswordType: "+wssc.getWSSPasswordType());
				writeLine("WSSPassword: "+wssc.getWSSPassword());
				writeLine("WSSTimeToLive: "+wssc.getWSSTimeToLive());
				writeLine("CertificareChecking: "+wssc.isEnableCertificateChecking());
				writeLine("TrustStore: "+wssc.getTrustStore());
				writeLine("TrustStorePassword: "+wssc.getTrustStorePassword());
			} else if ("2".equalsIgnoreCase(choice)) {
				context.setWebServiceDefaultSecurityContext(new WebServiceSecurityContext());
			} else if ("3".equalsIgnoreCase(choice)) {
				wssc.setHttpUsername(readLine("HttpUsername? "));
				wssc.setHttpPassword(readLine("HttpPassword? "));
			} else if ("4".equalsIgnoreCase(choice)) {
				wssc.setWSSUsername(readLine("WSSUsername? "));
				String wssPasswordType = readLine("WSSPasswordType? (PasswordDigest, [PasswordText])");
				if (wssPasswordType == null || "".equals(wssPasswordType)) {
					wssPasswordType = "PasswordText"; 
				}
				wssc.setWSSPasswordType(wssPasswordType);
				wssc.setWSSPassword(readLine("WSSPassword? "));
				wssc.setWSSTimeToLive(Integer.valueOf(readLine("WSSTimeToLive? [sec]")));
				wssc.setWSSMustUnderstand(false);
			} else if ("5".equalsIgnoreCase(choice)) {
				String enableCertificateChecking = readLine("EnableCertificateChecking? ([false], true)");
				if (enableCertificateChecking == null || "".equals(enableCertificateChecking)) {
					enableCertificateChecking = "false";
				}
				wssc.setCertificateChecking("true".equalsIgnoreCase(enableCertificateChecking));
				if (wssc.isEnableCertificateChecking()) {
					wssc.setTrustStore(readLine("TrustStore? "));
					wssc.setTrustStorePassword(readLine("TrustStorePassword? "));
				}
			} else if ("6".equalsIgnoreCase(choice)) {
				exit = true;
			}
		}
	}

	private void launchDeployedWF() {
		try {
			init(false);
			nots.clear();
			String id = readLine("Workflow id? ");
			WorkflowDescriptor wd = new WorkflowDescriptor(id);
			controller = ep.launch(wd, this, this, context, true);
			go();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void launchInterpretedWF() {
		try {
			init(false);
			nots.clear();
			String format = readLine("WF format? ");
			String fileName = readLine("WF representation file? ");
			String representation = getFileContent(fileName);
			WorkflowDescriptor wd = new WorkflowDescriptor("test");
			wd.setFormat(format);
			wd.setRepresentation(representation);
			controller = ep.launch(wd, this, this, context, true);
			go();
		} catch(Exception e) {
			writeLine("ERROR: "+e.getMessage());
		}
	}
	
	private void go() throws EngineProxyException {
		if (controller == null) {
			return;
		}
		
		Interaction interact = null;
		do {
			interact = controller.go(interact);
			interact = showInteraction(interact);
		} while(!interact.isLast());
	}

	private Interaction showInteraction(Interaction interact) {
		int intChoice;
		do {
			intChoice = -1;
			vars.clear();
			varCounter = 0;
			nodeCounter = 0;
			writeLine("----------------------------------");
			writeLine("--- "+interact.getTitle());
			showComponent(interact.getMainPanel(), 0);
	
			if (!interact.isLast()) {
				writeLine("--- Actions");
				if (varCounter > 0) {
					writeLine("s) Set variable value", 1);
				}
				if (nodeCounter > 0) {
					writeLine("r) Remove/Add node", 1);
				}
				writeLine("k) kill wf", 1);
				java.util.List<Action> actions = interact.getActions();
				for (int i=0; i<actions.size(); i++) {
					Action action = actions.get(i);
					String state = "";
					try {
						action.validate();
					} catch(ConstraintException e) {
						state = "(DISABLE)";
					}
					writeLine((i+1)+") "+action.getLabel()+" "+state, 1);
				}
				
				String choice = readLine("Choice? ");
				try {
					intChoice = Integer.parseInt(choice);
				} catch(Exception e) {}
				
				if (choice.equalsIgnoreCase("s") && varCounter > 0) {
					setValue();
				}
				else if (choice.equalsIgnoreCase("k")) {
					try {
						ep.kill(controller.getExecutionId());
					} catch (EngineProxyException e) {
						writeLine("WARNING: Error killing wf, "+e.getMessage());
					}
					interact.setLast(true);
				}
				else if (intChoice >=1 && intChoice <= actions.size()) {
					Action action = interact.getActions().get(intChoice-1);
					try {
						action.validate();
						action.setSelected(true);
					} catch (ConstraintException e) {
						writeLine("WARNING: "+e.getMessage()+", element="+e.getVisualElement().getLabel());
						intChoice = -1;
					}
				} else {
					intChoice = -1;
				}
			}
		} while(intChoice <= 0 && !interact.isLast());
			
		return interact;
	}

	private void setValue() {
		int varNumber = -1;
		try {
			varNumber = Integer.parseInt(readLine("Choice variable number? "));
		} catch(Exception e) {}
		String varValue = readLine("Value? ");
		
		if (varNumber >= 1 && varNumber <= vars.size()) {
			Variable var = vars.get(varNumber-1);
			Object parent = var.parent;
			if (parent instanceof StructuredDataElement) {
				((StructuredDataElement)parent).setValue(getTypedValue(varValue, var.type));
			} else {
				
				if (parent instanceof AbsAggregate) {
					AbsAggregate agg = (AbsAggregate)parent;
					int pos = Integer.parseInt(var.name.substring(1));
					AbsTerm template = agg.get(pos);
					agg.remove(pos);
					agg.add(getTypedValue(varValue, var.type));
					agg.add(template);
				}
				else {
					try {
						AbsHelper.setAttribute((AbsObject)parent, var.name, getTypedValue(varValue, var.type));
					} catch (OntologyException e) {
						e.printStackTrace();
					}
				}
			}
		}		
	}
	
	private AbsPrimitive getTypedValue(String value, String type) {
		if (type.equals(BasicOntology.STRING)) {
			return AbsPrimitive.wrap(value);	
		}
		if (type.equals(BasicOntology.FLOAT)) {
			return AbsPrimitive.wrap(Float.valueOf(value));	
		}
		if (type.equals(BasicOntology.INTEGER)) {
			return AbsPrimitive.wrap(Integer.valueOf(value));
		}
		if (type.equals(BasicOntology.BOOLEAN)) {
			if (value.equalsIgnoreCase("true")) {
				return AbsPrimitive.wrap(new Boolean(true));
			} else if (value.equalsIgnoreCase("false")) {
				return AbsPrimitive.wrap(new Boolean(false));
			} else {
				writeLine("WARNING: "+value+" not boolean value (true/false)");
			}
		}
		if (type.equals(BasicOntology.DATE)) {
			java.text.SimpleDateFormat W3CISO8601DateFormat = new java.text.SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSS");
			Date date;
			try {
				date = W3CISO8601DateFormat.parse(value);
				return AbsPrimitive.wrap(date);	
			} catch (ParseException e) {
				writeLine("WARNING: Date "+value+" not in W3C-ISO8601 format");
			}
		}
		return null;
	}

	private void showComponent(Component comp, int indentLevel) {
		if (comp instanceof Panel) {
			Panel panel = (Panel)comp;
			writeLine(panel.getLabel(), indentLevel);
			for (Component c : panel.getComponents()) {
				showComponent(c, indentLevel+1);
			}
		}
		else if (comp instanceof StructuredDataElement) {
			StructuredDataElement de = (StructuredDataElement)comp;
			showAbs(de, de.getValue(), de.getLabel(), indentLevel);
		}
		else {
			writeLine("-"+comp.getLabel()+" = NOT SUPPORTED", indentLevel);
		}
	}

	private void showAbs(Object parent, AbsObject value, String name, int currentLevel) {
		if (value instanceof AbsVariable) {
			writeLine("-"+name+" = ?"+(varCounter+1)+" "+getConstaints(parent, name), currentLevel);
			Variable var = new Variable();
			var.name = name;
			var.parent = parent;
			var.type = ((AbsVariable)value).getType();
			vars.add(var);
			varCounter++;
		}
		else if (value instanceof AbsPrimitive) {
			writeLine("-"+name+" = "+value, currentLevel);
		}
		else if (value instanceof AbsAggregate) {
			AbsAggregate agg = (AbsAggregate)value;
			writeLine("+"+name+" "+getConstaints(parent, name), currentLevel);
			for (int i=0; i<agg.size(); i++) {
				showAbs(value, agg.get(i), "#"+i, currentLevel+1);
			}
		}
		else if (value instanceof AbsConcept) {
			writeLine("+"+name+" "+getConstaints(parent, name), currentLevel);
			for (String slot : value.getNames()) {
				showAbs(value, value.getAbsObject(slot), slot, currentLevel+1);
			}
		}
	}

	private String getConstaints(Object parent, String name) {
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

	private void addToConstaintsList(StringBuilder sb, String text) {
		if (sb.length() != 0) {
			sb.append(", ");
		}
		sb.append(text);
	}
	
	private void exit() {
		writeLine("Goodbye!");
		System.exit(0);
	}
	
	private void init(boolean reinit) {
		if (!inited || reinit) {
			try {
				Properties props = new Properties();
				props.setProperty(jade.core.Profile.MAIN_HOST, host);
				props.setProperty(jade.core.Profile.MAIN_PORT, Integer.toString(port));
				
				gateway.init(null, props);
				inited = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private String readLine(String text) {
		String line = null;
		try {
			write(text);
			line = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		if (line == null || line.equals("")) {
			line = null;
		}
		return line;
	}
	
	private void writeLine() {
		writeLine("", 0);
	}
	
	private void writeLine(String text) {
		writeLine(text, 0);
	}

	private void writeLine(String text, int indentLevel) {
		if (text != null) {
			write(text, indentLevel);
			System.out.println();
		}
	}

	private void write(String text) {
		write(text, 0);
	}
	
	private void write(String text, int indentLevel) {
		if (text != null) {
			StringBuilder sb = new StringBuilder(); 
			for(int i = 0; i < indentLevel; i++) {
				sb.append("\t");
			}
			sb.append(text);
			System.out.print(sb.toString());
		}
	}
	
    private String getFileContent(String fileName) throws FileNotFoundException,IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader input = null;
        input = new BufferedReader( new FileReader(new File(fileName)));
        String line = null;
        while (( line = input.readLine()) != null){
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
        }
        if (input!= null) {
            input.close();
        }
        return contents.toString();
    }
	
	public void handleAssignedId(AID executor, String executionId) {
	}

	public void handleExecutionCompleted(jade.util.leap.List results, AID executor, String executionId) {
		nots.add("ExecutionCompleted "+executor+", "+executionId+", "+results);
	}

	public void handleExecutionError(ExecutionError er, AID executor, String executionId) {
		nots.add("ExecutionError "+executor+", "+executionId+", "+er);
	}

	public void handleLoadError(String reason) {
		nots.add("LoadError "+reason);
	}

	public void handleNotificationError(AID executor, String executionId) {
		nots.add("NotificationError "+executor+", "+executionId);
	}

	public void handleEvent(long time, Object ev, AID executor, String executionId) {
		nots.add("handleEvent "+ev+", "+executor+", "+executionId);
	}

	public void handleExecutionCompleted(AID executor, String executionId) {
	}

	private class Variable {
		Object parent;
		String name;
		String type;
	}
	
	
	public static void main(String[] args) {
		new EngineProxyShell();
	}

}
