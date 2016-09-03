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


import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author blitvin
 */
class BasicNode<EventType extends Enum<EventType>> implements FSMNode<EventType>{

    protected State<EventType> state;
    protected EnumMap<EventType,Transition<EventType>> transitions;
    protected Transition<EventType> defaultTransition;
    protected boolean holdsFinalState;
    protected StateMachineDriver<EventType> containingFSM;
    protected final String name;
    
    @Override
    public State<EventType> getState() {
        return state;
    }
    
    @Override
    public void setState(State<EventType> state){
        if (this.state != null)
            this.state.onStateDetachedFromFSM();
        this.state = state;
    }

    @Override
    public void eventIn(StateMachineEvent<EventType> event, FSMNode<EventType> prevState) {
        state.onStateBecomesCurrent(event, prevState.getState());
    }

    @Override
    public FSMNode<EventType> nodeToTransitTo(StateMachineEvent<EventType> event) throws InvalidEventException {
        Transition<EventType> transition = transitions.get(event.getEventType());
        if (transition == NullTransition.NULL_TRANSITION)
            return null;
        if (transition == null){
            if (defaultTransition == null) {
                state.onInvalidTransition(event);
                throw new InvalidEventException();
            }
            if (defaultTransition == NullTransition.NULL_TRANSITION)
                return null;
           transition = defaultTransition;
        }
        
        FSMNode<EventType> target =transition.getTarget(event);
        
        return target;
    }

    @Override
    public boolean holdsFinalState() {
        return holdsFinalState;
    }
    
    
    BasicNode(String name,State<EventType> state, EnumMap<EventType,Transition<EventType>> transitions, 
            Transition<EventType> defaultTransition, boolean holdsFinalState){
        this.name = name;
        this.state = state;
        this.transitions = transitions;
        this.defaultTransition = defaultTransition;
        this.holdsFinalState = holdsFinalState;
    }
    
    BasicNode(String name,EnumMap<EventType,Transition<EventType>> transitions){
        state = null;
        this.name = name;
        this.transitions = transitions;
        defaultTransition = null;
        holdsFinalState = false;
    }
    
    @Override
    public void setTransition(EventType event,Transition<EventType> transition){
        transitions.put(event, transition);
    }
    
    
    
   /* @Override
    public void setFSMDriver(StateMachineDriver<EventType> driver) {
        containingFSM = driver;}*/

    @Override
    public void eventOut(StateMachineEvent<EventType> event, FSMNode<EventType> target) {
        state.onStateIsNoLongerCurrent(event, target.getState());
    }

    @Override
    public void onStateMachineInitialized(Map<?, ?> initializer, StateMachineDriver<EventType> containingMachine) throws BadStateMachineSpecification {
        containingFSM = containingMachine;
       for(Entry<EventType,Transition<EventType>> cur: transitions.entrySet()){
           cur.getValue().onStateMachineInitialized((Map<?,?>)initializer.get(cur.getValue()),
                   containingMachine, state);
       }
       if (defaultTransition != null) {
           defaultTransition.onStateMachineInitialized((Map<?,?>)initializer.get(defaultTransition), containingMachine, state);
       }
       getState().onStateAttachedToFSM((Map<?,?>)initializer.get(this),containingMachine);
    }

    
    

    @Override
    public void setDefaultTransition(Transition<EventType> defaultTransition){
        this.defaultTransition = defaultTransition;
    }
    
    @Override
    public void doesHoldFinalState(){
        holdsFinalState = true;
    }

    @Override
    public String getName() {
        return name;
    }
}