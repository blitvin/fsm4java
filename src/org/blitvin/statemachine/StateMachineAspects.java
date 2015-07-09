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

/**
 * This interface defines object containing predefined aspects that can be used in AspectEnabledStateMachine.
 * Those aspects are invoked in predefined points of FSM transition  
 * @author blitvin
 *
 * @param <EventType>
 */
public interface StateMachineAspects <EventType extends Enum<EventType>>{
	/**
	 * Aspect of starting transition
	 * @param event that triggered the transition
	 * @return true in order to proceed with the transition
	 */
	boolean startTransition(StateMachineEvent<EventType> event);
	/**
	 * Invoked if transition returned null ( that is transition from and to current state  without
	 * need to invoke callbacks for the transition processing)
	 * @param event that triggered the transition
	 */
	void nullTransition(StateMachineEvent<EventType> event);
	/**
	 * This method invoked before 'otherStateBecomesCurrent' callback of current state's  
	 * @param event that triggered the transition
	 * @param currentState current state
	 * @param newState target state
	 * @return true to continue processing, false to stop processing of the event
	 */
	boolean otherStateBecomesCurrent(StateMachineEvent<EventType> event, 
				State<EventType>currentState,State<EventType> newState);
	/**
	 * This method invoked before 'stateBecomesCurrent' callback of target state
	 * @param event triggering event
	 * @param currentState new (target) state
	 * @param prevState  original state (i.e. current state before the transition)
	 * @return true to continue processing, false to stop the processing
	 */
	boolean stateBecomesCurrent(StateMachineEvent<EventType> event, 
				State<EventType>currentState,State<EventType> prevState);
	/**
	 * End of transition processing aspect. called after target state become current
	 * @param event triggering event
	 * @param currentState new (target) state, that has become current
	 * @param prevState current state before transition
	 */
	void endTransition(StateMachineEvent<EventType> event, 
				State<EventType>currentState,State<EventType> prevState);
	/**
	 * callback for specifying containing machine ( this may be usefull for aspects to access FSM the aspect object associated with)
	 * @param machine FSM the aspect associated with
	 */
	void setContainingMachine(StateMachine<EventType> machine);
	
}
