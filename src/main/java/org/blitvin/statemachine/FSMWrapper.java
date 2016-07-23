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

import java.util.Set;

/**
 * FSMWrapper is an classes adding functionality to basic FSM, e.g. multi-threading
 * safeness, on the fly reconfiguration of the FSM etc. In most cases those are 
 * implementations of decorator design pattern. 
 * Note that classes implementing this interface implicitly expected to have interface
 * accepting StateMachine object. This allows chaining of decorators etc.
 *  
 * @author blitvin
 * @param <EventType>
 */
public class FSMWrapper<EventType extends Enum<EventType>> implements StateMachine<EventType> {
    protected StateMachine<EventType> wrapped;
    
    public FSMWrapper(StateMachine<EventType> wrapped){
        this.wrapped = wrapped;
    }

    @Override
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        wrapped.transit(event);
    }

    @Override
    public boolean isInFinalState() {
 return wrapped.isInFinalState();
    }

    @Override
    public State<EventType> getCurrentState() {
        return wrapped.getCurrentState();
    }

    //@Override
    //public abstract void acceptWrapperTransport(FSMWrapperTransport<EventType> transport);
    
    @Override
    public Set<String> getStateNames() {
        return wrapped.getStateNames();
    }

    @Override
    public String getNameOfCurrentState() {
        return wrapped.getNameOfCurrentState();
    }

    @Override
    public State<EventType> getStateByName(String stateName) {
        return wrapped.getStateByName(stateName);
    }

    @Override
    public boolean setProperty(Object name, Object value) {
     return wrapped.setProperty(name, value);
    }

    @Override
    public Object getProperty(Object name) {
    return wrapped.getProperty(name);
    }
    
    
    public boolean replaceWrappedWith(StateMachine<EventType> newRef){
        wrapped = newRef;
        return true;
    }

    @Override
    public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport)
     throws FSMWrapperException{
        wrapped.acceptWrapperTransport(transport);
    }
}