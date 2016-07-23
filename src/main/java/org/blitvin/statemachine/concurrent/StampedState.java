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

import org.blitvin.statemachine.State;
/**
 * StampedState is immutable state of concurrent FSM including state of internal machine and current generation count
 * @author blitvin
 *
 * @param <EventType>
 */
public class StampedState<EventType extends Enum<EventType>> {
	private final State<EventType> state;
	private final int stamp;
	
	public StampedState(State<EventType> state, int stamp){
		this.stamp = stamp;
		this.state = state;
	}
	
	public State<EventType> getState(){
		return state;
	}
	
	public int getStamp(){
		return stamp;
	}
	
	/*@Override
	public String toString(){
		return "["+state.getStateName()+":"+stamp+"]";
	}*/
}