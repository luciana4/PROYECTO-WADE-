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

	set projects=/projects
	set addons=/add-ons
	set lib=/lib
	set cfg=/cfg
	
	set wade-projects=%wade-home%%projects%
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
	
:CHECK_PROJECT_PROPERTIES
	if  "%1"=="" goto LOAD_DEFAULT_PROPERTIES
	set project-name=%1
	echo PROJECT-NAME = %project-name%
	
	if exist "%wade-projects%/%1.properties" goto LOAD_PROJECT_PROPERTIES
	echo Project configuration file (%1.properties) not found in project directory
	goto EXIT

:LOAD_PROJECT_PROPERTIES	
	rem Change dir to wade-projects
	cd "%wade-projects%"
	
	rem Read %1.properties
	for	/F "eol=# delims=''" %%i in (%1.properties) do (
		set %%i
	)
	
:LOAD_DEFAULT_PROPERTIES
	if not "%project-home%"=="" goto CHECK_PROJECT_PATH
	
	echo PROJECT-NAME = Project is not specified, default project is used
	
	rem Change dir to wade-projects
	cd "%wade-projects%"
	
	rem Read default.properties
	for	/F "eol=# delims=''" %%i in (default.properties) do (
		set %%i
	)

:CHECK_PROJECT_PATH
	if %project-home:~0,1%==. set project-home=%wade-home%
	if %project-home:~1,1%==: goto INIT_PROJECT_PROPERTIES
	if %project-home:~1,2%==\: goto INIT_PROJECT_PROPERTIES

	rem Convert relative path to absolute
	set project-home=%wade-home%/%project-home%

:INIT_PROJECT_PROPERTIES
	set project-home=%project-home:\:=:%
	echo PROJECT-HOME = %project-home%

	rem PROJECT NAME
	if "%project-name%"=="" (set project-name-option=) else set project-name-option=-Dproject-name=%project-name%
	
	rem PROJECT CONFIGURATION DIRECTORY (relative to project-home)
	if "%project-cfg%"=="" set project-cfg=cfg
	set actual-project-cfg=%project-home%/%project-cfg%
	echo PROJECT-CFG = %actual-project-cfg%
	
	rem PROJECT CLASSES DIRECTORY (relative to project-home)
	if "%project-classes%"=="" set project-classes=classes
	set actual-project-classes=%project-home%/%project-classes%
	echo PROJECT-CLASSES = %actual-project-classes%
	
	rem PROJECT LIBRARY DIRECTORY (relative to project-home)
	if "%project-lib%"=="" set project-lib=lib
	set actual-project-lib=%project-home%/%project-lib%
	echo PROJECT-LIB = %actual-project-lib%
	echo .
	
	rem BOOT DAEMON PORT PROPERTY OPTION
	if "%bootdaemon-port%"=="" (set bootdaemon-port-option=) else set bootdaemon-port-option=-Dtsdaemon.port=%bootdaemon-port%

	rem BOOT DAEMON NAME PROPERTY OPTION
	if "%bootdaemon-name%"=="" (set bootdaemon-name-option=) else set bootdaemon-name-option=-Dtsdaemon.name=%bootdaemon-name%

	rem CONTAINERS CONTAINERS LOG PROPERTY FILE OPTION: use that of the project if present that of WADE otherwise
	if exist "%actual-project-cfg%/log/log.properties" (set containers-log-option=-Djava.util.logging.config.file="%project-cfg%/log/log.properties") else set containers-log-option=-Djava.util.logging.config.file="%cfg%/log/log.properties"

	rem WADE HOME OPTION
	set wade-home-option=-Dwade-home="%wade-home%"

	rem JVM arguments with MAIN CONTAINER PROPERTIES OPTION
	set jvm-args=%bootdaemon-port-option% %bootdaemon-name-option% %containers-log-option% %wade-home-option% %project-name-option% %WADE_MAIN_OPTS%
	echo JVM-ARGS = %jvm-args%
	echo .
	
:BUILD_PROJECT_CLASSPATH
	set class-path=
	
	rem Add project-cfg (if any)
	if not "%actual-project-cfg%"=="%wade-cfg%" (
		if exist "%actual-project-cfg%" (
			set class-path=%class-path%"%actual-project-cfg%";
		)
	)

	rem Add project-classes (if any)
	if exist "%actual-project-classes%" set class-path=%class-path%"%actual-project-classes%";

	rem Add project-lib libraries (if any)
	if not exist "%actual-project-lib%" goto BUILD_WADE_CLASSPATH
	if "%actual-project-lib%"=="%wade-lib%" goto BUILD_WADE_CLASSPATH
	
	set class-path=!class-path!"%actual-project-lib%/*";

:BUILD_WADE_CLASSPATH
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

:START_MAIN
	set class-path=!class-path!
	echo CLASS-PATH = %class-path%
	echo .
	echo Starting Main Container
	echo ..................................................................................................................
	
	rem Change dir to project-home
	cd %project-home%
	
	%RUN-JAVA% -classpath %class-path% %jvm-args% com.tilab.wade.Boot -conf main.properties
	
	goto END
	
:EXIT
	pause

:END
	endLocal
