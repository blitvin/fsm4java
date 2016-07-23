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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * This is a main way to obtain FSM object. 
 * Factories internally use this class to  construct FSM
 * @author blitvin
 * @param <EventType>
 */
public class StateMachineBuilder<EventType extends Enum<EventType>> {

    public static final int STATE_PROPERTIES_BASIC = 1;
    public static final int STATE_PROPERTIES_CUSTOMIZED = 2;
    public static final int STATE_PROPERTIES_ASPECT = 0x100;
    final static int NODE_TYPE_MASK = 0xFF;

    /* WARNING ! MODIFICATION OF BELOW ENUMS REQUIRES CORRESPONDING CHANGES IN 
     * state_machines.xsd ,DOMStateMachineFactory and AnnotatedStateMachine
     */
    public static enum FSM_TYPES {

        BASIC, SIMPLE, MULTI_INTERNAL_EVENTS, ASPECT
    };

    public static enum TRANSITION_TYPE {

        BASIC, NULL, CUSTOMIZED
    }

    static class CopyStatesFactory<EventType extends Enum<EventType>> implements FSMStateFactory<EventType> {

        private StateMachine<EventType> sourceFSM;

        public CopyStatesFactory(StateMachine<EventType> fsm) {
            sourceFSM = fsm;
        }

        @Override
        public State<EventType> get(String state, HashMap<Object, Object> initializers) {
            return sourceFSM.getStateByName(state);
        }

    }

    /**
     * property used for creation of state (state business logic object - one
     * defining call backs etc.) from class name. It assumes existence of
     * argument-less constructor see default flow state object creation
     */
    public static final String STATE_CLASS_PROPERTY = "class";
    /**
     * property used for obtaining state (state business logic object - one
     * defining call backs etc.) from FSM global properties. Property searched
     * is [statename]BusinessObject. see default flow state object creation
     */
    public static final String STATE_IN_GLOBAL_PROPERTIES_SUFFIX = "BusinessObject";
    /**
     * property used for obtaining state (state business logic object) from
     * factory passed as property of the state in FSM (by addProperty)
     */
    public static final String STATE_FACTORY_PROPERTY = "stateFactory";

    /**
     * property used for obtaining state(state business logic object) from
     * factory set in FSM global properties
     */
    public static final String TARGET_STATE = "toState";

    private static enum CURRENTLY_CONSTRUCTED_TRANSITION {

        NONE, EVENT, DEFAULT;
    }

    //private FSMNodeFactory<EventType> fsmNodeFactory;
    //private FSMStateFactory<EventType> fsmStateFactory;
    private int defaultProperties;
    // private EnumMap<EventType,Transition<EventType>> curTransitions;
    //private Transition<EventType> curDefaultTransition;
    private Transition<EventType> curTransition;
    private HashMap<Object, Object> curAttributes;
    private HashMap<Object, HashMap<Object, Object>> attributes;
    // private Object attributeObject;
    private HashMap<String, FSMNode<EventType>> nodes;
    private String curStateName = null;
    private FSMNode<EventType> curNode;
    private Class eventTypeClass;
    private FSMNode<EventType> initialNode;
    private CURRENTLY_CONSTRUCTED_TRANSITION curConstructedTransition = CURRENTLY_CONSTRUCTED_TRANSITION.NONE;
    private FSM_TYPES retValType;

    private void setAttributes() {
        if (curConstructedTransition == CURRENTLY_CONSTRUCTED_TRANSITION.NONE) {
            attributes.put(curNode, curAttributes);
        } else {
            attributes.put(curTransition, curAttributes);
        }
        curAttributes = new HashMap<>();
    }

    public static final String STATE_FACTORY_IN_GLOBAL_PROPERTIES = "globalFactory";

    public static final String ASPECTS_PROPERTY="aspects";
    
    public StateMachineBuilder(FSM_TYPES type, Class eventTypeClass) throws BadStateMachineSpecification {
        if (!eventTypeClass.isEnum()) {
            throw new BadStateMachineSpecification("provided class is not an enum");
        }
        this.eventTypeClass = eventTypeClass;
        nodes = new HashMap<>();
        curAttributes = new HashMap<>();
        attributes = new HashMap<>();
        if (type == FSM_TYPES.ASPECT) {
            defaultProperties = STATE_PROPERTIES_BASIC | STATE_PROPERTIES_ASPECT;
        } else {
            defaultProperties = STATE_PROPERTIES_BASIC;
        }
        retValType = type;
    }

    public StateMachineBuilder<EventType> overrideDefaultStateProperties(int stateProperties) {
        defaultProperties = stateProperties;
        return this;
    }

    public StateMachineBuilder<EventType> markStateAsInitial() throws BadStateMachineSpecification {
        if (curNode == null) {
            throw new BadStateMachineSpecification("add node before marking it as initial");
        }
        initialNode = curNode;
        return this;
    }

    public StateMachineBuilder<EventType> markStateAsFinal() throws BadStateMachineSpecification {
        if (curNode == null) {
            throw new BadStateMachineSpecification("add node before marking it as final");
        }
        curNode.doesHoldFinalState();
        return this;
    }

    public StateMachineBuilder<EventType> addState(String name)
            throws BadStateMachineSpecification {
        addState(name, defaultProperties);
        return this;
    }

    public StateMachineBuilder<EventType> addState(String name, State<EventType> state)
            throws BadStateMachineSpecification {
        addState(name, defaultProperties);
        curNode.setState(state);
        return this;
    }

    public StateMachineBuilder<EventType> addState(String name, State<EventType> state, int properties)
            throws BadStateMachineSpecification {
        addState(name, properties);
        curNode.setState(state);
        return this;
    }

    public StateMachineBuilder<EventType> addState(String name, int properties)
            throws BadStateMachineSpecification {
        if (curNode == null) {
            attributes.put(null, curAttributes);// attributes of FSM itself
            curAttributes = new HashMap<>();
        } else {
            setAttributes();
        }
        curConstructedTransition = CURRENTLY_CONSTRUCTED_TRANSITION.NONE;
        curTransition = null;
        switch (properties & NODE_TYPE_MASK) {
            case STATE_PROPERTIES_BASIC:
                curNode = new BasicNode<>(name, new EnumMap<>(eventTypeClass));
                break;
            case STATE_PROPERTIES_CUSTOMIZED:
                throw new BadStateMachineSpecification("customized logic state is not implemented yet");
            default:
                throw new BadStateMachineSpecification("bad state property :" + properties);
        }
        if ((properties & STATE_PROPERTIES_ASPECT) != 0) {
            curNode = new AspectNode<>(curNode);
        }
        nodes.put(name, curNode);
        return this;
    }

    public StateMachineBuilder<EventType> specifyStateObject(State<EventType> state){
        curNode.setState(state);
        return this;
    }
    
    public StateMachineBuilder<EventType> revisitState(String name) throws BadStateMachineSpecification {
       if (curNode == null) {
            attributes.put(null, curAttributes);// attributes of FSM itself
            curAttributes = new HashMap<>();
        } else {
            setAttributes();
        }
        curConstructedTransition = CURRENTLY_CONSTRUCTED_TRANSITION.NONE;
        curTransition = null;
        curNode = nodes.get(name);
        if (curNode == null) {
            throw new BadStateMachineSpecification("revisit unable to find state "+name);
        }
        curAttributes = attributes.get(curNode);
        return this;
    }
    
    public StateMachineBuilder<EventType> addTransition(EventType event) throws BadStateMachineSpecification {
        return addTransition(event, TRANSITION_TYPE.BASIC);
    }

    public StateMachineBuilder<EventType> addTransition(EventType event, String transitionTarget) throws BadStateMachineSpecification {
        return addTransition(event, TRANSITION_TYPE.BASIC).addProperty(TARGET_STATE, transitionTarget);
    }

    public StateMachineBuilder<EventType> addTransition(EventType event, TRANSITION_TYPE transition) throws BadStateMachineSpecification {
        if (curNode == null) {
            throw new BadStateMachineSpecification("add node before adding transitions to it");
        }
        setCurTransitionByType(transition);
        curNode.setTransition(event, curTransition);
        curConstructedTransition = CURRENTLY_CONSTRUCTED_TRANSITION.EVENT;
        return this;
    }

    private void setCurTransitionByType(TRANSITION_TYPE transition) {
        setAttributes();
        switch (transition) {
            case BASIC:
                curTransition = new BasicTransition<>();
                break;
            case NULL:
                curTransition = NullTransition.NULL_TRANSITION;
                break;
            case CUSTOMIZED:
                curTransition = new CustomizedLogicTransition<>();
                break;
        }
    }

    public StateMachineBuilder<EventType> addDefaultTransition(TRANSITION_TYPE transition) throws BadStateMachineSpecification {
        if (curNode == null) {
            throw new BadStateMachineSpecification("add node before adding transitions to it");
        }
        setCurTransitionByType(transition);
        curNode.setDefaultTransition(curTransition);
        curConstructedTransition = CURRENTLY_CONSTRUCTED_TRANSITION.DEFAULT;
        return this;
    }

    public StateMachineBuilder<EventType> addDefaultTransition() throws BadStateMachineSpecification {
        return addDefaultTransition(TRANSITION_TYPE.BASIC);
    }

    public StateMachineBuilder<EventType> addDefaultTransition(String transitionTarget) throws BadStateMachineSpecification {
        return addDefaultTransition(TRANSITION_TYPE.BASIC).addProperty(TARGET_STATE, transitionTarget);
    }
    /*
     public StateMachineBuilder<EventType> statesFrom(StateMachine<EventType> machine,
     boolean overrideStates){
     fsmStateFactory = new CopyStatesFactory(machine,fsmStateFactory,overrideStates);
     return this;
     }
    
     public StateMachineBuilder<EventType> statesFrom(FSMStateFactory<EventType> factory){
     fsmStateFactory = factory;
     return this;
     }*/

    public StateMachineBuilder<EventType> addProperty(Object name, Object value) {
        curAttributes.put(name, value);
        return this;
    }

    private void trySetStateFromFactory(FSMStateFactory<EventType> factory,
            FSMNode<EventType> node,
            String name,
            HashMap<Object, Object> initializers) {
        State<EventType> candidate = factory.get(name, initializers);
        if (candidate != null) {
            node.setState(candidate);
        }
    }

    protected StateMachine<EventType> buildImpl(FSMStateFactory<EventType> factory, boolean overrideDefinedStates) throws BadStateMachineSpecification {

        StateMachineDriver retVal = null;
        if (initialNode == null) {
            throw new BadStateMachineSpecification("Initial state is not defined");
        }

        // TBD refactor this
        switch (retValType) {
            case BASIC:
                retVal = new BasicStateMachine(nodes, initialNode);
                break;
            case SIMPLE:
                retVal = new SimpleStateMachine(nodes, initialNode);
                break;
            case MULTI_INTERNAL_EVENTS:
                retVal = new MultiInternalEventsStateMachine(nodes, initialNode);
                break;
            case ASPECT:
                retVal = new AspectEnabledStateMachine(nodes, initialNode);
                break;
            default:
                throw new BadStateMachineSpecification("This fsm type is not yet implemented");

        }
        setAttributes();
        HashMap<Object, Object> fsmAttributes = attributes.get(null);
        boolean useGlobalFactory = false;
        FSMStateFactory<EventType> globalFactory = null;
        if (fsmAttributes.containsKey(STATE_FACTORY_IN_GLOBAL_PROPERTIES)) {
            try {
                globalFactory = (FSMStateFactory<EventType>) fsmAttributes.get(STATE_FACTORY_IN_GLOBAL_PROPERTIES);
                useGlobalFactory = true;
            } catch (ClassCastException e) {
                // for now ignore attribute - no way to output warning
            }
        }
        
        //populate state objects
        for (HashMap.Entry<String, FSMNode<EventType>> entry : nodes.entrySet()) {
            FSMNode<EventType> theNode = entry.getValue();
            HashMap<Object, Object> nodeAttributes = attributes.get(entry.getValue());

            String stateName = entry.getKey();

            if (factory != null
                    && (theNode.getState() == null || overrideDefinedStates)) {
                trySetStateFromFactory(factory, theNode, stateName, nodeAttributes);
            }

            // try retreive state from fsm attributes
            if (theNode.getState() == null|| overrideDefinedStates) {
                if (fsmAttributes.containsKey(stateName + STATE_IN_GLOBAL_PROPERTIES_SUFFIX)) {
                    try {
                        theNode.setState((State) fsmAttributes.get(stateName + STATE_FACTORY_IN_GLOBAL_PROPERTIES));
                        continue;
                    } catch (ClassCastException e) {
                        throw new BadStateMachineSpecification("Attempt to get state from FSM properties failed: object doesn't implement State interface");
                    }
                }
                if (useGlobalFactory) {
                    trySetStateFromFactory(globalFactory, theNode, stateName, nodeAttributes);
                }
            }

            // fallback to try to get state from class attribute
            if (theNode.getState() == null) {
                Class<? extends State> cl = null;
                try {
                    if ((cl = (Class<? extends State>) nodeAttributes.get(STATE_CLASS_PROPERTY)) == null) {
                        throw new BadStateMachineSpecification("Can't figure out how to construct State " + stateName);
                    }
                } catch (ClassCastException e) {
                    throw new BadStateMachineSpecification("bad class in attribute " + STATE_CLASS_PROPERTY + " for state " + stateName);
                }
                final Class[] noArgsCls = new Class[0];
                try {
                    Constructor<? extends State> ctr = cl.getConstructor(noArgsCls);
                    theNode.setState(ctr.newInstance(new Object[0]));
                } catch (NoSuchMethodException | SecurityException ex) {
                    throw new BadStateMachineSpecification("Problem getting no-args constructor for state " + stateName + ":" + cl.getSimpleName() + ":" + ex.toString());
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new BadStateMachineSpecification("Problem creating instance of state for " + stateName + ":" + ex.toString());
                }
            }
        }

        retVal.completeInitialization(attributes);
        return retVal;
    }

    public StateMachineBuilder<EventType> addFSMProperties(HashMap<Object, Object> fsmProperties) {
        attributes.get(null).putAll(fsmProperties);
        return this;
    }
    public StateMachineBuilder<EventType> addFSMProperty(Object name, Object value){
        attributes.get(null).put(name, value);
        return this;
    }
    public StateMachine<EventType> build(StateMachine<EventType> statesFrom, boolean overrideDefinedStates)
            throws BadStateMachineSpecification {
        return buildImpl(new CopyStatesFactory(statesFrom), overrideDefinedStates);
    }

    public StateMachine<EventType> build(FSMStateFactory<EventType> factory, boolean overrideDefinedStates)
            throws BadStateMachineSpecification {
        return buildImpl(factory, overrideDefinedStates);
    }

    public StateMachine<EventType> build() throws BadStateMachineSpecification {
        return buildImpl(null, true);
    }
}