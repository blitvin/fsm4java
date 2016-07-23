/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.concurrent;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineAspects;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.AspectEnabledStateMachine;
import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.ASPECT;
import static org.blitvin.statemachine.StateMachineBuilder.TARGET_STATE;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.buildertest.BuilderTestState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author blitvin
 */
public class CancellationTest {

    private static class BarrierAspect implements StateMachineAspects {

        private final Semaphore fsmSemaphore;
        private final Semaphore mainSemaphore;

        public BarrierAspect(Semaphore fsmSemaphore, Semaphore mainSemaphore) {
            this.fsmSemaphore = fsmSemaphore;
            this.mainSemaphore = mainSemaphore;
        }

        @Override
        public boolean onTransitionStart(StateMachineEvent event) {
            return true;
        }

        @Override
        public void onNullTransition(StateMachineEvent event) {
        }

        @Override
        public boolean onControlLeavesState(StateMachineEvent event, State currentState, State newState) {
            return true;
        }

        @Override
        public boolean onControlEntersState(StateMachineEvent event, State currentState, State prevState) {
            try {
                fsmSemaphore.acquire();
            } catch (InterruptedException ex) {
                fail("Unexpected exception " + ex);
            } finally {
                return true;
            }
        }

        @Override
        public void onTransitionFinish(StateMachineEvent event, State currentState, State prevState) {
            mainSemaphore.release();
        }

        @Override
        public void setContainingMachine(StateMachine machine) {
        }

    }
    /*
     *                                    -> (third) 
     *                       A         A /
     *  The machine is (first)--> (second)
     *                                  B  \->(forth)
     * so if second A cancelled resulting state is (forth), if not - (third)
     * for sequence A-A-B
     */

    private ConcurrentStateMachine<TestEnum> getMachine(BarrierAspect aspect) {
        try {

            StateMachineBuilder<TestEnum> builder = new StateMachineBuilder(ASPECT, TestEnum.class);
            AspectEnabledStateMachine<TestEnum> aspectFSM = (AspectEnabledStateMachine<TestEnum>) builder
                    .addState("first", new BuilderTestState()).markStateAsInitial()
                    .addDefaultTransition().addProperty(TARGET_STATE, "second")
                    .addState("second", new BuilderTestState())
                    .addTransition(TestEnum.A).addProperty(TARGET_STATE, "third")
                    .addTransition(TestEnum.B).addProperty(TARGET_STATE, "forth")
                    .addState("third", new BuilderTestState())
                    .addDefaultTransition().addProperty(TARGET_STATE, "third")
                    .addState("forth", new BuilderTestState())
                    .addDefaultTransition().addProperty(TARGET_STATE, "forth")
                    .addFSMProperty(StateMachineBuilder.ASPECTS_PROPERTY, aspect)
                    .build();
            ConcurrentStateMachine<TestEnum> retVal = new ConcurrentStateMachine<>(aspectFSM);
            retVal.start();
            return retVal;
        } catch (BadStateMachineSpecification ex) {
            fail("Unexpected exception during construction of the machine:" + ex);
        }
        return null;
    }

    @Test
    public void testNoCancelation() {
        try {
            Semaphore fsmSemaphore = new Semaphore(1);
            Semaphore mainSemaphore = new Semaphore(1);
            fsmSemaphore.acquire();
            mainSemaphore.acquire();
            BarrierAspect aspect = new BarrierAspect(fsmSemaphore, mainSemaphore);
            ConcurrentStateMachine<TestEnum> cm = getMachine(aspect);
            cm.fireAndForgetTransit(new TestEvent<>(TestEnum.A));
            fsmSemaphore.release();
            mainSemaphore.acquire();
            cm.fireAndForgetTransit(new TestEvent<>(TestEnum.A));
            fsmSemaphore.release();
            mainSemaphore.acquire();
            cm.fireAndForgetTransit(new TestEvent<>(TestEnum.B));
            fsmSemaphore.release();
            mainSemaphore.acquire();
            State s1 = cm.getCurrentState();
            State s2 = cm.getStateByName("third");
            assertEquals("third", cm.getNameOfCurrentState());
            assertEquals(s1, s2);
            assertEquals(cm.getCurrentState(), cm.getStateByName("third"));
            fsmSemaphore.release();
            cm.shutDown();
        } catch (InterruptedException ex) {
            fail("Unexpected exception happened in testNoCancellation:" + ex);
        }
    }

    @Test
    public void testWithCancelation() {
        try {
            Semaphore fsmSemaphore = new Semaphore(1);
            Semaphore mainSemaphore = new Semaphore(1);
            BarrierAspect aspect = new BarrierAspect(fsmSemaphore, mainSemaphore);
            fsmSemaphore.acquire();
            mainSemaphore.acquire();
            ConcurrentStateMachine<TestEnum> cm = getMachine(aspect);
            Future<StampedState<TestEnum>> futureFirstTransition = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            Future<StampedState<TestEnum>> future = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            assertTrue(future.cancel(true));
            assertFalse(future.cancel(true));// second cancell must return false per Future spec
            fsmSemaphore.release();
            mainSemaphore.acquire();
            cm.fireAndForgetTransit(new TestEvent<>(TestEnum.B));
            fsmSemaphore.release();
            mainSemaphore.acquire();
            /* we need to send another event because of race  between state machine thread
             and main thread - aspect runs in "inner" state machine so it finishes before
             wrapper sets resulting state to eventqueue element. So if main calls 
             cm.getCurrentState() before wrapper assignes new value to stamped state
             it sees previous value of stamped state even if internal FSM's state is already
             changed to new one. There is no easy way to prevent this race without resync
             on next event, perhaps 
             */
            cm.fireAndForgetTransit(new TestEvent<>(TestEnum.A));
            fsmSemaphore.release();
            mainSemaphore.acquire();
            assertFalse(futureFirstTransition.cancel(true)); // future of completed task must return false
            State s1 = cm.getCurrentState();
            State s2 = cm.getStateByName("forth");
            assertEquals("forth", cm.getNameOfCurrentState());
            assertEquals(s1, s2);
            cm.shutDown();
        } catch (InterruptedException ex) {
            fail("Unexpected exception happened in testWithCancelation:" + ex);
        }
    }
}