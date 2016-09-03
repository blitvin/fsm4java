/*
 * This class measures preformance of LinkedBlockingQueue.
 * ConcurrentStateMachine uses it for inter-process communication
 * so throughput of Concurrent FSM can't be more than that of the queue
 * Hence, overhead of the framework per se is delta of its throughput
 * and one of the queue..
 */
package org.blitvin.statemachine.performancetest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * @author blitvin
 */
public class TempQueuePerformanceMeasurement {
    
    static class Consumer extends Thread{
        final CountDownLatch startLatch;
        
        final private BlockingQueue<Integer> queue;
        volatile long elapsed;
        final int total;
        long start;
        long finish;
        public Consumer(BlockingQueue<Integer> queue, CountDownLatch startLatch,
                int total) {
            super();
            this.queue = queue;
            this.startLatch = startLatch;
            
            this.total = total;
        }
        @Override
        public void run(){
            try {
                startLatch.await();
            } catch (InterruptedException ex) {
                System.err.println("Interrupted latch");
                return;
            }
           start = System.nanoTime();
            for(int i = 0 ; i < total; ++i){
                try {
                    queue.take();
                } catch (InterruptedException ex) {
                    System.err.println("Interrupted queue");
                }
            }
            finish = System.nanoTime();
            elapsed = finish - start;
            
        }
    }
    
    static class Producer extends Thread {
        final BlockingQueue queue;
        final CountDownLatch latch;
        int total;
        public Producer(BlockingQueue queue, CountDownLatch latch, int total) {
            super();
            this.queue = queue;
            this.latch = latch;
            this.total = total;
        }
        
        @Override
        public void run(){
             try {
                latch.await();
            } catch (InterruptedException ex) {
                System.err.println("latch interrupted");
                return;
            }
           for(int i = 0 ; i < total; ++i){
                try {
                    queue.put(i);
                } catch (InterruptedException ex) {
                    System.err.println("Producer interrupted "+ex.toString());
                }
            }
            
        }
    }
    
    public static void test(int producers, int totalItems) throws InterruptedException{
        System.out.println("Total="+totalItems+" producers="+producers);
        
        LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer con = new Consumer(queue, latch, totalItems);
        con.start();
        Producer[] prods = new Producer[producers];
        for(int i = 0 ; i< producers; ++i) {
            prods[i] = new Producer(queue, latch, totalItems/producers);
            prods[i].start();
        }
        latch.countDown();
        System.out.println(latch.getCount());
        con.join();
        System.out.println(" elapsed time="+con.elapsed +
                " throughput="+totalItems/(con.elapsed/1000000L));
            
    }
    public static void main(String[] args) throws InterruptedException {
       int totalItems[] = {1000000,10000000, 100000000, 1000000000};
       int producers[] = {1,2,5,10,20};
       for(int tot:totalItems)
           for(int prod:producers)
               test(prod,tot);
    }
}
