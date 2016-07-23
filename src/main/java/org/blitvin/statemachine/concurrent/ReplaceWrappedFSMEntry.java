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
import org.blitvin.statemachine.FSMWrapperException;
import org.blitvin.statemachine.FSMWrapperTransport;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineWrapperAcceptor;

/**
 *
 * @author blitvin
 * @param <EventType>
 */
public class ReplaceWrappedFSMEntry<EventType extends Enum<EventType>> implements FSMWrapperTransport<EventType>  {
    private final StateMachine<EventType> newRef;
    private final CountDownLatch latch;
    volatile boolean updated;
    @Override
    public void apply(StateMachineWrapperAcceptor<EventType> machine, 
            StateMachineWrapperAcceptor<EventType> wrapped) throws FSMWrapperException{
        updated  = ((ConcurrentStateMachine.ProcessingThread) machine).replaceWrappedWith(newRef);
        latch.countDown();
    }

    /*@Override
    public boolean shouldPropagate(StateMachine<EventType> machine, StateMachine<EventType> wrapped) {
        return false;
    }*/
    public boolean checkIsSuccessfull(){
        try {
            latch.await();
            return updated;
        } catch (InterruptedException ex) {
            return false;
        }
    }
    public ReplaceWrappedFSMEntry(StateMachine<EventType> newRef){
        this.newRef = newRef;
        latch = new CountDownLatch(1);
        updated = false;
    }
}
    
