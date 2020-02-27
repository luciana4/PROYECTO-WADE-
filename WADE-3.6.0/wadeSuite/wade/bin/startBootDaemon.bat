:START
	@echo off
	setLocal EnableDelayedExpansion
	cls

:CHECK_JAVA
	if not "%JAVA_HOME%"=="" goto GOT_JDK_HOME
	echo No JAVA_HOME environment variable is defined
	echo Java is needed to run this program
	goto EXIT
	
:GOT_JDK_HOME
	if exist "%JAVA_HOME%\bin\java.exe" goto SET_JAVA
	echo The JAVA_HOME environment variable is not defined correctly
	echo This environment variable is needed to run this program
	echo NB: JAVA_HOME should point to a JDK not a JRE
	goto EXIT

:SET_JAVA
	set RUN-JAVA="%JAVA_HOME%\bin\java"
	set EXEC-JAVA=start %RUN-JAVA%
	echo JAVA-HOME = %JAVA_HOME%
	echo .
	%RUN-JAVA% -version
	echo .
	
:SET_STANDARD_PATHS
	set CURRENT-DIR=%cd%
	set wade-home=%WADE_HOME%
	
	if not "%wade-home%"=="" goto SET_STANDARD_PATHS_2
	echo No WADE_HOME env variable defined: get the WADE installation's dir from the call's path ...
	rem Assuming to be into the 'bin' directory
	cd ..
	set wade-home=%cd%
	
:SET_STANDARD_PATHS_2	
	echo WADE-HOME = %wade-home%

	set addons=/add-ons
	set lib=/lib
	set cfg=/cfg
	
	set wade-addons=%wade-home%%addons%
	set wade-lib=%wade-home%%lib%
	set wade-cfg=%wade-home%%cfg%
	
:LOAD_BOOT_DAEMON_PROPERTIES
	rem Change dir to wade-cfg
	cd "%wade-cfg%"
	
	rem Read boot.properties
	for	/F "eol=# delims=''" %%i in (boot.properties) do (
		set %%i
	)
	
	rem BOOT DAEMON PORT PROPERTY OPTION
	if "%bootdaemon-port%"=="" (set bootdaemon-port-option=) else set bootdaemon-port-option=-Dtsdaemon.port=%bootdaemon-port%

	rem BOOT DAEMON NAME PROPERTY OPTION
	if "%bootdaemon-name%"=="" (set bootdaemon-name-option=) else set bootdaemon-name-option=-Dtsdaemon.name=%bootdaemon-name%

	rem BOOT DAEMON REMOTE-OBJECT-PORT PROPERTY OPTION
	if "%bootdaemon-remoteobjectport%"=="" (set bootdaemon-remoteobjectport-option=) else set bootdaemon-remoteobjectport-option=-Dtsdaemon.remoteobjectport=%bootdaemon-remoteobjectport%

	rem BOOT DAEMON RMI-SERVER-HISTNAME PROPERTY OPTION
	if "%bootdaemon-rmiserverhostname%"=="" (set bootdaemon-rmiserverhostname-option=) else set bootdaemon-rmiserverhostname-option=-Djava.rmi.server.hostname=%bootdaemon-rmiserverhostname%

	rem OUTPUT-FILTER PROPERTY OPTION
	if "%output-filter%"=="" (set output-filter-option=) else set output-filter-option=-Doutput-filter=%output-filter%
	
	rem BOOT DAEMON LOG PROPERTY FILE OPTION
	set bootdaemon-log-option=-Djava.util.logging.config.file="%wade-cfg%/log/bootdaemon-log.properties"
	
	rem WADE HOME OPTION
	set wade-home-option=-Dwade-home="%wade-home%"
	
	rem JVM arguments with BOOT DAEMON PROPERTIES OPTION
	set jvm-args=%bootdaemon-port-option% %bootdaemon-name-option% %bootdaemon-remoteobjectport-option% %bootdaemon-rmiserverhostname-option% %bootdaemon-log-option% %output-filter-option% %wade-home-option% %WADE_BOOT_OPTS%
	echo JVM-ARGS = %jvm-args%
	echo .	

:BUILD_WADE_CLASSPATH
	set class-path=

	rem Add cfg
	set class-path=%class-path%"%wade-cfg%";

	rem Add jars
	set class-path=!class-path!"%wade-lib%/*";

:BUILD_ADDONS_CLASSPATH	
	echo WADE-ADDONS = %wade-addons%
	
	rem Change dir to wade-addons
	cd %wade-addons%

	rem Add addons cfg (if any)
	for /f "tokens=*" %%G in ('dir /b /A:D') DO (
		if exist "%wade-addons%/%%G/cfg" set class-path=!class-path!"%wade-addons%/%%G/cfg";
	)

	rem Add addons libraries (if any)
	for /f "tokens=*" %%G in ('dir /b /A:D') DO (
		if exist "%wade-addons%/%%G/lib" (
			set class-path=!class-path!"%wade-addons%/%%G/lib/*";
		)
	)
	
:START_BOOTDAEMON
	set class-path=!class-path!
	echo CLASS-PATH = %class-path%
	echo .
	echo Starting Boot Daemon
	echo ..................................................................................................................

	rem Come back to wade-home
	cd %wade-home%

	%RUN-JAVA% -classpath %class-path% %jvm-args% com.tilab.wade.boot.BootDaemon
	
	goto END
	
:EXIT
	pause

:END
	endLocal
