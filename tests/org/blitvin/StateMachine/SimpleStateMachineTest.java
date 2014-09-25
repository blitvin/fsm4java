/*
 * (C) Copyright Boris Litvin 2014
 * This file is part of tests of StateMachine library.
 *
 *  StateMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   NioServer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with StateMachine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;

import java.util.HashMap;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.SimpleTransition;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.Transition;

enum STM_EVENTS {
	STM_A, STM_B, STM_C
}

class SimpleEvent implements StateMachineEvent<STM_EVENTS>{

	STM_EVENTS event;
	
	public SimpleEvent(STM_EVENTS event) {
		this.event = event;
	}
	public void setEventType(STM_EVENTS event){
		this.event = event;
	}
	
	@Override
	public STM_EVENTS getEventType() {
		return event;
	}
	
}
public class SimpleStateMachineTest {

	public static StateMachine<STM_EVENTS> createMachine() throws BadStateMachineSpecification {
		HashMap<String, State<STM_EVENTS>> states = new HashMap<>();
		State<STM_EVENTS> state1 = new State<>("INITIAL", false);
		State<STM_EVENTS> state2 = new State<>("GOT_B", false);
		State<STM_EVENTS> state3 = new State<>("GOT_A",  true);
		HashMap<STM_EVENTS,Transition<STM_EVENTS>> transitions = new HashMap<STM_EVENTS, Transition<STM_EVENTS>>();
		transitions.put(STM_EVENTS.STM_B, new SimpleTransition<STM_EVENTS>(state2,null));
		transitions.put(null,new SimpleTransition<STM_EVENTS>(state1,null));
		state1.setTransitions( transitions);
		transitions = new HashMap<STM_EVENTS, Transition<STM_EVENTS>>();
		transitions.put(STM_EVENTS.STM_A, new SimpleTransition<STM_EVENTS>(state3,null));
		transitions.put(STM_EVENTS.STM_B, new SimpleTransition<STM_EVENTS>(state2,null));
		transitions.put(null, new SimpleTransition<STM_EVENTS>(state1,null));
		state2.setTransitions(transitions);
		transitions = new HashMap<STM_EVENTS, Transition<STM_EVENTS>>();
		transitions.put(STM_EVENTS.STM_B, new SimpleTransition<>(state2,null));
		transitions.put(null, new SimpleTransition<>(state1,null));
		state3.setTransitions(transitions);
		states.put(state1.getStateName(), state1);
		states.put(state2.getStateName(), state2);
		states.put(state3.getStateName(), state3);
		return new StateMachine<>(states,state1);
	}
	public static void main(String[] args){
		StateMachine<STM_EVENTS> mach;
		try {
			mach = createMachine();
		} catch (BadStateMachineSpecification e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		SimpleEvent ev = new SimpleEvent(STM_EVENTS.STM_A);
		System.out.println("At the beginning :" +mach.isInFinalState());
		try {
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState() + "("+mach.getCurrentState().getStateName()+")");
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_A);
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_C);
			mach.transit(ev);
			System.out.println("After C :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
			ev.setEventType(STM_EVENTS.STM_A);
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getCurrentState().getStateName()+")");
		} catch (InvalidEventType e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
