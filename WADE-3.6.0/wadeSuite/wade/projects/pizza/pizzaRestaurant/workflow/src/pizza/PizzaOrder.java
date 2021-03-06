package pizza;

import com.tilab.wade.performer.RouteActivityBehaviour;
import com.tilab.wade.performer.layout.MarkerLayout;
import com.tilab.wade.performer.layout.TransitionLayout;
import com.tilab.wade.performer.Transition;
import com.tilab.wade.event.GenericEvent;
import com.tilab.wade.event.EventTemplate;
import com.tilab.wade.performer.WaitWebServiceBehaviour;
import com.tilab.wade.performer.layout.ActivityLayout;
import com.tilab.wade.performer.layout.WorkflowLayout;
import com.tilab.wade.performer.CodeExecutionBehaviour;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.FormalParameter;

@WorkflowLayout(exitPoints = { @MarkerLayout(position = "(326,423)", activityName = "NotifyUser") }, transitions = { @TransitionLayout(routingpoints = "(160,200)", to = "WaitForPizza", from = "Complain"), @TransitionLayout(to = "Complain", from = "PizzaOrderRouteActivity1"), @TransitionLayout(to = "NotifyUser", from = "PizzaOrderRouteActivity1"), @TransitionLayout(to = "PizzaOrderRouteActivity1", from = "WaitForPizza"), @TransitionLayout(to = "WaitForPizza", from = "OrderPizza") }, activities = { @ActivityLayout(position = "(105,253)", name = "Complain"), @ActivityLayout(label = "Is my Pizza?", size = "(184,48)", conditionName = "PizzaTypeCorrect", position = "(248,254)", name = "PizzaOrderRouteActivity1"), @ActivityLayout(position = "(290,337)", name = "NotifyUser"), @ActivityLayout(position = "(283,171)", name = "WaitForPizza"), @ActivityLayout(position = "(283,82)", name = "OrderPizza") })
public class PizzaOrder extends WorkflowBehaviour {

	public static final String COMPLAIN_ACTIVITY = "Complain";
	public static final String PIZZAORDERROUTEACTIVITY1_ACTIVITY = "PizzaOrderRouteActivity1";
	public static final String PIZZATYPECORRECT_CONDITION = "PizzaTypeCorrect";
	public static final String NOTIFYUSER_ACTIVITY = "NotifyUser";
	public static final String WAITFORPIZZA_ACTIVITY = "WaitForPizza";
	public static final String ORDERPIZZA_ACTIVITY = "OrderPizza";
	
	@FormalParameter(mode=FormalParameter.INPUT)
	private String pizzaType;
	@FormalParameter(mode=FormalParameter.INPUT)
	private String user;
	
	private String actualPizzaType;
	@FormalParameter(mode=FormalParameter.OUTPUT)
	private String message;
	
	private void defineActivities() {
		CodeExecutionBehaviour orderPizzaActivity = new CodeExecutionBehaviour(
				ORDERPIZZA_ACTIVITY, this);
		registerActivity(orderPizzaActivity, INITIAL);
		WaitWebServiceBehaviour waitForPizzaActivity = new WaitWebServiceBehaviour(
				WAITFORPIZZA_ACTIVITY, this);
		waitForPizzaActivity
				.setDescriptorClassName("pizza.PizzaServiceDescriptor");
		waitForPizzaActivity.setPort("PizzaPort");
		waitForPizzaActivity.setOperation("pizzaReady");
		waitForPizzaActivity
				.setEventIdentificationExpression("event.extract(\"clientName\").equals(name)");
		waitForPizzaActivity.setExclusive(true);
		registerActivity(waitForPizzaActivity);
		CodeExecutionBehaviour notifyUserActivity = new CodeExecutionBehaviour(
				NOTIFYUSER_ACTIVITY, this);
		registerActivity(notifyUserActivity, FINAL);
		RouteActivityBehaviour pizzaOrderRouteActivity1Activity = new RouteActivityBehaviour(
				PIZZAORDERROUTEACTIVITY1_ACTIVITY, this);
		registerActivity(pizzaOrderRouteActivity1Activity);
		CodeExecutionBehaviour complainActivity = new CodeExecutionBehaviour(
				COMPLAIN_ACTIVITY, this);
		registerActivity(complainActivity);
	}
	
	private void defineTransitions() {
		registerTransition(new Transition(), ORDERPIZZA_ACTIVITY,
				WAITFORPIZZA_ACTIVITY);
		registerTransition(new Transition(), WAITFORPIZZA_ACTIVITY,
				PIZZAORDERROUTEACTIVITY1_ACTIVITY);
		registerTransition(new Transition(PIZZATYPECORRECT_CONDITION, this),
				PIZZAORDERROUTEACTIVITY1_ACTIVITY, NOTIFYUSER_ACTIVITY);
		registerTransition(new Transition(), PIZZAORDERROUTEACTIVITY1_ACTIVITY,
				COMPLAIN_ACTIVITY);
		registerTransition(new Transition(), COMPLAIN_ACTIVITY,
				WAITFORPIZZA_ACTIVITY);
	}
	
	protected void executeOrderPizza() throws Exception {
		System.out.println(">>>>>> Simulate ordering a pizza of type "+pizzaType+" in the name of "+user);
	}
	
	protected void beforeWaitForPizza(EventTemplate et) throws Exception {
		et.fill("name", user);
		System.out.println(">>>>>> Wait for the pizza (use the PizzaService mock to invoke the PizzaReady Web Service and make the workflow go on) ....");
	}
	
	protected void afterWaitForPizza(GenericEvent ge) throws Exception {
		if (ge != null) {
			actualPizzaType = (String) ge.extract("pizzaType");
		}
	}
	
	protected void executeNotifyUser() throws Exception {
		message = "Pizza of type "+actualPizzaType+" is ready!!!!!";
		System.out.println(">>>>>> "+message);
	}
	
	protected boolean checkPizzaTypeCorrect() throws Exception {
		return pizzaType.equals(actualPizzaType);
	}
	
	protected void executeComplain() throws Exception {
		System.out.println(">>>>>> Wrong pizza type: received "+actualPizzaType+", ordered "+pizzaType+". Complain!!!");
	}

}
