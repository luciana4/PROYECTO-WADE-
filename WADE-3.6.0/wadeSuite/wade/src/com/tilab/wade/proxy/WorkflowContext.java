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

import java.util.ArrayList;
import java.util.List;

import com.tilab.wade.performer.DefaultParameterValues;
import com.tilab.wade.performer.WebServiceAddressingContext;
import com.tilab.wade.performer.WebServiceSecurityContext;

/**
 *  Workflow execution context
 */
public class WorkflowContext {
	
	private DefaultParameterValues dpv;
	private WebServiceSecurityContext defaultWssc;
	private List<WebServiceSecurityContext> activitiesWssc;
	private WebServiceAddressingContext defaultWsa;
	private List<WebServiceAddressingContext> activitiesWsa;
	
	/**
	 * Get the default parameter values
	 * @return default parameter values
	 */
	public DefaultParameterValues getDefaultParameterValues() {
		return dpv;
	}
	
	/**
	 * Set the default parameter values
	 * @param dpv default parameter values
	 */
	public void setDefaultParameterValues(DefaultParameterValues dpv) {
		this.dpv = dpv;
	}
	
	/**
	 * Get the default web-service security context
	 * @return default web-service security context
	 */
	public WebServiceSecurityContext getWebServiceDefaultSecurityContext() {
		return defaultWssc;
	}
	
	/**
	 * Set the default web-service security context
	 * @param defaultWssc default wssc web-service security context
	 */
	public void setWebServiceDefaultSecurityContext(WebServiceSecurityContext defaultWssc) {
		this.defaultWssc = defaultWssc;
	}
	
	/**
	 * Get the list of activities web-service security context
	 * @return web-service security context
	 */
	public List<WebServiceSecurityContext> getWebServiceActivitiesSecurityContext() {
		return activitiesWssc;
	}

	/**
	 * Set the list of activities web-service security context
	 * @param activitiesWssc activities wssc web-service security context list
	 */
	public void setWebServiceActivitiesSecurityContext(List<WebServiceSecurityContext> activitiesWssc) {
		this.activitiesWssc = activitiesWssc;
	}
	
	/**
	 * Add an activity web-service security context to list
	 * @param activityWssc activity wssc web-service security context
	 */
	public void addWebServiceActivitySecurityContext(WebServiceSecurityContext activityWssc) {
		if (activitiesWssc == null) {
			activitiesWssc = new ArrayList<WebServiceSecurityContext>();
		}
		activitiesWssc.add(activityWssc);
	}
	
	/**
	 * Get the default web-service addressing context
	 * @return default web-service addressing context
	 */
	public WebServiceAddressingContext getWebServiceAddressingContext() {
		return defaultWsa;
	}
	
	/**
	 * Set the default web-service addressing context
	 * @param defaultWsa default web-service addressing context
	 */
	public void setWebServiceAddressingContext(WebServiceAddressingContext defaultWsa) {
		this.defaultWsa = defaultWsa;
	}
	
	/**
	 * Get the list of activities web-service addressing context
	 * @return web-service addressing context
	 */
	public List<WebServiceAddressingContext> getWebServiceActivitiesAddressingContext() {
		return activitiesWsa;
	}

	/**
	 * Set the list of activities web-service addressing context
	 * @param activitiesWsa activities web-service addressing context list
	 */
	public void setWebServiceActivitiesAddressingContext(List<WebServiceAddressingContext> activitiesWsa) {
		this.activitiesWsa = activitiesWsa;
	}
	
	/**
	 * Add an activity web-service addressing context to list
	 * @param activityWsa activity web-service addressing context
	 */
	public void addWebServiceActivityAddressingContext(WebServiceAddressingContext activityWsa) {
		if (activitiesWsa == null) {
			activitiesWsa = new ArrayList<WebServiceAddressingContext>();
		}
		activitiesWsa.add(activityWsa);
	}
}
