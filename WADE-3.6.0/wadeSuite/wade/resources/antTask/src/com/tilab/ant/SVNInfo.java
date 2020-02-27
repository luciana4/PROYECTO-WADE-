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
package com.tilab.ant;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SVNInfo {

	public static final String WCREV_KEY = "WCREV";
	public static final String WCDATE_KEY = "WCDATE";
	public static final String WCURL_KEY = "WCURL";
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	public static Map<String, String> getSvnInfo(String workingCopyPath) throws SVNException {
		SVNClientManager clientManager = SVNClientManager.newInstance();
		SVNWCClient wcClient = clientManager.getWCClient();
		File wcFile = new File(workingCopyPath);
		org.tmatesoft.svn.core.wc.SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);

		Map<String, String> props = new HashMap<String, String>(); 
		props.put(WCREV_KEY, Long.toString(svnInfo.getCommittedRevision().getNumber()));
		props.put(WCDATE_KEY, dateFormat.format(svnInfo.getCommittedDate()));
		props.put(WCURL_KEY, svnInfo.getURL().toString());
		
		return props;
	}
}
