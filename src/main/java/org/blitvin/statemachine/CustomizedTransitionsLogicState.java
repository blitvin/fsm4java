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

/**
 * 
 * @author blitvin
 * @param <EventType>
 */
public interface CustomizedTransitionsLogicState<EventType extends Enum<EventType>> extends State<EventType>{
    /**
     * returns name of state to transit to.
     * The method is called by CustomizedLogicTransition to determine the
     * transition target state
     * @param event
     * @return 
     */
    String  stateToTransitTo(Object event);
}