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
 * this class allows state to defined custom logic of transition.
 * state object is consulted to obtain name of the target state for FSM to transit
 * to. State must implement CustomizedTransitionsLogicState interface
 * 
 * @author blitvin
 */
class CustomizedLogicTransition<EventType extends Enum<EventType>> implements Transition<EventType> {
    CustomizedTransitionsLogicState<EventType> myState= null;
    StateMachineDriver<EventType> containingFSM = null;
    
    @Override
    public FSMNode<EventType> getTarget(StateMachineEvent<EventType> event){
        return containingFSM.getNodeByName(myState.stateToTransitTo(event)); // error handling
    }
        
    @Override
    public void onStateMachineInitialized(Map<?, ?> initializer, StateMachineDriver<EventType> containingMachine,
                                        State<EventType> fromState) throws BadStateMachineSpecification {
        try {
        myState = (CustomizedTransitionsLogicState<EventType>) fromState;
        } catch(Exception e){
            throw new BadStateMachineSpecification("CustomizedLogicTransition works only with CustomizedTransitionsLogicState");
        }
        containingFSM = containingMachine;
    }
}