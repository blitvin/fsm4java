/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.performancetest;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineBuilder;

/**
 *
 * @author blitvin
 */
public class BasicFSMPerformanceMesurement {

    public static String STATE_INITIAL = "initial";
    public static String STATE_START = "start";
    public static String STATE_MIDDLE = "middle";
    public static String STATE_FINISH = "finish";

    public static void main(String args[]) throws BadStateMachineSpecification, InvalidEventException {
        StateMachine<PerformanceEnum> fsm
                = new StateMachineBuilder<PerformanceEnum>(StateMachineBuilder.FSM_TYPES.BASIC, PerformanceEnum.class).
                addState(STATE_INITIAL, new EmptyState()).markStateAsInitial().addDefaultTransition(STATE_START).
                addState(STATE_START, new MarkState(STATE_START)).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addState(STATE_MIDDLE, new EmptyState()).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addTransition(PerformanceEnum.B, STATE_FINISH).
                addState(STATE_FINISH, new MarkState(STATE_FINISH)).addDefaultTransition(STATE_INITIAL).build();
        PerformanceEvent ev = new PerformanceEvent(PerformanceEnum.A);
        for (int j = 0; j < 10; ++j) {
            for (int i = 0; i < 1000000000; ++i) {
                fsm.transit(ev);
            }
            ev.setEventType(PerformanceEnum.B);
            fsm.transit(ev);
            long start = (Long) fsm.getProperty(STATE_START);
            long finish = (Long) fsm.getProperty(STATE_FINISH);

            System.out.println("Running 1000000000 transitions : start=" + start + " finish=" + finish + " elapsed=" + (finish - start));
            ev.setEventType(PerformanceEnum.A);
        }

        fsm = new StateMachineBuilder<PerformanceEnum>(StateMachineBuilder.FSM_TYPES.BASIC, PerformanceEnum.class).
                addState(STATE_INITIAL, new EmptyState()).markStateAsInitial().addDefaultTransition(STATE_START).
                addState(STATE_START, new MarkState(STATE_START)).addTransition(PerformanceEnum.A, STATE_MIDDLE).
                addState(STATE_MIDDLE, new EmptyState()).addTransition(PerformanceEnum.A, StateMachineBuilder.TRANSITION_TYPE.NULL).
                addTransition(PerformanceEnum.B, STATE_FINISH).
                addState(STATE_FINISH, new MarkState(STATE_FINISH)).addDefaultTransition(STATE_INITIAL).build();
        for (int j = 0; j < 10; ++j) {
            for (int i = 0; i < 1000000000; ++i) {
                fsm.transit(ev);
            }
            ev.setEventType(PerformanceEnum.B);
            fsm.transit(ev);
            long start = (Long) fsm.getProperty(STATE_START);
            long finish = (Long) fsm.getProperty(STATE_FINISH);

            System.out.println("Running 1000000000 transitions : start=" + start + " finish=" + finish + " elapsed=" + (finish - start));
            ev.setEventType(PerformanceEnum.A);
        }
    }
}