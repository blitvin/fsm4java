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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.blitvin.statemachine.StateMachine;

/**
 * This class provides abstraction over ExecutorService (thread pool) for
 * AsyncStateMachine implementation. It ensures only single transition for
 * wrapped FSM is executed at given time, provides proxy for the pooled FSM
 * manages event execution etc. The implementation is close coupled with 
 * PooledStateMachine
 * 
 * @author blitvin
 * @param <EventType>
 */
public class FSMThreadPoolFacade<EventType extends Enum<EventType>> {

    static final class QuerryData<EventType extends Enum<EventType>> {

        final StateMachine<EventType> fsm; // <-- because of this the class is not immutable, carefull
        final StampedState<EventType> stampedState;
        final String currentStateName;
        final boolean isInFinalState;

        public QuerryData(StateMachine<EventType> fsm, int generation) {
            this.stampedState = new StampedState<>(fsm.getCurrentState(), generation);
            this.currentStateName = fsm.getNameOfCurrentState();
            this.isInFinalState = fsm.isInFinalState();
            this.fsm = fsm;
        }
    }
    static final Future queueingFailed = new Future(){

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            throw new ExecutionException("events queue is full", null);
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new ExecutionException("events queue is full", null);
        }
        
    };
    
    StateMachine<EventType> fsm;
    final private ExecutorService pool;
    final private AtomicInteger pending;
    final private BlockingQueue<FSMQueueSubmittable> queue;
    volatile int generation = 1; // need to be volatile?
    volatile QuerryData<EventType> querry;

    public FSMThreadPoolFacade(StateMachine<EventType> fsm,
            ExecutorService pool, BlockingQueue<FSMQueueSubmittable> queue) {
        this.pool = pool;
        this.queue = queue;
        pending = new AtomicInteger();
        this.fsm = fsm;
        querry = new QuerryData<>(fsm, generation);
    }

    public AsyncStateMachine<EventType> getProxy() {
        return new PooledStateMachineProxy<>(this);
    }

    StampedState<EventType> notifyFSMChange() {
        querry = new QuerryData<>(fsm, ++generation);
        return querry.stampedState;
    }

    <T> Future<T> process(Callable<T> entry, CallableQueueEntryProducer<T> producer) {
        if (pending.incrementAndGet() == 1) {
            return pool.submit(entry);
        } else {
            QueuePair<T> pair = producer.get(entry);
            if (queue.add(pair.submittable))
                return pair.future;
            else
                return queueingFailed;
        }
    }

    Future<?> process(Runnable entry, RunnableQueueEntryProducer<?> producer) {
        if (pending.incrementAndGet() == 1) {
            return pool.submit(entry);
        } else {
            QueuePair<?> pair = producer.get(entry);
            if (queue.add(pair.submittable))
                return pair.future;
            else
                return queueingFailed;
        }
    }

    final static class FSMRunnableQueueEntry implements FSMQueueSubmittable {

        private final Runnable task;

        public FSMRunnableQueueEntry(Runnable task) {
            this.task = task;
        }

        @Override
        public boolean isCacnelled() {
            return false;
        }

        @Override
        public void submit(ExecutorService pool) {
            pool.submit(task);
        }
    }
    
    // fire and forget
    boolean process(Runnable entry){
        if (pending.incrementAndGet() == 1) {
            pool.submit(entry);
            return true;
        } else {
           return queue.add(new FSMRunnableQueueEntry(entry));
        }
    }
    
    void setNextThingToProcess() {
        int queueLen;
        do {
            queueLen = pending.decrementAndGet();
            if (queueLen > 0) {
                try {
                    FSMQueueSubmittable entry = queue.take();
                    assert (entry != null);
                    if (entry.isCacnelled()) {
                        continue; // don't process this
                    }
                    entry.submit(pool);
                    return;
                } catch (InterruptedException ex) {
                    pending.incrementAndGet(); //return counter on bad attempt
                }
            }
        } while (queueLen > 0);
    }

    void replaceWrappedFSM(StateMachine<EventType> fsm) {
        this.fsm = fsm;
    }
}
