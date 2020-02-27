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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tilab.wade.utils.condition.FilterMatcher.Type;

public class Condition {

	public final static String ELEMENT_SEPARATOR = ";";
	public final static int ELEMENT_SEPARATOR_LENGTH = ELEMENT_SEPARATOR.length();
	public final static String ESCAPED_ELEMENT_SEPARATOR = "\\"+ELEMENT_SEPARATOR;
	public final static int ESCAPED_ELEMENT_SEPARATOR_LENGTH = ESCAPED_ELEMENT_SEPARATOR.length();

	private final static String FIELDNAME = "[A-Za-z_][A-Za-z0-9_.-]*";
	private final static String OPEQUALITY = "[=!]=";
	private final static String OPINEQUALITY = ">={0,1}|<={0,1}";
	private final static String OPREGEXPMATCH = "/|%";
	private final static String OPGENERIC = OPEQUALITY+"|"+OPINEQUALITY+"|"+OPREGEXPMATCH;
	//private final static String OPINTEGER = OPEQUALITY+"|"+OPINEQUALITY;
	private final static String VALGENERIC = ".*";
	//private final static String VALINTEGER = "[0-9]+";

	private final static Pattern ELEMENT_SEPARATOR_PATTERN = Pattern.compile("(?<!\\\\);");

	private final static Pattern genericFieldFilterPattern =
		Pattern.compile("^("+FIELDNAME+")\\s*("+OPGENERIC+")\\s*("+VALGENERIC+")$");

	// JAVA5
	private List<FilterMatcher> matchers;
	// JAVA1.4
	//private List matchers;
	private String filterString;

	private GetterFactory getterFactory = null;

	public static String escapeValue(String value) {
		StringBuffer sb = new StringBuffer();
		int i, j;

		if (value == null) {
			return null;
		} else if (value.length() == 0) {
			return value;
		}

		String s;
		j = 0;
		i = value.indexOf(ELEMENT_SEPARATOR);
		while (i >= 0) {
			s = value.substring(j, i);
			sb.append(s);
			sb.append(ESCAPED_ELEMENT_SEPARATOR);
			j = i+ELEMENT_SEPARATOR_LENGTH;
			i = value.indexOf(ESCAPED_ELEMENT_SEPARATOR, j);
		}
		sb.append(value.substring(j));
		return sb.toString();
	}

	protected static String unescapeValue(String value) {
		StringBuffer sb = new StringBuffer();
		int i, j;

		if (value == null) {
			return null;
		} else if (value.length() == 0) {
			return value;
		}

		String s;
		j = 0;
		i = value.indexOf(ESCAPED_ELEMENT_SEPARATOR);
		while (i >= 0) {
			s = value.substring(j, i);
			sb.append(s);
			sb.append(ELEMENT_SEPARATOR);
			j = i+ESCAPED_ELEMENT_SEPARATOR_LENGTH;
			i = value.indexOf(ESCAPED_ELEMENT_SEPARATOR, j);
		}
		sb.append(value.substring(j));
		return sb.toString();
	}

	private static Type getType(String op) {
		if (op.equals("==")) {
			return Type.EQ;
		} else if (op.equals("!=")) {
			return Type.NE;
		} else if (op.equals(">")) {
			return Type.GT;
		} else if (op.equals(">=")) {
			return Type.GE;
		} else if (op.equals("<")) {
			return Type.LT;
		} else if (op.equals("<=")) {
			return Type.LE;
		} else if (op.equals("/")) {
			return Type.REGEXMATCH;
		} else if (op.equals("%")) {
			return Type.REGEXMATCHALL;
		} else {
			// TODO throw some exception
			return null;
		}
	}

	public Condition(GetterFactory getterFactory) throws FilterException {
		this(getterFactory, "");
	}

	public Condition(GetterFactory getterFactory, String filterDescription) throws FilterException {
		// JAVA5
		matchers = new LinkedList<FilterMatcher>();
		// JAVA1.4
		//matchers = new LinkedList();
		this.getterFactory = getterFactory;
		set(filterDescription);
	}

	public void clear() {
		matchers.clear();
		filterString = "";
	}

	public void append(String filterDescription) throws FilterException {
		List<FilterMatcher> matchersBackup = new LinkedList<FilterMatcher>(matchers);
		try {
			String [] matcherDescriptions = ELEMENT_SEPARATOR_PATTERN.split(filterDescription);
			for (int i = 0; i < matcherDescriptions.length; i++) {
				addFilterMatcher(matcherDescriptions[i]);
			}
		} catch (FilterException fe) {
			matchers = matchersBackup;
			throw fe;
		} catch (Exception e) {
			matchers = matchersBackup;
			throw new FilterException(e);
		}
	}

	public void set(String filterDescription) throws FilterException {
		clear();
		append(filterDescription);
	}

	/**
	 * 
	 * field == const
	 * field != const
	 * field > const
	 * field >= const
	 * field < const
	 * field <= const
	 * field / const 
	 * 
	 */
	protected FilterMatcher buildMatcher(String s) throws FilterException {
		FilterMatcher result = null;
		s = s.trim();
		Matcher m = genericFieldFilterPattern.matcher(s);
		if (!m.matches()) {
			throw new FilterException("syntax error in expression \""+s+"\"");
		}
		String field = m.group(1);
		Type type = getType(m.group(2));
		String value = unescapeValue(m.group(3));
		Getter getter;
		try {
			getter = getterFactory.createGetter(field);
		} catch (GetterFactoryException gfe) {
			throw new FilterException(gfe);
		}
		try {
			if (getter.getValueClass().equals(java.lang.String.class)) {
				result = new StringMatcher(type, getter, value);
			} else if (getter.getValueClass().equals(java.lang.Integer.class)) {
				result = new IntegerMatcher(type, getter, value);
			}
		} catch (MatcherException me) {
			throw new FilterException(me);
		}
		if (result == null) {
			throw new FilterException("field "+field+" has an unsupported getter");
		}
		return result;
	}

	protected void addFilterMatcher(FilterMatcher matcher) {
		matchers.add(matcher);
	}

	public void addFilterMatcher(String s) throws FilterException {
		List<FilterMatcher> matchersBackup = new LinkedList<FilterMatcher>(matchers);
		try {
			addFilterMatcher(buildMatcher(s));
			if (filterString.length() > 0) {
				filterString += ELEMENT_SEPARATOR;
			}
			filterString += s;
		} catch (FilterException fe) {
			matchers = matchersBackup;
			throw fe;
		} catch (Exception e) {
			matchers = matchersBackup;
			throw new FilterException(e);
		}
	}

	public boolean match(Object obj) throws MatcherException {
		Iterator<FilterMatcher> iter = matchers.iterator();
		FilterMatcher matcher;
		while (iter.hasNext()) {
			matcher = iter.next();
			if (!matcher.matches(obj)) {
				// early bail out
				return false;
			}
		}
		return true;
	}

	public String getFilterString() {
		return filterString;
	}

	public String toString() {
		return "Condition { \""+filterString+"\" }";
	}
}
