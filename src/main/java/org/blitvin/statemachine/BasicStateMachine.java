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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author blitvin
 * @param <EventType>
 */
class BasicStateMachine<EventType extends Enum<EventType>> implements StateMachineDriver<EventType>, FSMStateView<EventType> {

    FSMNode<EventType> current;
    HashMap<String, FSMNode<EventType>> nodes;
    private boolean initialized = false;
    HashMap<Object, Object> properties;
    HashMap<Object, ArrayList<PropertyChangeListener>> propertyChangeListeners;

    public BasicStateMachine(HashMap<String, FSMNode<EventType>> nodes, FSMNode<EventType> initial) {
        this.nodes = nodes;
        current = initial;
        properties = new HashMap<>();
        propertyChangeListeners = new HashMap<>();
    }

    @Override
    public boolean setCurrentNode(FSMNode<EventType> node) {
        if (nodes.containsValue(node)) {
            current = node;
            return true;
        }
        return false;
    }

    @Override
    public boolean setCurrentNode(String nodeName){
        FSMNode<EventType> newCurrentNode = nodes.get(nodeName);
        if (newCurrentNode != null) {
            current = newCurrentNode;
            return true;
        }
        return false;
    }
    @Override
    public FSMNode<EventType> getNodeByName(String name) {
        return nodes.get(name);
    }

    @Override
    public void completeInitialization(Map<?, Map<?, ?>> initializer) throws BadStateMachineSpecification {
        properties.putAll(initializer.get(null));
        for (Entry<String, FSMNode<EventType>> cur : nodes.entrySet()) {
            cur.getValue().onStateMachineInitialized(initializer, this);
        }

        for (Map.Entry<Object, ArrayList<PropertyChangeListener>> subscribersEntry : propertyChangeListeners.entrySet()) {
            Object property = properties.get(subscribersEntry.getKey());
            for (PropertyChangeListener subscriber : subscribersEntry.getValue()) {
                subscriber.onPropertyChange(subscribersEntry.getKey(), property, null);
            }

        }
        initialized = true;
    }

    @Override
    public boolean initializationCompleted() {
        return initialized;
    }
  
    @Override
    public void transit(StateMachineEvent<EventType> event) throws InvalidEventException {
        FSMNode<EventType> next = current.nodeToTransitTo(event);
        if (next != null) {
            current.eventOut(event, next);
            next.eventIn(event, current);
            setCurrentNode(next);
        }
    }

    @Override
    public boolean isInFinalState() {
        return current.holdsFinalState();
    }

    @Override
    public State<EventType> getCurrentState() {
        return current.getState();
    }
  
    @Override
    public State<EventType> getStateByName(String stateName) {
        return nodes.get(stateName).getState();
    }

    private void notifySubscribers(ArrayList<PropertyChangeListener> subscribers,
            Object name, Object prev, Object value){
        if (subscribers != null){
           for (PropertyChangeListener cur : subscribers) {
                    cur.onPropertyChange(name, prev, value);
                }
        }
    }

    @Override
    public boolean setProperty(Object name, Object value) {
        Object prev = properties.put(name, value);
        if (initialized) {
            // notify subscibers for specific object
            notifySubscribers(propertyChangeListeners.get(name), name, prev, value);
            // notify catch-all subscribers
            notifySubscribers(propertyChangeListeners.get(null), name, prev, value);
        }
        return true;
    }

    @Override
    public Object getProperty(Object name) {
        return properties.get(name);
    }

    @Override
    public void registerPropertyChangeListener(PropertyChangeListener listener, Object propertyName) {
        ArrayList<PropertyChangeListener> subscribers = propertyChangeListeners.get(propertyName);
        if (subscribers == null) {
            subscribers = new ArrayList<>();
            propertyChangeListeners.put(propertyName, subscribers);
        }
        subscribers.add(listener);
    }

    @Override
    public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport)
        throws FSMWrapperException{
        transport.apply(this, null);
    }

    @Override
    public String getNameOfCurrentState() {
        return current.getName();
    }

    @Override
    public Set<String> getStateNames() {
        return nodes.keySet();
    }

    @Override
    public HashMap<Object, Object> getFSMProperties() {
        return properties;
    }

}