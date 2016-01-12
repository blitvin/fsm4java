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

/**
 * This class represents transition of state machine. When state machine receives an event
 * current state chooses transition according to its event type, the transition changes current
 * state according to internal logic. transit method should return new current state. Business 
 * logic payload should be implemented in this method. 
 * @author blitvin
 *
 * @param <EventType>
 */
public abstract class Transition<EventType extends Enum<EventType>> {
	protected StateMachine<EventType> containingMachine;
	
	/**
	 * 
	 * @return state machine of the transition
	 */
	public StateMachine<EventType> getContainingMachine() {
		return containingMachine;
	}

	/**
	 * defines machine the transition is part of 
	 * @param containingMachine
	 */
	public void setContainingMachine(StateMachine<EventType> containingMachine) {
		if (this.containingMachine == null)
			this.containingMachine = containingMachine;
	}

	/**
	 * constructor with containing state machine supplied
	 * @param machine state machine the transition belongs to 
	 */
	public  Transition( StateMachine<EventType> machine){
		containingMachine = machine;
	}
	
	public Transition(){
		
	}
	
	/**
	 * transit method returns new current state. Also business logic should be implemented in
	 * subclasses of Transition
	 * @param event
	 * @return new current state of the state machine
	 */
	abstract State<EventType> transit(StateMachineEvent<EventType> event);
	
	
	/**
	 * This callback is called after constructor of state machine initialized all states that is
	 * states are registered with the state machine, and constructor has called setContainingMachine on them
	 * This function used for completing initialization that depend on state of other states initialization
	 *  note that this callback can be called from within 
	 */
	abstract public void stateMachineInitializedCallback(StateMachine<EventType> containingMachine) throws BadStateMachineSpecification;
	/**
	 * This callback is called after constructor of state machine initialized all states that is
	 * states are registered with the state machine, and constructor has called setContainingMachine on them
	 * This function used for completing initialization that depend on state of other states initialization
	 *  note that this callback can be called from within StateMachine constructor so containingMachine parameter
	 *  contains not fully formed reference! That is you can store it , but you should not call virtual functions etc. 
	 * @param initializer map of key-value strings containing parameters for transition initialization 
	 * @param containingMachine - machine the transition belongs to
	 * @throws BadStateMachineSpecification - throw it if something gone wrong during initializaiton
	 */
	abstract public void stateMachineInitializedCallback(Map<Object,Object> initializer, StateMachine<EventType> containingMachine) throws BadStateMachineSpecification;
}
