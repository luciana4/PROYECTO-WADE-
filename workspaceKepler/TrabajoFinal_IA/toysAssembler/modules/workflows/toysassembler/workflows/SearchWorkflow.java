package toysassembler.workflows;
import java.awt.Point;

import javax.swing.JOptionPane;

import toysassembler.Component;
import toysassembler.ComponentSetDescriptor;
import toysassembler.Room;
import toysassembler.RoomGui;
import toysassembler.searcher.SearcherAgent;

import com.tilab.wade.performer.CodeExecutionBehaviour;
import com.tilab.wade.performer.FormalParameter;
import com.tilab.wade.performer.Transition;
import com.tilab.wade.performer.WorkflowBehaviour;
import com.tilab.wade.performer.layout.ActivityLayout;
import com.tilab.wade.performer.layout.MarkerLayout;
import com.tilab.wade.performer.layout.TransitionLayout;
import com.tilab.wade.performer.layout.WorkflowLayout;

@WorkflowLayout(entryPoint = @MarkerLayout(position = "(336,61)", activityName = "Initialize"), exitPoints = { @MarkerLayout(position = "(337,518)", activityName = "LoadComponents") }, transitions = {@TransitionLayout(routingpoints="(525,260);(524,467)", to = "LoadComponents", from="Move"), @TransitionLayout(routingpoints="(460,361);(460,304)", to="Move", from = "Look"), @TransitionLayout(routingpoints="(137,362)", to = "SelectRandomDestination", from = "Look") }, activities={@ActivityLayout(position="(301,337)", name = "Look"), @ActivityLayout(size = "(128,50)", label = "Load Components", position="(287,443)", name = "LoadComponents"), @ActivityLayout(position="(300,236)", name="Move"), @ActivityLayout(size = "(163,50)", label = "Select Random Destination ", position="(57,235)", name = "SelectRandomDestination"), @ActivityLayout(position="(300,118)", name = "Initialize")})
public class SearchWorkflow extends WorkflowBehaviour {
	public static final String LOOK_ACTIVITY = "Look";
	public static final String LOADCOMPONENTS_ACTIVITY = "LoadComponents";
	public static final String MOVE_ACTIVITY = "Move";
	public static final String INITIALIZE_ACTIVITY = "Initialize";

	public static final String SELECTRANDOMDESTINATION_ACTIVITY = "SelectRandomDestination";

	@FormalParameter
	private Room room;
	@FormalParameter
	private String componentType;
	@FormalParameter
	private int componentNumber;
	@FormalParameter(mode=FormalParameter.OUTPUT)
	private Component[] components;

	private RoomGui gui;
	private Point targetPosition = null;


	private void defineActivities() {
		CodeExecutionBehaviour Initialize = new CodeExecutionBehaviour(INITIALIZE_ACTIVITY, this);
		registerActivity(Initialize, INITIAL);
		CodeExecutionBehaviour SelectRandomDestination = new CodeExecutionBehaviour(SELECTRANDOMDESTINATION_ACTIVITY, this);
		registerActivity(SelectRandomDestination);
		CodeExecutionBehaviour moveActivity = new CodeExecutionBehaviour(MOVE_ACTIVITY, this);
		registerActivity(moveActivity);
		CodeExecutionBehaviour LoadComponents = new CodeExecutionBehaviour(LOADCOMPONENTS_ACTIVITY, this);
		registerActivity(LoadComponents, FINAL);
		CodeExecutionBehaviour Look = new CodeExecutionBehaviour(LOOK_ACTIVITY, this);
		registerActivity(Look);
	}

	private void defineTransitions() {
		registerTransition(new Transition(), SELECTRANDOMDESTINATION_ACTIVITY,MOVE_ACTIVITY);
		registerTransition(new Transition(), MOVE_ACTIVITY, LOOK_ACTIVITY);
		registerTransition(new Transition("CurrentDestinationReached", this), LOOK_ACTIVITY,SELECTRANDOMDESTINATION_ACTIVITY);
		registerTransition(new Transition(), LOOK_ACTIVITY,	MOVE_ACTIVITY);
		registerTransition(new Transition(), INITIALIZE_ACTIVITY, MOVE_ACTIVITY);
		registerTransition(new Transition("TargetReached", this), MOVE_ACTIVITY, LOADCOMPONENTS_ACTIVITY);
	}

	protected void executeInitialize() throws Exception {
		// Display a GUI showing a robot searching components in the store room
		gui = new RoomGui(room, getRobotViewSize(), "Agent "+myAgent.getLocalName()+" searching for "+ComponentSetDescriptor.toString(componentNumber, componentType));
		gui.start();
		
		// Set the robot destination to the nominal position of the components to retrieve
		room.setRobotDestination(room.getNominalTargetPosition());
	}

	protected void executeMove() throws Exception {
		room.move(getRobotSpeed());
	}

	protected void executeLook() throws Exception {
		targetPosition = room.look(getRobotViewSize());
		if (targetPosition != null) {
			room.setRobotDestination(targetPosition);	
		}
	}

	protected void executeSelectRandomDestination() throws Exception {
		int x = (int) (Math.random() * room.width);
		int y = (int) (Math.random() * (room.height-20));
		room.setRobotDestination(new Point(x, y));	
	}

	protected void executeLoadComponents() throws Exception {
		components = new Component[componentNumber];
		for (int i = 0; i < components.length; ++i) {
			components[i] = new Component(componentType);
		}
		gui.stop();
		gui.showMessage("Target reached! Press OK to load "+ComponentSetDescriptor.toString(componentNumber, componentType), "Success!", JOptionPane.INFORMATION_MESSAGE);
	}

	protected boolean checkCurrentDestinationReached() {
		return room.getRobotPosition().equals(room.getRobotDestination());
	}

	protected boolean checkTargetReached() {
		return room.getRobotPosition().equals(targetPosition);
	}

	private int getRobotViewSize() {
		return ((SearcherAgent) myAgent).getViewSize();
	}
	
	private int getRobotSpeed() {
		return ((SearcherAgent) myAgent).getSpeed();
	}
}

