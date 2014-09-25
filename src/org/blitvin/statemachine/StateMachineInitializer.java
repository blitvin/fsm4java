/*
 * (C) Copyright Boris Litvin 2014
 * This file is part of StateMachine library.
 *
 *  StateMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   NioServer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with StateMachine.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.blitvin.statemachine;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides initialization parameters for State and Transition objects of constructed
 * state machine. Parameters are String->String maps, the class contains mappings state name to 
 * such a map and state name to map of event type to map. 
 * The purpose of this map-of-maps :-) structure is to allow parameterized initialization of objects
 * within state machine. Each state contains table of transitions per event type (alphabet of the
 * machine), but it is quite possible that some transitions should point to states that are not 
 * constructed yet. So initialization is two phase process : 
 * 1. states (including transitions table) are constructed, but initialization of transition 
 * involving computation of related states
 * 2. Once all state and transition objects are created, transitions and state initialization 
 * finalized including figuring out destination states of transitions.
 * 
 * 
 * @author blitvin
 *
 * @param <EventType>
 */
public class StateMachineInitializer<EventType extends Enum<EventType>> {
	
	private HashMap<String,Map<String,String>> stateInits = new HashMap<>();
	private HashMap<String,HashMap<EventType,Map<String,String>>> transitionInits = new HashMap<>();
	/**
	 * returns property map for given state
	 * @param stateName name of the state
	 * @return map of properties, null if not found
	 */
	Map<String,String> getStateProperties(String stateName) {
		return stateInits.get(stateName);
	}
	
	/**
	 * provides parameters for given transition of given state
	 * @param stateName name of state to look for
	 * @param eventType event type corresponding to the transition
	 * @return map of properties, null if information doesn't exist
	 */
	Map<String,String>  getTransitionProperties(String stateName, EventType eventType) {
		HashMap<EventType,Map<String,String>> curStateinitializers = transitionInits.get(stateName);
		if (curStateinitializers == null )
			return null;
		else
			return curStateinitializers.get(eventType);
		
	}
	
	/**
	 * returns map of parameters for all transitions of given state
	 * @param stateName name of state
	 * @return map of parameters, null if not found
	 */
	HashMap<EventType,Map<String,String>> getProperties4StateTransitions(String stateName) {
		return transitionInits.get(stateName);
	}
	
	/**
	 * sets properties map for given state. Properties for default transition correspond to null key
	 * @param stateName state name 
	 * @param propertiesHash new parameters information
	 */
	void setProperties4StateTransitions(String stateName, HashMap<EventType,Map<String,String> > propertiesHash){
		transitionInits.put(stateName, propertiesHash);
	}
	
	/**
	 * sets properties of given state
	 * @param stateName state name
	 * @param properties map of parameters
	 */
	public void setStateProperties(String stateName,Map<String,String>  properties){
		Map<String,String>  props = stateInits.get(stateName);
		if (props == null) {
			stateInits.put(stateName, properties);
		}
		else
			props.putAll(properties);
	}
	
	/**
	 * clear entire map of properties related to given state
	 * @param stateName state name
	 */
	public void clearStateProperties(String stateName) {
		stateInits.remove(stateName);
	}
	
	/**
	 * Add parameters for individual transition of particular state 
	 * @param stateName state name
	 * @param event event corresponding to the transition
	 * @param properties properties map
	 */
	public void addTransitionProperties(String stateName,EventType event,Map<String,String>  properties){
		HashMap<EventType,Map<String,String> > props = transitionInits.get(stateName);
		if (props == null) {
			props = new HashMap<EventType, Map<String,String> >();
			props.put(event, properties);
			transitionInits.put(stateName, props);
			return;
		}
		
		Map<String,String>  transitionProperties = props.get(event);
		if (transitionProperties == null) {
			props.put(event,properties);
			return;
		}
		else
			transitionProperties.putAll(properties);
		
	}
	
	/**
	 * Adds properties for default (*) transition of given state
	 * @param stateName state name 
	 * @param properties parameters of default transition
	 */
	public void addProperties4DefaultTransition(String stateName,Map<String,String>  properties){
		addTransitionProperties(stateName, null, properties);
	}
	/**
	 * removes parameters map of given transition
	 * @param stateName state name of transition
	 * @param event corresponding event type
	 */
	public void clearTransitionProperties(String stateName,EventType event){
		HashMap<EventType,Map<String,String> > props = transitionInits.get(stateName);
		if (props == null)
			return;
		props.remove(event);
	}
	
	/**
	 * remove parameters for all transitions of given state
	 * @param stateName state name
	 */
	public void clearStateTransitions(String stateName){
		transitionInits.remove(stateName);
	}

}
