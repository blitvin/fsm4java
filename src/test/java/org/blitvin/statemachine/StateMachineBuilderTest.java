package org.blitvin.statemachine;

import java.util.HashMap;
import static org.junit.Assert.*;

import org.blitvin.statemachine.buildertest.BuilderTestState;
import org.blitvin.statemachine.buildertest.BuilderTestStateFactory;
import org.junit.Test;




public class StateMachineBuilderTest {
	enum STM_EVENTS {
		STM_A, STM_B, STM_C
	}
	
	private static class TestEvent implements StateMachineEvent<STM_EVENTS>{

		private STM_EVENTS event;
		public TestEvent setEvent(STM_EVENTS event){
			this.event = event;
			return this;
		}
		
		
		@Override
		public STM_EVENTS getEventType() {
			return event;
		}
		
	}
	
	@Test
	public void testBuilder() throws BadStateMachineSpecification,InvalidEventException{
		StateMachineBuilder<STM_EVENTS> builder = new StateMachineBuilder<>(StateMachineBuilder.FSM_TYPES.SIMPLE,STM_EVENTS.class);
		
		builder.addState("state1").markStateAsInitial().
                        addProperty("class", org.blitvin.statemachine.buildertest.BuilderTestState.class).
			addTransition(STM_EVENTS.STM_A).addProperty("toState", "state2");
		builder.addTransition(STM_EVENTS.STM_B).addProperty("toState", "state3");
		
		builder.addState("state2")
			.addTransition(STM_EVENTS.STM_A).addProperty("toState", "state3")
			.addDefaultTransition().addProperty("toState", "state2");
		
		BuilderTestState<STM_EVENTS> state3 = new BuilderTestState<>();
		
		builder.addState("state3",state3).addProperty("myAttribute", "isSet")
			.addTransition(STM_EVENTS.STM_A).addProperty("toState", "state1")
			.addTransition(STM_EVENTS.STM_B,StateMachineBuilder.TRANSITION_TYPE.NULL)
			.addTransition(STM_EVENTS.STM_C).addProperty("toState", "state2").markStateAsFinal();
		
                HashMap<Object,Object> fsmInit = new HashMap<>();
                fsmInit.put(StateMachineBuilder.STATE_FACTORY_IN_GLOBAL_PROPERTIES, new BuilderTestStateFactory<>());
		StateMachine<STM_EVENTS> machine = builder.addFSMProperties(fsmInit).build();
		
		assertEquals(machine.getNameOfCurrentState(),"state1");
		assertFalse(machine.isInFinalState());
		TestEvent event = new TestEvent();
		machine.transit(event.setEvent(STM_EVENTS.STM_A));
		assertEquals(machine.getNameOfCurrentState(),"state2");
		assertFalse(machine.isInFinalState());
		machine.transit(event);
		assertEquals(machine.getNameOfCurrentState(),"state3");
		assertEquals(((BuilderTestState<STM_EVENTS>)machine.getCurrentState()).getMyAttribute(),"isSet");
		assertTrue(machine.isInFinalState());
                machine.transit(event.setEvent(STM_EVENTS.STM_B));
                assertEquals(machine.getNameOfCurrentState(),"state3");
		machine.transit(event.setEvent(STM_EVENTS.STM_A));
		assertEquals(machine.getNameOfCurrentState(),"state1");
		try {
			machine.transit(event.setEvent(STM_EVENTS.STM_C));
			fail("Should throw InvalidEventType as STM_C transition is not defined");
		}
		catch(InvalidEventException e){
			// no transition defined on STM_C event
		}
		machine.transit(event.setEvent(STM_EVENTS.STM_B));
		assertEquals(machine.getNameOfCurrentState(),"state3");
		machine.transit(event.setEvent(STM_EVENTS.STM_C));
		assertEquals(machine.getNameOfCurrentState(),"state2");
		// test default transition
		machine.transit(event);
		assertEquals(machine.getNameOfCurrentState(),"state2");
		machine.transit(event.setEvent(STM_EVENTS.STM_B));
		assertEquals(machine.getNameOfCurrentState(),"state2");
		machine.transit(event.setEvent(STM_EVENTS.STM_A));
		assertEquals(machine.getNameOfCurrentState(),"state3");
		assertTrue(machine.isInFinalState());
		
		
		
	}
}