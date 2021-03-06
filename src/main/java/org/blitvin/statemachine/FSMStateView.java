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
 * FSMStateView is interface of interaction between FSM and State objects, that is to business logic
 * objects invoked by state machine during transitions. Object implementing this
 * interface is provided to State in onStateMachineInitialized() call. St
 * @author blitvin
 * @param <EventType>
 */
public interface FSMStateView<EventType extends Enum<EventType>> extends FSMCommonInterface<EventType>{
     void registerPropertyChangeListener(PropertyChangeListener listener, Object propertyName);
     void deregisterPropertyChangeListener(PropertyChangeListener listener, Object propertyName);
}