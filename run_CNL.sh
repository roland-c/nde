#!/bin/bash

CLASSPATH=.:bin/:lib/*:$CLASSPATH
export CLASSPATH
echo $CLASSPATH

java -cp $CLASSPATH com.metamatter.nde.CNLHarvester "$1"
