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

import java.util.ArrayList;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineAspects;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * helper class allowing to invoke multiple aspect objects on the same point-cuts
 * in FSM
 * @author blitvin
 */
public class CompoundAspect<EventType extends Enum<EventType>> implements StateMachineAspects<EventType>{

    ArrayList<StateMachineAspects<EventType>> aspects = new ArrayList<>();
    StateMachine<EventType> containing = null;
    
    void addAspect(StateMachineAspects<EventType> aspect){
        aspects.add(aspect);
        if (containing != null)
            aspect.setContainingMachine(containing);
    }
    @Override
    public boolean onTransitionStart(StateMachineEvent<EventType> event) {
        boolean cont = true;
        for(StateMachineAspects<EventType> cur:aspects){
            cont = cur.onTransitionStart(event);
            if (!cont) break;
        }
        return cont;
    }

    @Override
    public void onNullTransition(StateMachineEvent<EventType> event) {
        for(StateMachineAspects<EventType> asp:aspects)
            asp.onNullTransition(event);
    }

    @Override
    public boolean onControlLeavesState(StateMachineEvent<EventType> event, State<EventType> currentState, State<EventType> newState) {
        for(StateMachineAspects<EventType> asp:aspects)
            if (!asp.onControlLeavesState(event, currentState, newState))
                return false;
        return true;
    }

    @Override
    public boolean onControlEntersState(StateMachineEvent<EventType> event, State<EventType> currentState, State<EventType> prevState) {
        for(StateMachineAspects<EventType> asp:aspects)
            if (!asp.onControlEntersState(event, currentState, prevState))
                return false;
        return true;
    }

    @Override
    public void onTransitionFinish(StateMachineEvent<EventType> event, State<EventType> currentState, State<EventType> prevState) {
        for(StateMachineAspects<EventType> asp:aspects)
            asp.onTransitionFinish(event,currentState,prevState);
    }

    @Override
    public void setContainingMachine(StateMachine<EventType> machine) {
        containing = machine;
        for(StateMachineAspects<EventType> asp:aspects)
            asp.setContainingMachine(machine);
    }
    
}