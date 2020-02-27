@ECHO OFF
SETLOCAL

cd ..

REM Check project name
if  !%1==! goto START

REM If a project name is specified check if the properties exist
if exist "projects/%1.properties" goto ADD_PROJECT_FILE
echo Project configuration file (%1.properties) not found in project directory
goto END

:ADD_PROJECT_FILE
REM If a project name is specified read the properties from the related property file
set PROPERTY_FILE_OPTION=-Dproject-name=%1 -propertyfile projects/%1.properties

:START
echo ant %PROPERTY_FILE_OPTION% -f build.xml main
ant %PROPERTY_FILE_OPTION% main

:END
ENDLOCAL
