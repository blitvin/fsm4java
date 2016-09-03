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

package org.blitvin.statemachine.performancetest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.concurrent.Executors.newFixedThreadPool;
import java.util.concurrent.LinkedBlockingQueue;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.concurrent.AsyncStateMachine;
import org.blitvin.statemachine.concurrent.FSMQueueSubmittable;
import org.blitvin.statemachine.concurrent.FSMThreadPoolFacade;

/**
 *
 * @author blitvin
 */
public class PooledFSMPerformanceMeasurement {
    public static String STATE_INITIAL = "initial";
    public static String STATE_START = "start";
    public static String STATE_MIDDLE = "middle";
    public static String STATE_FINISH = "finish";
    
    public static class SenderThread extends Thread {

        int blocks;
        int blockSize;
        AsyncStateMachine<PerformanceEnum> concurrentFSM;
        CountDownLatch startLatch;

        public SenderThread(int blocks, int blockSize, AsyncStateMachine<PerformanceEnum> concurrentFSM,
                CountDownLatch latch) {
            this.blocks = blocks;
            this.blockSize = blockSize;
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
            for (int i = 0; i < blocks; ++i) {
                for(int j=0; j< blockSize; ++j)
                 //  concurrentFSM.fireAndForgetTransit(event);
                
                try {
                    concurrentFSM.transit(event);
                } catch (InvalidEventException ex) {
                    System.err.println("got invalid event");
                }
            }
                
            
        }
    }

    public static void runFSMTest(int blocks, int blockSize, int threads, 
            AsyncStateMachine<PerformanceEnum> fsm,
            CountDownLatch startLatch, CountDownLatch endLatch) throws InvalidEventException, InterruptedException {
        SenderThread[] senders = new SenderThread[threads];
       // CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < threads; ++i) {

            senders[i] = new SenderThread(blocks, blockSize, fsm, startLatch);
        }

        for (SenderThread sender : senders) {
            sender.start();
        }
        fsm.transit(new PerformanceEvent(PerformanceEnum.A));
        startLatch.countDown();
        for (SenderThread sender : senders) {
            sender.join();
        }
        fsm.transit(new PerformanceEvent(PerformanceEnum.B));
        fsm.transit(new PerformanceEvent(PerformanceEnum.A));
       
        endLatch.countDown();
        
    }
    public static class TestThread extends Thread {
        AsyncStateMachine<PerformanceEnum> fsm;
        CountDownLatch startLatch;
        SenderThread senders[];
        
        public TestThread(int blocks, int blockSize, int threads, 
            AsyncStateMachine<PerformanceEnum> fsm,
            CountDownLatch startLatch) throws InvalidEventException{
            
            this.startLatch = startLatch;
            this.fsm = fsm;
            
            senders = new SenderThread[threads];
            for(int i = 0 ; i < threads; ++i) {
                senders[i] = new SenderThread(blocks,blockSize,fsm,startLatch);
               
            }
            fsm.transit(new PerformanceEvent(PerformanceEnum.A));
           
            
        }
        
        @Override
        public void run(){
            
                for(SenderThread sender: senders)
                    sender.start();
                startLatch.countDown();
                for(SenderThread sender: senders)
                    try {
                        sender.join();
                    } catch (InterruptedException ex) {
                        System.err.println("interrupted join");
                        return;
                    }
             try {   
                fsm.transit(new PerformanceEvent(PerformanceEnum.B));
                fsm.transit(new PerformanceEvent(PerformanceEnum.A));
            } catch (InvalidEventException ex) {
                System.err.println("got invalid ivent"+ ex);
            }
        }
        
    }
    
    public static void runTest(int fsms, int clients, int transactions_per_fsm)
    throws BadStateMachineSpecification, InvalidEventException, InterruptedException{
        ExecutorService pool = Executors.newFixedThreadPool(4);
       
        FSMThreadPoolFacade<PerformanceEnum>[] concurrentFSM = new FSMThreadPoolFacade[FSMS];
       CountDownLatch latch = new CountDownLatch(fsms);
       TestThread[] testThreads = new TestThread[fsms];
       
       System.out.println(" FSMs="+fsms+" senders =" +fsms*clients+" blockSize="+QUEUE_SIZE);
       if (transactions_per_fsm < QUEUE_SIZE * clients) {
           System.out.println(" Skipping  - too many clients for transactions_per_fsm");
           return;
       }
       
       for( int i = 0 ; i < fsms; ++i) {
        StateMachine<PerformanceEnum> fsm
                = new StateMachineBuilder<PerformanceEnum>(StateMachineBuilder.FSM_TYPES.BASIC, PerformanceEnum.class).
                addState(STATE_INITIAL, new EmptyState()).markStateAsInitial().addDefaultTransition(STATE_START).
                addState(STATE_START, new MarkState(STATE_START)).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addState(STATE_MIDDLE, new EmptyState()).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addTransition(PerformanceEnum.B, STATE_FINISH).
                addState(STATE_FINISH, new MarkState(STATE_FINISH)).addDefaultTransition(STATE_INITIAL).build();
        concurrentFSM[i] = new FSMThreadPoolFacade<>(fsm, pool, new LinkedBlockingQueue<FSMQueueSubmittable>());
       }
       for(int i = 0 ; i< fsms; ++i){
           testThreads[i] = new TestThread(transactions_per_fsm/QUEUE_SIZE/clients , 
                   QUEUE_SIZE, clients, concurrentFSM[i].getProxy(), latch);
           testThreads[i].start();
       }
       for(int i = 0 ; i< fsms; ++i)
           testThreads[i].join();
       long maxDelta =0;
       for(int i = 0 ; i < fsms; ++i) {
           AsyncStateMachine<PerformanceEnum> proxy = concurrentFSM[i].getProxy();
           long start = (Long) proxy.getProperty(STATE_START);
           long finish = (Long) proxy.getProperty(STATE_FINISH);
           if ( maxDelta < (finish - start))
               maxDelta  = (finish - start);
           
       }
       
        
       System.out.println(" Total transitions="+transactions_per_fsm *fsms + " elapsed miliseconds="+maxDelta
       + " average throughput TP/miliSec "+ ((long)transactions_per_fsm)*fsms/maxDelta);
       pool.shutdown();
    }
    
    
    public static final int FSMS = 5;
    public static final int CLIENTS_PER_FSM = 2;
    public static final int QUEUE_SIZE = 10000;
    public static final int TRANSACTIONS_PER_FSM = 1000000;
    
    
    
    
    public static void main(String[] args) throws BadStateMachineSpecification, InvalidEventException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
       FSMThreadPoolFacade<PerformanceEnum>[] concurrentFSM = new FSMThreadPoolFacade[FSMS];
       CountDownLatch latch = new CountDownLatch(FSMS);
       TestThread[] testThreads = new TestThread[FSMS];
       for( int i = 0 ; i < FSMS; ++i) {
        StateMachine<PerformanceEnum> fsm
                = new StateMachineBuilder<PerformanceEnum>(StateMachineBuilder.FSM_TYPES.BASIC, PerformanceEnum.class).
                addState(STATE_INITIAL, new EmptyState()).markStateAsInitial().addDefaultTransition(STATE_START).
                addState(STATE_START, new MarkState(STATE_START)).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addState(STATE_MIDDLE, new EmptyState()).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addTransition(PerformanceEnum.B, STATE_FINISH).
                addState(STATE_FINISH, new MarkState(STATE_FINISH)).addDefaultTransition(STATE_INITIAL).build();
        concurrentFSM[i] = new FSMThreadPoolFacade<>(fsm, pool, new LinkedBlockingQueue<FSMQueueSubmittable>());
       }
       for(int i = 0 ; i< FSMS; ++i){
           testThreads[i] = new TestThread(TRANSACTIONS_PER_FSM/QUEUE_SIZE/CLIENTS_PER_FSM , 
                   QUEUE_SIZE, CLIENTS_PER_FSM, concurrentFSM[i].getProxy(), latch);
           testThreads[i].start();
       }
       for(int i = 0 ; i< FSMS; ++i)
           testThreads[i].join();
       long maxDelta =0;
       for(int i = 0 ; i < FSMS; ++i) {
           AsyncStateMachine<PerformanceEnum> proxy = concurrentFSM[i].getProxy();
           long start = (Long) proxy.getProperty(STATE_START);
           long finish = (Long) proxy.getProperty(STATE_FINISH);
           if ( maxDelta < (finish - start))
               maxDelta  = (finish - start);
       }
       
       System.out.println(" FSMs="+FSMS+" senders =" +FSMS*CLIENTS_PER_FSM+" blockSize="+QUEUE_SIZE);
       
       System.out.println(" Total transitions="+TRANSACTIONS_PER_FSM *FSMS + " elapsed miliseconds="+maxDelta
       + " average throughput TP/miliSec "+ ((long)TRANSACTIONS_PER_FSM)*FSMS/maxDelta);
    
    pool.shutdown();
    
    int[] FSMS_SET= {2,5,10,20};
    int[] CLIENTS_SET = {2,5,10};
    int[] TRANSICTIONS_PER_FSM_SET= {10000000,100000000/*,1000000000*/};
   
    for(int fsms:FSMS_SET)
        for(int clients: CLIENTS_SET)
            for(int transitions: TRANSICTIONS_PER_FSM_SET){
                runTest(fsms, clients, transitions/fsms);
            }
    } 

}