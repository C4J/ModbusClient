@echo off
cd /d "%~dp0"

rem Netty 4.2.x uses the Foreign Function & Memory API instead of sun.misc.Unsafe, so on
rem JDK 24+ it runs without the terminal-deprecation Unsafe warning - no flag needed
rem (4.1.x required -Dio.netty.noUnsafe=true).
java -jar modbusClient.jar

exit
