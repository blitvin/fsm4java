/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of FSM4Java library.
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
package org.blitvin.statemachine.annotated;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.concurrent.TestEnum;
import org.blitvin.statemachine.concurrent.TestEvent;
import org.junit.Test;

import static org.junit.Assert.*;

public class AnnotatedStateMachineTest {
	@Test
	public void testFieldFSM() throws BadStateMachineSpecification, InvalidEventType{
		ClassWithFSMMember cl = new ClassWithFSMMember(ClassWithFSMMember.NO_AUTODETECTION);
		
		runTestOnFSM(cl.machine);
	}
	
	@Test
	public void testAutodetectionFSM()throws BadStateMachineSpecification, InvalidEventType{
		ClassWithFSMMember cl = new ClassWithFSMMember(ClassWithFSMMember.AUTODETCTION_SUCCESSFUL);
		
		runTestOnFSM(cl.machine);
	}
	@Test
	public void testClassFSM() throws BadStateMachineSpecification, InvalidEventType{
		TestAnnotatedSubclass<TestEnum> machine = new TestAnnotatedSubclass<>(TestEnum.class);
		machine.completeInitialization(null);
		
		runTestOnFSM(machine);
	}
	
	@Test(expected=BadStateMachineSpecification.class)
	public void testAutodetectionFail() throws BadStateMachineSpecification, InvalidEventType{
		ClassWithFSMMember cl = new ClassWithFSMMember(ClassWithFSMMember.AUTODETECTION_FAIL);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAnnotatedFactory() throws BadStateMachineSpecification, InvalidEventType{
		AnnotatedStateMachinesFactoryClass factory = new AnnotatedStateMachinesFactoryClass();
		StateMachine<TestEnum> machine = (StateMachine<TestEnum>)factory.getStateMachine("MyStateMachine");
		assertNotNull(machine);
		runTestOnFSM(machine);
	}
	
	@Test
	public void testAnnotatedSubclass() throws BadStateMachineSpecification, InvalidEventType{
		TestAnnotatedSubclass<TestEnum> cl = new TestAnnotatedSubclass<>(TestEnum.class);
		runTestOnFSM(cl);
	}
	
	private void runTestOnFSM(StateMachine<TestEnum> machine) throws InvalidEventType {
		TestEvent<TestEnum> te = new TestEvent<TestEnum>(TestEnum.A);
		
		assertEquals("state1",machine.getCurrentState().getStateName());
		machine.transit(te);
		assertEquals("state2",machine.getCurrentState().getStateName());
		machine.transit(te.setEvent(TestEnum.C));
		assertEquals("state2",machine.getCurrentState().getStateName());
		machine.transit(te.setEvent(TestEnum.A));
		assertEquals("state3",machine.getCurrentState().getStateName());
		machine.transit(te.setEvent(TestEnum.B));
		try{
			machine.transit(te.setEvent(TestEnum.C));
			fail("Expected InvalidEventTypeException");
		}
		catch(InvalidEventType e){
			
		}
	}
}

