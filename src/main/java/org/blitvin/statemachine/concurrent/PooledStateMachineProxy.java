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

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.blitvin.statemachine.FSMWrapperException;
import org.blitvin.statemachine.FSMWrapperTransport;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.StateMachineWrapper;

/**
 *
 * @author blitvin
 * @param <EventType>
 */
class PooledStateMachineProxy<EventType extends Enum<EventType>> implements AsyncStateMachine<EventType> {

    private final FSMThreadPoolFacade<EventType> threadPoolFacade;
    private static final FSMSynchronousRunnableQueueEntry.Producer syncRunnableProducer
            = new FSMSynchronousRunnableQueueEntry.Producer();
    private final StampedTransitEntry.Producer<EventType> stampedProducer
            = new StampedTransitEntry.Producer<>();

    

    final class FSMTransitionCallable<EventType extends Enum<EventType>> implements Callable<StampedState<EventType>> {

        private final FSMThreadPoolFacade<EventType> threadPoolFacade;
        private final int generation;
        private final StateMachineEvent<EventType> event;

        public FSMTransitionCallable(FSMThreadPoolFacade<EventType> threadPoolFacade,
                int generation,
                StateMachineEvent<EventType> event) {
            this.threadPoolFacade = threadPoolFacade;
            this.generation = generation;
            this.event = event;
        }

        @Override
        public StampedState<EventType> call() throws Exception {
            try {
                if (generation == 0 || generation == threadPoolFacade.generation) {
                    threadPoolFacade.fsm.transit(event);
                    return threadPoolFacade.notifyFSMChange();
                } else {
                    return null;
                }
            } finally {
                threadPoolFacade.setNextThingToProcess();
            }
        }

    }

    final class FSMTransitionTask<EventType extends Enum<EventType>> implements Runnable {

        private final FSMThreadPoolFacade<EventType> threadPoolFacade;
        private final StateMachineEvent<EventType> event;
        private final boolean throwInvalidException;

        public FSMTransitionTask(FSMThreadPoolFacade<EventType> threadPoolFacade,
                StateMachineEvent<EventType> event,
                boolean throwInvalidException) {
            this.threadPoolFacade = threadPoolFacade;
            this.event = event;
            this.throwInvalidException = throwInvalidException;
        }

        @Override
        public void run() {
            try {
                threadPoolFacade.fsm.transit(event);
                threadPoolFacade.notifyFSMChange();
            } catch (InvalidEventException ex) {
                if (throwInvalidException)
                    throw new RuntimeException(ex);
            }
            finally {
                threadPoolFacade.setNextThingToProcess();
            }
        }

    }

    PooledStateMachineProxy(FSMThreadPoolFacade threadPoolFacade) {
        this.threadPoolFacade = threadPoolFacade;
    }

    static final class StampedTransitEntry<EventType extends Enum<EventType>> implements FSMQueueSubmittable {

        final Callable<StampedState<EventType>> callable;
        final QueuedCallableFuture<StampedState<EventType>> queuedFuture;

        public StampedTransitEntry(Callable<StampedState<EventType>> callable,
                QueuedCallableFuture<StampedState<EventType>> queuedFuture) {
            this.callable = callable;
            this.queuedFuture = queuedFuture;
        }

        @Override
        public boolean isCacnelled() {
            return queuedFuture.isCancelled();
        }

        @Override
        public void submit(ExecutorService pool) {
            queuedFuture.setFuture(pool.submit(callable));
        }

        final static class Producer<EventType extends Enum<EventType>>
                implements CallableQueueEntryProducer<StampedState<EventType>> {

            @Override
            public QueuePair<StampedState<EventType>> get(Callable<StampedState<EventType>> callable) {
                QueuePair<StampedState<EventType>> pair;
                QueuedCallableFuture<StampedState<EventType>> queuedFuture = new QueuedCallableFuture<>();
                pair = new QueuePair<>(new StampedTransitEntry(callable, queuedFuture),
                        queuedFuture);
                return pair;
            }

        }

    }

    @Override
    public StampedState<EventType> CAStransit(StateMachineEvent<EventType> event, int generation) throws InvalidEventException {
        try {
            return threadPoolFacade.process(new FSMTransitionCallable<>(threadPoolFacade, generation, event), stampedProducer).get();
        } catch (InterruptedException ex) {
            threadPoolFacade.setNextThingToProcess();
            throw new InvalidEventException("transition processing has been interrupted", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof InvalidEventException) {
                throw (InvalidEventException) ex.getCause();
            } else if (ex == FSMThreadPoolFacade.queueingFailed){
                throw new InvalidEventException("events queue is full");
            } else {
                throw new InvalidEventException("exception during executing transition", ex.getCause());
            }
        }
    }

    @Override
    public StampedState<EventType> transitAndGetResultingState(StateMachineEvent<EventType> event) throws InvalidEventException {
        return CAStransit(event, 0);
    }
    
    @Override
    public Future<StampedState<EventType>> asyncTransit(StateMachineEvent<EventType> event) {
        return threadPoolFacade.process(new FSMTransitionCallable<>(threadPoolFacade, 0, event), stampedProducer);
    }

    @Override
    public boolean fireAndForgetTransit(StateMachineEvent<EventType> event) {
        return threadPoolFacade.process(new FSMTransitionTask(threadPoolFacade, event,false));
    }

    @Override
    public StampedState<EventType> getCurrentStampedState() {
        return threadPoolFacade.querry.stampedState;
    }

    final static class FSMSynchronousRunnableQueueEntry implements FSMQueueSubmittable {

        final Runnable task;
        final QueuedCallableFuture queuedFuture;

        public FSMSynchronousRunnableQueueEntry(Runnable task,
                QueuedCallableFuture queuedFuture) {
            this.task = task;
            this.queuedFuture = queuedFuture;
        }

        @Override
        public boolean isCacnelled() {
            return false;
        }

        @Override
        public void submit(ExecutorService pool) {
            queuedFuture.setFuture(pool.submit(task));
        }

        final static class Producer implements RunnableQueueEntryProducer {

            @Override
            public QueuePair get(Runnable runnable) {
                QueuedCallableFuture queuedFuture = new QueuedCallableFuture();
                return new QueuePair(new FSMSynchronousRunnableQueueEntry(runnable, queuedFuture),
                        queuedFuture);

            }

        }
    }

    @Override
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        Future<?> future = threadPoolFacade.process(new FSMTransitionTask(threadPoolFacade, event,true), syncRunnableProducer);
        try {
            future.get();
        } catch (InterruptedException ex) {
            threadPoolFacade.setNextThingToProcess();
            throw new InvalidEventException("transition processing has been interrupted", ex);
        } catch (ExecutionException ex) {
            ex.getCause().printStackTrace();
            if (ex.getCause() instanceof InvalidEventException) {
                throw (InvalidEventException) ex.getCause();
            } if (ex == FSMThreadPoolFacade.queueingFailed){
                throw new InvalidEventException("events queue is full");
            } else if ((ex.getCause() instanceof RuntimeException)
                    && (ex.getCause().getCause() != null) && ex.getCause().getCause() instanceof InvalidEventException) {
                // FSMWrapperException is wrapped into RuntimeException and ExecutionException
                // all this because run() is not declared as throwing Exception ...
                throw (InvalidEventException) (ex.getCause().getCause());
            } else {
                throw new InvalidEventException("exception during processing of the transit", ex);
            }

        }
    }

    @Override
    public boolean isInFinalState() {
        return threadPoolFacade.querry.isInFinalState;
    }

    @Override
    public State<EventType> getCurrentState() {
        return threadPoolFacade.querry.stampedState.getState();
    }

    @Override
    public Set<String> getStateNames() {
        return threadPoolFacade.fsm.getStateNames();
    }

    @Override
    public String getNameOfCurrentState() {
        return threadPoolFacade.querry.currentStateName;
    }

    @Override
    public State<EventType> getStateByName(String stateName) {
        return threadPoolFacade.querry.fsm.getStateByName(stateName);
    }

    final static class FSMSetPropertyTask<EventType extends Enum<EventType>> implements Runnable {

        final FSMThreadPoolFacade<EventType> threadPoolFacade;
        final Object name;
        final Object value;

        public FSMSetPropertyTask(FSMThreadPoolFacade<EventType> threadPoolFacade, Object name, Object value) {
            this.threadPoolFacade = threadPoolFacade;
            this.name = name;
            this.value = value;
        }

        @Override
        public void run() {
            try {
                threadPoolFacade.fsm.setProperty(name, value);
            } finally {
                threadPoolFacade.setNextThingToProcess();
            }
        }
    }

    @Override
    public boolean setProperty(Object name, Object value) {
        Future future = threadPoolFacade.process(new FSMSetPropertyTask(threadPoolFacade, name, value), syncRunnableProducer);
        try {
            future.get();
        } catch (InterruptedException|ExecutionException ex) {
            return false;
        }
        return true;
    }

    @Override
    public Object getProperty(Object name) {
        return threadPoolFacade.querry.fsm.getProperty(name);
    }

    final static class FSMAcceptWrapperTask<EventType extends Enum<EventType>> implements Runnable,
            StateMachineWrapper<EventType> {

        final FSMThreadPoolFacade<EventType> threadPoolFacade;
        final FSMWrapperTransport<EventType> transport;

        public FSMAcceptWrapperTask(FSMThreadPoolFacade<EventType> threadPoolFacade,
                FSMWrapperTransport<EventType> transport) {
            this.threadPoolFacade = threadPoolFacade;
            this.transport = transport;
        }

        @Override
        public void run() {
            try {
                acceptWrapperTransport(transport);
            } catch (FSMWrapperException ex) {
                throw new RuntimeException(ex); // will be catched and forwarded to Future<>
            } finally {
                threadPoolFacade.setNextThingToProcess();
            }
        }

        @Override
        public boolean replaceWrappedWith(StateMachine<EventType> newRef) {
            threadPoolFacade.replaceWrappedFSM(newRef);
            return true;
        }

        @Override
        public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport) throws FSMWrapperException {
            transport.apply(this, threadPoolFacade.fsm);
        }

    }

    @Override
    public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport) throws FSMWrapperException {
        Future future = threadPoolFacade.process(new FSMAcceptWrapperTask(threadPoolFacade, transport), syncRunnableProducer);
        try {
            future.get();
        } catch (InterruptedException ex) {
            throw new FSMWrapperException("processing of the wrapper has been interrupted", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof FSMWrapperException) {
                throw (FSMWrapperException) ex.getCause();
            } if (ex == FSMThreadPoolFacade.queueingFailed){
                throw new FSMWrapperException("unable to deliver wrapper payload");
            } else if ((ex.getCause() instanceof RuntimeException)
                    && (ex.getCause().getCause() != null) && ex.getCause().getCause() instanceof FSMWrapperException) {
                // FSMWrapperException is wrapped into RuntimeException and ExecutionException
                // all this because run() is not declared as throwing Exception ...
                throw (FSMWrapperException) (ex.getCause().getCause());
            } else {
                throw new FSMWrapperException("exception during processing of the wrapper", ex);
            }
        }
    }

}
