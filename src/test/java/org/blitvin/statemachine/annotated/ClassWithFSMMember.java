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
import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.BASIC;
import org.blitvin.statemachine.concurrent.TestEnum;
import org.blitvin.statemachine.domfactorytest.TestState;

public class ClassWithFSMMember {
    /*@StateMachineSpec(eventTypeClass = TestEnum.class type=StateMachineBuilder.FSM_TYPES.BASIC name="" states=(
     @States(
     value={@StateSpec(name="state1" isFinal=falsem isInitial=true)}
     )  
     )
     @States(
     value={@StateSpec(name="state1", isFinal=false, isInitial=true, 
     transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state2")}),
     @TransitionSpec(event="B",params={@Param(name="toState",value="state3")})}			
     ),
     @StateSpec(name="state2",
     transitions={@TransitionSpec(event="A", params={@Param(name="toState",value="state3")}),
     @TransitionSpec(isDefaultTransition=true, params={@Param(name="toState", value="state2")})
     }
     ),
     @StateSpec(name="state3", isFinal=true,
     transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state1")}),
     @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
     @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})})
     }
     ))*/

    @StateMachineSpec(eventTypeClass = TestEnum.class, name = "myStateMachine", type = BASIC,
            states = {
                @StateSpec(name = "state1", isFinal = false, isInitial = true, implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {@Param(name = "toState", value = "state2")}),
                            @TransitionSpec(event = "B", params = {@Param(name="toState", value="state3")})
                        }),
                @StateSpec(name="state2", implClass = TestState.class, transitions = {
                    @TransitionSpec(event= "A", params={@Param(name="toState", value="state3")}),
                    @TransitionSpec(isDefaultTransition = true, params={@Param(name = "toState", value="state2")})
                    }),
                @StateSpec(name="state3",isFinal=true, implClass = TestState.class, transitions= {
                    @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
                    @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})
                })
            }
    )
    public AnnotatedStateMachine<TestEnum> machine;

    public static final int NO_AUTODETECTION = 0;
    public static final int AUTODETCTION_SUCCESSFUL = 1;
    public static final int AUTODETECTION_FAIL = 2;

    public ClassWithFSMMember(int autodetectVal) throws BadStateMachineSpecification {
        switch (autodetectVal) {
            case AUTODETCTION_SUCCESSFUL:
                machine = new AnnotatedStateMachine<>("myStateMachine");
                break;
            case NO_AUTODETECTION:
                machine = new AnnotatedStateMachine<>(ClassWithFSMMember.class, "machine");
                break;
            case AUTODETECTION_FAIL:
                machine = new AnnotatedStateMachine<>("noSuchMachine");

                break;
        }
    }
}