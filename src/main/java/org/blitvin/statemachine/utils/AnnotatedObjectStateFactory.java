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
package org.blitvin.statemachine.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateFactory;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

/**
 * This factory provides capability for constructing business logic objects for
 * FSM from annotated object. Methods of the object provided to constructor
 * according to annotation. The annotation specifies on which state and which
 * callback the method should be invoked. Note that all the methods are called
 * ON THE SAME OBJECT, the object is shared by handlers of all states
 *
 * @author blitvin
 * @param <EventType>
 */
public class AnnotatedObjectStateFactory<EventType extends Enum<EventType>> implements FSMStateFactory<EventType> {

    String fsm = "";
    HashMap<String, HashMap<String, Method[]>> methods;
    final Object annotated;
    private static final int ON_STATE_BECOMES_CURRENT = 0;
    private static final int ON_STATE_IS_NO_LONGER_CURRENT = 1;
    private static final int ON_INVALID_TRANSITION = 2;
    private static final int ON_STATE_MACHINE_INITITALIZED = 3;
    private static final int ON_STATE_DETACHED_FROM_FSM = 4;
    private static final int CALLBACKS = 5;

    private static class WrappedAnnotatedState<EventType extends Enum<EventType>> implements State<EventType> {

        private final Object obj;
        private final Method[] callbacks;

        public WrappedAnnotatedState(Object obj, Method[] callbacks) {
            this.obj = obj;
            this.callbacks = callbacks;
        }

        @Override
        public void onStateBecomesCurrent(StateMachineEvent<EventType> theEvent, State<EventType> prevState) {
            if (callbacks[ON_STATE_BECOMES_CURRENT] != null) {
                try {
                    callbacks[ON_STATE_BECOMES_CURRENT].invoke(obj, theEvent, prevState);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        @Override
        public void onStateIsNoLongerCurrent(StateMachineEvent<EventType> theEvent, State<EventType> nextState) {
            if (callbacks[ON_STATE_IS_NO_LONGER_CURRENT] != null) {
                try {
                    callbacks[ON_STATE_IS_NO_LONGER_CURRENT].invoke(obj, theEvent, nextState);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        @Override
        public void onInvalidTransition(StateMachineEvent<EventType> theEvent) {
            if (callbacks[ON_INVALID_TRANSITION] != null) {
                try {
                    callbacks[ON_INVALID_TRANSITION].invoke(obj, theEvent);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        @Override
        public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
            if (callbacks[ON_STATE_MACHINE_INITITALIZED] != null) {
                try {
                    callbacks[ON_STATE_MACHINE_INITITALIZED].invoke(obj, initializer, containingMachine);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        @Override
        public void onStateDetachedFromFSM() {
            if (callbacks[ON_STATE_DETACHED_FROM_FSM] != null) {
                try {
                    callbacks[ON_STATE_DETACHED_FROM_FSM].invoke(obj);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    private Method[] callbacks(String fsm, String state) {
        HashMap<String, Method[]> fsmStates = methods.get(fsm);
        if (fsmStates == null) {
            fsmStates = new HashMap<>();
            methods.put(fsm, fsmStates);
        }
        Method[] callbacks = fsmStates.get(state);
        if (callbacks == null) {
            callbacks = new Method[CALLBACKS];
            fsmStates.put(state, callbacks);
        }
        return callbacks;
    }

    public AnnotatedObjectStateFactory(Object annotated) {
        methods = new HashMap<>();
        this.annotated = annotated;
        for (Method m : annotated.getClass().getMethods()) {
            onStateBecomesCurrent becomesCurrentAn = m.getAnnotation(onStateBecomesCurrent.class);
            if (becomesCurrentAn != null && (m.getParameterCount() == 2)
                    && m.getParameterTypes()[0].equals(StateMachineEvent.class)
                    && m.getParameterTypes()[1].equals(State.class)) {
                callbacks(becomesCurrentAn.fsm(), becomesCurrentAn.state())[ON_STATE_BECOMES_CURRENT] = m;
                continue;
            }
            onStateIsNoLongerCurrent noLongerCurrentAn = m.getAnnotation(onStateIsNoLongerCurrent.class);
            if (noLongerCurrentAn != null && (m.getParameterCount() == 2)
                    && m.getParameterTypes()[0].equals(StateMachineEvent.class)
                    && m.getParameterTypes()[1].equals(State.class)) {
                callbacks(noLongerCurrentAn.fsm(), noLongerCurrentAn.state())[ON_STATE_IS_NO_LONGER_CURRENT] = m;
                continue;
            }
            onInvalidTransition invalidTransitionAn = m.getAnnotation(onInvalidTransition.class);
            if (invalidTransitionAn != null && (m.getParameterCount() == 1)
                    && m.getParameterTypes()[0].equals(StateMachineEvent.class)) {
                callbacks(invalidTransitionAn.fsm(), invalidTransitionAn.state())[ON_INVALID_TRANSITION] = m;
                continue;
            }
            onStateMachineInitialized fsmInitializedAn = m.getAnnotation(onStateMachineInitialized.class);
            if (fsmInitializedAn != null && (m.getParameterCount() == 2)
                    && m.getParameterTypes()[0].equals(Map.class)
                    && m.getParameterTypes()[1].equals(FSMStateView.class)) {
                callbacks(fsmInitializedAn.fsm(), fsmInitializedAn.state())[ON_STATE_MACHINE_INITITALIZED] = m;
            }
            onStateDetachedFromFSM fsmDetachedAn = m.getAnnotation(onStateDetachedFromFSM.class);
            if (fsmDetachedAn != null && m.getParameterCount() == 0) {
                callbacks(fsmDetachedAn.fsm(), fsmDetachedAn.state())[ON_STATE_DETACHED_FROM_FSM] = m;
            }
        }
    }

    public AnnotatedObjectStateFactory(Object annotated, String fsm) {
        this(annotated);
        this.fsm = fsm;
    }

    public AnnotatedObjectStateFactory specifyFSMName(String fsm) {
        this.fsm = fsm;
        return this;
    }

    @Override
    public State<EventType> get(String state, HashMap<Object, Object> initializers) {
        return new WrappedAnnotatedState<>(annotated, callbacks(fsm, state)); // TBD should it cache states?
    }

}
