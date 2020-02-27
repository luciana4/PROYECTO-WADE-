@ECHO OFF
SETLOCAL

rem ant run
java -cp lib/axis.jar;lib/commons-discovery-0.2.jar;lib/commons-logging-1.0.4.jar;lib/jaxrpc.jar;lib/log4j-1.2.8.jar;lib/log4j.properties;lib/saaj.jar;lib/wsdl4j-1.5.1.jar;lib/storekeeperServer.jar server.Server


ENDLOCAL