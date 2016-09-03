/*
 * Copyright (C) 2016 blitvin.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.blitvin.statemachine.concurrent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMWrapper;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineFactory;

/**
 *
 * @author blitvin
 */
public class FSMPool implements StateMachinePool {

    final ConcurrentHashMap<String, StateMachine> machines;
    final ConcurrentHashMap<String, StateMachineFactory> builders;
    final ExecutorService pool;
    
    public FSMPool(ExecutorService pool){
        this.pool = pool;
        machines = new ConcurrentHashMap<>();
        builders = new ConcurrentHashMap<>();
    }
    
    @Override
    public AsyncStateMachine<? extends Enum<?>> get(String name) throws BadStateMachineSpecification {
        return get(name,false);
    }

    @Override
    public AsyncStateMachine<? extends Enum<?>> get(String name, boolean getPrivateInstance) throws BadStateMachineSpecification {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AsyncStateMachine<? extends Enum<?>> registerStateMachine(StateMachine<? extends Enum<?>> machine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addFactory(StateMachineFactory factory) {
        
    }

    @Override
    public boolean setFSMWrappers(String name, List<FSMWrapper> wrappers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
