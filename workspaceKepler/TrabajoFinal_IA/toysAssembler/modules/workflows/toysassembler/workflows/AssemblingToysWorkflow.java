package toysassembler.workflows;

import storekeeper.Point;
import toysassembler.Catalogue;
import toysassembler.Component;
import toysassembler.ComponentSetDescriptor;
import toysassembler.Room;

import com.tilab.wade.performer.RouteActivityBehaviour;
import com.tilab.wade.performer.layout.AsynchFlowLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import toysassembler.assembler.AssemblerAgent;

import com.tilab.wade.performer.CodeExecutionBehaviour;
import com.tilab.wade.performer.FormalParameter;
import com.tilab.wade.performer.Subflow;
import com.tilab.wade.performer.SubflowDelegationBehaviour;
import com.tilab.wade.performer.SubflowJoinBehaviour;
import com.tilab.wade.performer.SubflowList;
import com.tilab.wade.performer.Transition;
import com.tilab.wade.performer.WebService;
import com.tilab.wade.performer.WebServiceInvocationBehaviour;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.layout.ActivityLayout;
import com.tilab.wade.performer.layout.MarkerLayout;
import com.tilab.wade.performer.layout.TransitionLayout;
import com.tilab.wade.performer.layout.WorkflowLayout;

/**
 * The main toy assembling workflow 
 */
@WorkflowLayout(asynchFlows = {@AsynchFlowLayout(routingpoints="(448,379)", to="CollectComponents", from="ActivateSearch"), @AsynchFlowLayout(routingpoints="(627,464)", to="collectComponents", from="ActivateSearch"), @AsynchFlowLayout(routingpoints="(625,463)", to="collectComponents", from="ActivateComponentSearch"), @AsynchFlowLayout(routingpoints="(628,486)", to="collectComponents", from="RetrieveComponent")}, entryPoint = @MarkerLayout(position = "(245,12)", activityName = "IdentifyToyComponents"), exitPoints = { @MarkerLayout(position="(436,598)", activityName = "AssembleToy"), @MarkerLayout(position="(462,285)", activityName = "ComponentNotAvailable") }, transitions = {@TransitionLayout(routingpoints="(48,469);(47,162)", to = "SelectNextComponentSetToSearch", from = "R1") }, activities={@ActivityLayout(size = "(177,60)", isDecisionPoint = true, position="(168,439)", name = "R1"), @ActivityLayout(size = "(175,53)", label = "Assemble Toy", position="(362,520)", name = "AssembleToy"), @ActivityLayout(size = "(192,50)", label = "Get Component-Set coordinates", position="(162,210)", name = "GetComponentSetCoordinates"), @ActivityLayout(size = "(180,50)", label = "Activate Search", position="(167,357)", name = "ActivateSearch"), @ActivityLayout(size = "(139,50)", label = "Collect Components", position="(380,444)", name = "CollectComponents"), @ActivityLayout(size = "(172,50)", label = "Component Not Available", position="(390,209)", name = "ComponentNotAvailable"), @ActivityLayout(size = "(183,50)", label = "Map Coordinates To Room", position="(165,284)", name = "MapCoordinatesToRoom"), @ActivityLayout(size = "(260,50)", label = "Select Next Component-Set To Search", position="(129,141)", name = "SelectNextComponentSetToSearch"), @ActivityLayout(size = "(173,50)", label = "Identify Toy Components", position="(172,69)", name = "IdentifyToyComponents")})
public class AssemblingToysWorkflow extends WorkflowBehaviour {
	public static final String R1_ACTIVITY = "R1";
	public static final String ASSEMBLETOY_ACTIVITY = "AssembleToy";
	public static final String GETCOMPONENTSETCOORDINATES_ACTIVITY = "GetComponentSetCoordinates";
	public static final String ACTIVATESEARCH_ACTIVITY = "ActivateSearch";
	public static final String COLLECTCOMPONENTS_ACTIVITY = "CollectComponents";
	public static final String COMPONENTNOTAVAILABLE_ACTIVITY = "ComponentNotAvailable";
	public static final String MAPCOORDINATESTOROOM_ACTIVITY = "MapCoordinatesToRoom";
	public static final String SELECTNEXTCOMPONENTSETTOSEARCH_ACTIVITY = "SelectNextComponentSetToSearch";
	public static final String IDENTIFYTOYCOMPONENTS_ACTIVITY = "IdentifyToyComponents";
	

	// INPUT parameter: the type of toy to assemble
	@FormalParameter
	private String toyType;
	// OUTPUT parameters: whether or not the toy was successfully assembled and, 
	// in case it is not, the component that was not available
	@FormalParameter(mode=FormalParameter.OUTPUT)
	private boolean toyAssembled=false;
	@FormalParameter(mode=FormalParameter.OUTPUT)
	private String missingComponent;
	
	private Iterator requiredComponents;
	private ComponentSetDescriptor currentComponentSet;
	private boolean componentSetAvailable;
	private Point componentPosition;
	private Room room;
	private List retrievedComponets = new ArrayList();
	
	private void defineActivities() {
		CodeExecutionBehaviour IdentifyToyComponents = new CodeExecutionBehaviour(IDENTIFYTOYCOMPONENTS_ACTIVITY, this);
		registerActivity(IdentifyToyComponents, INITIAL);
		CodeExecutionBehaviour SelectNextComponentSetToSearch = new CodeExecutionBehaviour(SELECTNEXTCOMPONENTSETTOSEARCH_ACTIVITY, this);
		registerActivity(SelectNextComponentSetToSearch);
		CodeExecutionBehaviour MapCoordinatesToRoom = new CodeExecutionBehaviour(MAPCOORDINATESTOROOM_ACTIVITY, this);
		registerActivity(MapCoordinatesToRoom);
		CodeExecutionBehaviour ComponentNotAvailable = new CodeExecutionBehaviour(COMPONENTNOTAVAILABLE_ACTIVITY, this);
		registerActivity(ComponentNotAvailable, FINAL);
		SubflowJoinBehaviour CollectComponents = new SubflowJoinBehaviour(COLLECTCOMPONENTS_ACTIVITY, this);
		CollectComponents.addSubflowDelegationActivity(ACTIVATESEARCH_ACTIVITY,
				SubflowJoinBehaviour.ALL);
		registerActivity(CollectComponents);
		SubflowDelegationBehaviour ActivateSearch = new SubflowDelegationBehaviour(ACTIVATESEARCH_ACTIVITY, this);
		ActivateSearch.setAsynch();
		ActivateSearch.setSubflow("toysassembler.workflows.SearchWorkflow");
		registerActivity(ActivateSearch);
		WebServiceInvocationBehaviour GetComponentSetCoordinates = new WebServiceInvocationBehaviour(GETCOMPONENTSETCOORDINATES_ACTIVITY, this);
		GetComponentSetCoordinates
				.setDescriptorClassName("storekeeper.StoreKeeperServiceDescriptor");
		GetComponentSetCoordinates.setPort("StoreKeeperPort");
		GetComponentSetCoordinates.setOperation("getComponents");
		registerActivity(GetComponentSetCoordinates);
		CodeExecutionBehaviour AssembleToy = new CodeExecutionBehaviour(ASSEMBLETOY_ACTIVITY, this);
		registerActivity(AssembleToy, FINAL);
		RouteActivityBehaviour R1 = new RouteActivityBehaviour(R1_ACTIVITY, this);
		registerActivity(R1);
	}

	private void defineTransitions() {
		registerTransition(new Transition(), IDENTIFYTOYCOMPONENTS_ACTIVITY,SELECTNEXTCOMPONENTSETTOSEARCH_ACTIVITY);
		registerTransition(new Transition(), MAPCOORDINATESTOROOM_ACTIVITY,ACTIVATESEARCH_ACTIVITY);
		registerTransition(new Transition(), SELECTNEXTCOMPONENTSETTOSEARCH_ACTIVITY,GETCOMPONENTSETCOORDINATES_ACTIVITY);
		registerTransition(new Transition(),	GETCOMPONENTSETCOORDINATES_ACTIVITY, COMPONENTNOTAVAILABLE_ACTIVITY);
		registerTransition(new Transition("ComponentSetAvailable", this), GETCOMPONENTSETCOORDINATES_ACTIVITY,MAPCOORDINATESTOROOM_ACTIVITY);
		registerTransition(new Transition(), COLLECTCOMPONENTS_ACTIVITY,ASSEMBLETOY_ACTIVITY);
		registerTransition(new Transition(), ACTIVATESEARCH_ACTIVITY,R1_ACTIVITY);
		registerTransition(new Transition(), R1_ACTIVITY,COLLECTCOMPONENTS_ACTIVITY);
		registerTransition(new Transition("MoreComponentSetToSearch", this), R1_ACTIVITY,SELECTNEXTCOMPONENTSETTOSEARCH_ACTIVITY);
	}

	/**
	 * Initialize the list of components to be assembled on the basis the type of toy indicated by the user.
	 * @layout visible=true;position=(430,42);size=(196,101)
	 */
	protected void executeIdentifyToyComponents() throws Exception {
		System.out.println("--- Agent "+myAgent.getLocalName()+": Start assembling "+toyType+"...");
		List l = Catalogue.getRequiredComponents(toyType);
		requiredComponents = l.iterator();
	}

	protected void executeSelectNextComponentSetToSearch() throws Exception {
		currentComponentSet = (ComponentSetDescriptor) requiredComponents.next();
		System.out.println("--- Agent "+myAgent.getLocalName()+": Searching for "+currentComponentSet);	
	}
	
	protected void executeGetComponentSetCoordinates(WebService ws) throws Exception {
		ws.fill("quantity", currentComponentSet.getNumber());
		ws.fill("type", currentComponentSet.getType());
		performWebService(ws);
		componentSetAvailable = (Boolean) ws.extract("availability");
		componentPosition = (Point) ws.extract("location");
	}

	protected void executeMapCoordinatesToRoom() throws Exception {
		room = new Room(componentPosition.getX(), componentPosition.getY());
	}
	
	protected void executeComponentNotAvailable() throws Exception {
		System.out.println("--- Agent "+myAgent.getLocalName()+": "+currentComponentSet+" not available!");
		missingComponent = currentComponentSet.getType();
	}

	protected void executeActivateSearch(Subflow s) throws Exception {
		System.out.println("--- Agent "+myAgent.getLocalName()+": Activating retrieval of "+currentComponentSet+" in store room");
		s.fill("room", room);
		s.fill("componentType", currentComponentSet.getType());
		s.fill("componentNumber", currentComponentSet.getNumber());
		s.setPerformer(((AssemblerAgent) myAgent).getSearcherAgent().getLocalName());
		performSubflow(s);		
	}

	protected void executeCollectComponents(SubflowList ss) throws Exception {
		Subflow s;
		List<Subflow> ls;
		ls = (List<Subflow>) ss.get(ACTIVATESEARCH_ACTIVITY);
		for (Subflow tmpS : ls) {
			retrievedComponets.add((Component[]) tmpS.extract("components"));
		}
	}

	protected void executeAssembleToy() throws Exception {
		System.out.println("--- Agent "+myAgent.getLocalName()+": All components retrieved:");
		for(int i = 0; i < retrievedComponets.size(); ++i) {
			Component[] cc = (Component[]) retrievedComponets.get(i);
			for (int j = 0; j < cc.length; ++j) {
				System.out.println("    - "+cc[j]);
			}
		}
		toyAssembled=true;
		System.out.println("--- Agent "+myAgent.getLocalName()+": "+toyType+" ASSEMBLED!");
	}

	protected boolean checkComponentSetAvailable() {
		return componentSetAvailable;
	}
	
	protected boolean checkMoreComponentSetToSearch() {
		return requiredComponents.hasNext();
	}
}
