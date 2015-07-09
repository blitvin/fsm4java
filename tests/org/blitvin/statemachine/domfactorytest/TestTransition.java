package org.blitvin.StateMachine.domfactorytest;

import java.util.HashMap;
import java.util.Map;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.SimpleStateMachine;
import org.blitvin.statemachine.StateMachine;


public class TestTransition<EventType extends Enum<EventType>> extends org.blitvin.statemachine.SimpleTransition<EventType> {

	public TestTransition(State<EventType> targetState, SimpleStateMachine<EventType> containingMachine) {
		super(targetState, containingMachine);
	}
		
	public TestTransition(){
		super();
	}

	@Override
	public void stateMachineInitializedCallback(Map<Object, Object>  initializer,
			StateMachine<EventType> containingMachine)
			throws BadStateMachineSpecification {
		if (initializer.get("testTransitionAttribute") == null)
			throw new BadStateMachineSpecification("test: testTransitionAttribute must be specified");
		super.stateMachineInitializedCallback(initializer, containingMachine);
	}
}
