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
import java.util.HashMap;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineFactory;
/**
 * This class represents factory creating state machines constructed according to annotations.
 * Actual factory is derived from this class and annotation {@literal @}StateMachines{ {@literal @}StateMachine{...},
 * 																					 {@literal @}StateMachine{...},
 * 																					  ....
 * 																					}
 * provides FSM definition. See also {@link StateMachineSpec}, {@link StateSpec} , {@link StateMachines}
 * 
 * Another way to use this class is to provide the above annotation to other class, and invoke setAnnotatedObject before creation
 * of FSMs 
 * @author blitvin
 *
 */
public class AnnotatedStateMachinesFactory extends StateMachineFactory {

	private Class annotatedClass;
	private final HashMap<String,StateMachineSpec> map;
	@SuppressWarnings({ "rawtypes", "unchecked" })
	
	@Override
	/**
	 * returns state machine instance from specification of state machine with given name. Each invocation creates new independent
	 * object
	 * @param name FSM name 
	 */
	public StateMachine getStateMachine(String name)
			throws BadStateMachineSpecification {
		StateMachineSpec spec = map.get(name);
		if (spec == null)
			return null;
		else {
			
			final Class[] constTypes = {HashMap.class, State.class};
			try {
				HashMap<String,State> statesMap = new HashMap<>();
				@SuppressWarnings("unchecked")
				AnnotationParser parser = new AnnotationParser(statesMap, spec.eventTypeClass());
				parser.parse(spec.states());
				Object[] args = {statesMap,parser.getInitialState()};
				Constructor<? extends StateMachine> constructor = spec.impClass().getConstructor(constTypes);
				StateMachine retVal = (StateMachine) constructor.newInstance(args);
				retVal.completeInitialization(parser.getInitializer());
				return retVal;
			} catch (NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|
					IllegalArgumentException|InvocationTargetException e) {
				throw new BadStateMachineSpecification("StateMachineBuilder.build got exception during construction of state machine "+e.toString(),e); 
			}
		}
	}
	
	/**
	 * default constructor. Assumes that FSM specification is to be derived from class of current object
	 */
	public AnnotatedStateMachinesFactory(){
		map = new HashMap<>();
		annotatedClass = this.getClass();
		rescanAnnotation();
	}
	
	/**
	 * Allows to derive specification of FSMs from annotation of given class
	 * @param c class annotated with {@literal @}StateMachines
	 */
	public void setAnnotatedObject(Class c){
		annotatedClass  = c;
		rescanAnnotation();
	}
	
	/**
	 * Constructor that derives FMS specification from given class
	 * @param c class annotated with {@literal @}StateMachine
	 */
	public AnnotatedStateMachinesFactory(Class c){
		map = new HashMap<>();
		annotatedClass = c;
		rescanAnnotation();
	}
	
	private void rescanAnnotation(){
		StateMachines machines = (StateMachines)annotatedClass.getAnnotation(StateMachines.class);
		map.clear();
		if (machines != null) {
			for(StateMachineSpec machineSpec: machines.value()){
				map.put(machineSpec.name(), machineSpec);
			}
		}
	}
}
