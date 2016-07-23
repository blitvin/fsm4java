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

import java.lang.reflect.Field;
import java.util.HashMap;


import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMWrapper;
import org.blitvin.statemachine.FSMWrapperException;
import org.blitvin.statemachine.FSMWrapperTransport;

import org.blitvin.statemachine.StateMachine;

public class AnnotatedStateMachine<EventType extends Enum<EventType>> extends FSMWrapper<EventType> {

    //private StateMachine<EventType> implemntation;

    /**
     * This constructor should be used for classes that extend
     * AnnotatedStateMachine and FSM spec provided as annotation to class e.g.
     * {@literal @}States({{name="..", transitions={...}}, {name="..",
     * transitions={...} ....... } public class MyAnnotatedFSM<EventType>
     * extends AnnotatedStateMachine<EventType> { .... } This constructor
     * doesn't require specification of class of the annotation at compile time,
     * it searches the annotation when the constructor is executed The class
     * which has declarations doesn't have to be direct subclass of
     * AnnotatedStateMachine, the constructor searches stack to find suitable
     * class, the only thing that required is super() in the constructor, all
     * the rest is figured out automatically.
     *
     * Note that constructor does not call completeInitialization internally, so
     * before using the FSM you must call this method, typically you want to
     * call it with null parameter as initializer is deduced from the
     * annotations.
     *
     * @throws BadStateMachineSpecification the exception is thrown
     * insufficient/incorrect data provided in annotation
     */
    public AnnotatedStateMachine() throws BadStateMachineSpecification {
        //super(new HashMap<String,State<EventType>>(), null);
        this((HashMap) null);
    }

    public AnnotatedStateMachine(HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        super(null);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Class<?> annotatedClass = this.getClass();
        StateMachineSpec an = null;
        for (int i = 2; i < stackTrace.length; ++i) {
            Class<?> cl;
            try {
                cl = Class.forName(stackTrace[i].getClassName());
            } catch (ClassNotFoundException e) {
                continue;
            }
            if (annotatedClass.isAssignableFrom(cl)) {
                an = cl.getAnnotation(StateMachineSpec.class);
            }
            if (an != null) {
                break; // found annotation - this is a class annotation
            }
        }
        setupFromAnnotation(an, fsmProperties);
    }

    /**
     * This constructor tried to auto detect annotation - it searches it through
     * stack for appropriate annotation You can define annotation at class
     * definition and at field referencing the FSM. E.g. for class
     *
     * {@literal @}States({{name="theFSMName", transitions={...}}, {name="..",
     * transitions={...} ....... } public class MyFSM extends SomeOtherClass {
     * .... public MyFSM() { super(..); }
     *
     * public class SomeOterClass extends AnnotatedStateMachine{@literal <}MyEvent{@literal >}{
     *  ...
     *    public SomeOtherClass() {
     *      super(MyEvent.class,"theFSMName");
     *    }
     * }
     *
     * Note that constructor does not call completeInitialization internally, so
     * before using the FSM you must call this method, typically you want to
     * call it with null parameter as initializer is deduced from the
     * annotations.
     *
     *
     * @param stateMachineName identifier of the state machine specification as
     * set in name field
     * @throws BadStateMachineSpecification thrown if problem happened during
     * the construction e.g. annotation hasn't been found
     */
    public AnnotatedStateMachine(String stateMachineName, HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        super(null);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Class<?> annotatedClass = this.getClass();
        StateMachineSpec an = null;
        for (int i = 2; i < stackTrace.length; ++i) {
            Class<?> cl;
            try {
                cl = Class.forName(stackTrace[i].getClassName());
            } catch (ClassNotFoundException e) {
                continue;
            }

            if (annotatedClass.isAssignableFrom(cl)) {
                an = cl.getAnnotation(StateMachineSpec.class);
                if (an != null && an.name().equals(stateMachineName)) {
                    break; // found annotation - this is a class annotation
                }
            }
            for (Field field : cl.getDeclaredFields()) {
                if (annotatedClass.isAssignableFrom(field.getType())) {
                    an = field.getAnnotation(StateMachineSpec.class);
                    if (an != null && an.name().equals(stateMachineName)) {
                        break; // got it
                    } else {
                        an = null; // no annotation or different state machine name - must continue search
                    }
                }
            }
        }
        setupFromAnnotation(an, fsmProperties);
    }

    public AnnotatedStateMachine(String stateMachineName) throws BadStateMachineSpecification {
        this(stateMachineName, null);
    }

    /**
     * This constructor should be use to instantiate field of
     * AnnotatedStateMachine class (or its subclass) e.g. class MyClass { ...
     * {@literal @}States({{name="..", transitions={...}}, {name="..",
     * transitions={...} ....... } AnnotatedStateMachine<MyEnum> myMachine; ...
     *
     * MyClass(){ ... myMachine = new
     * AnnotatedStateMachine(MyEnum.class,MyClass.class,"myMachine"); }
     *
     * Note that constructor does not calls completeInitialization internally,
     * so before using the FSM you must call this method, typically you want to
     * call it with null parameter as initializer is deduced from the
     * annotations.
     *
     * @param myClass class to retrieve annotation defining FSM
     * @param fieldName field name of the for which to construct the machine (
     * i.e. name of field for which {@literal}States supplied)
     * @param fsmProperties
     * @throws BadStateMachineSpecification he exception is thrown
     * insufficient/incorrect data provided in annotation or annotation is
     * missing
     */
    public AnnotatedStateMachine(Class<?> myClass, String fieldName, HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        super(null);
        try {
            StateMachineSpec an = myClass.getField(fieldName).getAnnotation(StateMachineSpec.class);
            setupFromAnnotation(an, fsmProperties);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new BadStateMachineSpecification("can't access field " + fieldName + " of class " + myClass.getName(), e);
        }
    }

    public AnnotatedStateMachine(Class<?> myClass, String fieldName) throws BadStateMachineSpecification {
        this(myClass, fieldName, null);
    }

    /**
     * This constructor should be used for classes that extend
     * AnnotatedStateMachine and FSM spec provided as annotation to class e.g.
     * {@literal @}States({{name="..", transitions={...}}, {name="..",
     * transitions={...} ....... } public class MyAnnotatedFSM { .... }
     *
     * public class OtherClass { AnnotatedStateMachine machine = new
     * AnnotatedStateMachine(EventType.class, MyAnnotatedFSM.class); }
     *
     * public class AnotherClass extends AnnotatedStateMachine{@literal <}EventType{@literal >}{
     *  	public AnotherClass() {
     *  		super(EventType.class, MyAnnotatedFSM.class);
     *         ....
     *  	}
     * }
     *
     * This constructor allows avoid auto detection by specification of class of
     * annotation
     *
     * Note that constructor does not call completeInitialization internally, so
     * before using the FSM you must call this method, typically you want to
     * call it with null parameter as initializer is deduced from the
     * annotations.
     *
     * @throws
     *
     * @param enumClass class of alphabet of the FSM
     * @param annotatedClass annotated class whose annotation is to be used for
     * FSM construction
     * @throws BadStateMachineSpecification BadStateMachineSpecification the
     * exception is thrown insufficient/incorrect data provided in annotation or
     * annotation is not present
     */
    public AnnotatedStateMachine(Class<?> annotatedClass, HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        super(null);
        try {
            setupFromAnnotation(annotatedClass.getAnnotation(StateMachineSpec.class), fsmProperties);
        } catch (SecurityException e) {
            throw new BadStateMachineSpecification("can't access annotations of class" + annotatedClass.toString(), e);
        }
    }

    public AnnotatedStateMachine(Class<?> annotatedClass) throws BadStateMachineSpecification {
        this(annotatedClass, (HashMap) null);
    }

    private void setupFromAnnotation(StateMachineSpec annotation, HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        if (annotation ==  null)
            throw new BadStateMachineSpecification("no annotation found for AnnotatedStateMachine to build FSM");
        if (fsmProperties != null) {
            wrapped = (StateMachine<EventType>) AnnotationParser.parse(annotation).addFSMProperties(fsmProperties).build();
        } else {
            wrapped = (StateMachine<EventType>) AnnotationParser.parse(annotation).build();
        }
    }

    @Override
    public void acceptWrapperTransport(FSMWrapperTransport<EventType> transport) throws FSMWrapperException {
      wrapped.acceptWrapperTransport(transport);
    }

    

}