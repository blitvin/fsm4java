#Using FSM4Java in your projects

In  order to use FSM4Java state machine(fsm - finite state machine) one should define the following:

- event types of event processed by the state machine. Event type should be enum and event should implement an interface returning
value associated with event type. in real world scenario, events contain far more than just type, but this is in most cases irrelevant
to transition from one state to another
- set of states, identified by names. Business logic code is placed in callbacks which are executed when event is processed. E.g. when new event
arrives, and as result FSM transitions to new current state, onBecomesCurrent() method of this new state is called.
- each state has transitions configuration defining what is "next" state when event of particular type arrives. Typically, static configuration
is enough for such a decision and it is provided in time of FSM creation. FSM4Java provides ways to making such decision more "dynamic" consulting
state object if needed. "Static" and "dynamic" configurations can co-exist in the same state, i.e. part of event types yield transitions to pre-defined
states, and some trigger more complex logic. FSM4Java allows definition of "catch-all-other" transition, that is used if event doesn't match to 
explicitely defined ones.
- one of states must be marked as initial - that is a current state before first event is processed
- FSM implementation allows definition of property objects - this is a mechanism of interaction between state business logic object and outside world. 
Please see discussion below.
- FSM4Java allows extension of state machine functionality by wrapper mechanism e.g. capabilities such as reconfiguration-on-the-fly of the FSM, multi-thread-safe
state machine are implemented as wrappers. For more information please see [Extending FSM functionality with wrappers](wrapper.md)

FSM4Java state machine used through 2 main interfaces : one is for interaction with outside world, e.g. transitioning to new states via new events and
another is API for interaction of state machine with business logic objects. State machine itself is "black box" by design - but it interacts with user logic with
clearly defined contracts in both interfaces, so user doesn't need to concern himself with details of FSM internals.

"Outside world" API is as following:
It is implemented by *StateMachine* interface. The interface defines (or inherits) following methods:

- *transit()* this is a way to notify state machine of new event
- *getCurrentState()* returns business logic object corresponding to current state
- *isInFinalState()* returns true if current state marked as final.
- *getNameOfCirrentState()* returns name of state that is current
- *getProperty()* returns FSM property with given name. See below discussion on usage of FSM properties
- *setProperty()* updates FSM property's value
- *getStateNames()* provides names of all states of the FSM

Contract between business logic of states and FSM comprises of two interfaces:
*State* interface defeines callbacks that are invoked during event processing

- *onStateBecomesCurrent()* callback for implementation of user defined logic upon state becoming current
- *onStateIsNoLongerCurrent()* callback for implementation of user defined logic upon other state becoming current
- *onInvalidTransition()* this callback if transition is impossible e.g. if no transition for particular event type is defined and yet such an event arrived
- *onStateAttachedToFSM()* lifecycle callback that is called so that state object complete initialization e.g. register callbacks, save reference to fsm etc.
- *onStateDetachedFromFSM()* lifecycle callback, it is called when state removed from the FSM

*FSMStateView* defines "services" provided by FSM to state objects

- *getNameOfCirrentState()* returns name of state that is current
- *getProperty()* returns FSM property with given name. See below discussion on usage of FSM properties
- *setProperty()* updates FSM property's value
- *getStateNames()* provides names of all states of the FSM
- *registerPropertyChangeListener()* provides method to be notified when FSM property is modified
- *deregisterPropertyChangeListener()* method of unregistering listener if notification is no more required

## utility classes
Several classes are provided to make developer's life easier when working with FSM4Java.

*StateSkeleton* provides State with empty callbacks. One can extend this class instead of implementing *State* interface. Nothing fancy, just saves several clicks
in IDE when your state object doesn't need to implement all the callbacks( which is typical situation, in many cases the only thing one needs is onStateBacomesCurrent(),
yes closure would be great here , but I decided that FSM4Java should support Java versions prior to 8)

*AnnotatedObjectStateFactory* another helper for creating state objects. Instead of creating distinct class for each or almost each state one can just specify with annotations
callback methods for states, create instance of the factory and it figures all the rest - correct states are injected during FSM creation. It is possible, of cause, provide objects
for some of states explicitely, and *AnnotatedObjectStateFactory* for others. In many cases, readability of code even benefits from placing code of logically related business logic in
single class. One can see example of the factory usage in example of [arithmetic expression parser](expression_parser.md) from test section (it is used for both to demonstrate features
provided by FSM4Java and testing).

*CompoundAspect* simple class allowing attachment several aspect objects to the same FSM. See AOP section below for discussion of aspect enabled state machines

##Internal, external and null transitions
Most common case of processing of events is case of event arrives externally via invocation of transit() method of *StateMachine*. Sometimes business logic requires yielding new event
in cause of processing current one. Such events are called internal ones. FSM4Java provides support of internal events in its state machines. Another use case in event processing is
"swallowing" events in certain situation. FSM4Java allows to do it by null transition mechanism. As name suggests such transition don't modify "state" of FSM at all. Note that there is
a difference between null transitions and transitions from a state to itself. In later case *onStateIsNoLongerCurrent()* and *onStateBecomesCurrent()* are invoked, while for null 
transitions no such invocation happen. Also, in case of AspectEnabledStateMachine distinct callback is defined for null transitions.

##FSM properties and property listeners
Each FSM4Java's state machine contains a map that is called FSM properties. It has several purposes. It is used as repository of utility and configuration objects used internally by FSM and
accessible by state objects. E.g. *AspectEnabledStateMachine* stores a key-value pair with string ""aspects"" as key and Aspect object as value. During initialization FSM nodes obtain reference of the aspect from FSM properties.
Another usage of FSM properties mechanism is interaction of business logic objects with outside world, e.g. providing additional parameters to state objects. For example, one can provide 
per state logging by setting references to specific logger objects, and state object can look up this specific object and use it.
Additional, and very important potential usage of this mechanism is creating "communication" channel between state objects and "outside world". It is possible to inject references of appropriate entities during construction
of state object or before putting it to FSM, but this is not always possible e.g. if the object is created via factory. Moreover sometimes it is needed to replace such communication objects during FSM lifecycle.
FSM properties are helpfull in such an scenario. Those can serve as "well known" location of "mailbox" available through
API both to code using FSM and state objects. Moreover, FSM4Java provides listeners mechanism for changes notification. State object can register property change listener, which is notified
when value object in particular key-value pair is replaced also, it is possible to register listener that is called at change of any value object in the map.

## Running state machine

In this section I discuss work of state machine once it created. Ways to create FSMs  discussed in [Creating FSM](creating_fsm.md).

Probably one creates FSM to perform some context specific code on each incoming event. Context determines which code is executed.
In case of FSM current state of the machine is the context. Initial state is current upon initialization. Then events start to come
in. Events fed to the state machine by invocation of *transit(StateMachineEvent)* method. For most cases you can use SimpleStateMachine as 
FSM implementation. *SimpleStateMachine* performs the following upon invocation of the method:

* it looks up transition corresponding for current state and current event. If no such transition exist (that is neither explicit nor implicit transition defined) *IllegalEventType* exception is thrown.
* *onStateIsNoLongerCurrent()* method is invoked on current state
* *onStateBecomesCurrent()* method is invoked on state returned by transition
* the state returned by transition becomes current state

Proper way to create state machine for your need is extend *State* by overriding callback methods and/or create implementation of Transition doing your logic.

In some cases  it is not desirable to run the callbacks if current method remains such as result of transition ( that is , in FSM diagram, the arrow that exit from the states points back to it).
*SimpleStateMachine* suppresses invocation of the callbacks if Transition returns null.

## State machine types provided by the library

- *BasicStateMachine* this is simplest FSM - events just go in with transit() method , appropriate business logic code is invoked
- *SimpleStateMachine* extends *BasicStateMachine* by support of internal event. It support single internal event at any given point
 of time. In other words, state allowed to emit one internal event.This state machine is sufficient for most use case scenarios. Note that if business logic emitted an internal event FSM correctly
processes new event yielded by target state of first internal event. In other words, it is OK to provide chain of internal transitions, the only requirement is that each "intermediate" state yield
only single event during processing.
- *MultiInternalEventsStateMachine* like *SimpleStateMachine* but allows state object emitting multiple internal events. Those are proceeded in the same order as emitted
Concurrent state machines - suited for multi-threaded environment including thread pooling, correct work in case of events arriving from several threads etc. See [Concurrent FSM document](concurrent_fsms.md) for details
- *ReconfigurableStateMachine* allows modification of transitions and states on the fly. See [wrappers](wrapper.md) document for discussion on why one can find this useful and how to use such
functionality
- *AspectEnabledStateMachine* provides AOP like functionality, see next section for details.

## AspectEnabledStateMachine: poor man's AOP for FSM

In some cases it is useful to run special code at certain point of transition execution. E.g. for logging all transitions defining start of transition
aspect allow running code without change of callbacks of each state used in the FSM. Class *AspectEnabledStateMachine* provides ability to 
run code at certain predefined points of transition. Aspects code should be placed in a class implementing *StateMachineAspects* interface. 
*AspectEnabledStateMachine* has method *setAspects* setting code for appropriate points, after the object passed to the state machine, appropriate
methods are called at following points of transition:

* *onTransitionStart* at beginning of transition
* *onNullTransition* if transition is null one i.e. current state's transit function returns null
* *onControlLeavesState* is called before  'onStateIsNoLongerCurrent' callback of current state's
* *onControlEntersState* is called before 'onStateBecomesCurrent' callback of target state
* *onTransitionFinish* is called before transition processing ends ( it is not called if the transition is null transition)

All methods except for onNullTransition and onTransitionFinish return boolean. If those return false processing of the transition halted.

TBD explain how to get state machine with Aspect enabled for only subset of states
TBD explain implementation of the FSM via aspect property and listeners

##How to continue from here
Now you know how to use state machines provided by FSM4Java. From here you can go to 

- discussion ways of [creating FSM](creating_fsm.md)
- discussion of FSM suited to work [in multi-threaded environments](concurrent_fsms.md)
- extension of fsm functionality with [wrappers](wrapper.md)

Also you can look into test section of the library source code, there you can find typical use cases (such use cases should be tested right). Perhaps it allows
better demonstration of how FSM4Java intended to be used.

