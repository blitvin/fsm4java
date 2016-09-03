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
package org.blitvin.statemachine.utils;

import java.util.Map;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * This is just an utility class to save developer time from writing empty 
 * call backs. One can extend this one or directly implement State<EventType>
 * Also see AnnotatedObjectStateFactory, perhaps this class suits your purpose too
 * @author blitvin
 * @param <EventType>
 */
public class StateSkeleton<EventType extends Enum<EventType>> implements State<EventType> {

    @Override
    public void onStateBecomesCurrent(StateMachineEvent<EventType> theEvent, State<EventType> prevState) {
    }

    @Override
    public void onStateIsNoLongerCurrent(StateMachineEvent<EventType> theEvent, State<EventType> nextState) {
    }

    @Override
    public void onInvalidTransition(StateMachineEvent<EventType> theEvent) {
    }

    @Override
    public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
    }

    @Override
    public void onStateDetachedFromFSM() {
    }
    
}