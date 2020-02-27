# The ToysAssembler Example


This example implements a simple WADE-based system that enables users to assemble toys (wagons and puppets).
The following agents are available in this application: one assembler agent and two or more searcher agents. 
The assembler agent tries to assemble a toy chosen by the user, while the searcher agents search for the components that are necessary to assemble it.  
The components are stored in a store-room. An external application (StoreKeeper) embeds an inventory where components availability and position in the store room are recorded. 
The StoreKeeper application makes available such information by means of a Web Service interface that gets the required type (head, leg, wheel...) 
and quantity of component as input parameters.

The Toys Assembler application must get the type of toy (puppet or wagon) to assemble from the user, check the availability 
of the related components and retrieve their position invoking the StoreKeeper web service, retrieve the components in the store room and finally assemble the toy.
See the "WADE TUTORIAL DEFINING AGENT TASKS AS WORKFLOWS" for more details.



#  How to use the Toyassembler Example

1) Unzip the WADE-examples distribution file into the same directory where the main WADE distribution
file was unzipped.

2) Start the Main Container of the ToysAssembler example by going to the wade-home directory and typing
startMain toysAssembler
Under UNIX/Linux type
./startMain.sh toysAssembler

3) Be sure the WADE Bootdaemon is up and running. If not, go to the wade-home directory and start it by typing
startBootdaemon
Under UNIX/Linux type
./startBootdaemon.sh

4) Start the StoreKeeper application by going to the examples/toys/storeKeeper directory and typing
startStoreKeeper.bat

5) Turn the WADE platform up from the WADE Management Console:
   - Import the "ToysAssembler" configuration
   - Press the Startup button

6) The gui of the Assembler agent asking for the toy to be assemble should appear in the upper left corner of the screen. 

7) Select the toy and press ok.