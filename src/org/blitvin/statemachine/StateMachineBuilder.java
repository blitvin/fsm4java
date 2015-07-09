/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FSM4Java is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * StateMachineBuilder is a helper class for programmatic building of FSM.
 * @author blitvin
 *
 * @param <EventType> alphabet of FSM to be built
 */
public class StateMachineBuilder<EventType extends Enum<EventType>> {
	private State<EventType> lastState;
	private State<EventType> initialState;
	private Transition<EventType> lastTransition;
	private final HashMap<String,State<EventType>> states;
	@SuppressWarnings("unused")
	private final String machineName;
	private final Class <? extends StateMachine<EventType>> implClass;
	private final HashMap<Object, HashMap<Object,Object>> initializers = new HashMap<Object, HashMap<Object,Object>>();
	private HashMap<Object,Object> lastInitializer = null;
	private Object lastInitializerAssociatedWith = null;
	
	private void setLastState(State<EventType> s){
			lastState = s;
			lastTransition = null;
	}
	/**
	 * This constructor allows to supply class of state machine to be built
	 * @param name string identifying state machine. Currently is not used, and provided for completeness ( factories provided by the package use
	 * state machine names for referencing) 
	 * @param implClass class of FSM object to be built
	 */
	public StateMachineBuilder(String name,Class <? extends StateMachine<EventType>> implClass){
		setLastState(null);
		machineName  = name;
		states  = new HashMap<String, State<EventType>>();
		initialState = null;
		this.implClass = implClass;
	}
	
	
	@SuppressWarnings("unchecked")
	/**
	 * This constructor assumes SimpleStateMachine class
	 * @param name name string identifying state machine. Currently is not used, and provided for  ( factories provided by the package use
	 * state machine names for referencing) 
	 */
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
	/**
	 * marks current state as initial one. There is only one initial state in FSM, if the method called multiple times, last choice 
	 * is saved
	 * @return this object
	 */
	public StateMachineBuilder<EventType> markStateAsInitial() {
		initialState = lastState;
		return this;
	}
	
	/**
	 * Adds new transition to current state 
	 * @param event event on which the transition is triggered
	 * @param transition transition object @see Transition
	 * @return this 
	 */
	public StateMachineBuilder<EventType> addTransition(EventType event,Transition<EventType> transition) {
		lastState.setTransition(event, transition);
		lastTransition = transition;
		return this;
	}
	
	/**
	 * adds transition to given state
	 * @param state the state to add transition to
	 * @param event trigger event for the transition
	 * @param transition transition object
	 * @return this
	 */
	public StateMachineBuilder<EventType> addTransition(State<EventType> state,EventType event,Transition<EventType> transition) {
		if (states.get(state.getStateName()) != state)
			states.put(state.getStateName(), state);
		state.setTransition(event, transition);
		lastState = state;
		lastTransition = transition;
		return this;
	}
	
	/**
	 * sets current state to one with given name.
	 * @param name of state to set as current
	 * @return
	 */
	public StateMachineBuilder<EventType> revisitState(String name){
		setLastState(states.get(name));
		return this;
	}
	
	/**
	 * set current transition
	 * @param event trigger event for transition to become current 
	 * @return this
	 */
	public StateMachineBuilder<EventType> revisitTransition(EventType e){
		lastTransition = lastState.getTransitionByEvent(e);
		lastInitializer = initializers.get(lastTransition);
		return this;
	}
	
	/**
	 * adds default transition i.e. one that used if no transition explicitly defined for event received by FSM 
	 * @param defaultTransition transition object
	 * @return this
	 */
	public StateMachineBuilder<EventType> addDefaultTransition(Transition<EventType> defaultTransition){
		lastState.setTransition(null, defaultTransition);
		lastTransition = defaultTransition;
		return this;
	}
	
	/**
	 * Adds initialization attribute for state or transition, e.g. for SimpleTransition attribute named "toState" containing 
	 * name of destination state must be supplied 
	 * @param attributeName name of the attribute
	 * @param attributeValue value of the attribute
	 * @return this
	 */
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
	
	/**
	 * constructs fully initialized StateMachine object using information supplied by invocation of methods addTransitoin, addState etc. 
	 * @return fully initialized state machine
	 * @throws BadStateMachineSpecification if state machine can't be constructed e.g. transition of type SimpleTransition was not
	 * supplied with "toState" attribute
	 */
	public StateMachine<EventType> build() throws BadStateMachineSpecification{
		@SuppressWarnings("rawtypes")
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
	
	/**
	 * constructs fully initialized StateMachine object using information supplied by invocation of methods addTransitoin, 
	 * addState etc. Note this method assumes existence of the object has constructor with arguments 
	 * (HashMap<String,State>,State,Object)
	 * @param additional parameters passed to the state machine object constructor
	 * @return fully initialized state machine
	 * @throws BadStateMachineSpecification if state machine can't be constructed e.g. transition of type SimpleTransition was not
	 * supplied with "toState" attribute
	 */
	public StateMachine<EventType> build(Object constructorArgs) throws BadStateMachineSpecification{
		@SuppressWarnings("rawtypes")
		final Class[] constTypes = {HashMap.class, State.class, Object.class};
		try {
			Object[] args = {states,initialState, constructorArgs};
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
