package org.blitvin.statemachine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class StateMachineBuilder<EventType extends Enum<EventType>> {
	private State<EventType> lastState;
	private State<EventType> initialState;
	private Transition<EventType> lastTransition;
	private final HashMap<String,State<EventType>> states;
	private final String machineName;
	private final Class <? extends StateMachine<EventType>> implClass;
	private final HashMap<Object, HashMap<Object,Object>> initializers = new HashMap<Object, HashMap<Object,Object>>();
	private HashMap<Object,Object> lastInitializer = null;
	private Object lastInitializerAssociatedWith = null;
	
	private void setLastState(State<EventType> s){
			lastState = s;
			lastTransition = null;
	}
	
	public StateMachineBuilder(String name,Class <? extends StateMachine<EventType>> implClass){
		setLastState(null);
		machineName  = name;
		states  = new HashMap<String, State<EventType>>();
		initialState = null;
		this.implClass = implClass;
	}
	
	
	@SuppressWarnings("unchecked")
	public StateMachineBuilder(String name){
		this(name,(Class<? extends StateMachine<EventType>>) SimpleStateMachine.class);
	}
	
	public StateMachineBuilder<EventType> addState(State<EventType> newState){
		states.put(newState.getStateName(),newState);
		setLastState(newState);
		if (initialState == null) //first state considered to be initial
			initialState = newState;
		return this;
	}
	public StateMachineBuilder<EventType> markStateAsInitial() {
		initialState = lastState;
		return this;
	}
	
	public StateMachineBuilder<EventType> addTransition(EventType event,Transition<EventType> transition) {
		lastState.setTransition(event, transition);
		lastTransition = transition;
		return this;
	}
	
	
	public StateMachineBuilder<EventType> addTransition(State<EventType> state,EventType event,Transition<EventType> transition) {
		if (states.get(state.getStateName()) != state)
			states.put(state.getStateName(), state);
		state.setTransition(event, transition);
		lastState = state;
		lastTransition = transition;
		return this;
	}
	
	public StateMachineBuilder<EventType> revisitState(String name){
		setLastState(states.get(name));
		return this;
	}
	
	public StateMachineBuilder<EventType> revisitTransition(EventType e){
		lastTransition = lastState.getTransitionByEvent(e);
		lastInitializer = initializers.get(lastTransition);
		return this;
	}
	
	public StateMachineBuilder<EventType> addDefaultTransition(Transition<EventType> defaultTransition){
		lastState.setTransition(null, defaultTransition);
		lastTransition = defaultTransition;
		return this;
	}
	
	public StateMachineBuilder<EventType> addAttribute(String attributeName, String attributeValue){
		Object initializerAssociatedWith = (lastTransition == null)? lastState : lastTransition;
		if (lastInitializerAssociatedWith != initializerAssociatedWith) {
			lastInitializer = initializers.get(initializerAssociatedWith);
			lastInitializerAssociatedWith = initializerAssociatedWith;
		}
		if (lastInitializer == null) {
			lastInitializer = new HashMap<Object, Object>();
			initializers.put(initializerAssociatedWith, lastInitializer);
		}
		lastInitializer.put(attributeName, attributeValue);
		return this;
	}
	public StateMachine<EventType> build() throws BadStateMachineSpecification{
		final Class[] constTypes = {HashMap.class, State.class};
		try {
			Object[] args = {states,initialState};
			Constructor<? extends StateMachine<EventType>> constructor = implClass.getConstructor(constTypes);
			StateMachine<EventType> retVal = (StateMachine<EventType>) constructor.newInstance(args);
			retVal.completeInitialization(initializers);
			return retVal;
		} catch (NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|
				IllegalArgumentException|InvocationTargetException e) {
			throw new BadStateMachineSpecification("StateMachineBuilder.build got exception during construction of state machine "+e.toString(),e); 
		}
	}
}
