/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.performancetest;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.concurrent.ConcurrentStateMachine;
import static org.blitvin.statemachine.performancetest.BasicConcurrentFSMPerformanceMeasurement.STATE_FINISH;
import static org.blitvin.statemachine.performancetest.BasicConcurrentFSMPerformanceMeasurement.STATE_START;

/**
 *
 * @author blitvin
 */
public class MultipleConcurrentFSMPerformanceMeasurement {
    public static String STATE_INITIAL = "initial";
    public static String STATE_START = "start";
    public static String STATE_MIDDLE = "middle";
    public static String STATE_FINISH = "finish";
    
    public static class SenderThread extends Thread {

        int blocks;
        int blockSize;
        ConcurrentStateMachine<PerformanceEnum> concurrentFSM;
        CountDownLatch startLatch;

        public SenderThread(int blocks, int blockSize, ConcurrentStateMachine<PerformanceEnum> concurrentFSM,
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
                   concurrentFSM.fireAndForgetTransit(event);
                
                try {
                    concurrentFSM.transit(event);
                } catch (InvalidEventException ex) {
                    System.err.println("got invalid event");
                }
            }
                
            
        }
    }

    public static void runFSMTest(int blocks, int blockSize, int threads, 
            ConcurrentStateMachine<PerformanceEnum> fsm,
            CountDownLatch startLatch, CountDownLatch endLatch) throws InvalidEventException, InterruptedException {
        BasicConcurrentFSMPerformanceMeasurement.SenderThread[] senders = new BasicConcurrentFSMPerformanceMeasurement.SenderThread[threads];
       // CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < threads; ++i) {

            senders[i] = new BasicConcurrentFSMPerformanceMeasurement.SenderThread(blocks, blockSize, fsm, startLatch);
        }

        for (BasicConcurrentFSMPerformanceMeasurement.SenderThread sender : senders) {
            sender.start();
        }
        fsm.transit(new PerformanceEvent(PerformanceEnum.A));
        startLatch.countDown();
        for (BasicConcurrentFSMPerformanceMeasurement.SenderThread sender : senders) {
            sender.join();
        }
        fsm.transit(new PerformanceEvent(PerformanceEnum.B));
        fsm.transit(new PerformanceEvent(PerformanceEnum.A));
       
        endLatch.countDown();
        
    }
    public static class TestThread extends Thread {
        ConcurrentStateMachine<PerformanceEnum> fsm;
        CountDownLatch startLatch;
        SenderThread senders[];
        
        public TestThread(int blocks, int blockSize, int threads, 
            ConcurrentStateMachine<PerformanceEnum> fsm,
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
        ConcurrentStateMachine<PerformanceEnum>[] concurrentFSM = new ConcurrentStateMachine[fsms];
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
        concurrentFSM[i] = new ConcurrentStateMachine<>(fsm, QUEUE_SIZE);
        concurrentFSM[i].completeInitialization();
       }
       for(int i = 0 ; i< fsms; ++i){
           testThreads[i] = new TestThread(transactions_per_fsm/QUEUE_SIZE/clients , 
                   QUEUE_SIZE, clients, concurrentFSM[i], latch);
           testThreads[i].start();
       }
       for(int i = 0 ; i< fsms; ++i)
           testThreads[i].join();
       long maxDelta =0;
       for(int i = 0 ; i < fsms; ++i) {
           long start = (Long) concurrentFSM[i].getProperty(STATE_START);
           long finish = (Long) concurrentFSM[i].getProperty(STATE_FINISH);
           if ( maxDelta < (finish - start))
               maxDelta  = (finish - start);
       }
       
        
       System.out.println(" Total transitions="+transactions_per_fsm *fsms + " elapsed miliseconds="+maxDelta
       + " average throughput TP/miliSec "+ ((long)transactions_per_fsm)*fsms/maxDelta);
    }
    
    
    public static final int FSMS = 10;
    public static final int CLIENTS_PER_FSM = 5;
    public static final int QUEUE_SIZE = 10000;
    public static final int TRANSACTIONS_PER_FSM = 1000000;
    
    
    
    
    public static void main(String[] args) throws BadStateMachineSpecification, InvalidEventException, InterruptedException {
     
       ConcurrentStateMachine<PerformanceEnum>[] concurrentFSM = new ConcurrentStateMachine[FSMS];
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
        concurrentFSM[i] = new ConcurrentStateMachine<>(fsm, QUEUE_SIZE);
        concurrentFSM[i].completeInitialization();
       }
       for(int i = 0 ; i< FSMS; ++i){
           testThreads[i] = new TestThread(TRANSACTIONS_PER_FSM/QUEUE_SIZE/CLIENTS_PER_FSM , 
                   QUEUE_SIZE, CLIENTS_PER_FSM, concurrentFSM[i], latch);
           testThreads[i].start();
       }
       for(int i = 0 ; i< FSMS; ++i)
           testThreads[i].join();
       long maxDelta =0;
       for(int i = 0 ; i < FSMS; ++i) {
           long start = (Long) concurrentFSM[i].getProperty(STATE_START);
           long finish = (Long) concurrentFSM[i].getProperty(STATE_FINISH);
           if ( maxDelta < (finish - start))
               maxDelta  = (finish - start);
       }
       
       System.out.println(" FSMs="+FSMS+" senders =" +FSMS*CLIENTS_PER_FSM+" blockSize="+QUEUE_SIZE);
       
       System.out.println(" Total transitions="+TRANSACTIONS_PER_FSM *FSMS + " elapsed miliseconds="+maxDelta
       + " average throughput TP/miliSec "+ ((long)TRANSACTIONS_PER_FSM)*FSMS/maxDelta);
    
    
    int[] FSMS_SET= {2,5,10,20};
    int[] CLIENTS_SET = {2,5,10};
    int[] TRANSICTIONS_PER_FSM_SET= {10000000,100000000/*,1000000000*/};
   
    for(int fsms:FSMS_SET)
        for(int clients: CLIENTS_SET)
            for(int transitions: TRANSICTIONS_PER_FSM_SET){
                runTest(fsms, clients, transitions);
            }
    } 

}