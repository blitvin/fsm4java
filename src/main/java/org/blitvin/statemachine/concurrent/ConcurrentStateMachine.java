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

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * ConcurrentStateMachine provides thread safe implementation of state machine. The machine runs in dedicated thread
 * events can come from multiple threads. ConcurrentStateMachine allows sending events both synchronously and and 
 * a-synchronously. Also CAS like conditional processing of events is supported, that is event is processed only if no other
 * events processed science sending the event.
 * @author blitvin
 *
 * @param <EventType> alphabet of the state machine
 */
public class ConcurrentStateMachine<EventType extends Enum<EventType>> implements StateMachine<EventType> {

	private boolean initialized = false;
	private final LinkedBlockingQueue<EventQueueEntry<EventType>> queue;
	private final StateMachine<EventType> machine;
	private final StateMachineWrapper<EventType> wrapper;
	
	/**
	 * The constructor accepts (possible not concurrent state safe)state machine and wraps it in  ConcurrentStateMachine
	 * Note that one must call completeInitialization so that the state machine thread start running
	 * @param machine
	 */
	public ConcurrentStateMachine(StateMachine<EventType> machine){
		this.machine = machine;
		this.queue = new LinkedBlockingQueue<EventQueueEntry<EventType>>();
		this.wrapper = new StateMachineWrapper<>(queue, machine);
	}
	private static class EventQueueEntry<EventType extends Enum<EventType>> implements Future<StampedState<EventType>>{
		private final StateMachineEvent<EventType> event;
		private final int stamp;
		private final CountDownLatch latch;
		private volatile StampedState<EventType> resultingState; 
		private volatile Exception exception;
		
		public EventQueueEntry(StateMachineEvent<EventType> event, int generation, CountDownLatch latch){
			this.event = event;
			this.stamp = generation;
			this.latch = latch;
			resultingState = null;
			exception = null;
		}
		
		StateMachineEvent<EventType> getEvent(){
			return event;
		}
		int getGeneration(){
			return stamp;
		}
		
		void releaseLatch(){
			if (latch != null)
				latch.countDown();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isDone() {
			try {
				return latch.await(0, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return false;
			}
		}

		@Override
		public StampedState<EventType> get() throws InterruptedException,
				ExecutionException {
			latch.await();
			return resultingState;
		}

		@Override
		public StampedState<EventType> get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			if (latch.await(timeout, unit))
				return resultingState;
			else
				return null;
		}
	}
	
	
	
	private static class StateMachineWrapper<EventType extends Enum<EventType>> extends Thread{
		private final BlockingQueue<EventQueueEntry<EventType>> queue;
		private volatile StampedState<EventType> curState;
		private int curGeneration;
		private final StateMachine<EventType> machine;
		
		public StateMachineWrapper(BlockingQueue<EventQueueEntry<EventType>> queue,
					StateMachine<EventType> machine){
			this.queue = queue;
			this.machine = machine;
			curState = new StampedState<>(machine.getCurrentState(), 1);
			curGeneration= 1;
		}
		@Override
		public void run(){
			while(true){
				EventQueueEntry<EventType> entry = null;
				try {
					 entry = queue.take();
				} catch (InterruptedException e) {
					return;
				}
				try {
					if (curGeneration != entry.getGeneration() && entry.getGeneration() != 0)
						entry.resultingState = null;
					else {
						machine.transit(entry.getEvent());
						curState = new StampedState<>(machine.getCurrentState(), ++curGeneration);
						entry.resultingState = curState;
						
					}
				}
				catch(Exception e){
					entry.exception = e;
					curState = new StampedState<>(machine.getCurrentState(), ++curGeneration);
				}
				finally{
					entry.releaseLatch();
				}
			}
		}
	}
	
	
	@Override
	/**
	 * send an event for synchronous processing ( that is , pause until the event is processed in the state machine thread)
	 * @param event event to process in state machine
	 */
	public void transit(StateMachineEvent<EventType> event)	throws InvalidEventType {
			CAStransit(event, 0);
	}
	/**
	 * send event to the state machine , don't wait for processing completion 
	 * @param event event to process in state machine
	 */
	public void asyncTransit(StateMachineEvent<EventType> event){
		queue.add(new EventQueueEntry<>(event, 0, null));
	}
	
	/**
	 * send event to FSM and get Future object for obtaining result when available
	 * @param event event to process
	 * @return Future returning state when event is processed
	 */
	public Future<StampedState<EventType>> asyncTransitAndGetFutureState(StateMachineEvent<EventType> event){
		EventQueueEntry<EventType> retVal=  new EventQueueEntry<>(event,0, new CountDownLatch(1));
		queue.add(retVal);
		return retVal;
	}
	/**
	 * send event to the state and pause until processing is completed. Return StampedState object representing new state of the
	 * machine
	 * @param event event to process
	 * @return pair of timestamp and state after transition triggered by the event
	 * @throws InvalidEventType thrown if there is no valid transition exists for current state and particular event type of the event 
	 */
	public StampedState<EventType> transitAndGetResultingState(StateMachineEvent<EventType> event)
			throws InvalidEventType {
		return CAStransit(event, 0);
	}
	
	/**
	 * This method executes transition if current generation of the inner FSM is equal to "generation" parameter. This is kind-of
	 * poor mens CAS operation (operation is executed completely if concurrency predicate is correct or not at all). If execution of
	 * inner machine yields exception, this method wraps it to StateMachineMultiThreadingException exception and re-throws it up
	 * @param event - event to transit on
	 * @param generation - expected generation count
	 * @return - new state if transition happen, null otherwise
	 * @throws InvalidEventType - current event is not applicable to current state
	 */
	public StampedState<EventType> CAStransit(StateMachineEvent<EventType> event, int generation) throws InvalidEventType{
		EventQueueEntry<EventType> entry = new EventQueueEntry<>(event, generation, new CountDownLatch(1));
		try {
			queue.put(entry);
			entry.latch.await();
			if (entry.exception != null) {
				if (entry.exception instanceof InvalidEventType) 
					throw (InvalidEventType)entry.exception;
				else
					throw new StateMachineMultiThreadingException(entry.exception);
			}
			return entry.resultingState;
		
		} catch (InterruptedException e) {
			throw new StateMachineMultiThreadingException(e); // actually don't think this can happen
		}
	}
	
	/**
	 * @see org.blitvin.statemachine.StateMachine#isValidState()
	 */
	@Override
	public boolean isValidState(State<EventType> state) {
		return machine.isValidState(state);
	}

	/**
	 * @see org.blitvin.statemachine.StateMachine#isInFinalState
	 */
	@Override
	public boolean isInFinalState() {
		return wrapper.curState.getState().isFinalState();
	}

	/**
	 * @see org.blitvin.statemachine.StateMachine#getCurrentState
	 */
	@Override
	public State<EventType> getCurrentState() {
		return wrapper.curState.getState();
	}

	/**
	 * 
	 * @return current state along with current generation count
	 */
	public StampedState<EventType> getCurrentStampedState(){
		return wrapper.curState;
	}
	/**
	 * @see org.blitvin.statemachine.StateMachine#getStates
	 */
	@Override
	public Collection<State<EventType>> getStates() {
		return machine.getStates();
	}

	/**
	 * @see org.blitvin.statemachine.StateMachine#getStateByName
	 */
	@Override
	public State<EventType> getStateByName(String stateName) {
		return machine.getStateByName(stateName);
	}

	/**
	 * Completes initialization of both internal FSM and concurrent machine
	 * @see org.blitvin.statemachine.StateMachine#completeInitialization
	 */
	@Override
	public void completeInitialization(
			HashMap<Object, HashMap<Object, Object>> initializer)
			throws BadStateMachineSpecification {
		if (!machine.initializationCompleted())
			machine.completeInitialization(initializer);
		if (!initialized){
			
			wrapper.setDaemon(true);
			wrapper.start();
			initialized = true;
		}
		
	}

	/**
	 * Completes initialization of both internal FSM and concurrent machine
	 * @see org.blitvin.statemachine.StateMachine#completeInitialization
	 */
	@Override
	public boolean initializationCompleted() {
		return machine.initializationCompleted() && initialized;
	}
	/**
	 * stop internal FSM and exit its dedicated thread
	 */
	public void shutDown(){
		wrapper.interrupt();
	}
	@Override
	public void generateInternalEvent(StateMachineEvent<EventType> internalEvent) {
		
		try {
			CAStransit(internalEvent, 0);
		} catch (InvalidEventType e) {
			throw new StateMachineMultiThreadingException(e);
		}
		
		
	}

}
