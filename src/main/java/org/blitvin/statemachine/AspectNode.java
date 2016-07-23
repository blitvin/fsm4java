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
 *
 * @author blitvin
 * 
 * 
 */
class AspectNode<EventType extends Enum<EventType>> implements FSMNode<EventType>{

    final private FSMNode delegate;
    private StateMachineAspects<EventType> aspects;
    StateMachineDriver<EventType> containingFSM;
    

    @Override
    public State<EventType> getState() {
        return delegate.getState();
    }

    @Override
    public boolean holdsFinalState() {
        return delegate.holdsFinalState();
    }

    @Override
    public void eventOut(StateMachineEvent<EventType> event, FSMNode<EventType> target) {
        delegate.eventOut(event,target);
    }

    @Override
    public void setState(State<EventType> state) {
        delegate.setState(state);
    }

    @Override
    public void setTransition(EventType event, Transition<EventType> transition) {
        delegate.setTransition(event, transition);
    }

    @Override
    public void setDefaultTransition(Transition<EventType> transition) {
        delegate.setDefaultTransition(transition);
    }

    @Override
    public void doesHoldFinalState() {
        delegate.doesHoldFinalState();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
    
    private static class AspectPropertyListener<EventType extends Enum<EventType>> implements PropertyChangeListener{

        final private AspectNode<EventType> node;
    
        public AspectPropertyListener(AspectNode<EventType> node){
            this.node = node;
        }
        
        @Override
        public void onPropertyChange(Object property, Object newVale, Object oldValue) {
            assert(property.equals(StateMachineBuilder.ASPECTS_PROPERTY));
            node.aspects = (StateMachineAspects<EventType>)newVale;
        }
        
    }
    
    private AspectPropertyListener<EventType> listener = null;
    public void setAspects(StateMachineAspects<EventType> aspects) {
        this.aspects = aspects;
    }
    
    AspectNode(FSMNode<EventType> delegate){
        this.delegate = delegate;
        aspects = null;
        containingFSM = null;
    }
       
    @Override
    public void eventIn(StateMachineEvent<EventType> event, FSMNode<EventType> prevState) {
        if (aspects == null)
            delegate.eventIn(event, prevState);
        else {
            if (aspects.onControlEntersState(event, delegate.getState(), prevState.getState()))
               delegate.eventIn(event,prevState);
            aspects.onTransitionFinish(event,containingFSM.getCurrentState(), prevState.getState());
        }
    }
    
    @Override
    public FSMNode<EventType> nodeToTransitTo(StateMachineEvent<EventType> event) throws InvalidEventException {
        if (aspects != null && !aspects.onTransitionStart(event))
            return null;
        try {
            FSMNode<EventType> target = delegate.nodeToTransitTo(event);
            if (target == null) {
                if (aspects != null) 
                    aspects.onNullTransition(event);
                return null;
            }
            if (aspects != null && !aspects.onControlLeavesState(event, delegate.getState(), target.getState()))
                return null;
            else
                return target;
        } catch(InvalidEventException e){
            if (aspects != null) 
                aspects.onTransitionFinish(event, delegate.getState(), delegate.getState());
            throw e;
        }
    }
    
    @Override
    public void onStateMachineInitialized(Map<?, ?> initializer, StateMachineDriver<EventType> containingMachine) throws BadStateMachineSpecification {
       /* initializer.put((?)delegate,
                (Object)initializer.get(this));*/
        ((Map<Object,Object>)initializer).put(delegate, initializer.get(this));
        delegate.onStateMachineInitialized(initializer,containingMachine);
        this.containingFSM = containingMachine;
        listener = new AspectPropertyListener<>(this);
        containingMachine.registerPropertyChangeListener(listener, StateMachineBuilder.ASPECTS_PROPERTY);
    }
}