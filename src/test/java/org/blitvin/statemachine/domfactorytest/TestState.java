/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.domfactorytest;

import java.util.Map;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

/**
 *
 * @author blitvin
 * @param <EventType>
 */
public class TestState<EventType extends Enum<EventType>> implements State<EventType> {

    int counter = 0;

    @Override
    public void onStateBecomesCurrent(StateMachineEvent<EventType> theEvent, State<EventType> prevState) {
        ++counter;
    }

    public int getCounter() {
        return counter;
    }

    @Override
    public void onStateIsNoLongerCurrent(StateMachineEvent<EventType> theEvent, State<EventType> nextState) {
    }

    @Override
    public void onInvalidTransition(StateMachineEvent<EventType> theEvent) {
    }

    @Override
    public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
    }

    @Override
    public void onStateDetachedFromFSM() {
    }

}
