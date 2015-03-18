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

import java.util.Map;
import java.util.HashMap;


/**
 * State represents state of the machine. It has table of transitions and callback method
 * invoked upon it becomes current.  
 * @author blitvin
 *
 * @param <EventType> state machine alphabet
 */
public class State<EventType extends Enum<EventType>> {
	private final String stateName;
	private final HashMap<EventType,Transition<EventType>> transitions;
	private StateMachine<EventType> machine =null;
	private final boolean isFinalState;
	public State(String stateName,Boolean isFinal){
		this.stateName = stateName;
		this.transitions = new HashMap<EventType, Transition<EventType>>(); 
		this.isFinalState = isFinal.booleanValue();
	}
	
	public void setTransition(EventType event, Transition<EventType> transition){
		transitions.put(event, transition);
	}
	
	public Transition<EventType> getTransitionByEvent(EventType event){
		Transition<EventType> retVal = transitions.get(event);
		if (retVal == null) // no explicit transition => return default transition
			retVal = transitions.get(null);  
		return retVal;
	}
	/**
	 * set transition discards previous transitions and sets new ones (including default
	 * transition)
	 * @param transitions
	 */
	public void setTransitions(Map<EventType,Transition<EventType>> transitions) {
		this.transitions.clear();
		this.transitions.putAll(transitions);
	}
	
	/**
	 * this method transits state machine to a new state according to incoming event and transitions
	 * table
	 * @param event to proceed
	 * @return new current event
	 * @throws InvalidEventType thrown if there is no transition specified (including * default transition) 
	 * for particular event type
	 */
	public State<EventType> transit(StateMachineEvent<EventType> event)  throws InvalidEventType{
		Transition<EventType> curTransition = transitions.get(event.getEventType());
		if (curTransition == null) { //check for (*) transition
			curTransition = transitions.get(null);
			if (curTransition == null) {
				invalidTransitionCallback(event);
				throw new InvalidEventType();
			}
		}
		return curTransition.transit(event);
	}

	public boolean isFinalState() {
		return isFinalState;
	}

	public String getStateName() {
		return stateName;
	}

	public StateMachine<EventType> getContatiningStateMachine() {
		return machine;
	}

	public void setContainingStateMachine(SimpleStateMachine<EventType> machine) {
		if (this.machine != null) 
			return;
		this.machine = machine;
	}
	public Map<EventType, Transition<EventType>> getTransitions(){
		return java.util.Collections.unmodifiableMap(transitions);
	}
	
	/**
	 * callback for implementation of user defined logic upon state becoming current
	 *
	 * @param theEvent event upon which state becomes current
	 * @param prevState previous current state
	 */
	public void stateBecomesCurrentCallback(StateMachineEvent<EventType> theEvent, State<EventType> prevState){
		
	}
	
	/**
	 * callback for implementation of user defined logic upon other state becoming current.
	 * This function is called after appropriate transition transit method executed and returned
	 * state other than current  and before stateBecomesCurrentCallback of the new state is called
	 *
	 * @param theEvent event upon which state becomes current
	 * @param nextState previous current state
	 */
	public void otherStateBecomesCurrentCallback(StateMachineEvent<EventType> theEvent, State<EventType> nextState){
		
	}
	
	public void invalidTransitionCallback(StateMachineEvent<EventType> theEvent){
		
	}
	/**
	 * initialization callback, which completes initialization of  the state. Note that this method can be called
	 * from within conataining state machine's constructor
	 * @param initializer map of initialization parameters
	 * @throws BadStateMachineSpecification
	 */
	public void stateMachineInitializedCallback(Map<Object,Object>  initializer) throws BadStateMachineSpecification
	{
	}
	
}
