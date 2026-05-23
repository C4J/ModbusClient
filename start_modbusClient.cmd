@echo off
cd /d "%~dp0"

rem -Dio.netty.noUnsafe=true keeps Netty off sun.misc.Unsafe, so the JDK does not
rem print its "terminally deprecated Unsafe" warning. The safe fallback path is
rem more than fast enough for Modbus traffic.
java -Dio.netty.noUnsafe=true -jar modbusClient.jar

exit
