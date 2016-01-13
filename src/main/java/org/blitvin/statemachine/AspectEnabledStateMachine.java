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

import java.util.HashMap;
/**
 * AspectEnabledStateMachine is a subclass of SimpleStateMachine that allows poor man's AOP.
 * This class works mostly as SimpleStateMachine, with the only difference that it injects aspect
 * invocations during processing of transition
 * 
 * @author blitvin
 *
 * @param <EventType> state machine's alphabet
 */
public class AspectEnabledStateMachine<EventType extends Enum<EventType>> extends
		SimpleStateMachine<EventType> {
	private StateMachineAspects<EventType> aspects = null;
	
	public void setAspects(StateMachineAspects<EventType> aspects) {
		this.aspects = aspects;
	}
	
	public AspectEnabledStateMachine(HashMap<String, State<EventType>> states,
			State<EventType> initialState) throws BadStateMachineSpecification {
		super(states, initialState);
	}

	public AspectEnabledStateMachine(HashMap<String, State<EventType>> states,
			State<EventType> initialState,
			HashMap<Object, HashMap<Object, Object>> initializer)
			throws BadStateMachineSpecification {
		super(states, initialState, initializer);
	}
	
	/**
	 * the same business logic as transit of SimpleStateMachine.
	 */
	@Override
	public void transit(StateMachineEvent<EventType> event) throws InvalidEventType{
		if (aspects != null &&!aspects.onTransitionStart(event)) return;
		event2Proceed = event;
		do {
			State<EventType> newState = currentState.transit(event);
			event2Proceed = null;
			if (newState != null) {
				if (aspects != null && !aspects.onControlLeavesState(event,currentState,newState))
					return;
				currentState.onStateIsNoLongerCurrent(event, newState);
				State<EventType> prevState = currentState;
				currentState = newState;
				if (aspects != null && !aspects.onControlEntersState(event,currentState,prevState))
					return;
				currentState.onStateBecomesCurrent(event, prevState);
				if (aspects != null )
					aspects.onTransitionFinish(event, currentState, prevState);
			} else {
				if (aspects != null)
					aspects.onNullTransition(event);
			}
		}while( event2Proceed != null);
		
	}


}
