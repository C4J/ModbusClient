#!/bin/sh

BASEDIR=$(dirname "$0")
cd "$BASEDIR" || exit 1

# -Dio.netty.noUnsafe=true keeps Netty off sun.misc.Unsafe, so the JDK does not
# print its "terminally deprecated Unsafe" warning. The safe fallback path is
# more than fast enough for Modbus traffic.
java -Dio.netty.noUnsafe=true -jar modbusClient.jar

exit
