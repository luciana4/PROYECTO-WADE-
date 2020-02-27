#!/bin/bash

CHECK_JAVA()
{
    if [ -z "$JAVA_HOME" ]; then
	{
		echo "JAVA_HOME does not exist."
		echo  "JAVA is needed to run this program!"
		exit 1
	}
	fi
	if [ -z "$JAVA_HOME" ] && [ "{JAVA_HOME+Nothing}" = "${JAVA_HOME+Nothing}"]; then
	{
		echo "JAVA_HOME exists but is NULL."
		echo  "JAVA is needed to run this program!"
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

PARSE_PARAMETERS()
{
	while test $# -gt 0; do
		case "$1" in
			-s)
				export param_service=true
				shift
				;;
			*)
				export param_project=$1
				shift
				;;
		esac
	done
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
	cd $WADE_HOME
	export wade_addons=$WADE_HOME/add-ons
	export wade_lib=$WADE_HOME/lib
	export wade_cfg=$WADE_HOME/cfg
	export wade_projects=$WADE_HOME/projects
	echo "WADE_HOME = $WADE_HOME"
	echo "WADE_ADDONS = $wade_addons"
	echo "WADE_CFG = $wade_cfg"
	echo "WADE_LIB = $wade_lib"
	echo "WADE_PROJECTS = $wade_projects"
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
	# Additional, installation specific, JVM options may be declared with the WADE_MAIN_OPTS environment variable
	echo "WADE_MAIN_OPTS = $WADE_MAIN_OPTS"
	
	export bootdaemon_log_option="-Djava.util.logging.config.file=${wade_cfg}/log/bootdaemon-log.properties"     
	export wade_home_option="-Dwade-home=${WADE_HOME}"
	export jvm_args="${bootdaemon_port_option} ${bootdaemon_name_option} ${bootdaemon_log_option} ${wade_home_option} ${WADE_MAIN_OPTS}"
	echo "JVM-ARGS = $jvm_args"
	echo "."
}

CHECK_PROJECT_PROPERTIES()
{
    if [ -z "$param_project" ]
    then
        LOAD_DEFAULT_PROPERTIES
    else
    { 
        export project_name=$param_project
        export project_properties=${wade_projects}/$param_project.properties
        if [ -e "$project_properties" ]; then
        {
			tr -d '\r' < ${project_properties} > tmp
			mv -f tmp ${project_properties}
	        LOAD_PROJECT_PROPERTIES
        }
		else
             echo "Project configuration file "$project_properties" not found"
             exit 4 
       fi
    }
    fi
}
	
LOAD_PROJECT_PROPERTIES()
{	
  	   QHome=`cat $project_properties | awk '/project-home/ {print $1}' | awk '/project-home/'`
	   QHomeOne=${QHome:0:1}
 	   if [ -z "$QHomeOne" -o "$QHomeOne" = "#" ]
             then
               export project_home=""
             else
               export project_home=${QHome:13:250}
        fi 
      QCfg=`cat $project_properties | awk '/project-cfg/ {print $1}' | awk '/project-cfg/'`
   	  QCfgOne=${QCfg:0:1}
	  if [ -z "$QCfgOne" -o "$QCfgOne" = "#" ]
            then
              export project_cfg=""
            else
              export project_cfg=${QCfg:12:250}
       fi
           QClasses=`cat $project_properties | awk '/project-classes/ {print $1}' | awk '/project-classes/'`
		  QClassesOne=${QClasses:0:1}
		  if [ -z "$QClassesOne" -o "$QClassesOne" = "#" ]
				 then
				   export project_classes=""
				 else
				   export project_classes=${QClasses:16:250}
          fi
        QLib=`cat $project_properties | awk '/project-lib/ {print $1}' | awk '/project-lib/'`
  	    QLibOne=${QLib:0:1}
  	    if [ -z "$QLibOne" -o "$QLibOne" = "#" ]
              then
                export project_lib=""
              else
                export project_lib=${QLib:12:250}
       fi  
	   
       if [ -e "$project_home" ]; then
	    {
            CHECK_PROJECT_PATH
        }
        else 
        {
            LOAD_DEFAULT_PROPERTIES
        }
       fi    
}

LOAD_DEFAULT_PROPERTIES()
{
        if [ -e "$project_home" ]; then
	    {
             CHECK_PROJECT_PATH
        }
        else
              export project_properties="${wade_projects}/default.properties"
			  tr -d '\r' < ${project_properties} > tmp
			  mv -f tmp ${project_properties}
			  
  	          QHome=`cat $project_properties | awk '/project-home/ {print $1}' | awk '/project-home/'`
      	      QHomeOne=${QHome:0:1}
 	          if [ -z "$QHomeOne" -o "$QHomeOne" = "#" ]
                    then
                      export project_home=""
                    else
                      export project_home=${QHome:13:250}
              fi 
      
              QCfg=`cat $project_properties | awk '/project-cfg/ {print $1}' | awk '/project-cfg/'`
   	          QCfgOne=${QCfg:0:1}
  	          if [ -z "$QCfgOne" -o "$QCfgOne" = "#" ]
                    then
                      export project_cfg=""
                    else
                      export project_cfg=${QCfg:12:250}
             fi
            QClasses=`cat $project_properties | awk '/project-classes/ {print $1}' | awk '/project-classes/'`
	        QClassesOne=${QClasses:0:1}
	        if [ -z "$QClassesOne" -o "$QClassesOne" = "#" ]
                  then
                   export project_classes=""
                  else
                   export project_classes=${QClasses:16:250}
            fi 
            QLib=`cat $project_properties | awk '/project-lib/ {print $1}' | awk '/project-lib/'`
  	        QLibOne=${QLib:0:1}
  	        if [ -z "$QLibOne" -o "$QLibOne" = "#" ]
                  then
                    export project_lib=""
                  else
                    export project_lib=${QLib:12:250}
            fi  
	    QHomeProject=${project_home:0:1}
	    QHomeProject1=${project_home:0:2}
	    if [ "$QHomeProject" = "." ]
              then
                if [ "$QHomeProject1" = ".." ]
		   then 		
		    cd $project_home
		    export project_home=`pwd`
 	            INIT_PROJECT_PROPERTIES
	           else             
                    export project_home=$WADE_HOME
                    INIT_PROJECT_PROPERTIES
                fi        
            fi
     fi
}

CHECK_PROJECT_PATH()
{       
	QHomeProject=${project_home:0:1}
	QHomeProject1=${project_home:0:2}
	if [ "$QHomeProject" = "." ]
          then
	        if [ "$QHomeProject1" = ".." ]
		     then 
		      cd $project_home
		      export project_home=`pwd`
	          INIT_PROJECT_PROPERTIES
	         else             
              export project_home=$WADE_HOME                
            fi        
    fi
    if [ "$QHomeProject" != "." ]; then
          if   [ "$QHomeProject" = "/" ]; then
  	    {    
              # Convert relative path to absolute
  	      export project_home=$project_home
              INIT_PROJECT_PROPERTIES
             }
            else 
  	      # Convert relative path to absolute
  	      export project_home=$WADE_HOME/$project_home
              INIT_PROJECT_PROPERTIES
          fi       
 
   fi      
}
	
INIT_PROJECT_PROPERTIES()
{
	# PROJECT NAME
	if [ -z "$project_name" ]
	  then
	    export project_name_option=""
	  else 
		export project_name_option="-Dproject-name="$project_name
	fi	
	echo PROJECT_NAME $project_name
    echo PROJECT_HOME $project_home	
	# PROJECT CONFIGURATION DIRECTORY (relative to project-home)
	
	if [ -z "$project_cfg" ]; then
	  export project_cfg="cfg"
	fi	
	export actual_project_cfg=$project_home/$project_cfg
	echo PROJECT-CFG = $actual_project_cfg
	
	# PROJECT CLASSES DIRECTORY (relative to project-home)
	
	if [ -z "$project_classes" ]
	  then
	  export project_classes="classes"
	fi	
	export actual_project_classes=$project_home/$project_classes
	echo PROJECT-CLASSES = $actual_project_classes
	
	# PROJECT LIBRARY DIRECTORY (relative to project-home)
	
	if [ -z "$project_lib" ]
	  then
	  export project_lib="lib"
	fi	
	export actual_project_lib=$project_home/$project_lib
	echo PROJECT-LIB = $actual_project_lib
	
	echo "."
	
	# BOOT DAEMON PORT PROPERTY OPTION
	
	QPort=`cat $wade_cfg/boot.properties | awk '/bootdaemon-port/ {print $1}' | awk '/bootdaemon-port/'`
	QPortOne=${QPort:0:1}
	if [ -z "$QPortOne" -o "$QPortOne" = "#" ]
           then
             export bootdaemon_port_option=""
           else
             export bootdaemon_port_option=-Dtsdaemon.port=${QPort:16:6}
       fi

   # BOOT DAEMON NAME PROPERTY OPTION
      
    QName=`cat $wade_cfg/boot.properties | awk '/bootdaemon-name/ {print $1}' | awk '/bootdaemon-name/'`
	QNameOne=${QName:0:1}
	if [ -z "$QNameOne" -o "$QNameOne" = "#" ]
        then
            export bootdaemon_name_option=""
        else
            export bootdaemon_name_option="-Dtsdaemon.name="${QName:16:250}
    fi

	# CONTAINERS CONTAINERS LOG PROPERTY FILE OPTION: use that of the project if present that of WADE otherwise

  if [ -e "$actual_project_cfg/log/log.properties" ] 
    then
    export containers_log_option="-Djava.util.logging.config.file=${project_cfg}/log/log.properties"
    else
    export containers_log_option="-Djava.util.logging.config.file=${cfg}/log/log.properties"
  fi
	
	# WADE HOME OPTION
	export wade_home_option="-Dwade-home=${WADE_HOME}"
	
	# JVM arguments with MAIN CONTAINER PROPERTIES OPTION
	export jvm_args="${bootdaemon_port_option} ${bootdaemon_name_option} ${containers_log_option} ${wade_home_option} ${project_name_option}"
	echo "JVM-ARGS = $jvm_args"
	echo "."	
}

BUILD_PROJECT_CLASSPATH()
{
 	export class_path=""
	# Add project-cfg (if any)
	if [ "$actual_project_cfg"!="$wade_cfg" ]; then
	   if [ -e $actual_project_cfg ] 
              then
	         export class_path=$class_path$actual_project_cfg
	   fi
	 fi
	
	# Add project-classes (if any)
	
	if [ -e "$actual_project_classes" ] 
        then
	         export class_path=$class_path$actual_project_classes
	fi
	
	# Add project-lib libraries (if any)
	
	if [  -e "$actual_project_lib" ] ;then
	    if [ "$actual_project_lib"!="$wade_lib" ] ; then
             { 
				export class_path=$class_path":"$actual_project_lib"/*"
    	      } 
           else 
 	         BUILD_WADE_CLASSPATH	     	
          fi
          else 	
 	         BUILD_WADE_CLASSPATH
	fi	
}

BUILD_WADE_CLASSPATH()
{
    # Add cfg and libs
	export class_path=$class_path":"$wade_cfg":"$wade_lib"/*"
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

START_MAIN()
{
	echo CLASS_PATH = $class_path        
	echo "."
	cd $project_home

	if [ -z "$param_service" ]
	then
		echo "Starting Main Container in foreground"
		echo "................................................................................................................."
		$RUN_JAVA -classpath "$class_path" $jvm_args com.tilab.wade.Boot -conf main.properties
	else
		echo "Starting Main Container in background"
		nohup $RUN_JAVA -classpath "$class_path" $jvm_args com.tilab.wade.Boot -conf main.properties > /dev/null 2>&1 & 
		export MAIN_PID=$!
		echo "Main Container PID = $MAIN_PID"	
	fi
}

set +v
clear
export CURRENT_DIR=$PWD
CHECK_JAVA
PARSE_PARAMETERS $1 $2
SET_STANDARD_PATHS
LOAD_BOOT_DAEMON_PROPERTIES
CHECK_PROJECT_PROPERTIES
BUILD_PROJECT_CLASSPATH
BUILD_WADE_CLASSPATH
BUILD_ADDONS_CLASSPATH
START_MAIN
cd $CURRENT_DIR
exit 0
