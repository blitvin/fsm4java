/*
 * (C) Copyright Boris Litvin 2014- 2016
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

import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.ASPECT;
import static org.blitvin.statemachine.StateMachineBuilder.TARGET_STATE;
import org.blitvin.statemachine.buildertest.BuilderTestState;
import org.blitvin.statemachine.concurrent.TestEnum;
import org.blitvin.statemachine.concurrent.TestEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Boris
 */
public class AspectStateMachineTest {

    private static class CountAspect implements StateMachineAspects {

        int enterstates = 0;
        int nulltransitions = 0;

        @Override
        public boolean onTransitionStart(StateMachineEvent event) {
            return true;
        }

        @Override
        public void onNullTransition(StateMachineEvent event) {
            ++nulltransitions;
        }

        @Override
        public boolean onControlLeavesState(StateMachineEvent event, State currentState, State newState) {
            return true;
        }

        @Override
        public boolean onControlEntersState(StateMachineEvent event, State currentState, State prevState) {
            ++enterstates;
            return true;
        }

        public int getEntriesCount() {
            return enterstates;
        }

        public int getNullCount() {
            return nulltransitions;
        }

        @Override
        public void onTransitionFinish(StateMachineEvent event, State currentState, State prevState) {

        }

        @Override
        public void setContainingMachine(StateMachine machine) {

        }

    }

    @Test
    public void testAspectMachine() throws BadStateMachineSpecification, InvalidEventException {
        CountAspect aspect = new CountAspect();
        StateMachineBuilder<TestEnum> builder = new StateMachineBuilder(ASPECT, TestEnum.class);
        AspectEnabledStateMachine<TestEnum> aspectFSM = (AspectEnabledStateMachine<TestEnum>) builder
                .addState("first", new BuilderTestState()).markStateAsInitial()
                .addDefaultTransition().addProperty(TARGET_STATE, "second")
                .addState("second", new BuilderTestState())
                .addTransition(TestEnum.A).addProperty(TARGET_STATE, "third")
                .addTransition(TestEnum.B).addProperty(TARGET_STATE, "forth")
                .addTransition(TestEnum.C, StateMachineBuilder.TRANSITION_TYPE.NULL)
                .addState("third", new BuilderTestState())
                .addDefaultTransition().addProperty(TARGET_STATE, "third")
                .addState("forth", new BuilderTestState())
                .addDefaultTransition().addProperty(TARGET_STATE, "forth")
                .addFSMProperty(StateMachineBuilder.ASPECTS_PROPERTY, aspect)
                .build();

        aspectFSM.transit(new TestEvent<>(TestEnum.A));
        aspectFSM.transit(new TestEvent<>(TestEnum.C));
        aspectFSM.transit(new TestEvent<>(TestEnum.A));

        aspectFSM.transit(new TestEvent<>(TestEnum.B));
        assertEquals("third", aspectFSM.getNameOfCurrentState());
        assertEquals(aspectFSM.getCurrentState(), aspectFSM.getStateByName("third"));
        assertEquals(3, aspect.getEntriesCount());
        assertEquals(1, aspect.getNullCount());
    }

    @Test
    public void testNoAspectSpecified() throws BadStateMachineSpecification {
        StateMachineBuilder<TestEnum> builder = new StateMachineBuilder(ASPECT, TestEnum.class);
        builder
                .addState("first", new BuilderTestState()).markStateAsInitial()
                .addDefaultTransition().addProperty(TARGET_STATE, "second")
                .addState("second", new BuilderTestState())
                .addTransition(TestEnum.A).addProperty(TARGET_STATE, "third")
                .addTransition(TestEnum.B).addProperty(TARGET_STATE, "forth")
                .addTransition(TestEnum.C, StateMachineBuilder.TRANSITION_TYPE.NULL)
                .addState("third", new BuilderTestState())
                .addDefaultTransition().addProperty(TARGET_STATE, "third")
                .addState("forth", new BuilderTestState())
                .addDefaultTransition().addProperty(TARGET_STATE, "forth");
        try {
            AspectEnabledStateMachine<TestEnum> aspectFSM = (AspectEnabledStateMachine<TestEnum>) builder.build();
            fail("expecting failure to create Aspect FSM without specifying aspect");
        } catch (BadStateMachineSpecification e) {
            assertEquals("Aspects object is not defined or not instance of StateMachineAspects", e.getMessage());
        }
    }
}