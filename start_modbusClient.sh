#!/bin/sh

BASEDIR=$(dirname "$0")
cd "$BASEDIR" || exit 1

# Netty 4.2.x uses the Foreign Function & Memory API instead of sun.misc.Unsafe, so on
# JDK 24+ it runs without the terminal-deprecation Unsafe warning - no flag needed
# (4.1.x required -Dio.netty.noUnsafe=true).
java -jar modbusClient.jar

exit
