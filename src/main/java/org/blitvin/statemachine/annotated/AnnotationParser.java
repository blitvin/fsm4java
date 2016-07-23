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

package org.blitvin.statemachine.annotated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.StateMachineBuilder;

/**
 * This class converts annotation data to builder methods
 * @author blitvin
 * 
 */
class AnnotationParser {
    public static StateMachineBuilder parse(StateMachineSpec annotation) throws BadStateMachineSpecification{
        StateMachineBuilder retVal = new StateMachineBuilder<>(annotation.type(),annotation.eventTypeClass());
        for(StateSpec curStateSpec: annotation.states()){
            if (curStateSpec.applyAspectOnState()) {
                retVal.addState(curStateSpec.name(), curStateSpec.applyAspectOnState()? 
                        StateMachineBuilder.STATE_PROPERTIES_ASPECT | StateMachineBuilder.STATE_PROPERTIES_BASIC :
                        StateMachineBuilder.STATE_PROPERTIES_BASIC);
            } else {
                retVal.addState(curStateSpec.name());
            }
            if (curStateSpec.isFinal())
                retVal.markStateAsFinal();
            if (curStateSpec.isInitial())
                retVal.markStateAsInitial();
            for(Param param: curStateSpec.params())
                retVal.addProperty(param.name(), param.value());
            if (curStateSpec.implClass() != null)
                retVal.addProperty(StateMachineBuilder.STATE_CLASS_PROPERTY, curStateSpec.implClass());
            
            for(TransitionSpec transition:curStateSpec.transitions()){
                if (transition.isDefaultTransition()) {
                    if (transition.type() == null)
                        retVal.addDefaultTransition();
                    else
                        retVal.addDefaultTransition(transition.type());
                } else {
                    if (transition.type() == null)
                        retVal.addTransition(getEventTypeConst(annotation.eventTypeClass(), transition.event()));
                    else
                        retVal.addTransition(getEventTypeConst(annotation.eventTypeClass(), transition.event()), 
                                transition.type());
                }
                for(Param param: transition.params())
                    retVal.addProperty(param.name(), param.value());
            }
        }
        return retVal;
    }
    
    private static final Class<?>[] VALUE_OF_PARAMS = {String.class};
	
    private static Enum getEventTypeConst(Class<? extends Enum<?>> eventTypeClass, String value) throws BadStateMachineSpecification{
		Object[] mvargs =  new Object[1];
		mvargs[0] = value;
		try {
			Method vo = eventTypeClass.getMethod("valueOf", VALUE_OF_PARAMS);
			return (Enum)vo.invoke(null, mvargs);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new BadStateMachineSpecification(value +" is not a valid event type constant", e);
		}
	}
}