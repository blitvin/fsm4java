/*
 * (C) Copyright Boris Litvin 2014, 2015, 2015
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * State machine is implementation of CS state machine with adaptations to allow it
 * do the work in the real world. Probably , one needs state machine to perform differently
 * according to current state of the machine. In order to implement this business logic one 
 * have two ways - run this logic as part of transition between states or invoke business code
 * upon state becoming current.
 * For first way one should supply class specification with transit method overridden for state machine factory.
 * For the second one, class extending State should implement stateBecomesCurrentCallback method with appropriate
 * business logic code
 * Also, in real world events sent to state machine contain some information for the above business logic code (e.g.
 * for authentication state machine for authentication attempt event type can be e.g. AUTH_PASSWORD, but business logic
 * should receive password in order to verify it. Then class representing transition in its method transit can check the
 * password and e.g. return state Authenticated if password is correct, and state AuthFailed otherwise). For that purpose
 * events should be implemented as objects of classes implementing StateMachineEvent. Upon receiving new event everything
 * state machine cares about is mapping to particular alphabet value, so it can chose appropriate transition, all the rest
 * is for business logic code.
 * Note on initialization: state machine is initialized in two stages, first, constructor receives map of states and initial state,
 * then constructor of state machine calls stateMachineInitializedCallback for each state, and then stateMachineInitializedCallback
 * for each transition of each state. The reason for this is that state transition can be to state that is not yet created, so all
 * states are constructed and initialized, and then transition initialization completed. When transition's 
 * stateMachineInitializedCallback called it is guaranteed that states are already exist and initialized.
 * States and Transitions can be initialized according to map of string key-values passed to them. E.g. in DOMStateMachineFactory
 * initializers constructed from attributes of appropriate XML node. That way one can pass parameters to customer defined classes
 * 
 * @author blitvin
 *
 * @param <EventType> state machine's alphabet
 */
public class SimpleStateMachine<EventType extends Enum<EventType>> implements StateMachine<EventType>{
	private final Map<String,State<EventType>> states;
	private State<EventType> currentState;
	private boolean fullyInitialized = false;
	
	/**
	 * constructor that doesn't  fully initializes state  machine. You should call comepleteInitialization with appropriate
	 * initializer to complete state machine initialization. This is preferred way as it doesn't leak not fully formed reference
	 * the state machine to states and transitions during inits of those
	 * @param states map of name of states to state objects
	 * @param initialState reference to initial state. should be one in states map
	 * @throws BadStateMachineSpecification
	 */
	public SimpleStateMachine(HashMap<String, State<EventType>> states,State<EventType> initialState) throws BadStateMachineSpecification{
		this.states = states;
		setCurrentState(initialState);
	}
	
	protected void setCurrentState(State<EventType> newCurrent) throws BadStateMachineSpecification
	{
		if (newCurrent != null && !states.containsValue(newCurrent))
			throw new BadStateMachineSpecification("initial state is not part of states map");
		this.currentState = newCurrent;
	}
	
	protected Map<String,State<EventType>> getStatesMap(){
		return states;
	}
	/**
	 * Constructor doesn't require call of comepleteInitialization, but leaks not fully formed reference of the StateMachine
	 * to State and Transition objects, which can be a problem in certain situations e.g. if stateMachineInitializedCallback
	 * of State or Transition attempts to use virtual functions of passed StatemMachine object.Be warned! Better way is to call
	 * initializer-less constructor and then call comepleteInitialization method
	 * @param states - map of states
	 * @param initialState - initial state
	 * @param initializer - initializer i.e. structure containing key-value maps for each state and transition
	 * @throws BadStateMachineSpecification
	 */
	public SimpleStateMachine(HashMap<String, State<EventType>> states,State<EventType> initialState,
			HashMap<Object,HashMap<Object,Object>>initializer) throws BadStateMachineSpecification {
		this(states,initialState);
		completeInitialization(initializer);
	}
	
	
	/**
	 * Transit state machine to a new state according to received event
	 * @param event 
	 * @throws InvalidEventType thrown if event type is invalid for current state
	 */
	@Override
	public void transit(StateMachineEvent<EventType> event) throws InvalidEventType{
		State<EventType> newState = currentState.transit(event);
		if (newState != null) {
			currentState.otherStateBecomesCurrentCallback(event, newState);
			newState.stateBecomesCurrentCallback(event, currentState);
			currentState = newState;
		}
	}
	
	/**
	 * check whether given state belongs to the state machine
	 * @param state
	 * @return true if state belongs to the StateMachine
	 */
	@Override
	public boolean isValidState(State<EventType> state) {
		return states.containsValue(state);
	}
	
	@Override
	public State<EventType> getCurrentState(){
		return currentState;
	}
	
	/**
	 * 
	 * @return true if current state is final one
	 */
	@Override
	public boolean isInFinalState(){
		return currentState.isFinalState();
	}
	
	/**
	 * 
	 * @return collection of all states of the machine
	 */
	@Override
	public Collection<State<EventType>>getStates(){
		return java.util.Collections.unmodifiableCollection(states.values());
	}
	
	@Override
	public State<EventType> getStateByName(String stateName){
		return states.get(stateName);
	
	}
	/**
	 * this method is second phase of state machine initialization. It is called when all the objects
	 * are created ( by constructor), and goes through all states and then all transitions. If initializer 
	 * contains map for the object, stateMachineInitializedCallback method with the map is called. If not 
	 * stateMachineInitializedCallback(null) is called
	 * @param initializer map with key object of machine ( state or transition) and value is map of key-values
	 * to pass to given object for phase 2 initialization 
	 */
	@Override
	public void completeInitialization(
			HashMap<Object, HashMap<Object, Object>> initializer) throws BadStateMachineSpecification {
		
		if (currentState ==  null)
			throw new BadStateMachineSpecification("initial state is not set");
		if (initializationCompleted())
			return;
		for(State<EventType> cur   : states.values()) {
			cur.setContainingStateMachine(this);
			if (initializer != null) {
				cur.stateMachineInitializedCallback(initializer.get(cur));
			}
			else
				cur.stateMachineInitializedCallback(null);
		}
		
		for(State<EventType> curState : states.values()) {
			for(Transition<EventType> curTransition: curState.getTransitions().values()){
				if (initializer != null) {
					curTransition.stateMachineInitializedCallback(initializer.get(curTransition),this);
				} else {
					curTransition.stateMachineInitializedCallback(null,this);
				}
				
			}
		}
		fullyInitialized = true;
	}

	
	/**
	 * @see org.blitvin.statemachine.StateMachine#initializationCompleted()
	 */
	@Override
	public boolean initializationCompleted() {
		return fullyInitialized;
	}	
}
