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

import java.util.Date;

public class ComparisonHelper {
	
	public static boolean equals(Object obj1, Object obj2) {
		if (obj1 == obj2) {
			return true;
		}
		
		if (obj1 == null) {
			return obj2 == null;
		}

		// Compare numbers
		if (Number.class.isAssignableFrom(obj1.getClass()) && Number.class.isAssignableFrom(obj2.getClass()) && obj1.getClass() != obj2.getClass()) {
			return Double.parseDouble(obj1.toString()) == Double.parseDouble(obj2.toString());
		}
		
		// Uniform date object
		boolean obj1IsDate = obj1 instanceof Date;
		boolean obj2IsDate = obj2 instanceof Date;
		if (obj1IsDate ^ obj2IsDate) {
			if (obj1IsDate) {
				obj2 = BasicOntology.adjustPrimitiveValue(obj2, Date.class);
			} else {
				obj1 = BasicOntology.adjustPrimitiveValue(obj1, Date.class);
			}
		}
		
		// Compare objects
		return obj1.equals(obj2);
	}

	public static boolean notEquals(Object obj1, Object obj2) {
		return !equals(obj1, obj2);
	}
	
	public static boolean gt(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null) {
			return false;
		}

		// Compare numbers
		if (Number.class.isAssignableFrom(obj1.getClass()) && Number.class.isAssignableFrom(obj2.getClass())) {
			return Double.parseDouble(obj1.toString()) > Double.parseDouble(obj2.toString());
		}

		// Uniform date object
		boolean obj1IsDate = obj1 instanceof Date;
		boolean obj2IsDate = obj2 instanceof Date;
		if (obj1IsDate ^ obj2IsDate) {
			if (obj1IsDate) {
				obj2 = BasicOntology.adjustPrimitiveValue(obj2, Date.class);
			} else {
				obj1 = BasicOntology.adjustPrimitiveValue(obj1, Date.class);
			}
		}
		
		// Compare objects
		if (obj1.getClass() == obj2.getClass() && Comparable.class.isAssignableFrom(obj1.getClass()) && Comparable.class.isAssignableFrom(obj2.getClass())) {
			return ((Comparable)obj1).compareTo((Comparable)obj2) > 0;
		}

		return false;
	}
	
	public static boolean lt(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null) {
			return false;
		}

		// Compare numbers
		if (Number.class.isAssignableFrom(obj1.getClass()) && Number.class.isAssignableFrom(obj2.getClass())) {
			return Double.parseDouble(obj1.toString()) < Double.parseDouble(obj2.toString());
		}

		// Uniform date object
		boolean obj1IsDate = obj1 instanceof Date;
		boolean obj2IsDate = obj2 instanceof Date;
		if (obj1IsDate ^ obj2IsDate) {
			if (obj1IsDate) {
				obj2 = BasicOntology.adjustPrimitiveValue(obj2, Date.class);
			} else {
				obj1 = BasicOntology.adjustPrimitiveValue(obj1, Date.class);
			}
		}
		
		// Compare objects
		if (obj1.getClass() == obj2.getClass() && Comparable.class.isAssignableFrom(obj1.getClass()) && Comparable.class.isAssignableFrom(obj2.getClass())) {
			return ((Comparable)obj1).compareTo((Comparable)obj2) < 0;
		}

		return false;
	}

	public static boolean ge(Object obj1, Object obj2) {
		return gt(obj1, obj2) || equals(obj1, obj2);
	}
	
	public static boolean le(Object obj1, Object obj2) {
		return lt(obj1, obj2) || equals(obj1, obj2);
	}
}
