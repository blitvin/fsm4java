/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   NioServer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachineBuilder;
import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.BASIC;
import static org.blitvin.statemachine.StateMachineBuilder.TARGET_STATE;
import org.blitvin.statemachine.buildertest.BuilderTestState;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThreadSafeMachineTest {

    private ConcurrentStateMachine<TestEnum> buildMachine() throws BadStateMachineSpecification {

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
        ConcurrentStateMachine<TestEnum> retVal = new ConcurrentStateMachine<>(b.build());
        retVal.start();
        return retVal;

    }

    @Test
    public void testRegularFuncitonality() throws BadStateMachineSpecification, InvalidEventException {
        ConcurrentStateMachine<TestEnum> machine = buildMachine();
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

    private static abstract class CASTester extends Thread {

        protected final CyclicBarrier barrier;
        protected final ConcurrentStateMachine<TestEnum> machine;
        protected volatile String failureReason;
        protected boolean shouldStop;
        protected Thread counterpart;

        private void notifyCounterpart() {
            if (counterpart != null) {
                counterpart.interrupt();
            }
        }

        public CASTester(ConcurrentStateMachine<TestEnum> machine, CyclicBarrier barrier, String name) {
            super(name);
            this.barrier = barrier;
            this.machine = machine;
            failureReason = null;
        }

        public void fail(String reason) {
            failureReason = reason;
            if (!Thread.currentThread().isInterrupted()) {
                notifyCounterpart();
            }
        }

        public boolean assertNull(Object o) {
            if (o != null) {
                failureReason = "expected null, but found " + o.toString();
                notifyCounterpart();
                return true;
            }
            return false;

        }

        public boolean assertNotNull(Object o) {
            if (o == null) {
                failureReason = "expected object not to be null";
                notifyCounterpart();
                return true;
            }
            return false;

        }

        public boolean assertEquals(Object o1, Object o2) {
            if (!o1.equals(o2)) {
                failureReason = o1.toString() + "!=" + o2.toString();
                notifyCounterpart();
                return true;
            }
            return false;
        }

        public boolean assertEquals(int expected, int actual, String message) {
            if (expected != actual) {
                failureReason = message + ": expected" + expected + " and found " + actual;
                notifyCounterpart();
                return true;
            }
            return false;
        }

        public void setCounterpart(Thread counterpart) {
            this.counterpart = counterpart;
        }
    }

    @Test
    public void testCASBackoff() throws BadStateMachineSpecification, InvalidEventException {
        ConcurrentStateMachine<TestEnum> machine = buildMachine();
        CyclicBarrier barrier = new CyclicBarrier(2);

        CASTester thread1 = new CASTester(machine, barrier, "testing thread 1") {
            @Override
            public void run() {
                StampedState<TestEnum> stamped = machine.getCurrentStampedState();
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }

                try {
                    if (assertNull(machine.CAStransit(
                            new TestEvent<TestEnum>(TestEnum.A), stamped.getStamp()))) {
                        return;
                    }
                } catch (InvalidEventException e) {
                    fail("Unexpected Exception:" + e.toString());
                }
            }
        };

        CASTester thread2 = new CASTester(machine, barrier, "testing thread2") {
            @Override
            public void run() {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
                try {
                    machine.transit(new TestEvent<TestEnum>(TestEnum.A));
                } catch (InvalidEventException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
            }
        };
        thread1.setCounterpart(thread2);
        thread2.setCounterpart(thread1);
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
        }
        assertNull(thread1.failureReason);
        assertNull(thread2.failureReason);
    }

    @Test
    public void testCASsuccessful() throws BadStateMachineSpecification, InvalidEventException {
        ConcurrentStateMachine<TestEnum> machine = buildMachine();
        CyclicBarrier barrier = new CyclicBarrier(2);

        CASTester thread1 = new CASTester(machine, barrier, "testing thread 1") {
            @Override
            public void run() {
                StampedState<TestEnum> stamped = machine.getCurrentStampedState();
                if (assertEquals(stamped.getState(), machine.getStateByName("first"))
                        || assertEquals(1, stamped.getStamp(), "Initial stamped state")) {
                    return;
                }

                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }
                stamped = machine.getCurrentStampedState();
                if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState(), machine.getStateByName("second"))
                        || assertEquals(2, stamped.getStamp(), "Stamped state after first transit")) {
                    return;
                }

                try {
                    stamped = machine.CAStransit(
                            new TestEvent<TestEnum>(TestEnum.A), stamped.getStamp());
                    if (assertNotNull(stamped.getState())
                            || assertEquals(stamped.getState(),machine.getStateByName("third"))
                            || assertEquals(3, stamped.getStamp(), "Stamped state after first transit")) {
                        return;
                    }
                } catch (InvalidEventException e) {
                    fail("Unexpected Exception:" + e.toString());
                    return;
                }

                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    fail("Unexpected InterruptedException:" + e.toString());
                    return;
                } catch (BrokenBarrierException e) {
                    fail("Unexpected BrokenBarrierException:" + e.toString());
                    return;
                }
                stamped = machine.getCurrentStampedState();
                if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState(),machine.getStateByName("third"))
                        || assertEquals(3, stamped.getStamp(), "Stamped state after first transit")) {
                    return;
                }
            }
        };

        CASTester thread2 = new CASTester(machine, barrier, "testing thread 2") {
            @Override
            public void run() {

                StampedState<TestEnum> stamped = machine.getCurrentStampedState();
                if (assertEquals(stamped.getState(),machine.getStateByName("first"))
                        || assertEquals(1, stamped.getStamp(), "Initial stamped state")) {
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }

                try {
                    stamped = machine.transitAndGetResultingState(new TestEvent<TestEnum>(TestEnum.A));
                    if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState(),machine.getStateByName("second"))
                            || assertEquals(2, stamped.getStamp(), "Stamped state after first transit")) {
                        return;
                    }
                } catch (InvalidEventException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
                stamped = machine.getCurrentStampedState();
                if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState(),machine.getStateByName("third"))
                        || assertEquals(3, stamped.getStamp(), "Stamped state after first transit")) {
                    return;
                }
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    fail("unexpected interrupt " + e.toString());
                    return;
                }
            }
        };
        thread1.setCounterpart(thread2);
        thread2.setCounterpart(thread1);
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
        }
        assertNull(thread1.failureReason);
        assertNull(thread2.failureReason);
    }
}