/*
 * (C) Copyright Boris Litvin 2014 - 2016
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FSM4Java is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;

import java.util.HashMap;
import java.util.Map;

/**
 * ReconfigurableStateMachine allows change of FSM on the fly. The reason for it
 * to exist is scenario when you need to modify behavior of FSM during runtime.
 * One certainly can create new FSM with required transitions and state business
 * objects, but other parts of program can retain reference to outdated version
 * of FSM. If you have reference to ReconfigurableStateMachine, it manages all
 * the changes. ReconfigurableStateMachine allows reconfiguration of the FSM in
 * two ways : updating one or several State objects (e.g. business logic
 * objects) and modification of "graph" of transitions for the FSM. In both
 * cases ReconfigurableStateMachine takes care of proper (re)initialization of
 * newly added (or updated) objects, correctly works in case of
 * ReconfigurableStateMachine is part of wrapper chain etc.
 *
 * @author blitvin
 * @param <EventType>
 */
public class ReconfigurableStateMachine<EventType extends Enum<EventType>>
        extends FSMWrapper<EventType> {

    private static class ReconfigurationTransport<EventType extends Enum<EventType>>
            implements FSMWrapperTransport<EventType> {

        private final StateMachineBuilder<EventType> builder;
        private BadStateMachineSpecification ex = null;
        private final Map<String, State<EventType>> newStates;

        public BadStateMachineSpecification getReconfigurationException() {
            return ex;
        }

        public ReconfigurationTransport(StateMachineBuilder<EventType> builder,
                Map<String, State<EventType>> newStates
        ) {
            this.builder = builder;
            this.newStates = newStates;
        }

        public static class ProvidedOrCopyFromFSMFactory<EventType extends Enum<EventType>>
                implements FSMStateFactory<EventType> {

            //private final HashMap<String, State<String>> suppliedStates;
            private final StateMachine<EventType> fsm;

            private Map<String, State<EventType>> suppliedStates;

            public ProvidedOrCopyFromFSMFactory(Map<String, State<EventType>> suppliedStates,
                    StateMachine<EventType> fsm) {
                this.suppliedStates = suppliedStates;
                assert (fsm != null);
                this.fsm = fsm;
            }

            @Override
            public State<EventType> get(String state, HashMap<Object, Object> initializers) {

                State<EventType> retVal;
                if (suppliedStates != null) {
                    retVal = suppliedStates.get(state);
                    if (retVal != null) {
                        return retVal;
                    }
                }

                return fsm.getStateByName(state);

            }

        }

        @Override
        public void apply(StateMachineWrapperAcceptor<EventType> machine, StateMachineWrapperAcceptor<EventType> wrapped)
                throws FSMWrapperException {
            if (wrapped instanceof FSMWrapper) {
                wrapped.acceptWrapperTransport(this);
            } else {
                if (builder != null) {

                    try {
                        StateMachineDriver<EventType> wrappedFSM = (StateMachineDriver<EventType>) wrapped;
                        HashMap<Object,Object> savedFSMProperties = builder.getFSMProperties();
                        StateMachineDriver<EventType> newFSM = (StateMachineDriver<EventType>) builder
                                .addFSMProperties(wrappedFSM.getFSMProperties()).build(
                                new ProvidedOrCopyFromFSMFactory(newStates, wrappedFSM), true);
                        builder.setFSMProperties(savedFSMProperties); //restore original properties
                        newFSM.setCurrentNode(wrappedFSM.getNameOfCurrentState());
                        ((StateMachineWrapper) machine).replaceWrappedWith(newFSM);
                    } catch (BadStateMachineSpecification ex) {
                        this.ex = ex;
                    } /*finally {
                     latch.countDown();
                     }*/

                } else {
                    if (newStates != null) {
                        for (HashMap.Entry<String, State<EventType>> pair : newStates.entrySet()) {
                            FSMNode<EventType> node = ((StateMachineDriver) wrapped).getNodeByName(pair.getKey());
                            if (node == null) {
                                ex = new BadStateMachineSpecification(pair.getKey() + " is not known state");
                                return;
                            }
                            node.setState(pair.getValue());
                            try {
                                pair.getValue().onStateAttachedToFSM(null, (FSMStateView) wrapped);
                            } catch (BadStateMachineSpecification ex) {
                                this.ex = ex;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private volatile ReconfigurationTransport transport = null;

    public ReconfigurableStateMachine(StateMachine<EventType> wrapped) {
        super(wrapped);
    }

    /**
     * sets new business logic object for given state
     *
     * @param name name of the state for new business logic object
     * @param state business logic objects
     * @throws BadStateMachineSpecification thrown if for some reason update
     * failed e.g if one state's transitions is CustomizedLogicTransition and
     * new state object doesn't implement CustomizedTranistionsLogicState
     */
    public void updateState(String name, State<EventType> state) throws BadStateMachineSpecification {
        HashMap<String, State<EventType>> map = new HashMap<>(1);
        map.put(name, state);
        updateStates(map);
    }

    /**
     * like updateState, but for multiple state objects to be updated in single
     * call ( it is important in certain situations e.g. for multi-threading
     * scenarios)
     *
     * @param states
     * @throws BadStateMachineSpecification
     */
    public void updateStates(Map<String, State<EventType>> states) throws BadStateMachineSpecification {
        reconfigure(null, states);
    }

    /**
     * update "topology" of FSM that is nodes,transactions etc. according to the
     * builder. For states with name the same as original FSM configuration,
     * respective business logic object is preserved. After topology is updated
     * life cycle callback of business logic object is invoked to adjust
     * internal state with new topology
     *
     * @param builder - builder contains information on new topology
     * @throws BadStateMachineSpecification
     */
    public void reconfigure(StateMachineBuilder<EventType> builder) throws BadStateMachineSpecification {
        reconfigure(builder, null);
    }

    /**
     * update "topology" of FSM that is nodes,transactions etc. according to the
     * builder. For states with name the same as original FSM configuration,
     * respective business logic object is preserved. After topology is updated
     * life cycle callback of business logic object is invoked to adjust
     * internal state with new topology. Second argument is map new business
     * object objects replacing "old" ones
     *
     * @param builder
     * @param states
     * @throws BadStateMachineSpecification
     */
    public void reconfigure(StateMachineBuilder<EventType> builder,
            Map<String, State<EventType>> states) throws BadStateMachineSpecification {

        transport = new ReconfigurationTransport(builder, states);
        try {
            if (wrapped instanceof FSMWrapper) {
                wrapped.acceptWrapperTransport(transport);
            } else {
                transport.apply(this, wrapped);
            }
        } catch (FSMWrapperException ex) {
            throw new BadStateMachineSpecification("reconfiguration propagation failed", ex);
        }
    }
}
