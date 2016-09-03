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
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.Future;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * interface defining methods for asynchronous events handling 
 * @author blitvin
 * @param <EventType>
 */
public interface AsyncStateMachine<EventType extends Enum<EventType>> extends StateMachine<EventType>{

    /**
     * This method executes transition if current generation of the inner FSM is equal to "generation" parameter. This is kind-of
     * poor mens CAS operation (operation is executed completely if concurrency predicate is correct or not at all). If execution of
     * inner machine yields exception, this method wraps it to StateMachineMultiThreadingException exception and re-throws it up
     * @param event - event to transit on
     * @param generation - expected generation count
     * @return - new state if transition happen, null otherwise
     * @throws InvalidEventException - current event is not applicable to current state
     */
    StampedState<EventType> CAStransit(StateMachineEvent<EventType> event, int generation) throws InvalidEventException;

    /**
     * send event to the state and pause until processing is completed. Return
     * StampedState object representing new state of the machine
     * This method is logically equivalent to CAStransit(event,0);
     * @param event event to process
     * @return pair of timestamp and state after transition triggered by the
     * event
     * @throws InvalidEventException thrown if there is no valid transition
     * exists for current state and particular event type of the event
     */
    public StampedState<EventType> transitAndGetResultingState(StateMachineEvent<EventType> event)
            throws InvalidEventException;
    /**
     * send event to FSM and get Future object for obtaining result when available
     * @param event event to process
     * @return Future returning state when event is processed
     */
    Future<StampedState<EventType>> asyncTransit(StateMachineEvent<EventType> event);

    /**
     * send event to the state machine , don't wait for processing completion
     * @param event event to process in state machine
     * @return true if event accepted for processing e.g. there is no problem with room to accommodate it
     */
    boolean fireAndForgetTransit(StateMachineEvent<EventType> event);
    
    StampedState<EventType> getCurrentStampedState();
}