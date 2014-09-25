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

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.blitvin.StateMachine.domfactorytest.*;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.DOMStateMachineFactory;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.StateMachine;

public class DomFactoryTest {

	public static final String XML_FILE = "DomFactoryTest.xml";
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DOMStateMachineFactory defaultFactory = new DOMStateMachineFactory(XML_FILE);
		DOMStateMachineFactory.setDefaultFactory(defaultFactory);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testCorrect() throws BadStateMachineSpecification, InvalidEventType {
		StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("correctMachine");
		machine.transit(new TestMachineEvent<TestEnum>(TestEnum.enum1));
		assertEquals("state2",machine.getCurrentState().getStateName());
		assertTrue(machine.getCurrentState().getTransitionByEvent(TestEnum.enum1) instanceof TestTransition);
		assertFalse(machine.isInFinalState());
		machine.transit(new TestMachineEvent<TestEnum>(TestEnum.enum2));
		assertEquals("state3",machine.getCurrentState().getStateName());
		assertFalse(machine.getCurrentState().getTransitionByEvent(TestEnum.enum1) instanceof TestTransition);
		assertTrue(machine.isInFinalState());
	}
	
	@Test
	public void testIncorrectEnum() {
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("incorrectEnumMachine");
			fail("machine cration should fail because of bad enum");
		}
		catch(BadStateMachineSpecification e){
			assertTrue(e.getCause() != null && e.getCause() instanceof InvocationTargetException && 
					((InvocationTargetException)e.getCause()).getTargetException() instanceof IllegalArgumentException);
		}
	}
	
	@Test
	public void testNonExistingState(){
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("nonExistingStateMachine");
			fail("machine cration should fail because of bad state");
		}
		catch(BadStateMachineSpecification e){
			assertEquals("SimpleTransaction : can't find state with name state5", e.getMessage());
		}
	}
	@Test
	public void testBadClass(){
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("badClassMachine");
			fail("machine cration should fail because of bad state class");
		}
		catch(BadStateMachineSpecification e){
			assertTrue(e.getCause() != null && e.getCause() instanceof ClassNotFoundException);
			
		}
	}
	
	@Test
	public void testClassNotcorrectInheriror(){
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("classNotcorrectInherirorMachine");
			fail("machine cration should fail because of class specified in as transition is not inherited from Transition");
		}
		catch(BadStateMachineSpecification e){
			assertEquals("org.blitvin.StateMachine.domfactorytest.TestEnum is not inherited from org.blitvin.statemachine.Transition",e.getMessage());
		}
	}
	
	@Test
	public void testEventTypeIsNotEnum(){
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("eventTypeIsNotEnumMachine");
			fail("machine cration should fail because of bad enum class");
		}
		catch(BadStateMachineSpecification e){
			assertEquals("Expecting enum class name in attribute eventTypeClass", e.getMessage());
		}
	}
	
	@Test
	public void testUnknownMachineName(){
		try {
			StateMachine<TestEnum> machine =DOMStateMachineFactory.getDefaultFactory().getStateMachine("nonExistingMachine");
			fail("machine cration should fail because machine with the name doesn't exist");
		}
		catch(BadStateMachineSpecification e){
			assertEquals("Unknown state machine name:nonExistingMachine", e.getMessage());
		}
	}
	
}
