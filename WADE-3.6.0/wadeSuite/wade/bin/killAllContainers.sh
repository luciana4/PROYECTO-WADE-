#!/bin/bash

echo "Kill all WADE containers"
pkill -SIGKILL -f com.tilab.wade.Boot
