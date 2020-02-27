package client;

import jade.core.AID;
import jade.util.leap.List;

import java.util.HashMap;
import java.util.Map;

import com.tilab.wade.dispatcher.WorkflowResultListener;
import com.tilab.wade.performer.descriptors.ElementDescriptor;
import com.tilab.wade.performer.descriptors.WorkflowDescriptor;
import com.tilab.wade.performer.ontology.ExecutionError;
import com.tilab.wade.proxy.EngineProxy;
import com.tilab.wade.proxy.EngineProxyException;

/**
 * This class shows how an external non-WADE application can control the execution 
 * of workflows on a WADE platform by means of the EngineProxy API.
 * In this example we assume that the WADE platform Main Container is running in the
 * local host and is listening on the default port 1099. 
 */
public class PizzaClient {
	
	public static final void main(String[] args) {
		// We expect two arguments: 
		// 1) user name
		// 2) type of pizza (e.g. margherita)
		if (args == null || args.length != 2) {
			System.out.println("Wrong arguments.");
			System.out.println("USAGE:");
			System.out.println("pizzaClient <user name> <pizza type>");
			System.exit(0);
		}
		else {
			String userName = args[0];
			String pizzaType = args[1];
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("Activate workflow to order a pizza "+pizzaType+" in the name of "+userName);
			System.out.println("----------------------------------------------------------------------------");
			
			// The EngineProxy internally uses a JadeGateway to connect to the WADE platform 
			// where workflows will be executed. In this example we assume the WADE platform
			// Main Container is running in the localhost and is listening on the default port
			// 1099 --> We don't need to make any specific JadeGateway configuration.			
			EngineProxy engine = EngineProxy.getEngineProxy();
			
			WorkflowDescriptor wd = new WorkflowDescriptor("pizza.PizzaOrder");
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("pizzaType", pizzaType);
			params.put("user", userName);
			wd.setParametersMap(params);
			try {
				engine.launch(wd, new WorkflowResultListener() {
					public void handleAssignedId(AID executor, String executionId) {
						System.out.println("Workflow PizzaOrder succesfully submitted. Executor agent = "+executor.getLocalName()+", executionId = "+executionId);
					}

					public void handleExecutionCompleted(List results, AID executor, String executionId) {
						System.out.println("----------------------------------------------------------------------------");
						System.out.println("Workflow PizzaOrder succesfully completed");
						Map<String, Object> outParams = ElementDescriptor.paramListToMap(results);
						System.out.println("OUTPUT MESSAGE: "+outParams.get("message"));
						System.out.println("----------------------------------------------------------------------------");
					}

					public void handleExecutionError(ExecutionError er, AID executor, String executionId) {
						System.out.println("----------------------------------------------------------------------------");
						System.out.println("Workflow PizzaOrder failed. "+er.getReason());
						System.out.println("----------------------------------------------------------------------------");
					}

					public void handleLoadError(String errorMsg) {
						System.out.println("----------------------------------------------------------------------------");
						System.out.println("Workflow PizzaOrder loading error. "+errorMsg);
						System.out.println("----------------------------------------------------------------------------");
					}

					public void handleNotificationError(AID executor, String executionId) {
						System.out.println("Communication Error.");
					}
					
				}, false);
			} catch (EngineProxyException e) {
				e.printStackTrace();
			}
		}
	}

}
