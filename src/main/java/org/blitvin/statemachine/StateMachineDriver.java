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
import java.util.Map;

/**
 * Internal interface of FSM. Methods defined here should be used only by package
 * classes and should not be revealed to outside as those are rely on implementation
 * of state machines
 * @author blitvin
 */
interface StateMachineDriver<EventType extends Enum<EventType>> extends StateMachine<EventType>, FSMStateView<EventType>{
    void setCurrentNode(FSMNode<EventType> node);
    /**
     * Because the FSM states and transitions may reference other states initialization is
     * performed in two stages - constructor creates all objects (states and transitions) and
     * this method completes initialization (intended to set up all inter-state and transition-state
     * relations)
     * @param initializer should contain map for states and transitions to key-values map used for initialization
     * That is keys should be either State of Transition objects and value is actually class specific e.g. SimpleTransition
     * expects String mapping "toState" -> name of destination state
     * @throws BadStateMachineSpecification can be thrown if initialization can't be completed e.g. completeInitializationCallback
     * of state or transition throws exception
     */
    
    FSMNode<EventType> getNodeByName(String name);
    
    void completeInitialization(Map<?,Map<?,?>> initializer) throws BadStateMachineSpecification;
    /**
     * 
     * @return true if FSM is fully initialized, that is in most cases completeInitializaiton was called
     */
    boolean initializationCompleted();
	
  /*      
    FSMNodeFactory<EventType> getDefaultFSMNodeFactory();
    FSMStateFactory<EventType> getDefaultFSMStateFactory();
    */
    HashMap<Object,Object> getFSMProperties();
}