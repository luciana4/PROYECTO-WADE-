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
package com.tilab.wade.raa;

import com.tilab.wade.cfa.beans.AgentInfo;
import com.tilab.wade.utils.condition.Condition;
import com.tilab.wade.utils.condition.FilterException;
import com.tilab.wade.utils.condition.MatcherException;

class PolicyCondition {

	private String expression;
	private Condition filter;

	public PolicyCondition(String expression) throws ConfigurationException {
		parseExpression(expression.trim());
	}

	private void parseExpression(String expression) throws ConfigurationException {
		try {
			filter = new Condition(new AgentInfoGetterFactory(), expression);
			this.expression = expression;
		} catch (FilterException fe) {
			throw new ConfigurationException("cannot create PolicyCondition from expression "+expression, fe);
		}
	}

	public String getExpression() {
		return expression;
	}

	public String toString() {
		return "PolicyCondition {expression=\""+expression+"\"}";
	}

	public Condition getFilter() {
		return filter;
	}

	public boolean matches(AgentInfo ai) throws MatcherException {
		return filter.match(ai);
	}
}
