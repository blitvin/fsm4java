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

import java.util.Set;

/**
 * This is common interface containing methods defined in both external interface
 * of state machine and FSM state view interface
 * @author blitvin
 */
public interface FSMCommonInterface<EventType extends Enum<EventType>> {
     
    Set<String> getStateNames();
     
     /**
     *
     * @return name of current state 
     */
    String getNameOfCurrentState();

    /**
     *
     * @return immutable collection containing states of the FSM /
     * Collection<State<EventType>>getStates();
     */

    /**
     *
     * @param stateName name of desired state
     * @return state with given name , null if no such state exists in FSM
     */
    State<EventType> getStateByName(String stateName);

    boolean setProperty(Object name, Object value);

    Object getProperty(Object name);

}