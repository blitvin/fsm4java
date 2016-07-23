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

/**
 * FSMWrapperTransport defined standard way of propagating wrapper effects in chain
 * of wrappers. The interface more or less follows visitor-acceptor pattern.
 * Wrapper should determine whether transport intended target is other wrapper (e.g.
 * Concurrent state machine should propagate reconfiguration request to state mahine confined
 * in dedicated thread, and wrapped object should take care of request )
 * @author blitvin
 * @param <EventType>
 */
public interface FSMWrapperTransport<EventType extends Enum<EventType>> {
    /**
     * run payload on provided state machine (typically wrapper)
     * apply should decide whether perform an action on wrapped method, current
     * machine, or propagate request to wrapped typically by invoking 
     * wrapped.acceptWrapperTransport()
     * @param machine FSM to apply payload on
     *  
     */
    void apply(StateMachineWrapperAcceptor<EventType> machine, StateMachineWrapperAcceptor<EventType> wrapped) 
            throws FSMWrapperException;
    
    /**
     * This method return true if payload is not intended to current state machine
     * hence it should be propagated to the next wrapper in chain. Typically this
     * method is called by wrapper with machine argument = this
     * @param machine current wrapper in chain
     * @param wrapped next wrapper in chain
     * @return 
     */
    //boolean shouldPropagate(StateMachine<EventType> machine, StateMachine<EventType> wrapped);
    //boolean requiresSyncronousProcessing();
}