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
 * External world's interface of FSM. Transition from state to state is 
 * triggered by invocation of transit() with particular event
 * @author blitvin
 * @param <EventType>
 */
public interface StateMachine<EventType extends Enum<EventType>> extends FSMCommonInterface<EventType>,
        StateMachineWrapperAcceptor<EventType>{

    /**
     * transit to new state according to event
     *
     * @param event new event
     * @throws InvalidEventException thrown if state machine doesn't define
     * transition for this type of event( including absence of default
     * transition for current state)
     */
    void transit(StateMachineEvent<EventType> event) throws InvalidEventException;

    /**
     *
     * @return true if state machine is in final state i.e. current state marked
     * as final
     */
    boolean isInFinalState();

    /**
     *
     * @return current state of the machine
     */
    State<EventType> getCurrentState();

   
}