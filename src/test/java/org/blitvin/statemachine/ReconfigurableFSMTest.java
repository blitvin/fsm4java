/*
 * (C) Copyright Boris Litvin 2014 -2016
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

import java.util.Map;
import org.blitvin.statemachine.concurrent.TestEvent;
import org.blitvin.statemachine.domfactorytest.TestEnum;
import org.blitvin.statemachine.domfactorytest.TestState;
import org.blitvin.statemachine.utils.StateSkeleton;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author blitvin
 */
public class ReconfigurableFSMTest {

    public static final String FIRST = "first";
    public static final String SECOND = "second";
    public static final String THIRD = "third";

    private static class TestPropertyListener implements PropertyChangeListener{

        private ReconfigTestState state;
        
        public TestPropertyListener(ReconfigTestState state) {
            this.state = state;
        }
        @Override
        public void onPropertyChange(Object property, Object newVale, Object oldValue) {
            state.propertyListenerCalled = true;
        }
        
    }
    private static class ReconfigTestState extends StateSkeleton<TestEnum> {
        private FSMStateView<TestEnum> fsm;
        boolean propertyListenerCalled = false;
        boolean stateBecameCurrent = false;
               
        @Override
        public void onStateBecomesCurrent(StateMachineEvent<TestEnum> theEvent, State<TestEnum> prevState) {
            fsm.setProperty(THIRD, "called");
            stateBecameCurrent = true;
        }

        @Override
        public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
            fsm = containingMachine;
            fsm.registerPropertyChangeListener(new TestPropertyListener(this), FIRST);
        }
    }

    @Test
    public void testFSMTopologyChange() throws BadStateMachineSpecification, InvalidEventException {
        StateMachine<TestEnum> fsm = (new StateMachineBuilder<TestEnum>(StateMachineBuilder.FSM_TYPES.BASIC, TestEnum.class))
                .addState(FIRST, new TestState<TestEnum>()).markStateAsInitial().addDefaultTransition(SECOND)
                .addState(SECOND, new TestState<TestEnum>()).addTransition(TestEnum.enum1, THIRD)
                .addTransition(TestEnum.enum2, FIRST).addState(THIRD, new TestState<TestEnum>())
                .addDefaultTransition(FIRST).build();
        ReconfigurableStateMachine<TestEnum> reconfigurable = new ReconfigurableStateMachine<>(fsm);
        TestEvent<TestEnum> event = new TestEvent<>(TestEnum.enum1);
        // ensure original FSM works 1->2->3->1 on event enum1
        assertEquals(FIRST, reconfigurable.getNameOfCurrentState());
        reconfigurable.transit(event);
        assertEquals(SECOND, reconfigurable.getNameOfCurrentState());
        reconfigurable.transit(event);
        assertEquals(THIRD, reconfigurable.getNameOfCurrentState());
        reconfigurable.transit(event);
        reconfigurable.transit(event);
        assertEquals(SECOND, reconfigurable.getNameOfCurrentState());
        // now current state is second, following builder inverts transitions on enum1 and enum2

        StateMachineBuilder<TestEnum> replaceBuilder = new StateMachineBuilder<TestEnum>(StateMachineBuilder.FSM_TYPES.BASIC, TestEnum.class)
                .addState(FIRST, new TestState<TestEnum>()).markStateAsInitial().addDefaultTransition(SECOND)
                .addState(SECOND, new TestState<TestEnum>()).addTransition(TestEnum.enum1, FIRST)
                .addTransition(TestEnum.enum2, THIRD).addState(THIRD, new TestState<TestEnum>())
                .addDefaultTransition(FIRST);

        reconfigurable.reconfigure(replaceBuilder);
        assertEquals(SECOND, reconfigurable.getNameOfCurrentState());
        reconfigurable.transit(event);
        assertEquals(FIRST, reconfigurable.getNameOfCurrentState());
        reconfigurable.transit(event);
        event.setEvent(TestEnum.enum2);
        reconfigurable.transit(event);
        assertEquals(THIRD, reconfigurable.getNameOfCurrentState());
    }

    @Test
    public void testReconfigurableFSMPropertiesMigration() {
        //TBD
    }

    @Test
    public void testStateChange() throws BadStateMachineSpecification, InvalidEventException {
        StateMachine<TestEnum> fsm = (new StateMachineBuilder<TestEnum>(StateMachineBuilder.FSM_TYPES.BASIC, TestEnum.class))
                .addState(FIRST, new TestState<TestEnum>()).markStateAsInitial().addDefaultTransition(SECOND)
                .addState(SECOND, new TestState<TestEnum>()).addTransition(TestEnum.enum1, THIRD)
                .addTransition(TestEnum.enum2, FIRST).addState(THIRD, new TestState<TestEnum>())
                .addDefaultTransition(FIRST).build();
        ReconfigurableStateMachine<TestEnum> reconfigurable = new ReconfigurableStateMachine<>(fsm);
        TestEvent<TestEnum> event = new TestEvent<>(TestEnum.enum1);
        reconfigurable.transit(event);
        ReconfigTestState testState = new ReconfigTestState();
        reconfigurable.updateState(THIRD, testState);
        reconfigurable.transit(event);
        assertEquals(THIRD, reconfigurable.getNameOfCurrentState());
        assertTrue(testState.stateBecameCurrent);
        assertEquals("called", reconfigurable.getProperty(THIRD));
        reconfigurable.setProperty(FIRST, testState);
        assertTrue(testState.propertyListenerCalled);
    }
}