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
package com.tilab.wade.utils.condition;

import java.util.regex.Pattern;

public class StringMatcher implements FilterMatcher {

	private Type type;
	private String matchExpr;
	private Getter stringGetter = null;
	private Pattern pattern;
	static private String WILDCARD = "*";

	public StringMatcher(Type type, Getter stringGetter, String matchExpr) throws MatcherException {
		try {
			this.type = type;
			this.stringGetter = stringGetter;
			setMatchExpr(matchExpr);
		} catch (Exception e) {
			throw new MatcherException(e);
		}
	}

	public void setMatchExpr(String matchExpr) {
		if ((this.matchExpr == null && matchExpr != null) || (this.matchExpr != null && (!this.matchExpr.equals(matchExpr))))
		{
			this.matchExpr = matchExpr;
			if (type == Type.REGEXMATCH || type == Type.REGEXMATCHALL) {
				pattern = Pattern.compile(matchExpr);
			}
		}
	}

	public void setType(Type type) {
		if (this.type != type) {
			this.type = type;
			this.matchExpr = null;
			this.pattern = null;
		}
	}

	public boolean matches(Object obj) throws MatcherException {
		if (matchExpr == null) {
			throw new MatcherException("match expression not set");
		}
		String value = (String) stringGetter.getValue(obj);
		if (value == null) {
			return false;
		}
		switch (type) {
			case EQ:
				return matchExpr.equals(WILDCARD) || value.equals(matchExpr);
			case NE:
				return !value.equals(matchExpr);
			case GT:
				return value.compareTo(matchExpr) > 0;
			case GE:
				return value.compareTo(matchExpr) >= 0;
			case LT:
				return value.compareTo(matchExpr) < 0;
			case LE:
				return value.compareTo(matchExpr) <= 0;
			case REGEXMATCH:
				return pattern.matcher(value).find();
			case REGEXMATCHALL:
				return pattern.matcher(value).matches();
			default:
				throw new MatcherException("unassigned type");
		}
	}
}
