package com.tilab.wade.performer.descriptors.webservice;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tilab.wade.performer.descriptors.webservice.ServiceDescriptor.ServiceUsage;

@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceMetaInfo {
	public static final String NULL = "__NULL__";
	
	ServiceUsage usage();
	String purpose() default NULL;
	String jarName() default NULL;
}
