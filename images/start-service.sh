#!/bin/sh

echo "Hello! Indy Generic Proxy Service starts!"

export JAVA_HOME=/usr/lib/jvm/jre-11-openjdk
export JAVA_CMD=$JAVA_HOME/bin/java

cd /deployment
$JAVA_CMD $JAVA_OPTS -jar ./indy-generic-proxy-service-runner.jar