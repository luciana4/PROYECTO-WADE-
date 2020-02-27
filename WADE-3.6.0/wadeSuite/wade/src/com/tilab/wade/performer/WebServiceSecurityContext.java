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

import jade.content.onto.BasicOntology;
import jade.util.leap.List;
import jade.util.leap.Serializable;

import com.tilab.wade.performer.ontology.Modifier;

public class WebServiceSecurityContext implements Serializable {

	public final static String PW_TEXT = "PasswordText";
	public final static String PW_DIGEST = "PasswordDigest";

	final static String WEBSERVICE_SECURITY_MODIFIER = "WEBSERVICE_SECURITY_MODIFIER";
	final static String ACTIVITY_ID = "ACTIVITY_ID";
	final static String ACTIVITY_WEBSERVICE_SECURITY_MODIFIER = "ACTIVITY_WEBSERVICE_SECURITY_MODIFIER";
	final static String HTTP_USERNAME = "HTTP_USERNAME";
	final static String HTTP_PASSWORD = "HTTP_PASSWORD";
	final static String WSDL_HTTP_USERNAME = "WSDL_HTTP_USERNAME";
	final static String WSDL_HTTP_PASSWORD = "WSDL_HTTP_PASSWORD";
	final static String WSS_USERNAME = "WSS_USERNAME";
	final static String WSS_PASSWORD = "WSS_PASSWORD";
	final static String WSS_PASSWORD_TYPE = "WSS_PASSWORD_TYPE";
	final static String WSS_MUST_UNDERSTAND = "WSS_MUST_UNDERSTAND";
	final static String WSS_TIME_TO_LIVE = "WSS_TIME_TO_LIVE";
	final static String TRUST_STORE = "TRUST_STORE";
	final static String TRUST_STORE_PASSWORD = "TRUST_STORE_PASSWORD";
	final static String CERTIFICATE_CHEKING = "CERTIFICATE_CHEKING";

	private Modifier wssModifier;
	
	/**
	 * Create a default WebServiceSecurityContext
	 */
	public WebServiceSecurityContext() {
		wssModifier = new Modifier(WEBSERVICE_SECURITY_MODIFIER);
		wssModifier.setProperty(ACTIVITY_ID, null);
	}

	/**
	 * Create a activity specific WebServiceSecurityContext 
	 * @param activityId activity id
	 */
	public WebServiceSecurityContext(String activityId) {
		wssModifier = new Modifier(ACTIVITY_WEBSERVICE_SECURITY_MODIFIER+"_"+activityId);
		wssModifier.setProperty(ACTIVITY_ID, activityId);
	}
	
	WebServiceSecurityContext(Modifier wssModifier) {
		this.wssModifier = wssModifier;
	}

	// Activity ID
	public String getActivityId() {
		return (String)wssModifier.getProperty(ACTIVITY_ID);
	}

	// HTTP Basic Authentication
	public void setHttpUsername(String httpUsername) {
		wssModifier.setProperty(HTTP_USERNAME, httpUsername);
	}

	public String getHttpUsername() {
		return (String)wssModifier.getProperty(HTTP_USERNAME);
	}
	
	public void setHttpPassword(String httpPassword) {
		wssModifier.setProperty(HTTP_PASSWORD, httpPassword);
	}

	public String getHttpPassword() {
		return (String)wssModifier.getProperty(HTTP_PASSWORD);
	}
	
	public void setWsdlHttpUsername(String wsdlHttpUsername) {
		wssModifier.setProperty(WSDL_HTTP_USERNAME, wsdlHttpUsername);
	}

	public String getWsdlHttpUsername() {
		return (String)wssModifier.getProperty(WSDL_HTTP_USERNAME);
	}
	
	public void setWsdlHttpPassword(String wsdlHttpPassword) {
		wssModifier.setProperty(WSDL_HTTP_PASSWORD, wsdlHttpPassword);
	}

	public String getWsdlHttpPassword() {
		return (String)wssModifier.getProperty(WSDL_HTTP_PASSWORD);
	}
	
	// WS-Security Username Token profile
	public void setWSSUsername(String wssUsername) {
		wssModifier.setProperty(WSS_USERNAME, wssUsername);
	}

	public String getWSSUsername() {
		return (String)wssModifier.getProperty(WSS_USERNAME);
	}
	
	public void setWSSPassword(String wssPassword) {
		wssModifier.setProperty(WSS_PASSWORD, wssPassword);
	}

	public String getWSSPassword() {
		return (String)wssModifier.getProperty(WSS_PASSWORD);
	}

	public void setWSSPasswordType(String wssPasswordType) {
		wssModifier.setProperty(WSS_PASSWORD_TYPE, wssPasswordType);
	}

	public String getWSSPasswordType() {
		return (String)wssModifier.getProperty(WSS_PASSWORD_TYPE);
	}
	
	public void setWSSMustUnderstand(Boolean wssMustUnderstand) {
		wssModifier.setProperty(WSS_MUST_UNDERSTAND, wssMustUnderstand);
	}

	public Boolean isWSSMustUnderstand() {
		return (Boolean)wssModifier.getProperty(WSS_MUST_UNDERSTAND);
	}

	public void setWSSTimeToLive(Integer wssTimeToLive) {
		wssModifier.setProperty(WSS_TIME_TO_LIVE, wssTimeToLive);
	}

	public Integer getWSSTimeToLive() {
		return (Integer)BasicOntology.adjustPrimitiveValue(wssModifier.getProperty(WSS_TIME_TO_LIVE), Integer.class);
	}
	
	// Certificates
	public void setTrustStore(String trustStore) {
		wssModifier.setProperty(TRUST_STORE, trustStore);
	}

	public String getTrustStore() {
		return (String)wssModifier.getProperty(TRUST_STORE);
	}
	
	public void setTrustStorePassword(String trustStorePassword) {
		wssModifier.setProperty(TRUST_STORE_PASSWORD, trustStorePassword);
	}

	public String getTrustStorePassword() {
		return (String)wssModifier.getProperty(TRUST_STORE_PASSWORD);
	}
	
	public void setCertificateChecking(Boolean certificateChecking) {
		wssModifier.setProperty(CERTIFICATE_CHEKING, certificateChecking);
	}

	public Boolean isEnableCertificateChecking() {
		return (Boolean)wssModifier.getProperty(CERTIFICATE_CHEKING);
	}
	
	// Helper
	public void apply(List modifiers) {
		modifiers.add(wssModifier);
	}
}
