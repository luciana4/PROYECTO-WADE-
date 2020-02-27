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

//import org.apache.axis.message.addressing.ReferenceParametersType;
//import org.apache.axis.message.addressing.ReferencePropertiesType;
//import org.apache.axis.message.addressing.RelatesTo;

import com.tilab.wade.performer.ontology.Modifier;

import jade.util.leap.List;
import jade.util.leap.Serializable;


public class WebServiceAddressingContext implements Serializable {

	public static final String VERSION_2003_03 = "http://schemas.xmlsoap.org/ws/2003/03/addressing";
	public static final String VERSION_2004_03 = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
	public static final String VERSION_2004_08 = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
	public static final String VERSION_2005_08 = "http://www.w3.org/2005/08/addressing";
	
	static final String WEBSERVICE_ADDRESSING_MODIFIER = "WEBSERVICE_ADDRESSING_MODIFIER";
	final static String ACTIVITY_WEBSERVICE_ADDRESSING_MODIFIER = "ACTIVITY_WEBSERVICE_ADDRESSING_MODIFIER";

	final static String SEND_DEFAULT_MESSAGE_ID = "SEND_DEFAULT_MESSAGE_ID";
	final static String SEND_DEFAULT_FROM = "SEND_DEFAULT_FROM";
	final static String SEND_DEFAULT_TO = "SEND_DEFAULT_TO";
	final static String VERSION = "VERSION";
	final static String ACTION = "ACTION";
	final static String MESSAGE_ID = "MESSAGE_ID";
	final static String FAULT_TO = "FAULT_TO";
	final static String FROM = "FROM";
	final static String TO = "TO";
	final static String MUST_UNDERSTAND = "MUST_UNDERSTAND";
	final static String REPLY_TO = "REPLY_TO";
	final static String REFERENCE_PARAMETERS = "REFERENCE_PARAMETERS";
	final static String REFERENCE_PROPERTIES = "REFERENCE_PROPERTIES";
//	final static String RELATES_TO = "RELATES_TO";
	
	
	private Modifier addressingModifier;
	
	public WebServiceAddressingContext() {
		addressingModifier = new Modifier(WEBSERVICE_ADDRESSING_MODIFIER);
	}
	
	WebServiceAddressingContext(Modifier addressingModifier) {
		this.addressingModifier = addressingModifier;
	}
	 
	public void setSendDefaultMessageID(Boolean sendDefault) {
		addressingModifier.setProperty(SEND_DEFAULT_MESSAGE_ID, sendDefault);
	}
	
	public Boolean isSendDefaultMessageID() {
		return (Boolean)addressingModifier.getProperty(SEND_DEFAULT_MESSAGE_ID);
	}
	
	public void setSendDefaultFrom(Boolean sendDefault) {
		addressingModifier.setProperty(SEND_DEFAULT_FROM, sendDefault);
	}
	
	public Boolean isSendDefaultFrom() {
		return (Boolean)addressingModifier.getProperty(SEND_DEFAULT_FROM);
	}
	
	public void setSendDefaultTo(Boolean sendDefault) {
		addressingModifier.setProperty(SEND_DEFAULT_TO, sendDefault);
	}
	
	public Boolean isSendDefaultTo() {
		return (Boolean)addressingModifier.getProperty(SEND_DEFAULT_TO);
	}
	
	public void setVersion(String version) {
		addressingModifier.setProperty(VERSION, version);
	}
	
	public String getVersion() {
		return (String)addressingModifier.getProperty(VERSION);
	}
	
	public void setAction(String uri) {
		addressingModifier.setProperty(ACTION, uri);
	}
	
	public String getAction() {
		return (String)addressingModifier.getProperty(ACTION);
	}
	
	public void setMessageID(String messageID) {
		addressingModifier.setProperty(MESSAGE_ID, messageID);
	}
	
	public String getMessageID() {
		return (String)addressingModifier.getProperty(MESSAGE_ID);
	}
	
	public void setFaultTo(String uri) {
		addressingModifier.setProperty(FAULT_TO, uri);
	}
	
	public String getFaultTo() {
		return (String)addressingModifier.getProperty(FAULT_TO);
	}
	
	public void setFrom(String uri) {
		addressingModifier.setProperty(FROM, uri);
	}
	
	public String getFrom() {
		return (String)addressingModifier.getProperty(FROM);
	}
	
	public void setTo(String uri) {
		addressingModifier.setProperty(TO, uri);
	}
	
	public String getTo() {
		return (String)addressingModifier.getProperty(TO);
	}
	
	public void setMustUnderstand(Boolean mustUnderstand) {
		addressingModifier.setProperty(MUST_UNDERSTAND, mustUnderstand);
	}
	
	public Boolean isMustUnderstand() {
		return (Boolean)addressingModifier.getProperty(MUST_UNDERSTAND);
	}
	
	public void setReplyTo(String uri) {
		addressingModifier.setProperty(REPLY_TO, uri);
	}
	
	public String getReplyTo() {
		return (String)addressingModifier.getProperty(REPLY_TO);
	}
	
//	public void setReferenceParameters(ReferenceParametersType params) {
//		addressingModifier.setProperty(REFERENCE_PARAMETERS, params);
//	}
//	
//	public ReferenceParametersType getReferenceParameters() {
//		return (ReferenceParametersType)addressingModifier.getProperty(REFERENCE_PARAMETERS);
//	}
//	
//	public void setReferenceProperties(ReferencePropertiesType props) {
//		addressingModifier.setProperty(REFERENCE_PROPERTIES, props);
//	}
//	
//	public ReferencePropertiesType getReferenceProperties() {
//		return (ReferencePropertiesType)addressingModifier.getProperty(REFERENCE_PROPERTIES);
//	}
//	
//	public void setRelatesTo(java.util.List<RelatesTo> relatesTo) {
//		addressingModifier.setProperty(RELATES_TO, relatesTo);
//	}
//	
//	public java.util.List<RelatesTo> getRelatesTo() {
//		return (java.util.List<RelatesTo>)addressingModifier.getProperty(RELATES_TO);
//	}
	
	// Helper
	public void apply(List modifiers) {
		modifiers.add(addressingModifier);
	}
}
