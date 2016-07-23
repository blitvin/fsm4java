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

package org.blitvin.statemachine;

import java.util.HashMap;


/**
 * StateMachineFactory specifies interface of factory for creating state machines
 * It also defined default factory
 * @author blitvin
 *
 */
public abstract class StateMachineFactory {
	/**
	 * property defining class of default state machine factory
	 */
	public static final String DEFAULT_FACTORY_CLASS_PROPERTY =  "org.blitvin.StateMachine.defaultFactory";
	/**
	 * property defining class of default state machine if not specified by DEFAULT_FACTORY_CLASS_PROPERTY
	 */
	public static final String DEFAULT_FACTORY_CLASS_DEFAULT_VALUE =  "org.blitvin.StateMachine.DOMStateMachineFactory";
	private static class DefaultFactoryInstance {
		private  static StateMachineFactory defaultFactory = null;
		private static boolean initialized = false;
	
		/**
		 * return (and if necessary, initialize) default factory
		 * @return default state machine factory
		 */
		static StateMachineFactory getDefaultFactory(){
			if (!initialized) {
				String defaultFactoryClass = System.getProperty(DEFAULT_FACTORY_CLASS_PROPERTY,
						DEFAULT_FACTORY_CLASS_DEFAULT_VALUE);
			
				try {
					initialized = true;
					Class<StateMachineFactory> cl = (Class<StateMachineFactory>) Class.forName(defaultFactoryClass);
					defaultFactory = (StateMachineFactory)cl.newInstance();
				} catch (ClassCastException|ClassNotFoundException|SecurityException|IllegalAccessException | InstantiationException e){
					// report failure where?
				
				} 
			}
			return defaultFactory;
		}
		
		static void setDefaultFactory(StateMachineFactory _defaultFactory){
			defaultFactory = _defaultFactory;
			initialized = true;
		}
	}
	
	/**
	 * set new default state machine factory
	 * @param defaultFactory
	 */
	static public void setDefaultFactory(StateMachineFactory defaultFactory) {
		DefaultFactoryInstance.setDefaultFactory(defaultFactory);
	}
	
	/**
	 * get default state machine factory. It is lazy initialized.
	 * @return
	 */
	static public StateMachineFactory getDefaultFactory(){
		return DefaultFactoryInstance.getDefaultFactory();
	}
	
	
	public abstract StateMachine<? extends Enum<?>> getStateMachine(String name) throws BadStateMachineSpecification; 
        public abstract StateMachine<? extends Enum<?>> getStateMachine(String name, HashMap<Object,Object> fsmProperties) 
                throws  BadStateMachineSpecification;
	
        public abstract StateMachineBuilder<? extends Enum<?>> getBuilder(String name) throws BadStateMachineSpecification;
}