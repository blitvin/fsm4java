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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachineBuilder;
import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.BASIC;
import static org.blitvin.statemachine.StateMachineBuilder.TARGET_STATE;
import org.blitvin.statemachine.buildertest.BuilderTestState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author blitvin
 */
public class PooledMachineTest {
    
    static ExecutorService pool = Executors.newFixedThreadPool(4);
private AsyncStateMachine<TestEnum> buildMachine(ExecutorService pool) throws BadStateMachineSpecification {

        StateMachineBuilder<TestEnum> b = new StateMachineBuilder<>(BASIC, TestEnum.class);
        b.addState("first", new BuilderTestState()).markStateAsInitial()
                .addTransition(TestEnum.A).addProperty(TARGET_STATE, "second")
                .addDefaultTransition().addProperty(TARGET_STATE, "third")
                .addState("second", new BuilderTestState())
                .addTransition(TestEnum.A).addProperty(TARGET_STATE, "third")
                .addTransition(TestEnum.B).addProperty(TARGET_STATE, "first")
                .addState("third", new BuilderTestState()).markStateAsFinal()
                .addTransition(TestEnum.A).addProperty(TARGET_STATE, "first")
                .addTransition(TestEnum.B).addProperty(TARGET_STATE, "second")
                .addTransition(TestEnum.C).addProperty(TARGET_STATE, "third");
        
        return new FSMThreadPoolFacade<>(b.build(),pool,
                new LinkedBlockingQueue<FSMQueueSubmittable>()).getProxy();
    }

    @Test
    public void testRegularFlow() throws BadStateMachineSpecification, InvalidEventException{
        AsyncStateMachine<TestEnum> machine = buildMachine(pool);
        assertNotNull(machine);
        assertEquals("first", machine.getNameOfCurrentState());
        TestEvent<TestEnum> event = new TestEvent<>(TestEnum.A);
        machine.transit(event);
        assertEquals("second", machine.getNameOfCurrentState());
        assertFalse(machine.isInFinalState());
        try {
            machine.transit(event.setEvent(TestEnum.C));
            fail("Expecting to get InvalidEventType exception");
        } catch (InvalidEventException e) {
        }
        machine.transit(event.setEvent(TestEnum.B));
        assertEquals("first", machine.getNameOfCurrentState());
        assertFalse(machine.isInFinalState());

        machine.transit(event.setEvent(TestEnum.C));
        assertEquals("third", machine.getNameOfCurrentState());
        assertTrue(machine.isInFinalState());
        machine.transit(event);
        assertEquals("third", machine.getNameOfCurrentState());
    }
}
