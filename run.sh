#!/bin/bash

#CLASSPATH=.:/home/roland/workspace/NDE/bin:/home/roland/workspace/NDE/lib:$CLASSPATH
CLASSPATH=.:bin/:lib/*:$CLASSPATH
export CLASSPATH
echo $CLASSPATH

java -cp $CLASSPATH com.metamatter.nde.CKANHarvester "$1"
