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
 * FSMSupportingInternalEvents provides a way for State objects to generate 
 * internal events
 * 
 * @author blitvin
 * @param <EventType>
 */
public interface FSMSupportingInternalEvents<EventType extends Enum<EventType>> extends FSMStateView<EventType> {
    /**
     * This function can be invoked to notify the state machine of new event resulting from business logic processing 
     * (internal event) as opposite to external events coming from outside of FSM
     * @param internalEvent new internal event
     * @throws org.blitvin.statemachine.InvalidEventException thrown if event can't be processed
     */
	void generateInternalEvent(StateMachineEvent<EventType> internalEvent) throws InvalidEventException;
}