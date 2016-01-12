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
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.SimpleTransition;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.concurrent.StateMachineMultiThreadingException;
import org.blitvin.statemachine.concurrent.ConcurrentStateMachine;
import org.junit.Test;

import static org.junit.Assert.*;
public class ThreadSafeMachineTest {

	private ConcurrentStateMachine<TestEnum> buildMachine() throws BadStateMachineSpecification{
		
		StateMachineBuilder<TestEnum> b =new StateMachineBuilder<>("internalMachine");
		b.addState(new State<TestEnum>("first",false)).markStateAsInitial()
			.addTransition(TestEnum.A,new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "second")
			.addDefaultTransition(new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "third")
			.addState(new State<TestEnum>("second",false))
			.addTransition(TestEnum.A, new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "third")
			.addTransition(TestEnum.B, new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "first")
			.addState(new State<TestEnum>("third",true))
			.addTransition(TestEnum.A, new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "first")
			.addTransition(TestEnum.B, new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "second")
			.addTransition(TestEnum.C, new SimpleTransition<TestEnum>()).addAttribute(SimpleTransition.TARGET_STATE, "third");
		ConcurrentStateMachine<TestEnum> retVal = new ConcurrentStateMachine<>(b.build());
		retVal.completeInitialization(null);
		return retVal;
		
	}
	@Test
	public void testRegularFuncitonality() throws BadStateMachineSpecification, InvalidEventType{
		ConcurrentStateMachine<TestEnum> machine = buildMachine();
		assertEquals("first", machine.getCurrentState().getStateName());
		TestEvent<TestEnum> event = new TestEvent<>(TestEnum.A);
		machine.transit(event);
		assertEquals("second",machine.getCurrentState().getStateName());
		assertFalse(machine.getCurrentState().isFinalState());
		try {
			machine.transit(event.setEvent(TestEnum.C));
			fail("Expecting to get InvalidEventType exception");
		}
		catch(InvalidEventType e){
		}
		machine.transit(event.setEvent(TestEnum.B));
		assertEquals("first",machine.getCurrentState().getStateName());
		assertFalse(machine.getCurrentState().isFinalState());
		
		machine.transit(event.setEvent(TestEnum.C));
		assertEquals("third",machine.getCurrentState().getStateName());
		assertTrue(machine.getCurrentState().isFinalState());
		machine.transit(event);
		assertEquals("third",machine.getCurrentState().getStateName());
		
	}
	
	private static abstract class CASTester extends Thread {
		protected final CyclicBarrier barrier;
		protected final ConcurrentStateMachine<TestEnum> machine;
		protected volatile String failureReason;
		protected boolean shouldStop;
		protected Thread counterpart;
		
		private void notifyCounterpart(){
			if (counterpart != null)
				counterpart.interrupt();
		}
		public CASTester(ConcurrentStateMachine<TestEnum> machine,CyclicBarrier barrier,String name){
			super(name);
			this.barrier = barrier;
			this.machine = machine;
			failureReason = null;
		}
		public void fail(String reason){
			failureReason = reason;
			if (!Thread.currentThread().isInterrupted())
				notifyCounterpart();
		}
		public boolean assertNull(Object o){
			if (o != null) {
				failureReason="expected null, but found "+o.toString();
				notifyCounterpart();
				return true;
			}
			return false;
			
		}
		
		public boolean assertNotNull(Object o){
			if (o == null) {
				failureReason="expected object not to be null";
				notifyCounterpart();
				return true;
			}
			return false;
			
		}
		
		public boolean assertEquals(Object o1, Object o2){
			if (!o1.equals(o2)){
					failureReason=o1.toString()+"!="+o2.toString();
					notifyCounterpart();
					return true;
			}
			return false;
		}
		
		public boolean assertEquals(int expected, int actual, String message ){
			if (expected!= actual){
				failureReason = message+": expected"+expected+" and found "+actual;
				notifyCounterpart();
				return true;
			}
			return false;
		}
		
		public void setCounterpart(Thread counterpart){
			this.counterpart = counterpart;
		}
	}
	@Test
	public void testCASBackoff() throws BadStateMachineSpecification, InvalidEventType{
		ConcurrentStateMachine<TestEnum> machine = buildMachine();
		CyclicBarrier barrier = new CyclicBarrier(2);
		
		CASTester thread1 = new CASTester(machine,barrier,"testing thread 1"){
			@Override public void run(){
				StampedState<TestEnum> stamped =machine.getCurrentStampedState();
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				
				try {
					if (assertNull(machine.CAStransit(
							new TestEvent<TestEnum>(TestEnum.A), stamped.getStamp())))
						return;
				} catch (StateMachineMultiThreadingException|InvalidEventType e) {
					fail("Unexpected Exception:"+e.toString());
				}
			}
		};
		
		CASTester thread2 = new CASTester(machine,barrier,"testing thread2"){
			@Override public void run(){
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				try {
					machine.transit(new TestEvent<TestEnum>(TestEnum.A));
				} catch (InvalidEventType e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
			}
		};
		thread1.setCounterpart(thread2);
		thread2.setCounterpart(thread1);
		thread1.start();
		thread2.start();
		try {
			thread1.join();
			thread2.join();
		} catch (InterruptedException e) {
		}
		assertNull(thread1.failureReason);
		assertNull(thread2.failureReason);
	}
	
	@Test
	public void testCASsuccessful() throws BadStateMachineSpecification, InvalidEventType{
		ConcurrentStateMachine<TestEnum> machine = buildMachine();
		CyclicBarrier barrier = new CyclicBarrier(2);
		
		CASTester thread1 = new CASTester(machine,barrier,"testing thread 1"){
			@Override public void run(){
				StampedState<TestEnum> stamped =machine.getCurrentStampedState();
				if (assertEquals(stamped.getState().getStateName(),"first") ||
					assertEquals(1, stamped.getStamp(), "Initial stamped state"))
					return;
				
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				 stamped =machine.getCurrentStampedState();
				if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState().getStateName(),"second") ||
					assertEquals(2, stamped.getStamp(), "Stamped state after first transit"))
					return;
				
				try {
					stamped = machine.CAStransit(
							new TestEvent<TestEnum>(TestEnum.A), stamped.getStamp());
					if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState().getStateName(),"third") ||
							assertEquals(3, stamped.getStamp(), "Stamped state after first transit"))
							return;
				} catch (StateMachineMultiThreadingException|InvalidEventType e) {
					fail("Unexpected Exception:"+e.toString());
					return;
				}
				
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException:"+e.toString());
					return;
				} catch (BrokenBarrierException e) {
					fail("Unexpected BrokenBarrierException:"+e.toString());
					return;
				}
				stamped =machine.getCurrentStampedState();
				if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState().getStateName(),"third") ||
						assertEquals(3, stamped.getStamp(), "Stamped state after first transit"))
						return;
			}
		};
		
		CASTester thread2 = new CASTester(machine,barrier, "testing thread 2"){
			@Override public void run(){
				
				StampedState<TestEnum> stamped =machine.getCurrentStampedState();
				if (assertEquals(stamped.getState().getStateName(),"first") ||
					assertEquals(1, stamped.getStamp(), "Initial stamped state"))
					return;
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				
				try {
					stamped = machine.transitAndGetResultingState(new TestEvent<TestEnum>(TestEnum.A));
					if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState().getStateName(),"second") ||
							assertEquals(2, stamped.getStamp(), "Stamped state after first transit"))
							return;
				} catch (InvalidEventType e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
				stamped =machine.getCurrentStampedState();
				if (assertNotNull(stamped.getState()) || assertEquals(stamped.getState().getStateName(),"third") ||
						assertEquals(3, stamped.getStamp(), "Stamped state after first transit"))
						return;
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					fail("unexpected interrupt "+e.toString());
					return;
				}
			}
		};
		thread1.setCounterpart(thread2);
		thread2.setCounterpart(thread1);
		thread1.start();
		thread2.start();
		try {
			thread1.join();
			thread2.join();
		} catch (InterruptedException e) {
		}
		assertNull(thread1.failureReason);
		assertNull(thread2.failureReason);
	}
}
