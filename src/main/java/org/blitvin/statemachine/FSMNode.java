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
 * This is an internal interface for package only part of Node
 * @author blitvin
 */
interface FSMNode<EventType extends Enum<EventType>> {
    State<EventType> getState();
    void eventIn(StateMachineEvent<EventType> event, FSMNode<EventType> prevState);
    FSMNode<EventType> nodeToTransitTo(StateMachineEvent<EventType> event) throws InvalidEventException;
    boolean holdsFinalState();
    void eventOut(StateMachineEvent<EventType> event, FSMNode<EventType>target);
    void onStateMachineInitialized(Map<?,?>  initializer,
			         StateMachineDriver<EventType> containingMachine)
			throws BadStateMachineSpecification;
    
    
    void setState(State<EventType> state);
    void setTransition(EventType event ,Transition<EventType> transition);
    void setDefaultTransition(Transition<EventType> transition);
    void doesHoldFinalState();
    String getName();
}