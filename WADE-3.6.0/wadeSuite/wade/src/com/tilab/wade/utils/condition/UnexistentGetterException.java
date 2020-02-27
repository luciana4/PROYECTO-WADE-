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

public class UnexistentGetterException extends GetterFactoryException {

	static final long serialVersionUID = 3124780739663520852L;

	public UnexistentGetterException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnexistentGetterException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UnexistentGetterException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UnexistentGetterException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
