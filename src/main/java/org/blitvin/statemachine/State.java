/*
 * (C) Copyright Boris Litvin 2014 - 2016
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
 * Interface defining callbacks for particular state (that is business logic 
 * piece corresponding to particular state).
 *
 * @author blitvin
 * @param <EventType>
 */
public interface State<EventType extends Enum<EventType>> {
    /**
	 * callback for implementation of user defined logic upon state becoming current
	 *
	 * @param theEvent event upon which state becomes current
	 * @param prevState previous current state
	 */
	void onStateBecomesCurrent(StateMachineEvent<EventType> theEvent, State<EventType> prevState);
        
	/**
	 * callback for implementation of user defined logic upon other state becoming current.
	 * This function is called after appropriate transition transit method executed and returned
	 * state other than current  and before stateBecomesCurrentCallback of the new state is called
	 *
	 * @param theEvent event upon which state becomes current
	 * @param nextState previous current state
	 */
	void onStateIsNoLongerCurrent(StateMachineEvent<EventType> theEvent, State<EventType> nextState);
	/**
         * This callback is called in case of invalid transition e.g. if transition
         * for this event is not not defined by FSM
         * @param theEvent 
         */
	void onInvalidTransition(StateMachineEvent<EventType> theEvent);
        
	/**
	 * initialization callback, which completes initialization of  the state. Note that this method can be called
	 * from within containing state machine's constructor
	 * @param initializer map of initialization parameters
         * @param containingMachine
	 * @throws BadStateMachineSpecification
	 */
	public void onStateMachineInitialized(Map<?,?>  initializer, FSMStateView containingMachine) throws BadStateMachineSpecification;
	
}