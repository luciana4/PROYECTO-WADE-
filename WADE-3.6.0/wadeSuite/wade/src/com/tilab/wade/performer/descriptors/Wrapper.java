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
package com.tilab.wade.performer.descriptors;

import jade.util.leap.Serializable;

/**
 * This class has only an internal use.
 * Is used to wrap values (of type AbsObject) contained in the Parameter class.
 * For this class there isn't an ontology representation, 
 * so during encoding and decoding the contained value is serialized and unmanaged in ontologically way, 
 * this allows to manipulate a Parameter not even in the presence of a reference ontology.
 *  
 * @see com.tilab.wade.performer.descriptors.Parameter  
 */
class Wrapper implements Serializable {

	private Object value;
	
	Wrapper(Object value) {
		this.value = value;
	}
	
	Object getValue() {
		return value;
	}
}
