#!/bin/bash

cd ..

if [ "x$1" != "x" ]; then
	if [ -f projects/$1.properties ]; then
		PROPERTY_FILE_OPTION="-Dproject-name=$1 -propertyfile projects/$1.properties"
	else
		echo Project configuration file $1.properties not found in project directory
		exit 1;
	fi
fi
ant $PROPERTY_FILE_OPTION -f build.xml main
