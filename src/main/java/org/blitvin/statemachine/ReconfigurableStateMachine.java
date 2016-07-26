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
import java.util.concurrent.CountDownLatch;

/**
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
        private final CountDownLatch latch;

        public BadStateMachineSpecification getReconfigurationException() {
            return ex;
        }

        public ReconfigurationTransport(StateMachineBuilder<EventType> builder,
                Map<String, State<EventType>> newStates
        ) {
            this.builder = builder;
            this.newStates = newStates;
            latch = new CountDownLatch(1);
        }

        public void blockUntilCompletion() throws BadStateMachineSpecification {
            try {
                latch.await();
            } catch (InterruptedException ex) {
                throw new BadStateMachineSpecification("interrupted during waiting for reconfiguration completion", ex);
            }
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
        throws FSMWrapperException{
            if (wrapped instanceof FSMWrapper) {
                wrapped.acceptWrapperTransport(this);
            } else {
                if (builder != null) {

                    try {
                        StateMachineDriver<EventType> wrappedFSM = (StateMachineDriver<EventType>)wrapped;
                        StateMachineDriver<EventType> newFSM = (StateMachineDriver<EventType>) builder.build(
                                new ProvidedOrCopyFromFSMFactory(newStates, wrappedFSM), true);
                        newFSM.setCurrentNode(wrappedFSM.getNameOfCurrentState());
                        for(Object name:wrappedFSM.getFSMProperties().keySet()){
                            newFSM.setProperty(name, wrappedFSM.getProperty(name));
                        }
                        ((FSMWrapper) machine).replaceWrappedWith(newFSM);
                    } catch (BadStateMachineSpecification ex) {
                        this.ex = ex;
                    } finally {
                        latch.countDown();
                    }
                } else {
                    try {
                        if (newStates != null) {
                            for (HashMap.Entry<String, State<EventType>> pair : newStates.entrySet()) {
                                FSMNode<EventType> node = ((StateMachineDriver) wrapped).getNodeByName(pair.getKey());
                                if (node == null) {
                                    ex = new BadStateMachineSpecification(pair.getKey() + " is not known state");
                                    return;
                                }
                                node.setState(pair.getValue());
                                try {
                                    pair.getValue().onStateMachineInitialized(null, (FSMStateView) wrapped);
                                } catch (BadStateMachineSpecification ex) {
                                    this.ex = ex;
                                    return;
                                }
                            }
                        }

                    } finally {
                        latch.countDown();
                    }
                }

            }
        }
    }
    private volatile ReconfigurationTransport transport = null;

    public ReconfigurableStateMachine(StateMachine<EventType> wrapped) {
        super(wrapped);
    }

    public void updateState(String name, State<EventType> state) throws BadStateMachineSpecification {
        HashMap<String, State<EventType>> map = new HashMap<>(1);
        map.put(name, state);
        updateStates(map);
    }

    
    public void updateStates(Map<String, State<EventType>> states) throws BadStateMachineSpecification {
        reconfigure(null,states);
    }

    public void reconfigure(StateMachineBuilder<EventType> builder)throws BadStateMachineSpecification {
        reconfigure(builder,null);
    }

    public void reconfigure(StateMachineBuilder<EventType> builder,
            Map<String, State<EventType>> states) throws BadStateMachineSpecification {

        transport = new ReconfigurationTransport(builder, states);
        try {
        if (wrapped instanceof FSMWrapper)
            wrapped.acceptWrapperTransport(transport);
        else
            transport.apply(this, wrapped);
        } catch( FSMWrapperException ex) {
            throw new BadStateMachineSpecification("reconfiguration propagation failed", ex);
        }
    }
}