/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author blitvin
 */
@RunWith(Parameterized.class)
public class CancellationTest {

    public enum AsyncTypes {
        CONCURRENT_FSM,
        POOLED_FSM
    };
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private AsyncTypes fsmType;

    @Parameterized.Parameters
    public static Collection asyncMachineBuilders() {
        Object[][] builders = {
            {AsyncTypes.CONCURRENT_FSM},
            {AsyncTypes.POOLED_FSM}
        };
        return Arrays.asList(builders);
    }

    public CancellationTest(AsyncTypes fsmType) {
        this.fsmType = fsmType;
    }

    @AfterClass
    public static void shutdownPool(){
        pool.shutdown();
    }
    private static class DelayExecutionAspect implements StateMachineAspects {

        private final CountDownLatch latch;

        public DelayExecutionAspect(CountDownLatch latch) {
            this.latch = latch;
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
                latch.await();
            } catch (InterruptedException ex) {
                fail("Unexpected exception " + ex);
            } finally {
                return true;
            }
        }

        @Override
        public void onTransitionFinish(StateMachineEvent event, State currentState, State prevState) {
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

    private AsyncStateMachine<TestEnum> getMachine(DelayExecutionAspect aspect) {
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
            switch (fsmType) {
                case CONCURRENT_FSM:
                    ConcurrentStateMachine<TestEnum> retVal = new ConcurrentStateMachine<>(aspectFSM);
                    retVal.start();
                    return retVal;
                case POOLED_FSM:
                    return new FSMThreadPoolFacade<>(aspectFSM, pool,
                            new LinkedBlockingQueue<FSMQueueSubmittable>()).getProxy();

            }
            return null;

        } catch (BadStateMachineSpecification ex) {
            fail("Unexpected exception during construction of the machine:" + ex);
        }
        return null;
    }

    private void shutdownConcurrent(AsyncStateMachine<TestEnum> fsm) {
        if (fsmType == AsyncTypes.CONCURRENT_FSM) {
            ((ConcurrentStateMachine<TestEnum>) fsm).shutDown();
        }
    }

    @Test
    public void testNoCancelation() throws ExecutionException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            DelayExecutionAspect aspect = new DelayExecutionAspect(latch);
            AsyncStateMachine<TestEnum> cm = getMachine(aspect);
            Future<StampedState<TestEnum>> future = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            Future<StampedState<TestEnum>> future2 = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            Future<StampedState<TestEnum>> future3 = cm.asyncTransit(new TestEvent<>(TestEnum.B));
            
            latch.countDown();
            StampedState<TestEnum> state = future.get();
            assertEquals(2,state.getStamp());
            assertEquals(state.getState(),cm.getStateByName("second"));
            state = future2.get();
            assertEquals(state.getState(),cm.getStateByName("third"));
            assertEquals(3,state.getStamp());
            state = future3.get();
            assertEquals(state.getState(),cm.getStateByName("third"));
            assertEquals(4,state.getStamp());
            state = cm.getCurrentStampedState();
            assertEquals(state.getState(),cm.getStateByName("third"));
            assertEquals(4,state.getStamp());
            assertEquals("third", cm.getNameOfCurrentState());
            shutdownConcurrent(cm);
        } catch (InterruptedException ex) {
            fail("Unexpected exception happened in testNoCancellation:" + ex);
        }
    }

    @Test
    public void testWithCancelation() throws ExecutionException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            DelayExecutionAspect aspect = new DelayExecutionAspect(latch);
            AsyncStateMachine<TestEnum> cm = getMachine(aspect);
            Future<StampedState<TestEnum>> future = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            Future<StampedState<TestEnum>> future2 = cm.asyncTransit(new TestEvent<>(TestEnum.A));
            Future<StampedState<TestEnum>> future3 = cm.asyncTransit(new TestEvent<>(TestEnum.B));
            assertTrue(future2.cancel(true));
            assertFalse(future2.cancel(true));// second cancell must return false per Future spec
            latch.countDown();
            StampedState<TestEnum> state = future.get();
            assertTrue(future2.isCancelled());
            try {
                future2.get();
                        
            }
            catch(CancellationException ex){
                
            }
            catch(ExecutionException ex) {
                fail("expecting cancellation exception");
            }
            state = future3.get();
            assertEquals(state.getState(),cm.getStateByName("forth"));
            assertEquals(3, state.getStamp());
            
            state = cm.getCurrentStampedState();
            assertEquals(state.getState(),cm.getStateByName("forth"));
            assertEquals(3, state.getStamp());
            assertEquals("forth",cm.getNameOfCurrentState());
            shutdownConcurrent(cm);
        } catch (InterruptedException ex) {
            fail("Unexpected exception happened in testWithCancelation:" + ex);
        }
    }
}
