This is the directory where the WADE startup scripts search for WADE project property files.

A WADE project property file is needed to run the Main Container and Boot Daemon of a WADE-based
application by means of the WADE startup scripts (startMain.bat/sh and startBootDaemon.bat/sh) and 
defines properties that are passed as environment variables to the Main Container.

In particular the WADE startup scripts will automatically add to the classpath
- the directory specified by the project-cfg property or the directory ${project-home}/cfg 
if project-cfg is not specified
- all jar files included in the directory specified by the project-lib property or the
directory ${project-home}/lib if project-lib is not specified  

 