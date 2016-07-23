package org.blitvin.statemachine.domfactorytest;

import org.blitvin.statemachine.StateMachineEvent;

public class TestMachineEvent<EventType extends Enum<EventType>> implements StateMachineEvent<EventType> {

	private EventType type;
	@Override
	public EventType getEventType() {
		// TODO Auto-generated method stub
		return type;
	}
	
	public TestMachineEvent(EventType type){
		this.type = type;
	}

}
