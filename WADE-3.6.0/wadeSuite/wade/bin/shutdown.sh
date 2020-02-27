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
	export wade_lib=$WADE_HOME/lib
	export wade_cfg=$WADE_HOME/cfg
	export wade_projects=$WADE_HOME/projects
	echo "WADE_HOME = $WADE_HOME"
	echo "WADE_CFG = $wade_cfg"
	echo "WADE_LIB = $wade_lib"
	echo "WADE_PROJECTS = $wade_projects"
}

CHECK_PROJECT_PROPERTIES()
{
    if [ -z "$1" ]
    then
        LOAD_DEFAULT_PROPERTIES
    else
    { 
        export project_name="$1"
        export project_properties=${wade_projects}/"$1".properties
        if [ -e $project_properties ]; then
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
	echo "."
	
	# MAIN CONTAINER PORT PROPERTY OPTION
	tr -d '\r' < $actual_project_cfg/main.properties > tmp
	mv -f tmp $actual_project_cfg/main.properties

	QPort=`cat $actual_project_cfg/main.properties | awk '/local-port/ {print $1}' | awk '/local-port/'`
	QPortOne=${QPort:0:1}
	if [ -z "$QPortOne" -o "$QPortOne" = "#" ]
           then
             export port_option="-port 1099"
           else
             export port_option="-port "${QPort:11:6}
       fi

    # PLATFORM NAME PROPERTY OPTION
    QName=`cat $actual_project_cfg/main.properties | awk '/platform-id/ {print $1}' | awk '/platform-id/'`
	QNameOne=${QName:0:1}
	if [ -z "$QNameOne" -o "$QNameOne" = "#" ]
        then
            export platform_name_option="-name WADE"
        else
            export platform_name_option="-name "${QName:12:250}
  fi
  
	# Program arguments 
	export args="${port_option} ${platform_name_option}"
	echo "ARGS = $args"
	echo "."	
}

BUILD_WADE_CLASSPATH()
{
    # Add libs
	export class_path=$wade_lib"/*"
}

EXECUTE_SHUTDOWN()
{
	echo "Execute Shutdown"
	echo "."
	$RUN_JAVA -classpath "$class_path" jade.cli.ShutdownPlatform $args
}

set +v
clear
export CURRENT_DIR=$PWD
CHECK_JAVA
SET_STANDARD_PATHS
CHECK_PROJECT_PROPERTIES $1
BUILD_WADE_CLASSPATH
EXECUTE_SHUTDOWN
cd $CURRENT_DIR
exit 0
