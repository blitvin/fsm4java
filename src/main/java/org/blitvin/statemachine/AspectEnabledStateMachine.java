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
import static org.blitvin.statemachine.StateMachineBuilder.ASPECTS_PROPERTY;

/**
 *
 * @author blitvin
 * @param <EventType>
 */
public class AspectEnabledStateMachine <EventType extends Enum<EventType>> extends
		SimpleStateMachine<EventType> {
	
	public void setAspects(StateMachineAspects<EventType> aspects) {
                setProperty(ASPECTS_PROPERTY, aspects);
	}
	
	AspectEnabledStateMachine(HashMap<String, FSMNode<EventType>> nodes,
			FSMNode<EventType> initial) throws BadStateMachineSpecification {
		super(nodes, initial);
	}
        
    @Override
    public void completeInitialization(Map<?, Map<?, ?>> initializer) throws BadStateMachineSpecification {
        super.completeInitialization(initializer);
        if (getProperty(ASPECTS_PROPERTY) == null || 
                (!(getProperty(ASPECTS_PROPERTY) instanceof StateMachineAspects)))
            throw new BadStateMachineSpecification("Aspects object is not defined or not instance of StateMachineAspects");
    }
    
}