package org.blitvin.statemachine;

import static org.junit.Assert.*;

import org.blitvin.statemachine.buildertest.BuilderTestState;
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
	public void testBuilder() throws BadStateMachineSpecification,InvalidEventType{
		StateMachineBuilder<STM_EVENTS> builder = new StateMachineBuilder<>("myMachine");
		
		builder.addState(new State<STM_EVENTS>("state1", false)).markStateAsInitial().
			addTransition(STM_EVENTS.STM_A, new SimpleTransition<STM_EVENTS>()).addAttribute("toState", "state2");
		builder.addTransition(STM_EVENTS.STM_B, new SimpleTransition<STM_EVENTS>()).addAttribute("toState", "state3");
		
		builder.addState(new State<STM_EVENTS>("state2",false))
			.addTransition(STM_EVENTS.STM_A,new SimpleTransition<STM_EVENTS>()).addAttribute("toState", "state3")
			.addDefaultTransition(new SimpleTransition<STM_EVENTS>()).addAttribute("toState", "state2");
		
		BuilderTestState<STM_EVENTS> state3 = new BuilderTestState<>("state3", true);
		SimpleTransition<STM_EVENTS> transition = new SimpleTransition<>();
		
		builder.addState(state3).addAttribute("myAttribute", "isSet")
			.addTransition(STM_EVENTS.STM_A,transition).addAttribute("toState", "state1")
			.addTransition(STM_EVENTS.STM_B, transition)
			.addTransition(STM_EVENTS.STM_C, new SimpleTransition<STM_EVENTS>()).addAttribute("toState", "state2");
		
		StateMachine<STM_EVENTS> machine = builder.build();
		
		assertEquals(machine.getCurrentState().getStateName(),"state1");
		assertFalse(machine.getCurrentState().isFinalState());
		TestEvent event = new TestEvent();
		machine.transit(event.setEvent(STM_EVENTS.STM_A));
		assertEquals(machine.getCurrentState().getStateName(),"state2");
		assertFalse(machine.getCurrentState().isFinalState());
		machine.transit(event);
		assertEquals(machine.getCurrentState().getStateName(),"state3");
		assertEquals(((BuilderTestState<STM_EVENTS>)machine.getCurrentState()).getMyAttribute(),"isSet");
		assertTrue(machine.getCurrentState().isFinalState());
		machine.transit(event);
		assertEquals(machine.getCurrentState().getStateName(),"state1");
		try {
			machine.transit(event.setEvent(STM_EVENTS.STM_C));
			fail("Should throw InvalidEventType as STM_C transition is not defined");
		}
		catch(InvalidEventType e){
			// no transition defined on STM_C event
		}
		machine.transit(event.setEvent(STM_EVENTS.STM_B));
		assertEquals(machine.getCurrentState().getStateName(),"state3");
		machine.transit(event.setEvent(STM_EVENTS.STM_C));
		assertEquals(machine.getCurrentState().getStateName(),"state2");
		// test default transition
		machine.transit(event);
		assertEquals(machine.getCurrentState().getStateName(),"state2");
		machine.transit(event.setEvent(STM_EVENTS.STM_B));
		assertEquals(machine.getCurrentState().getStateName(),"state2");
		machine.transit(event.setEvent(STM_EVENTS.STM_A));
		assertEquals(machine.getCurrentState().getStateName(),"state3");
		assertTrue(machine.getCurrentState().isFinalState());
		
		
		
	}
}