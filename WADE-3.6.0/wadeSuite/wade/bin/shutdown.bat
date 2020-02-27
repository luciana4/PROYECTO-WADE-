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
	set lib=/lib
	set cfg=/cfg
	
	set wade-projects=%wade-home%%projects%
	set wade-lib=%wade-home%%lib%
	set wade-cfg=%wade-home%%cfg%
	
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

	rem PROJECT CONFIGURATION DIRECTORY (relative to project-home)
	if "%project-cfg%"=="" set project-cfg=cfg
	set actual-project-cfg=%project-home%/%project-cfg%
	echo PROJECT-CFG = %actual-project-cfg%
	echo .

:LOAD_MAIN_PROPERTIES
	rem Change dir to project-cfg
	cd "%actual-project-cfg%"
	
	rem Read default.properties
	for	/F "eol=# delims=''" %%i in (main.properties) do (
		set %%i
	)

:INIT_CONNECTION_PROPERTIES	
	if "%local-port%"=="" set local-port=1099
	if "%platform-id%"=="" set platform-id=WADE

	set args=-port %local-port% -name %platform-id%
	echo ARGS = %args%
	echo .
	
:EXECUTE_SHUTDOWN
	set class-path="%wade-lib%/jade.jar;%wade-lib%/jadeMisc.jar"
	echo CLASS-PATH = %class-path%
	echo .
	echo Execute Shutdown
	echo .
	
	cd %CURRENT-DIR%
	
	%RUN-JAVA% -classpath %class-path% jade.cli.ShutdownPlatform %args%
	
	goto END
	
:EXIT
	pause

:END
	endLocal
