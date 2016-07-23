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
import java.util.LinkedList;

/**
 * MultiInternalEventsStateMachine allows generation multiple internal events
 * during execution of business logic. The events are stored in a queue and 
 * processed sequentially. Handling of original event is finished when all internal
 * events (if any) are processed.
 * @author blitvin
 * @param <EventType>
 */
class MultiInternalEventsStateMachine<EventType extends Enum<EventType>> 
    extends BasicStateMachine<EventType> implements FSMSupportingInternalEvents<EventType>  {
    LinkedList<StateMachineEvent<EventType>> events2process;

    public MultiInternalEventsStateMachine(HashMap<String,FSMNode<EventType>>nodes, FSMNode<EventType> initial){
         super(nodes,initial);
         events2process = new LinkedList<>();
    }
    
    @Override
    public void generateInternalEvent(StateMachineEvent<EventType> internalEvent) throws InvalidEventException {
        events2process.add(internalEvent);
    }
    
    @Override
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        events2process.add(event);
        do {
            StateMachineEvent<EventType> nextEvent = events2process.poll();
            FSMNode<EventType> next = current.nodeToTransitTo(nextEvent);
            if (next != null) {
                current.eventOut(event, next);
                next.eventIn(event, current);
                setCurrentNode(next);
            }
        } while (!events2process.isEmpty());
    }
    
}