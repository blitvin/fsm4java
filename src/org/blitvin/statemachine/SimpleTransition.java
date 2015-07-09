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
 * SimpleTransition is default transition implementation. It represents transition to predefined state
 * the state can be given explicitly or it's name can be passed in initializer in value for key "toState"
 * To make the target be the same state as current (and not call state callbacks) provide toState=""
 * @author blitvin
 *
 * @param <EventType> state machine alphabet
 */
public class SimpleTransition<EventType extends Enum<EventType>> extends Transition<EventType> {
	
	/**
	 * property name used by stateMachineInitializedCallback to determine target state
	 */
	public static final String TARGET_STATE="toState";
	private State<EventType> targetState = null;
	private String targetStateName = null;
	
	public State<EventType> getTargetState() {
		return targetState;
	}

	public void setTargetState(State<EventType> targetState) {
		this.targetState = targetState;
	}

	public String getTargetStateName() {
		return targetStateName;
	}

	public void setTargetStateName(String targetStateName) {
		this.targetStateName = targetStateName;
	}

		
	@Override
	public State<EventType> transit(StateMachineEvent<EventType>  event) {
		
			return targetState;
		
	}
	
	public SimpleTransition(SimpleStateMachine<EventType> machine) throws BadStateMachineSpecification{
		super(machine);
		
	}
	
	public SimpleTransition(State<EventType>  targetState,SimpleStateMachine<EventType> containingMachine){
		super(containingMachine);
		this.targetState = targetState;
	}

	public SimpleTransition(){
		super();
	}
	
	@Override
	public void stateMachineInitializedCallback(StateMachine<EventType> containingMachine)  throws BadStateMachineSpecification{
		if (targetState != null) {
			targetStateName = targetState.getStateName();
			return;
		}
		if (targetStateName == null|| containingMachine == null) {
			throw new BadStateMachineSpecification("SimpleTransaction requires at least state name and containing machine");
		}
		
		targetState = containingMachine.getStateByName(targetStateName);
		
		if (targetState == null)
			throw new BadStateMachineSpecification("Can't find state with name "+ targetStateName );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void stateMachineInitializedCallback(Map<Object,Object>  initializer,
			StateMachine<EventType> containingMachine)
			throws BadStateMachineSpecification {
		this.containingMachine = containingMachine;
		Object targetStateObj = initializer.get(TARGET_STATE);
		if (targetStateObj == null) 
			throw new BadStateMachineSpecification("SimpleTransaction : target state is not specified");
		if (targetStateObj instanceof String) {
			targetStateName = (String) targetStateObj;
			
			if (targetStateName.length() == 0)
				targetState = null;
			else if ((targetState = containingMachine.getStateByName(targetStateName)) == null) {
				throw new BadStateMachineSpecification("SimpleTransaction : can't find state with name "+targetStateName);
			}
		} else {
			targetState = (State<EventType>) targetStateObj;
			targetStateName = targetState.getStateName();
		}
	}

	
}
