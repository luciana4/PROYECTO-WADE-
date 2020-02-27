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
package com.tilab.wade.cfa.beans;

import jade.content.Concept;
import java.util.Map;
import java.util.HashMap;

/**
 * 
 * @author max
 *Classe astratta semantica per la parte Platform
 */

public abstract class PlatformElement implements Concept{

	public final static String COMPARISON_MISMATCH = "mismatch";
	public final static String COMPARISON_NEW = "new";
	public final static String COMPARISON_MISSING = "missing";
	public final static String COMPARISON_MOVED = "moved";

    private String errorCode;
    private Map extendedAttributes;

    public String getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public Map getExtendedAttributes() {
    	if (extendedAttributes == null) {
    		// Lazy creation
    		extendedAttributes = new HashMap();
    	}
    	return extendedAttributes;
    }
    
    public void setExtendedAttributes(Map extendedAttributes) {
    	this.extendedAttributes = extendedAttributes;
    }
}
