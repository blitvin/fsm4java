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
package org.blitvin.statemachine.concurrent;

import java.util.Set;
import org.blitvin.statemachine.FSMWrapper;
import org.blitvin.statemachine.FSMWrapperException;
import org.blitvin.statemachine.FSMWrapperTransport;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * This class wraps regular (not thread safe) FSM. It enforces thread safety by
 * usage of intrinsic lock on all state modifying operations. One should use
 * this class for situation of low congestion of processed events. For more
 * sophisticated processing (e.g. including speculative execution) or congested
 * scenarios use ConcurrentStateMachine class
 *
 * @author blitvin
 *
 * @param <EventType>
 */
public class SyncronizedStateMachine<EventType extends Enum<EventType>> extends FSMWrapper<EventType> {

    public SyncronizedStateMachine(StateMachine<EventType> wrapped) {
        super(wrapped);
    }

    public StateMachine<EventType> getUnderlyingFSM() {
        return wrapped;
    }

    @Override
    public synchronized void transit(StateMachineEvent<EventType> event)
            throws InvalidEventException {
        wrapped.transit(event);
    }

    /*@Override
     public boolean isValidState(State<EventType> state) {
     return wrapped.isValidState(state);
     }*/
    @Override
    public synchronized boolean isInFinalState() {
        return wrapped.isInFinalState();
    }

    @Override
    public synchronized State<EventType> getCurrentState() {
        return wrapped.getCurrentState();
    }

    /*@Override
     public Collection<State<EventType>> getStates() {
     return wrapped.getStates();
     }*/
    @Override
    public State<EventType> getStateByName(String stateName) {
        return wrapped.getStateByName(stateName);
    }
    /*
     @Override
     public synchronized void completeInitialization(
     HashMap<Object, HashMap<Object, Object>> initializer)
     throws BadStateMachineSpecification {
     if (!wrapped.initializationCompleted())
     wrapped.completeInitialization(initializer);
		
     }

     @Override
     public synchronized boolean initializationCompleted() {
     return wrapped.initializationCompleted();
     }
     @Override
     public void generateInternalEvent(StateMachineEvent<EventType> internalEvent) {
     synchronized (this) {
     wrapped.generateInternalEvent(internalEvent);
     } 
		
     }
     */

    @Override
    public synchronized boolean setProperty(Object name, Object value) {
        return wrapped.setProperty(name, value);
    }

    @Override
    public synchronized Object getProperty(Object name) {
        return wrapped.getProperty(name);
    }

    @Override
    public synchronized void acceptWrapperTransport(FSMWrapperTransport<EventType> transport)
    throws FSMWrapperException{
        wrapped.acceptWrapperTransport(transport);
    }

    @Override
    public String getNameOfCurrentState() {
        return wrapped.getNameOfCurrentState();
    }

    @Override
    public Set<String> getStateNames() {
        return wrapped.getStateNames();
    }

    @Override
    public synchronized boolean replaceWrappedWith(StateMachine<EventType> newRef) {
        wrapped = newRef;
        return true;
    }

}