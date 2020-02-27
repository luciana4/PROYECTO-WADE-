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

public class IntegerMatcher implements FilterMatcher {

	protected Getter integerGetter = null;
	protected Integer n;
	protected Type type;

	public IntegerMatcher(Type type, Getter integerGetter, Integer n) throws MatcherException {
		try {
			this.integerGetter = integerGetter;
			this.n = n;
		} catch (Exception e) {
			throw new MatcherException(e);
		}
		setType(type);
	}

	public IntegerMatcher(Getter integerGetter, Integer n) throws MatcherException {
		this(Type.EQ, integerGetter, n);
	}

	public IntegerMatcher(Type type, Getter integerGetter, String s) throws MatcherException {
		try {
			this.integerGetter = integerGetter;
			this.n = new Integer(Integer.parseInt(s));
		} catch (Exception e) {
			throw new MatcherException(e);
		}
		setType(type);
	}

	public IntegerMatcher(Getter integerGetter, String s) throws MatcherException {
		this(Type.EQ, integerGetter, s);
	}

	public void setMatchExpr(Integer n) {
		this.n = n;
	}

	public void setType(Type type) throws InvalidMatchTypeException {
		if (this.type != type) {
			if ((type == Type.REGEXMATCH) || (type == Type.REGEXMATCHALL)) {
				throw new InvalidMatchTypeException("invalid type "+type+" for IntegerMatcher");
			}
			this.type = type;
		}
	}

	public boolean matches(Object obj) {
		boolean result = false;
		int val1 = n.intValue();
		int val2 = ((Integer)integerGetter.getValue(obj)).intValue();
		switch(type) {
		case EQ:
			result = val2 == val1;
			break;
		case NE:
			result = val2 != val1;
			break;
		case GT:
			result = val2 > val1;
			break;
		case LT:
			result = val2 < val1;
			break;
		case GE:
			result = val2 >= val1;
			break;
		case LE:
			result = val2 <= val1;
			break;
		}
		return result;
	}

}
