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

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * This class wraps regular (not thread safe) FSM. It enforces thread safety by usage of intrinsic lock on all state modifying 
 * operations. One should use this class for situation of low congestion of processed events. For more sophisticated processing
 * (e.g. including speculative execution) or congested scenarios use ConcurrentStateMachine class 
 * @author blitvin
 *
 * @param <EventType>
 */
public class SyncronizedStateMachine<EventType extends Enum<EventType>> implements StateMachine<EventType> {

	private final StateMachine<EventType> implementation;
	public SyncronizedStateMachine(StateMachine<EventType> implementation){
		this.implementation = implementation;
	}
	@Override
	public synchronized void transit(StateMachineEvent<EventType> event)
			throws InvalidEventType {
			implementation.transit(event);
	}

	@Override
	public boolean isValidState(State<EventType> state) {
		return implementation.isValidState(state);
	}

	@Override
	public synchronized boolean isInFinalState() {
		return implementation.isInFinalState();
	}

	@Override
	public synchronized State<EventType> getCurrentState() {
		return implementation.getCurrentState();
	}

	@Override
	public Collection<State<EventType>> getStates() {
		return implementation.getStates();
	}

	@Override
	public State<EventType> getStateByName(String stateName) {
		return implementation.getStateByName(stateName);
	}

	@Override
	public synchronized void completeInitialization(
			HashMap<Object, HashMap<Object, Object>> initializer)
			throws BadStateMachineSpecification {
		if (!implementation.initializationCompleted())
			implementation.completeInitialization(initializer);
		
	}

	@Override
	public synchronized boolean initializationCompleted() {
		return implementation.initializationCompleted();
	}

}
