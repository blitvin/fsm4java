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

/**
 * This interface defines factory for creating State<EventType> i.e. user
 * part of FSM state ( that is business logic encapsulated in object that
 * defines callbacks called by FSM e.g. when state becomes current state)
 * @author blitvin
 * @param <EventType>
 */
public interface FSMStateFactory<EventType extends Enum<EventType>> {
    /**
     * the method returns State object associated with state name
     * @param state - name of the state for which State object is constructed
     * @param initializers - map of initializers for proper initialization of the object
     * @return State object
     */
    State<EventType> get(String state,HashMap<Object,Object> initializers);
}