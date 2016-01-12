/*
 * (C) Copyright Boris Litvin 2014, 2015
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.Transition;

/**
 * This class parses annotation and converts those to form that can be used by StateMachine constructors
 * @author blitvin
 *
 * @param <EventType> FSM alphabet 
 */
class AnnotationParser<EventType extends Enum<EventType>> {
	
	@SuppressWarnings("rawtypes")
	protected static final Class[] STATE_CONSTUCTOR_PARAMS={String.class,Boolean.class};
	@SuppressWarnings("rawtypes")
	protected static final Class[] VALUE_OF_PARAMS = {String.class};
	
	final private HashMap<String,org.blitvin.statemachine.State<EventType>> states;
	private HashMap<Object, HashMap<Object, Object>> initializer;
	private org.blitvin.statemachine.State<EventType> initialState;
	final private Method valueOf;
	public AnnotationParser(HashMap<String,org.blitvin.statemachine.State<EventType>> map,
			Class<? extends Enum<EventType>> eventTypeClass) throws BadStateMachineSpecification{
		states = map;
		initializer = new HashMap<Object, HashMap<Object,Object>>();
		try {
			valueOf = eventTypeClass.getMethod("valueOf", VALUE_OF_PARAMS);
		} catch ( IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new BadStateMachineSpecification(eventTypeClass.toString() +" is not a valid event type constant", e);
		}
	}
	
	
	public State<EventType> getInitialState(){
		return initialState;
	}
	
	public HashMap<Object, HashMap<Object, Object>> getInitializer(){
		return initializer;
	}
	@SuppressWarnings("unchecked")
	public void parse(org.blitvin.statemachine.annotated.StateSpec[] statesAnnotations) throws BadStateMachineSpecification {
		for(StateSpec stateAnnotation : statesAnnotations){
			if(states.containsKey(stateAnnotation.name())){
				throw new BadStateMachineSpecification("Multiple states with the same name "+stateAnnotation.name());
			}
			@SuppressWarnings({  "rawtypes" })
			Constructor<? extends State> c;
			try {
				c = stateAnnotation.implClass().getConstructor(STATE_CONSTUCTOR_PARAMS);
			} catch (NoSuchMethodException | SecurityException e1) {
				throw new BadStateMachineSpecification("Can't find constructor for "+stateAnnotation.implClass().toString(), e1);
			}
			Object[] args = new Object[2];
			args[0] = stateAnnotation.name();
			args[1] =  stateAnnotation.isFinal();
			State<EventType> s;
			try {
			 s = (State<EventType>) c.newInstance(args);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new BadStateMachineSpecification("Failed to instantiate object of "+stateAnnotation.implClass().toString(),e);
			}
			states.put(stateAnnotation.name(), s);
			if (stateAnnotation.isInitial())
				initialState = s;
			setInitializers(s, stateAnnotation.params());
			setTransitions(s, stateAnnotation.transitions());
		}
	}
	
	private void setInitializers(Object o, Param[] params){
		if (params.length == 0)
			return;
		HashMap<Object,Object> initHash = new HashMap<Object, Object>();
		for(Param param: params){
			initHash.put(param.name(), param.value());
		}
		initializer.put(o, initHash);
	}
	
	private void setTransitions(State<EventType> state,TransitionSpec[] transitions) throws BadStateMachineSpecification{
		HashMap<EventType,Transition<EventType>> transitionsMap= new HashMap<EventType, Transition<EventType>>();
		for(TransitionSpec transitionSpec: transitions){
			try {
				Transition<EventType> t = transitionSpec.implClass().newInstance();
				
				if (transitionSpec.isDefaultTransition())
					transitionsMap.put(null,t);
				else {
					Object[] args = new Object[1];
					args[0] = transitionSpec.event();
					transitionsMap.put((EventType)valueOf.invoke(null, args),t);
				}
				setInitializers(t, transitionSpec.params());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new BadStateMachineSpecification("can't create transition object of class"+transitionSpec.implClass().toString(), e);
			}
		}
		state.setTransitions(transitionsMap);
	}
}
