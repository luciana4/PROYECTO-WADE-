#!/bin/bash

CHECK_JAVA()
{
    if [ -z "$JAVA_HOME" ]; then
	{
		echo "JAVA_HOME does not exist."
		echo "JAVA is needed to run this program!"
		exit 1
	}
	fi
	if [ -z "$JAVA_HOME" ] && [ "{JAVA_HOME+Nothing}" = "${JAVA_HOME+Nothing}" ]; then
	{
		echo "JAVA_HOME exists but is NULL."
		echo "JAVA is needed to run this program!"
		exit 2
	}
	fi
	if [ ! -f "$JAVA_HOME/bin/java" ]; then
	{
		echo "Cannot find JVM or JAVA_HOME is not correctly defined!"
		echo "The two elements are necessary to run this program."
		exit 3
	}
	fi
	export RUN_JAVA="$JAVA_HOME/bin/java"
	echo "JAVA_HOME = $JAVA_HOME"
	$RUN_JAVA -version
	echo "."
}

SET_STANDARD_PATHS()
{
	if [ -z "$WADE_HOME" ]; then
	{
		echo "No WADE_HOME env variable defined: get the WADE installation's dir from the call's path ..."
		export call_path=`dirname $0`
		echo "CALLPATH = $call_path"
		if [ -z "$call_path" -o "$call_path" = "." ] 
		then
			# assuming to be already into the 'bin' directory 
			cd ..
		else
			# 'bin' directory contains this script so grab the call's full path
			cd $call_path/..
		fi
		export WADE_HOME=$PWD
		cd $OLDPWD
	}
	fi
	export wade_addons=$WADE_HOME/add-ons
	export wade_lib=$WADE_HOME/lib
	export wade_cfg=$WADE_HOME/cfg
	echo "WADE_HOME = $WADE_HOME"
	echo "WADE_ADDONS = $wade_addons"
	echo "WADE_CFG = $wade_cfg"
	echo "WADE_LIB = $wade_lib"
}

LOAD_BOOT_DAEMON_PROPERTIES()
{
	tr -d '\r' < $wade_cfg/boot.properties > tmp
	mv -f tmp $wade_cfg/boot.properties

	QPort=`cat $wade_cfg/boot.properties | awk '/bootdaemon-port/ {print $1}' | awk '/bootdaemon-port/'`
	QPortOne=${QPort:0:1}
	if [ -z "$QPortOne" -o "$QPortOne" = "#" ]
      then
          export bootdaemon_port_option=""
      else
          export bootdaemon_port_option=-Dtsdaemon.port=${QPort:16:6}
    fi
    QName=`cat $wade_cfg/boot.properties | awk '/bootdaemon-name/ {print $1}' | awk '/bootdaemon-name/'`
	QNameOne=${QName:0:1}
	if [ -z "$QNameOne" -o "$QNameOne" = "#" ]
        then
            export bootdaemon_name_option=""
        else
            export bootdaemon_name_option="-Dtsdaemon.name="${QName:16:250}
    fi
    QRemotePort=`cat $wade_cfg/boot.properties | awk '/bootdaemon-remoteobjectport/ {print $1}' | awk '/bootdaemon-remoteobjectport/'`
	QRemotePortOne=${QRemotePort:0:1}
	if [ -z "$QRemotePortOne" -o "$QRemotePortOne" = "#" ]
        then
            export bootdaemon_remoteobjectport_option=""
        else
            export bootdaemon_remoteobjectport_option="-Dtsdaemon.remoteobjectport="${QRemotePort:28:6}
    fi
    QRMIServerHost=`cat $wade_cfg/boot.properties | awk '/bootdaemon-rmiserverhostname/ {print $1}' | awk '/bootdaemon-rmiserverhostname/'`
	QRMIServerHostOne=${QRMIServerHost:0:1}
	if [ -z "$QRMIServerHostOne" -o "$QRMIServerHostOne" = "#" ]
        then
            export bootdaemon_rmiserverhostname_option=""
        else
            export bootdaemon_rmiserverhostname_option="-Djava.rmi.server.hostname="${QRMIServerHost:29:250}
    fi
    QOutputFilter=`cat $wade_cfg/boot.properties | awk '/output-filter/ {print $1}' | awk '/output-filter/'`
	QOutputFilterOne=${QOutputFilter:0:1}
	if [ -z "$QOutputFilterOne" -o "$QOutputFilterOne" = "#" ]
        then
            export output_filter_option=""
        else
            export output_filter_option="-Doutput-filter="${QOutputFilter:14:250}
    fi
	
	# Additional, installation specific, JVM options may be declared with the WADE_BOOT_OPTS environment variable
	echo "WADE_BOOT_OPTS = $WADE_BOOT_OPTS"
	
	export bootdaemon_log_option="-Djava.util.logging.config.file=${wade_cfg}/log/bootdaemon-log.properties"     
	export wade_home_option="-Dwade-home=${WADE_HOME}"
	export jvm_args="${bootdaemon_port_option} ${bootdaemon_name_option} ${bootdaemon_remoteobjectport_option} ${bootdaemon_rmiserverhostname_option} ${bootdaemon_log_option} ${output_filter_option} ${wade_home_option} ${WADE_BOOT_OPTS}"
	echo "JVM-ARGS = $jvm_args"
	echo "."
}

BUILD_WADE_CLASSPATH()
{
	export class_path=$wade_cfg":"$wade_lib"/*"
}

BUILD_ADDONS_CLASSPATH()
{
	cd $wade_addons
	dirCnt=`ls -l . | grep '^d' | wc -l`
	if [ "$dirCnt" -ge 1 ]; then 
		for i in $(ls -d */)
			do
		{
			if [ -d "${i%%/}/cfg" ]; then
				{
					export class_path=$class_path":"$wade_addons"/"${i%%/}"/cfg"
				}
			fi
			if [ -d "${i%%/}/lib" ]; then
				{
					export class_path=$class_path":"$wade_addons"/"${i%%/}"/lib/*"
				}
			fi
		}
		done
	fi
}
	
START_BOOTDAEMON()
{
	cd $WADE_HOME

	if [ "$1" = "-s" ]
	then
		echo "Starting Boot Daemon in background"
  		nohup $RUN_JAVA -classpath "$class_path" $jvm_args com.tilab.wade.boot.BootDaemon > /dev/null 2>&1 & 		
		export BOOT_PID=$!
		echo "Boot Daemon PID = $BOOT_PID"
	else
		echo "Starting Boot Daemon in foreground"
		echo "................................................................................................................."
		$RUN_JAVA -classpath "$class_path" $jvm_args com.tilab.wade.boot.BootDaemon
	fi
}

set +x
clear
export CURRENT_DIR=$PWD
CHECK_JAVA
SET_STANDARD_PATHS
LOAD_BOOT_DAEMON_PROPERTIES
BUILD_WADE_CLASSPATH
BUILD_ADDONS_CLASSPATH
START_BOOTDAEMON $1
cd $CURRENT_DIR
exit 0
