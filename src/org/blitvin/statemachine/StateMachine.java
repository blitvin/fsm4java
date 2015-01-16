package org.blitvin.statemachine;

import java.util.Collection;
import java.util.HashMap;

public interface StateMachine<EventType extends Enum<EventType>>{
	void transit(StateMachineEvent<EventType> event) throws InvalidEventType;
	boolean isValidState(State<EventType> state);
	boolean isInFinalState();
	State<EventType> getCurrentState();
	Collection<State<EventType>>getStates();
	State<EventType> getStateByName(String stateName);
	void completeInitialization(HashMap<Object,HashMap<Object,Object>> initializer) throws BadStateMachineSpecification;
	boolean initializationCompleted();
}
