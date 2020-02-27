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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Fields of BaseApplication-s, inline subflows and other building blocks 
 * whose internal status is defined in terms of fields of a Java class, are 
 * managed by default by the WADE framework as follows:
 * - They are automatically cleared just before each new invocation that occurs
 * during the execution of a workflow.
 * - In case they are invoked within a transaction, the values they had
 * at the end of invocation # N is automatically restored just before 
 * committing/rolling-back the transaction entry corresponding to invocation # N.
 * 
 * The UmanagedField annotation allows marking a field as "unmanaged" that is 
 * the behaviour described above is disabled.
 * 
 * @author 00917536
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UnmanagedField {

}
