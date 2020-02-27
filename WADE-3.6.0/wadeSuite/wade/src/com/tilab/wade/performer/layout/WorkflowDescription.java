package com.tilab.wade.performer.layout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WorkflowDescription {
	public static final String NULL = "__NULL__";
	public static final String DEFAULT_COLOR = "DEFAULT_COLOR";

	String name() default NULL;
	String documentation() default NULL;
	String category() default NULL;
	boolean component() default false;
	String icon() default NULL;
	String color() default DEFAULT_COLOR; // es: (80,70,50)
}
