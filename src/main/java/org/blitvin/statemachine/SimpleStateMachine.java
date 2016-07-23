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

import java.util.HashMap;

/**
 * SimpleStateMachine is FSM allowing one internal event per state processing,
 * once internal event emitted by state callback, FSM trigger corresponding transition
 * after handler finishes execution. SimpleStateMachine emulates receiving internal 
 * event just after completion of original event processing. It is possible to emit
 * multiple internal events during handling "external" one, but only one internal event
 * is allowed at any particular time. If state callback invokes generateInternal
 * event more than once InvalidEvent exception is raised. One internal event per
 * state handling is enough for most cases. See expression parser example, syntax FSM
 * in particular for illustration of internal events usage
 *
 * @author blitvin
 */
 class SimpleStateMachine<EventType extends Enum<EventType>> 
    extends BasicStateMachine<EventType> implements FSMSupportingInternalEvents<EventType> {
    StateMachineEvent<EventType> event2process;
     
    public SimpleStateMachine(HashMap<String,FSMNode<EventType>>nodes, FSMNode<EventType> initial){
         super(nodes,initial);
         event2process = null;
     }

    @Override
    public void generateInternalEvent(StateMachineEvent<EventType> internalEvent) throws InvalidEventException {
        if (event2process != null)
            throw new InvalidEventException();
        event2process = internalEvent;
    }
    
    
    @Override
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        event2process = event;
        do {
            StateMachineEvent<EventType> nextEvent = event2process;
            event2process = null;
            FSMNode<EventType> next = current.nodeToTransitTo(nextEvent);
            if (next != null) {
                current.eventOut(event, next);
                next.eventIn(event, current);
                setCurrentNode(next);
            }
        } while (event2process != null);
    }
}