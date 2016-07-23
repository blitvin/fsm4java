/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of tests of StateMachine library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
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
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;

import java.util.HashMap;
import org.blitvin.statemachine.buildertest.BuilderTestState;


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
/*
	public static SimpleStateMachine<STM_EVENTS> createMachine() throws BadStateMachineSpecification {
            StateMachineBuilder<STM_EVENTS> builder = new StateMachineBuilder<>(StateMachineBuilder.FSM_TYPES.SIMPLE,STM_EVENTS.class);
		BuilderTestState<STM_EVENTS> state1 = new BuilderTestState<>(/*"INITIAL", false* /);
		BuilderTestState<STM_EVENTS> state2 = new BuilderTestState<>(/*"GOT_B", false* /);
		BuilderTestState<STM_EVENTS> state3 = new BuilderTestState<>(/*"GOT_A",  true* /);
                builder.addState("INITIAL", state1).addTransition(STM_EVENTS.STM_B,"GOT_A").
                        addDefaultTransition()
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
		return new SimpleStateMachine<>(states,state1);
	}
	public static void main(String[] args){
		SimpleStateMachine<STM_EVENTS> mach;
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
			System.out.println("After A :" +mach.isInFinalState() + "("+mach.getNameOfCurrentState()+")");
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_A);
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_C);
			mach.transit(ev);
			System.out.println("After C :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_B);
			mach.transit(ev);
			System.out.println("After B :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
			ev.setEventType(STM_EVENTS.STM_A);
			mach.transit(ev);
			System.out.println("After A :" +mach.isInFinalState()+ "("+mach.getNameOfCurrentState()+")");
		} catch (InvalidEventType e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}*/
}