#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "killContainer is a command to kill the process of a specific WADE container"
  echo "Usage: killContainer <container-name>"
  echo "where:"
  echo " - container-name: name of the container (for the main container use Main-Container)"
  echo "Example: ./killContainer.sh Execution-Node"
  exit 0
fi

if [ "$1" = "Main-Container" ]
then
	echo "Kill Main-Container"
	pkill -SIGKILL -f "conf main.properties"
else
	echo "Kill container $1"
	pkill -SIGKILL -f "container-name $1"
fi

