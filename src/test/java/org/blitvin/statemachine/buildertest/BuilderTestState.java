package org.blitvin.statemachine.buildertest;

import java.util.Map;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

public class BuilderTestState<EventType extends Enum<EventType>> implements State<EventType> {

    private String myAttribute = null;

    public String getMyAttribute() {
        return myAttribute;
    }

    @Override
    public void onStateBecomesCurrent(StateMachineEvent<EventType> theEvent, State<EventType> prevState) {
        
    }

    @Override
    public void onStateIsNoLongerCurrent(StateMachineEvent<EventType> theEvent, State<EventType> nextState) {
    }

    @Override
    public void onInvalidTransition(StateMachineEvent<EventType> theEvent) {
    }

    @Override
    public void onStateAttachedToFSM(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
        myAttribute = (String) initializer.get("myAttribute");
    }

    @Override
    public void onStateDetachedFromFSM() {
    }

}