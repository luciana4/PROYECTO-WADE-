#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "getContainerPID is a command to get the process PID of a specific WADE container"
  echo "Usage: getContainerPID <container-name>"
  echo "where:"
  echo " - container-name: name of the container (for the main container use Main-Container)"
  echo "Example: ./getContainerPID.sh Execution-Node"
  exit 0
fi

if [ "$1" = "Main-Container" ]
then
	pgrep -f "conf main.properties"
else
	pgrep -f "container-name $1"
fi



