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

//#J2ME_EXCLUDE_FILE

import jade.content.schema.ObjectSchema;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tilab.wade.performer.descriptors.Parameter;

@Retention(RetentionPolicy.RUNTIME)
public @interface FormalParameter {
	public static final String NULL = "__NULL__";
	
	public static final int INPUT = Constants.IN_MODE;
	public static final int OUTPUT = Constants.OUT_MODE;
	public static final int INOUT = Constants.INOUT_MODE;
	
	public static final int UNINDEXED = -1;
	
	public static final int UNBOUNDED = Parameter.UNBOUNDED;
	public static final int UNCARDINALIZED = -2;
	
	int mode() default INPUT;
	int index() default UNINDEXED;
	String defaultValue() default NULL;
	boolean mandatory() default true;
	String regex() default NULL;
	int cardMin() default UNCARDINALIZED;
	int cardMax() default UNCARDINALIZED;
	String documentation() default NULL;
	String[] permittedValues() default {};
	Class elementType() default Object.class;
}
