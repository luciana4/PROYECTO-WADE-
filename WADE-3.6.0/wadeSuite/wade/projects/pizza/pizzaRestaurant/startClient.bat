@ECHO OFF
SETLOCAL

REM Check Arguments
if  !%1==! goto USAGE
if  !%2==! goto USAGE

java -cp lib/pizzaRestaurant.jar;../../../lib/jade.jar;../../../lib/wadeInterface.jar client.PizzaClient %1 %2
goto END

:USAGE
echo "USAGE: startClient <user name> <pizza type>"
pause

:END
ENDLOCAL
