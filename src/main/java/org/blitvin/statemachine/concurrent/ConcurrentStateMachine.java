/*
 * (C) Copyright Boris Litvin 2014, 2015
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

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.blitvin.statemachine.FSMWrapper;
import org.blitvin.statemachine.FSMWrapperException;
import org.blitvin.statemachine.FSMWrapperTransport;

import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * ConcurrentStateMachine provides thread safe implementation of state machine.
 * The machine runs in dedicated thread events can come from multiple threads.
 * ConcurrentStateMachine allows sending events both synchronously and and
 * a-synchronously. Also CAS like conditional processing of events is supported,
 * that is event is processed only if no other events processed science sending
 * the event.
 *
 * @author blitvin
 *
 * @param <EventType> alphabet of the state machine
 */
public class ConcurrentStateMachine<EventType extends Enum<EventType>> extends FSMWrapper<EventType> implements AsyncStateMachine<EventType> {

    private boolean initialized = false;
    private volatile ProcessingThread<EventType> wrapper;

    protected final InterThreadCom<EventType> interThreadCom;

    

    protected static abstract class InterThreadCom<EventType extends Enum<EventType>> {

        protected final LinkedBlockingQueue<FSMWrapperTransport<EventType>> queue;

        public InterThreadCom(LinkedBlockingQueue<FSMWrapperTransport<EventType>> queue) {
            this.queue = queue;
        }

        abstract boolean send(FSMWrapperTransport<EventType> transport);

        FSMWrapperTransport<EventType> get() throws InterruptedException {
            return queue.take();
        }
    }

    private static class UnboundQueueCom<EventType extends Enum<EventType>>
            extends InterThreadCom<EventType> {

        public UnboundQueueCom() {
            super(new LinkedBlockingQueue<FSMWrapperTransport<EventType>>());
        }

        @Override
        public boolean send(FSMWrapperTransport<EventType> transport) {
            return queue.offer(transport);
        }

    }

    private class BoundedQueueCom<EventType extends Enum<EventType>> extends
            InterThreadCom<EventType> {

        public BoundedQueueCom(int queueCapacity) {
            super(new LinkedBlockingQueue<FSMWrapperTransport<EventType>>(queueCapacity));
        }

        @Override
        public boolean send(FSMWrapperTransport<EventType> transport) {
            try {
                queue.put(transport);
                return true;
            } catch (InterruptedException ex) {
                return false;
            }
        }
    }

    private class BoundedQueueWithTimeoutCom<EventType extends Enum<EventType>> extends
            InterThreadCom<EventType> {

        private final long timeout;
        private final TimeUnit timeUnit;

        public BoundedQueueWithTimeoutCom(int queueCapacity, long timeout, TimeUnit timeUnit) {
            super(new LinkedBlockingQueue<FSMWrapperTransport<EventType>>(queueCapacity));
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        @Override
        boolean send(FSMWrapperTransport<EventType> transport) {
            try {
                return queue.offer(transport, timeout, timeUnit);
            } catch (InterruptedException ex) {
                return false;
            }
        }

    }

    private class BoundedQueueNoWaitCom<EventType extends Enum<EventType>> extends
            InterThreadCom<EventType> {

        public BoundedQueueNoWaitCom(int queueCapacity) {
            super(new LinkedBlockingQueue<FSMWrapperTransport<EventType>>(queueCapacity));
        }

        @Override
        boolean send(FSMWrapperTransport<EventType> transport) {
            return queue.offer(transport);

        }

    }

    /**
     * The constructor accepts (possible not concurrent state safe)state machine
     * and wraps it in ConcurrentStateMachine Note that one must call
     * completeInitialization so that the state machine thread start running
     * Note that this constructor creates concurrent stater machine with no restrictions
     * on how many events are waiting for processing, so it is possible to get OutOfMemory
     * exception if too much event sent and processing state machine can't keep up with
     * senders
     * @param machine
     * @param threadName name of wrapper thread ( for display and debug usage
     * only)
     */
    public ConcurrentStateMachine(StateMachine<EventType> machine, String threadName) {
        super(machine);
        interThreadCom = new UnboundQueueCom<>();
        this.wrapper = new ProcessingThread<>(interThreadCom, machine);
        wrapper.setName(threadName);
        wrapper.setDaemon(true);
    }

    /**
     * The constructor accepts (possible not concurrent state safe)state machine
     * and wraps it in ConcurrentStateMachine Note that one must call
     * completeInitialization so that the state machine thread start running
     * This constructor creates FSM with at most queueCapacity events waiting
     * for processing. Thread attempting to place event with not enough room for events
     * is blocked until room becomes available
     * @param machine wrapped FSM
     * @param threadName name of dedicated thread ( good for debugging purposes etc.)
     * @param queueCapacity  how much events can wait processing
     */
    public ConcurrentStateMachine(StateMachine<EventType> machine, String threadName,
            int queueCapacity) {
        super(machine);
        interThreadCom = new BoundedQueueCom<>(queueCapacity);
        this.wrapper = new ProcessingThread<>(interThreadCom, machine);
        wrapper.setName(threadName);
        wrapper.setDaemon(true);
    }

    /**
     *  The constructor accepts (possible not concurrent state safe)state machine
     * and wraps it in ConcurrentStateMachine Note that one must call
     * completeInitialization so that the state machine thread start running
     * This constructor creates FSM with at most queueCapacity events waiting
     * for processing. Thread attempting to place event with not enough room for events
     * is blocked until room becomes available
     * @param machine wrapped FSM
     * @param queueCapacity 
     */
    public ConcurrentStateMachine(StateMachine<EventType> machine, int queueCapacity) {
        this(machine, "Async FSM", queueCapacity);
    }

    /**
     * The constructor accepts (possible not concurrent state safe)state machine
     * and wraps it in ConcurrentStateMachine Note that one must call
     * completeInitialization so that the state machine thread start running
     *
     * @param machine
     */
    public ConcurrentStateMachine(StateMachine<EventType> machine) {
        this(machine, "Async FSM");
    }

    /**
     * he constructor accepts (possible not concurrent state safe)state machine
     * and wraps it in ConcurrentStateMachine Note that one must call
     * completeInitialization so that the state machine thread start running
     * This constructor creates FSM with at most queueCapacity events waiting
     * for processing. Thread attempting to put new event when no room available
     * waits time specified by timeout. If event can't be placed during this timeout
     * exception is thrown
     * @param machine wrapped FSM
     * @param threadName 
     * @param queueCapacity maximal number of events awaiting processing
     * @param timeout 
     * @param timeUnit 
     */
    public ConcurrentStateMachine(StateMachine<EventType> machine,
            String threadName, int queueCapacity, long timeout, TimeUnit timeUnit) {
        super(machine);
        if (timeout > 0) {
            interThreadCom = new BoundedQueueWithTimeoutCom<>(queueCapacity, timeout, timeUnit);
        } else {
            interThreadCom = new BoundedQueueNoWaitCom<>(queueCapacity);
        }
        this.wrapper = new ProcessingThread<>(interThreadCom, machine);
        wrapper.setName(threadName);
        wrapper.setDaemon(true);
    }

    public ConcurrentStateMachine(StateMachine<EventType> machine,
            int queueCapacity, long timeout, TimeUnit timeUnit) {
        this(machine, "Async FSM", queueCapacity, timeout, timeUnit);
    }

    @Override
    public boolean setProperty(Object name, Object value) {
        SetPropertyEntry<EventType> transport = new SetPropertyEntry<>(name, value);
        interThreadCom.send(transport);
        return transport.getResult();
    }

    @Override
    public Object getProperty(Object name) {
        return wrapper.getMachine().getProperty(name);
    }

    @Override
    public String getNameOfCurrentState() {
        return wrapper.getMachine().getNameOfCurrentState();
    }

    @Override
    public Set<String> getStateNames() {
        return wrapper.getMachine().getStateNames();
    }

    @Override
    public boolean replaceWrappedWith(StateMachine<EventType> newRef) {
       ReplaceWrappedFSMEntry<EventType> transport = new ReplaceWrappedFSMEntry<>(newRef);
       if (interThreadCom.send(transport)){
           return transport.checkIsSuccessfull();
       }
        return false; // couldn't propagate to dedicated thread
    }

    static class ProcessingThread<EventType extends Enum<EventType>> extends Thread
            implements org.blitvin.statemachine.StateMachineWrapper<EventType> {

        //private final BlockingQueue<FSMWrapperTransport<EventType>> queue;
        private final InterThreadCom<EventType> interThreadCom;
        private StampedState<EventType> curState;

        private int generation;
        
        int advanceGeneration(){
            return ++generation;
        }
        int getGeneration(){
            return generation;
        }
        
        void setCurState(StampedState<EventType> newCurState) {
            curState = newCurState;
        }

        //private final Generation curGeneration;
        private StateMachine<EventType> machine;

        public StateMachine<EventType> getMachine() {
            return machine;
        }

        public ProcessingThread(InterThreadCom<EventType> interThreadCom,
                StateMachine<EventType> machine) {
            this.interThreadCom = interThreadCom;
            this.machine = machine;
            generation = 1;
            curState = new StampedState<>(machine.getCurrentState(),generation);
        }

       /* public Generation getCurrentGeneration() {
            return curGeneration;
        }*/

        @Override
        public void run() {
            while (true) {
                try {
                    interThreadCom.get().apply(this, machine);
                } catch (InterruptedException e) {
                    return;
                } catch (FSMWrapperException ex) { //TBD what to do with this?
                }

            }
        }

        @Override
        public boolean replaceWrappedWith(StateMachine<EventType> newRef) {
            machine = newRef;
            return true;
        }

        @Override
        public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport)
                throws FSMWrapperException {
            machine.acceptWrapperTransport(transport);
        }
    }

    @Override
    /**
     * send an event for synchronous processing ( that is , pause until the
     * event is processed in the state machine thread)
     *
     * @param event event to process in state machine
     */
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        CAStransit(event, 0);
    }

    /**
     * send event to the state machine , don't wait for processing completion
     *
     * @param event event to process in state machine
     */
    @Override
    public boolean fireAndForgetTransit(StateMachineEvent<EventType> event) {
        return interThreadCom.send(new EventQueueEntry<>(event, 0, null));
    }

    /**
     * send event to FSM and get Future object for obtaining result when
     * available
     *
     * @param event event to process
     * @return Future returning state when event is processed
     */
    @Override
    public Future<StampedState<EventType>> asyncTransit(StateMachineEvent<EventType> event) {
        EventQueueEntry<EventType> retVal = new EventQueueEntry<>(event, 0, new CountDownLatch(1));
        if (!interThreadCom.send((FSMWrapperTransport< EventType>) retVal))
            return null;
        else
            return retVal;
    }

    /**
     * send event to the state and pause until processing is completed. Return
     * StampedState object representing new state of the machine
     *
     * @param event event to process
     * @return pair of timestamp and state after transition triggered by the
     * event
     * @throws InvalidEventException thrown if there is no valid transition
     * exists for current state and particular event type of the event
     */
    public StampedState<EventType> transitAndGetResultingState(StateMachineEvent<EventType> event)
            throws InvalidEventException {
        return CAStransit(event, 0);
    }

    /**
     * This method executes transition if current generation of the inner FSM is
     * equal to "generation" parameter. This is kind-of poor mens CAS operation
     * (operation is executed completely if concurrency predicate is correct or
     * not at all). If execution of inner machine yields exception, this method
     * wraps it to StateMachineMultiThreadingException exception and re-throws
     * it up
     *
     * @param event - event to transit on
     * @param generation - expected generation count
     * @return - new state if transition happen, null otherwise
     * @throws InvalidEventException - current event is not applicable to
     * current state
     */
    @Override
    public StampedState<EventType> CAStransit(StateMachineEvent<EventType> event, int generation) throws InvalidEventException {
        EventQueueEntry<EventType> entry = new EventQueueEntry<>(event, generation, new CountDownLatch(1));
        try {
            if (!interThreadCom.send((FSMWrapperTransport< EventType>) entry))
                throw new InvalidEventException("failed to send event to processing thread");
            StampedState<EventType> retVal = entry.get();
            Exception e = entry.getException();
            if (e != null) {
                if (e instanceof InvalidEventException) {
                    throw (InvalidEventException) e;
                } else {
                    throw new InvalidEventException("exception happened during transition processing",e);
                }
            }
            return retVal;

        } catch (InterruptedException | ExecutionException e) {
            throw new InvalidEventException("failed to send event to processing thread",e); // actually don't think this can happen
        }
    }

    /* *
     * @see org.blitvin.statemachine.StateMachine#isValidState()
     * /
     @Override
     public boolean isValidState(State<EventType> state) {
     return machine.isValidState(state);
     }
     */
    /**
     * @see org.blitvin.statemachine.StateMachine#isInFinalState
     */
    @Override
    public boolean isInFinalState() {
        return wrapper.getMachine().isInFinalState();
    }

    /**
     * @see org.blitvin.statemachine.StateMachine#getCurrentState
     */
    @Override
    public State<EventType> getCurrentState() {
        return wrapper.getMachine().getCurrentState();
    }

    /**
     *
     * @return current state along with current generation count
     */
    @Override
    public StampedState<EventType> getCurrentStampedState() {
        return wrapper.curState;
    }
    /*
     @Override
     public Collection<State<EventType>> getStates() {
     return machine.getStates();
     }
     */

    /**
     * @param stateName
     * @see org.blitvin.statemachine.StateMachine#getStateByName
     */
    @Override
    public State<EventType> getStateByName(String stateName) {
        return wrapper.getMachine().getStateByName(stateName);
    }

    /**
     * Completes initialization of concurrent machine
     *
     * @see org.blitvin.statemachine.StateMachine#completeInitialization
     */
//	@Override
    public void completeInitialization() {
        if (!initialized) {
            wrapper.setDaemon(true);
            wrapper.start();
            initialized = true;
        }

    }

    /**
     * Completes initialization of both internal FSM and concurrent machine
     *
     * @return
     * @see org.blitvin.statemachine.StateMachine#completeInitialization
     */
    public boolean initializationCompleted() {
        return initialized;
    }

    @Override
    public void start() {
        completeInitialization();
    }

    /**
     * stop internal FSM and exit its dedicated thread
     */
    @Override
    public void shutDown() {
        wrapper.interrupt();
    }

    @Override
    public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport) throws FSMWrapperException {
        if (!interThreadCom.send(transport))
            throw new FSMWrapperException("can't propagate transport to processing thread");
        // processing thread decides what to do with this transport
    }
}