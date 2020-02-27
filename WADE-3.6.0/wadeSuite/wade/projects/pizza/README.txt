# The Book Trading Example


This example shows how an existing application can launch and control the execution of workflows on WADE.
That is, WADE is used as a black-box workflow engine and executes workflows on behalf of an existing application.  
Furthermore the example shows a workflow that includes a WaitWebService Activity i.e. an activity that makes the 
workflow suspend until a given operation of a previously exposed WebService is invoked.

The example simulates an application that supports a user in ordering a pizza at a pizza restaurant and being 
notified when the pizza is ready. It is composed of 2 main parts:
- The pizzaRestaurant part is the actual pizza ordering application that uses WADE as workflow engine. The PizzaOrder 
workflow includes a Wait WebService activity where the execution flow suspends until the pizzaReady Web Service operation is 
invoked.    
- The pizzaService part is a minimal application that allows invoking the pizzaReady WebService described by 
the WSDL included in the pizzaService/wsdl/ directory.

On its turn the pizza ordering application (pizzaRestaurant) includes
- The PizzaOrder workflow
- The pizza ordering client application itself
- The PizzaService exposure jar file (PizzaServices_S.jar) that allows exposing the pizzaReady Web Service 
by means of the WadeServices component of the WADE Suite (see the WADE User Guide for details).



#  How to use  Pizza Restaurant Example

1) Unzip the WADE-examples distribution file into the same directory where the main WADE distribution
file was unzipped.

2) Start the Main Container of the Pizza Restaurant example by going to the wade-home directory and typing
startMain pizzaRestaurant
Under UNIX/Linux type
./startMain.sh pizzaRestaurant

3) Be sure the WADE Bootdaemon is up and running. If not, go to the wade-home directory and start it by typing
startBootdaemon
Under UNIX/Linux type
./startBootdaemon.sh

4) Turn the WADE platform up from the WADE Management Console:
   - Import the "pizza" configuration
   - Press the Startup button

5) Be sure the WadeServices module is up and running (see the WADE User Guide for details) ad use the WadeServices 
administration web page (http://localhost:8080/wadeServices) to expose the pizzaReady WebService.

6) Start the pizza ordering client application by going to the examples/pizza/pizzaRestaurant directory and typing
startClient <user name> <pizza type>;
the workflow goes to in SUSPENDED state. 

7) Start the pizza delivery web service client  by going to the examples/pizza/pizzaService directory and typing
startPizzaProducer; insert <Client name> <pizza type> with previous values in the gui "Pizza Producer" and 
click on Delivery Button, to TERMINATED the workflow.

   