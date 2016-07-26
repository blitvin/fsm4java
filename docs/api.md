#Using FSM4Java in your projects

THIS DOCUMENT IS OUT OF DATE AND DESCRIBES 1.5 version of FSM4JAVA
IT WILL BE UPDATED TO v2.0 ASAP. Sorry for inconvenience. 


The library allows creation of FSM programmatically as well as via declarative programming. Also FSM4Java provides thread safe implementations of FSM 
and aspect enabled FSM. See below sections for discussion of those.

FSM is defined by 

- set of states
- input alphabet
- initial state
- final state(s)
- transition function 

Each of the above have corresponding entity in the library.

FSM is represented by *StateMachine* interface. The library provides default implementation of the interface called
*SimpleStateMachine*.

Class *State* represents FSM state. Along with code required for FSM to work it contains callbacks that are triggered when
the state becomes current, the state ceases to  be current etc. One way to create FSM that performs actions you need is to 
extend State class with one overriding those callbacks. State maintains a table of transitions corresponding to events. One can
define '*' all catcher transition. It is invoked for all events without explicitly defined transition.

*Transition* interface represents FSM transitions. Creating class that implements the Transition interface is another way to make FSM that doing what
you need. FSM4java provides simple implementation of transition - class *SimpleTransition* that just makes some predefined state current.
In a way this class corresponds to transition in classical computer science finite state machine. However sometimes more sophisticated is 
required, something that determines destination state according to some more parameters of current state as well as event details.
FSM4Java allows easy implementation of customized transitions.

Input alphabet is set of events that is fed to the FSM. In practice events contain additional information so FSM4Java defines interface
*StateMachineEvent* with *getEventType()* method. The only thing relevant to the machine itself is enum *EventType*. FSM determine what transition to run by looking up
transition object corresponding to the value returned by *getEventType()*.

Initial state is State object that is current state before first event comes in. Final states are states that marked with special (isFinal)
flag. 

## Running state machine

In this section I discuss work of state machine once it created. Ways to create FSMs  discussed in details in the next section.

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
