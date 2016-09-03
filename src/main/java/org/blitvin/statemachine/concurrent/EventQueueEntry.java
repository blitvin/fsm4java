/*
 * (C) Copyright Boris Litvin 2014 - 2016
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FSM4Java is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.blitvin.statemachine.FSMWrapperTransport;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.StateMachineWrapperAcceptor;

/**
 * Transport to propagate events to FSM wrapped in dedicated thread
 *
 * @author blitvin
 */
final class EventQueueEntry<EventType extends Enum<EventType>> implements Future<StampedState<EventType>>, FSMWrapperTransport<EventType> {

    public static final int BEFORE_RUN = 0;
    public static final int RUNNING = 1;
    public static final int CANCELLED = 2;
    public static final int FINISHED = 3;

    private final StateMachineEvent<EventType> event;
    private final int stamp;
    private final CountDownLatch latch;
    private volatile StampedState<EventType> resultingState;
    private final boolean replyExpected;
    private volatile Exception exception;
    final AtomicInteger processingState;

    public EventQueueEntry(StateMachineEvent<EventType> event, int generation, CountDownLatch latch) {
        this.event = event;
        this.stamp = generation;
        this.latch = latch;
        this.replyExpected = (latch != null);
        resultingState = null;
        exception = null;
        processingState = new AtomicInteger(BEFORE_RUN);
    }

    public StampedState<EventType> getResultingState() {
        return resultingState;
    }

    public void setResultingState(StampedState<EventType> resultingState) {
        this.resultingState = resultingState;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    StateMachineEvent<EventType> getEvent() {
        return event;
    }

    int getGeneration() {
        return stamp;
    }

    /* void releaseLatch() {
        if (replyExpected) {
            latch.countDown();
        }
    }*/
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (processingState.compareAndSet(BEFORE_RUN, CANCELLED)) {
            latch.countDown();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return processingState.get() == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return processingState.get() == FINISHED;

    }

    @Override
    public StampedState<EventType> get() throws InterruptedException,
            ExecutionException {
        latch.await();
        if (isCancelled()) {
            throw new CancellationException();
        }
        return resultingState;
    }

    @Override
    public StampedState<EventType> get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException,
            TimeoutException {

        if (latch.await(timeout, unit)) {
            if (isCancelled()) {
                throw new CancellationException();
            }

            return resultingState;
        } else {
            throw new TimeoutException();
        }
    }

    void awaitResults() throws InterruptedException {
        latch.await();
    }

    @Override
    public void apply(StateMachineWrapperAcceptor<EventType> machine,
            StateMachineWrapperAcceptor<EventType> wrapped) {
        ConcurrentStateMachine.ProcessingThread wrapper = (ConcurrentStateMachine.ProcessingThread<EventType>) machine;
        //ConcurrentStateMachine.Generation gen = wrapper.getCurrentGeneration();
        StateMachine<EventType> fsm = (StateMachine<EventType>) wrapped;
        try {
            if (!processingState.compareAndSet(EventQueueEntry.BEFORE_RUN, EventQueueEntry.RUNNING)) {
                return;
            }
            if (getGeneration() != 0 && wrapper.getGeneration() != getGeneration()) {
                setResultingState(null);
            } else {
                fsm.transit(getEvent());
                StampedState<EventType> result = new StampedState<>(fsm.getCurrentState(), wrapper.advanceGeneration());
                if (replyExpected) {
                    setResultingState(result);
                }
                wrapper.setCurState(result);
            }
        } catch (Exception e) {
            setException(e);
            setResultingState(new StampedState<>(fsm.getCurrentState(), wrapper.advanceGeneration()));
            wrapper.setCurState(resultingState);
        } finally {
            if (replyExpected) {
                processingState.compareAndSet(EventQueueEntry.RUNNING,EventQueueEntry.FINISHED);
                latch.countDown();
            }
        }
    }

    /*
    @Override
    public boolean shouldPropagate(StateMachine<EventType> machine, StateMachine<EventType> wrapped) {
        return false;
    }*/
}
