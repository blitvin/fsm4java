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

import static org.blitvin.statemachine.StateMachineBuilder.FSM_TYPES.BASIC;
import org.blitvin.statemachine.concurrent.TestEnum;
import org.blitvin.statemachine.domfactorytest.TestState;

@StateMachines({
    @StateMachineSpec(name = "MyStateMachine", type=BASIC,
            eventTypeClass = TestEnum.class,
            states = {
                @StateSpec(name = "state1", isFinal = false, isInitial = true,
                        implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state2")}),
                            @TransitionSpec(event = "B", params = {
                                @Param(name = "toState", value = "state3")})}
                ),
                @StateSpec(name = "state2",implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state3")}),
                            @TransitionSpec(isDefaultTransition = true, params = {
                                @Param(name = "toState", value = "state2")})
                        }
                ),
                @StateSpec(name = "state3", isFinal = true,implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state1")}),
                            @TransitionSpec(event = "B", params = {
                                @Param(name = "toState", value = "state1")}),
                            @TransitionSpec(event = "C", params = {
                                @Param(name = "toState", value = "state2")})
                        }
                )
            }
    )

}
)
public class AnnotatedStateMachinesFactoryClass extends
        AnnotatedStateMachinesFactory {

}