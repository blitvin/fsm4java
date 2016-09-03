/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.performancetest;

import java.util.Map;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

/**
 *
 * @author blitvin
 */
public class MarkState implements State<PerformanceEnum> {

    private FSMStateView fsm = null;
    public String propertyName;

    public MarkState(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public void onStateBecomesCurrent(StateMachineEvent<PerformanceEnum> theEvent, State<PerformanceEnum> prevState) {
        fsm.setProperty(propertyName, System.currentTimeMillis());
    }

    @Override
    public void onStateIsNoLongerCurrent(StateMachineEvent<PerformanceEnum> theEvent, State<PerformanceEnum> nextState) {
    }

    @Override
    public void onInvalidTransition(StateMachineEvent<PerformanceEnum> theEvent) {
    }

    @Override
    public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
        fsm = containingMachine;
    }

    @Override
    public void onStateDetachedFromFSM() {
    }

}
