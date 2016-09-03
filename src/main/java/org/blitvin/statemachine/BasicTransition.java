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
import static org.blitvin.statemachine.StateMachineBuilder.TARGET_STATE;

/**
 * implementation of transition to predefined state
 * @author blitvin
 */
class BasicTransition<EventType extends Enum<EventType>> implements Transition<EventType> {

    //public static final String TARGET_STATE = "toState";
    FSMNode<EventType> target;

    public BasicTransition(FSMNode<EventType> target) {
        this.target = target;
    }

    public BasicTransition() {
    }

    @Override
    public FSMNode<EventType> getTarget(StateMachineEvent<EventType> event) {
        return target;
    }

    @Override
    public void onStateMachineInitialized(Map<?, ?> initializer, StateMachineDriver<EventType> containingMachine,
            State<EventType> fromState) throws BadStateMachineSpecification {
        String targetName = (String) initializer.get(TARGET_STATE);
        if (targetName == null) {
            throw new BadStateMachineSpecification("BasicTransition requires property " + TARGET_STATE+ " to be set");
        }
        target = containingMachine.getNodeByName(targetName);
        if (target == null) {
            throw new BadStateMachineSpecification("Can't find state " + targetName);
        }
    }
}