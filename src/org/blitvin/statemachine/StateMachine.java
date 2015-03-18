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

import java.util.Collection;
import java.util.HashMap;
/**
 * StateMachine is main API of the library. This is how state machine interacts with other code
 * @author blitvin
 *
 * @param <EventType>
 */
public interface StateMachine<EventType extends Enum<EventType>>{
	/**
	 * transit to new state according to event
	 * @param event new event
	 * @throws InvalidEventType thrown if state machine dosn't define transition for this type of event( including 
	 * absence of default transition for current state)
	 */
	void transit(StateMachineEvent<EventType> event) throws InvalidEventType;
	/**
	 * 
	 * @param state state to check
	 * @return true if state is part of the state macine
	 */
	boolean isValidState(State<EventType> state);
	/**
	 * 
	 * @return true if state machine is in final state i.e. current state marked as final
	 */
	boolean isInFinalState();
	/**
	 * 
	 * @return current state of the machine
	 */
	State<EventType> getCurrentState();
	/**
	 * 
	 * @return immutable collection containing states of the FSM
	 */
	Collection<State<EventType>>getStates();
	
	/**
	 * 
	 * @param stateName name of desired state
	 * @return state with given name , null if no such state exists in FSM
	 */
	State<EventType> getStateByName(String stateName);
	
	/**
	 * Because the FSM states and transitions may reference other states initialization is
	 * performed in two stages - constructor creates all objects (states and transitions) and
	 * this method completes initialization (intended to set up all inter-state and transition-state
	 * relations)
	 * @param initializer should contain map for states and transitions to key-values map used for initialization
	 * That is keys should be either State of Transition objects and value is actually class specific e.g. SimpleTransition
	 * expects String mapping "toState" -> name of destination state
	 * @throws BadStateMachineSpecification can be thrown if initialization can't be completed e.g. completeInitializationCallback
	 * of state or transition throws exception
	 */
	void completeInitialization(HashMap<Object,HashMap<Object,Object>> initializer) throws BadStateMachineSpecification;
	/**
	 * 
	 * @return true if FSM is fully initialized, that is in most cases completeInitializaiton was called
	 */
	boolean initializationCompleted();
}
