package com.tilab.wade.utils.cli;

import java.util.Date;
import java.util.Properties;

import com.tilab.wade.commons.ontology.Attribute;
import com.tilab.wade.commons.ontology.WadeManagementOntology;
import com.tilab.wade.utils.behaviours.SimpleFipaRequestInitiator;

import jade.cli.CLICommand;
import jade.cli.CLIManager;
import jade.cli.Option;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Iterator;

public class GetAttributes extends CLICommand {

	@Option(value="<name>", description="The name of the agent whose attributes must be retrieved")
	public static final String AGENT_OPTION = "agent";
	
	public static void main(String[] args) {
		CLIManager.execute(new GetAttributes(), args);
	}

	@Override
	public Behaviour getBehaviour(Properties pp) throws IllegalArgumentException {
		final String agentName = CLIManager.getMandatoryOption(AGENT_OPTION, pp);
		return new SimpleFipaRequestInitiator() {
			public void onStart() {
				myAgent.getContentManager().registerLanguage(new SLCodec(true));
				myAgent.getContentManager().registerOntology(WadeManagementOntology.getInstance());
				super.onStart();
			}
			
			protected ACLMessage prepareRequest(ACLMessage request) {
				com.tilab.wade.commons.ontology.GetAgentAttributes gaa = new com.tilab.wade.commons.ontology.GetAgentAttributes();
				gaa.setAgentName(agentName);
				
				AID target = new AID(agentName, AID.ISLOCALNAME);
				
				Action actExpr = new Action();
				actExpr.setActor(target);
				actExpr.setAction(gaa);
				
				request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(target);
				request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
				request.setOntology(WadeManagementOntology.getInstance().getName());
				request.setReplyByDate(new Date(System.currentTimeMillis() + 20000));
				
				try {
					myAgent.getContentManager().fillContent(request, actExpr);
					return request;
				}
				catch (Exception e) {
					out.println(e.getMessage());
					return null;
				}
			}
			
			@Override
			public void handleInform(ACLMessage inform) {
				try {
					Result result = (Result) myAgent.getContentManager().extractContent(inform);
					jade.util.leap.List attributes = (jade.util.leap.List) result.getValue();
					Iterator it = attributes.iterator();
					while (it.hasNext()) {
						Attribute attr = (Attribute) it.next();
						out.println(attr.getName()+" = "+attr.getValue());
					}
				}
				catch (Exception e) {
					out.println(e.getMessage());
				}
			}
			
			@Override
			public void handleError(ACLMessage msg) {
				out.println("Unexpected "+ACLMessage.getPerformative(msg.getPerformative())+" retrieving agent attributes. "+msg.getContent());
			}
			
			@Override
			public void handleTimeout() {
				out.println("Timeout");
			}
		};
	}

}
