package org.blitvin.statemachine.buildertest;

import java.util.Map;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.State;

public class BuilderTestState<EventType extends Enum<EventType>> extends State<EventType> {

	private String myAttribute = null;
	
	public BuilderTestState(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
	}
	
	@Override
	public void onStateMachineInitialized(Map<Object,Object>  initializer) throws BadStateMachineSpecification
	{
		myAttribute = (String) initializer.get("myAttribute");
	}
	
	public String getMyAttribute(){
		return myAttribute;
	}

}
