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
package com.tilab.wade.utils;

import jade.util.Logger;

import java.util.List;
import java.util.Arrays;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by IntelliJ IDEA.
 * User: 00917598
 * Date: 21-giu-2006
 * Time: 17.19.14
 * To change this template use File | Settings | File Templates.
 */
public class UriUtils {
	private static Logger logger = Logger.getMyLogger(UriUtils.class.getName());

	public final static String LOCALHOST = "localhost";
	public final static String PORT_SEPARATOR = ":";
	public final static int DEFAULT_PORT = -1;

	public static List toList(String s, String separator) {
		return Arrays.asList(s.split(separator));
	}

	/**
	 * Returns the hostname of local host, converted to lowercase.
	 *
	 * @return name of localhost
	 */
	public static String getLocalCanonicalHostname() {
		String hostname = null;
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
		} catch (UnknownHostException uhe) {
			logger.log(Logger.SEVERE,"getLocalCanonicalHostname(): no IP address for localhost!", uhe);
		}
		return hostname;
	}

	/**
	 * Compares two host names regardless of whether they include domain or not.
	 * Note: this method does not work if "localhost" is used as host name.
	 */
	public static boolean compareHostNames(String host1, String host2) {
		if (host1.equalsIgnoreCase(host2)) {
			return true;
		}

		if (host1.equalsIgnoreCase(LOCALHOST)) {
			try {
				host1 = InetAddress.getLocalHost().getHostName();
			}
			catch (Exception e) {
				logger.log(Logger.WARNING, "Cannot retrieve local host name.", e);
			}
		}
		if (host2 != null && host2.equalsIgnoreCase(LOCALHOST)) {
			try {
				host2 = InetAddress.getLocalHost().getHostName();
			}
			catch (Exception e) {
				logger.log(Logger.WARNING, "Cannot retrieve local host name.", e);
			}
		}

		try {
			InetAddress host1Addrs[] = InetAddress.getAllByName(host1);
			InetAddress host2Addrs[] = InetAddress.getAllByName(host2);

			// The trick here is to compare the InetAddress
			// objects, not the strings since the one string might be a
			// fully qualified Internet domain name for the host and the
			// other might be a simple name.
			// Example: myHost.hpl.hp.com and myHost might
			// acutally be the same host even though the hostname strings do
			// not match.  When the InetAddress objects are compared, the IP
			// addresses will be compared.
			int i = 0;
			boolean isEqual = false;

			while ((!isEqual) && (i < host1Addrs.length)) {
				int j = 0;

				while ((!isEqual) && (j < host2Addrs.length)) {
					isEqual = host1Addrs[i].equals(host2Addrs[j]);
					j++;
				}

				i++;
			}
			return isEqual;
		}
		catch (UnknownHostException uhe) {
			// An unknown host is certainly false
			return false;
		}
	}

	/**
	 * Returns a string composed by the FQDN of local host and the specified port separated by ":"
	 * (example: "antbala.cselt.it:8085").
	 *
	 * @param port
	 * @return String
	 */
	public static String getLocalUrlString(int port) {
		return getLocalCanonicalHostname()+PORT_SEPARATOR+port;
	}

	/**
	 * Returns a string composed by the FQDN of local host and the specified port separated by ":"
	 * (example: "antbala.cselt.it:8085"). The port is converted to an integer before being appended
	 * to the string.
	 *
	 * @param port
	 * @return String
	 */
	public static String getLocalUrlString(String port) {
		String hostname = null;
		hostname = getLocalUrlString(Integer.parseInt(port));
		return hostname;
	}

	/**
	 * Extracts host name from a string with format "some.host.name:some_port".
	 *
	 * @param urlStr
	 * @return String
	 */
	public static String getHostnameFromUrlString(String urlStr) {
		String hostname = urlStr;
		int i = urlStr.lastIndexOf(PORT_SEPARATOR);
		if (i >= 0) {
			hostname = urlStr.substring(0, i);
		}
		return hostname;
	}

	/**
	 * Extracts port from a string with format "some.host.name:some_port".
	 *
	 * @param urlStr
	 * @return String
	 */
	public static int getPortFromUrlString(String urlStr) {
		int port = DEFAULT_PORT;
		int i = urlStr.lastIndexOf(PORT_SEPARATOR);
		if (i >= 0) {
			String portStr = null;
			portStr = urlStr.substring(i+1, urlStr.length());
			port = Integer.parseInt(portStr);
		}
		return port;
	}

	/**
	 * Convert hostname into ip
	 * 
	 * @param hostname
	 * @return ip
	 */
	public static String hostsname2IP(String hostname) {
		String ip = null;
		try {
			ip = InetAddress.getByName(hostname).getHostAddress();
		} catch (UnknownHostException uhe) {
			logger.log(Logger.WARNING, "hostsname2IP(): hostname not correct!", uhe);
		}
		return ip;
	}

	/**
	 * Returns the ip of local host.
	 *
	 * @return ip
	 */
	public static String getLocalIP() {
		String ip = null;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhe) {
			logger.log(Logger.WARNING, "getLocalIP(): no IP address for localhost!", uhe);
		}
		return ip;
	}

	/*
	 * test method
	 */
	private static void test() {
		System.out.println("getLocalCanonicalHostname()=\""+getLocalCanonicalHostname()+"\"");
		System.out.println("getLocalUrlString(1234)=\""+getLocalUrlString(1234)+"\"");
		System.out.println("getLocalUrlString(\"5678\")=\""+getLocalUrlString("5678")+"\"");
		System.out.println("getHostnameFromUrlString(\"antbala.cselt.it:4566\")=\""+getHostnameFromUrlString("antbala.cselt.it:4566")+"\"");
		System.out.println("getPortFromUrlString(\"antbala.cselt.it:4566\")="+getPortFromUrlString("antbala.cselt.it:4566"));
		System.out.println("getHostnameFromUrlString(\"antbala.cselt.it\")=\""+getHostnameFromUrlString("antbala.cselt.it")+"\"");
		System.out.println("getPortFromUrlString(\"antbala.cselt.it\")="+getPortFromUrlString("antbala.cselt.it"));
	}

	public static void main(String[] args) throws Exception {
		test();
	}

}
