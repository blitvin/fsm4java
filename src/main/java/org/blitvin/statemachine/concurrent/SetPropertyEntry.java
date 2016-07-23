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
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.blitvin.statemachine.FSMWrapperTransport;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineWrapperAcceptor;

/**
 * transport for propagating FSM property changes
 * @author blitvin
 */
public class SetPropertyEntry<EventType extends Enum<EventType>> implements FSMWrapperTransport<EventType>  {

    private final Object name;
    private final Object value;
    volatile boolean result;
    final CountDownLatch latch;
    
    public SetPropertyEntry(Object name, Object value){
        this.name= name;
        this.value = value;
        latch = new CountDownLatch(1);
    }
    
    public boolean  getResult(){
        try {
            latch.await();
        } catch (InterruptedException ex) {
            return false;
        }
        return result;
    }
    @Override
    public void apply(StateMachineWrapperAcceptor<EventType> machine, 
            StateMachineWrapperAcceptor<EventType> wrapped) {
        ((StateMachine<EventType>)wrapped).setProperty(name, value);
        latch.countDown();
    }
/*
    @Override
    public boolean shouldPropagate(StateMachine<EventType> machine, StateMachine<EventType> wrapped) {
        return true;// go through all wrappers
    }
  */  
}