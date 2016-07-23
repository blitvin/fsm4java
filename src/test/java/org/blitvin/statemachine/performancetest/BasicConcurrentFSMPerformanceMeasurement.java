package org.blitvin.statemachine.performancetest;

import java.util.concurrent.CountDownLatch;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.concurrent.ConcurrentStateMachine;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author blitvin
 */
public class BasicConcurrentFSMPerformanceMeasurement {

    public static String STATE_INITIAL = "initial";
    public static String STATE_START = "start";
    public static String STATE_MIDDLE = "middle";
    public static String STATE_FINISH = "finish";

    public static class SenderThread extends Thread {

        int batches;
        int batchSize;
        ConcurrentStateMachine<PerformanceEnum> concurrentFSM;
        CountDownLatch startLatch;

        public SenderThread(int batches, int batchSize, ConcurrentStateMachine<PerformanceEnum> concurrentFSM,
                CountDownLatch latch) {
            this.batches = batches;
            this.batchSize = batchSize;
            this.concurrentFSM = concurrentFSM;
            this.startLatch = latch;
        }

        @Override
        public void run() {
            PerformanceEvent event = new PerformanceEvent(PerformanceEnum.A);
            try {
                startLatch.await();
            } catch (InterruptedException ex) {
                return;
            }
            for (int i = 0; i < batches; ++i) {
                try {
                    for (int j = 1; j < batchSize; ++j) {
                        concurrentFSM.fireAndForgetTransit(event);
                    }
                    concurrentFSM.transit(event);
                } catch (InvalidEventException ex) {
                }
            }
        }
    }

    public static void runTest(int batches, int batchSize, int threads,
            ConcurrentStateMachine<PerformanceEnum> concurrentFSM) throws InvalidEventException, InterruptedException {
        SenderThread[] senders = new SenderThread[threads];
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < threads; ++i) {

            senders[i] = new SenderThread(batches, batchSize, concurrentFSM, latch);
        }

        for (SenderThread sender : senders) {
            sender.start();
        }
        concurrentFSM.transit(new PerformanceEvent(PerformanceEnum.A));
        latch.countDown();
        for (SenderThread sender : senders) {
            sender.join();
        }
        concurrentFSM.transit(new PerformanceEvent(PerformanceEnum.B));
        concurrentFSM.transit(new PerformanceEvent(PerformanceEnum.A));
        long start = (Long) concurrentFSM.getProperty(STATE_START);
        long finish = (Long) concurrentFSM.getProperty(STATE_FINISH);
        //System.out.println("Testing " + threads + " threads, total " + iterations * threads + " iterations,"
        //, starting "+ start + " ending "+finish 
        //      + " total runtime " + (finish - start) + " average throughput = " + (iterations * threads / (finish - start)));
        System.out.println("" + threads + '\t' + batches * batchSize * threads
                + '\t' + (finish - start) + '\t' + (batches * batchSize * threads) / (finish - start));
    }

    public static void main(String[] args) throws BadStateMachineSpecification, InvalidEventException, InterruptedException {
        StateMachine<PerformanceEnum> fsm
                = new StateMachineBuilder<PerformanceEnum>(StateMachineBuilder.FSM_TYPES.BASIC, PerformanceEnum.class).
                addState(STATE_INITIAL, new EmptyState()).markStateAsInitial().addDefaultTransition(STATE_START).
                addState(STATE_START, new MarkState(STATE_START)).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addState(STATE_MIDDLE, new EmptyState()).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addTransition(PerformanceEnum.B, STATE_FINISH).
                addState(STATE_FINISH, new MarkState(STATE_FINISH)).addDefaultTransition(STATE_INITIAL).build();
        ConcurrentStateMachine<PerformanceEnum> concurrentFSM = new ConcurrentStateMachine<>(fsm);
        concurrentFSM.completeInitialization();
        runTest(100, 10000, 10, concurrentFSM);
        int[] threads = {1, 2, 4, 5, 10, 20};
        int[] batchSizes = {1000, 5000, 10000};
        for (int j = 100000; j <= 1000000000; j *= 10) {
            for (int batchSize = 0; batchSize < batchSizes.length; ++batchSize) {
                System.out.println("Testing " + j + " iterations with block of" + batchSizes[batchSize]);
                System.out.println("Th\titer\truntime\tavg throughput (multiply by 1000 for TPS)");
                for (int i = 0; i < threads.length; ++i) {
                    if (j < batchSizes[batchSize] * threads[i]) {
                        System.out.println("Skipping case iterations=" + j + " batch size=" + batchSizes[batchSize] + " threads=" + threads[i]);
                    } else {
                        System.gc();
                        Thread.sleep(1000);
                        runTest(j / threads[i] / batchSizes[batchSize], batchSizes[batchSize], threads[i], concurrentFSM);
                    }
                }
            }
        }
    }
}