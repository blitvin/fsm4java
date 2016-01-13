#FSM4Java
FSM4Java is a library that allows creation and usage of deterministic finite state machine ( aka finite state automata aka state machine aka FSM).
More details on what FSM is can be found e.g. on [http://en.wikipedia.org/wiki/Finite-state_machine](wikipedia).

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

## Creation of state machine

There are two ways to build FSM with FSM4Java: programmaticaly and using declarative programming.

### Programmatic creation of FSM: class StateMachineBuilder

*StateMachineBuilder*, as one can guess, implements the builder pattern for creation of FSM objects.

It is parameterized with event type (that is, alphabet) of the state machines to be created. One can invoke constructor with or without class implementing StateMachine. E.g. creating the builder looks like `StateMachineBuilder<EVENTS_TYPE> builder = new StateMachineBuilder<>("",MyStateMachine.class)`. 

Constructor without state machine class assumes that FSM to be implemented is of *SimpleStateMachine* class.

Once builder object created, one can start supply details of the state machine, that is states, transitions between states and parameters for state and transition creation. Calls can be chained e.g. typical snippet for creation of state object with all its transitions  can look like
```java
builder.addState(new State("state1",false)).addAttribute("myAttribute", "isSet").markAsInitial().
			.addTransition(EVENT_A,transition).addAttribute("toState", "state2")
			.addTransition(EVENT_B, transition)
			.addTransition(EVENT_C, new SimpleTransition<EVENTS_TYPE>()).addAttribute("toState", "state3")
			.addDefaultTransition(new SimpleTransition<EVENTS_TYPE>()).addAttribute("toState","state4");
```

In this example, we  create new state named "state1", than provide a parameter that will be passed to the state and mark the state as initial one. Then we create all its transitions. As you can see the same transition can be reuesed if sevderal events require the same behaviour.

Also, this example shows how to use the attributes: *SimpleTransition* is a transition class that always transits to predefined state. Name of this state is supplied in *addAttribute* invocation with parameter "toState". Usage of such attributes allows to reference states that are not yet defined. Builder checks correctness of this data before finalizing FSM object (actually it delegates the check to objects themselves, e.g. after creation of all states transition's appropriate callback is called, so if state with particular name doesn't exist, the method can throw exception).


Once all the data of FSM is supplied one can call `build()` method to get state machine.
```java 
MyStateMachine<EVENTS_TYPE> machine = new StateMachineBuilder<EVENTS_TYPE>("",MyStateMachine.class).addState(...).addTransition(...)
				    	.addState().addTransition(...)
					.....
                                        .build();
```

### Declarative programming: creating FSM from XML configuration

FSM4Java allows creation of state machine corresponding to provided xml specification. Class *DOMStateMachineFactory* handles all the details.
You can either explecitely specify location of XML file, or use argument-less constructor,which uses value specified by system
property *org.blitvin.statemachine.DOMStateMachineFactoryImplementation.defaultXmlFileName*. FSM object can be obtained by 
invocation of *getStateMachine(String name)* where name is name of the state machine specification in the XML file.

```java
DOMStateMachineFactory factory = ....;
....
SimpleStateMachine<TestEnum> machine =(SimpleStateMachine<TestEnum>)factory.getStateMachine("myFSM");
```

*DOMStateMachineFactory* also provides default factory object that can be obtained by invocation static method *DOMStateMachineFactory.getDefaultFactory()*. getStateMachine always create new object, so one can obtain multiple instances corresponding to the same FSM spec (in our example myFSM) and use them independently one from another.

Now let's see what the XML file looks like.  Below is a little example.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<stateMachines>
        <stateMachine name="myFSM" eventTypeClass="org.blitvin.StateMachine.domfactorytest.TestEnum">
                <state name="state1" isInitial="true">
                       <transition event="enum1" toState="state2"/>
                       <transition event="enum2" toState="state3"/>
                       <other_events_transition toState="state1"/>
                </state>
                <state name="state2" isFinal="false">
                        <transition event="enum1" toState="state1"
                                class="org.blitvin.StateMachine.domfactorytest.TestTransition"
                				testTransitionAttribute="true"/>
                        <other_events_transition toState="state3"/>
                </state>
                <state name="state3" isFinal="true">
                        <other_events_transition toState="state2"/>
                </state>
        </stateMachine>
</stateMachines>
```

The file must contain node *stateMachines* which includes list of nodes *stateMachine*.
Each such a node defines specification of an FSM. Let's go over the stateMachine in the example.

* *name* attribute provides name of the FSM. This is a name to be supplied to *getStateMachine*, this is how the factory knows which FSM to construct. This atribute is mandatory.
* *eventTypeClass* is another mandatory attribute specifying class name of the Alphabet (a.k.a. type of events consumed by the state machine)
* *class* is optional attribute specifying class of the state machine. That is the resulting object will be of class *className&lt;eventTypeClass&gt;*. 
Actually, of cause, the type is className because of generics erasure , but you should think of it as *className&lt;eventTypeClass&gt;*. E.g. assignment 
to the object of the same class with incorrect Alphabet makes the machine useless as you can't trigger correct transitions.

*stateMachine* node contains list of *state* nodes. Those nodes have the following attributes:

* *name* the state's name. It can be used to allocate appropriate state. E.g. *SimpleTransition* requires the state name of target state. The name must be unique in the list of states of the FSM.
* *isFinal* boolean property that marks the state as final
* *isInitial* boolean property that marks the state is initial i.e. it is a state of FSM before first event arrives. Exactly one state must be marked as initial
* *class* is optional attribute defining the class of the state object. If omitted *org.blitvin.statemachine.State* assumed

The state contains list of transitions and optionally *other_events_transition* node representing default (*) transition. Transition has the following attributes:

* *event* event on which the transition is invoked
* *class* optional attribute containing class of the transition object. Default is  *org.blitvin.statemachine.SimepleTransition*

*other_events_transition* can contain *class* , but not *event* attribute.

Nodes statesMachine, state, transition can define additional attributes. When new FSM constructed, map of those attributes are passed to constructor of each entity (e.g. see toState attribute for events).

The file format is defined by *state_machines.xsd* provided with the library.

### Declarative programming: Annotation driven FSM creation

FSM4Java provides two ways of creating FSM from specification provided in annotations - factory and stand alone classes. 
Annotation factory class is *org.blitvin.statemachine.annotated.AnnotationStateMachineFactory*.
Annotation used by factory , *@StateMachines* describes list of state machine specifications. State machine can be obtained from the factory by providing
the FSM specification name, similar to *DOMStateMachineFactory* operation.

E.g.
```java

@StateMachines({@StateMachineSpec(name="MyStateMachine", 
					eventTypeClass = TestEnum.class , 
					states = {@StateSpec(name="state1", isFinal=false, isInitial=true, 
								transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state2")}),
							 				@TransitionSpec(event="B",params={@Param(name="toState",value="state3")})}			
									),
						   @StateSpec(name="state2",
								transitions={@TransitionSpec(event="A", params={@Param(name="toState",value="state3")}),
								 			@TransitionSpec(isDefaultTransition=true, params={@Param(name="toState", value="state2")})
											}
								 	),
						   @StateSpec(name="state3", isFinal=true,
								transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state1")}),
											@TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
											@TransitionSpec(event="C",params={@Param(name="toState",value="state2")})
											}
									)
							}
					),
				@StateMachineSpec(name="MyOtherStateMachine",
					eventTypeClass = TestEnum2.class,
					states = {@StateSpec(name="state1", isInitial=true,
								transitions={@TransitionSpec(event="AAA", params={@Param(name="toState", value="otherState")}),
											 @TransitionSpec(isDefaultTransition=true, params={@Param(name="toState",value="")})
											}
									),
							 @StateSpec(name="otherState", isFinal=true, class=org.blitvin.statemachine.annotated.SomeState.class,
								transitions={@TransitionSpec(event="BBB", params={@Param(name="toState", value="state1")}),
											 @TransitionSpec(isDefaultTransition=true, params={@Param(name="toState",value="")})
											}
									)
							}
				 )			 
							
			}
		)
public class AnnotatedStateMachinesFactoryClass extends
		AnnotatedStateMachinesFactory {

}


...
AnnotatedStateMachinesFactoryClass factory = new AnnotatedStateMachineFactoryClass();
....
StateMachine<TestEnum> machine = (StateMachine<TestEnum>)factory.getStateMachine("MyStateMachine");
....


```
In the above example the factory defines two state machines - MyStateMachine and MyOtherStateMachine.

FSM4java doesn't require usage of factory. It provides a way to put annotation in the FSM defining class itself. There are two possible use cases:
one is FSM is distinct class, and second one is FSM is member of some other class. Actually there is a third possible option - FSM is local variable,
but unfortunately, due to bug in JVM specification, annotations of local variables are not stored in class files and therefore not available at runtime.
There is a JSR redefining annotation in progress, so hopefully at some point in the future this last use case will be supported.

Let's see how FSM class can be constructed using specification in annotations.
The class must be subclassed from *org.blitvin.statemachine.annotated.AnnotatedStateMachine*. In order to fully define FSM The only missing part
is states specification as class itself and event type enum are provided in code. The annotated class looks like
```java
@States(name="annotatedSubclass",value=
{@StateSpec(name="state1", isFinal=false, isInitial=true, 
		transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state2")}),
					 @TransitionSpec(event="B",params={@Param(name="toState",value="state3")})}			
		),
@StateSpec(name="state2",
		transitions={@TransitionSpec(event="A", params={@Param(name="toState",value="state3")}),
					 @TransitionSpec(isDefaultTransition=true, params={@Param(name="toState", value="state2")})
					}
		),
@StateSpec(name="state3", isFinal=true,
		transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state1")}),
					 @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
					 @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})})
}
)
public class TestAnnotatedSubclass<EventType extends Enum<EventType>> extends AnnotatedStateMachine<EventType> {

	
	public TestAnnotatedSubclass(Class<? extends Enum<EventType>> enumClass)
			throws BadStateMachineSpecification {
		super(enumClass);
		completeInitialization(null);
	}

}
```

If FSM is member of the class (second use case ) the definition looks like
```java
public class ClassWithFSMMember {
    ....
	@States(name="myMachine",
			value={@StateSpec(name="state1", isFinal=false, isInitial=true, 
					transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state2")}),
								 @TransitionSpec(event="B",params={@Param(name="toState",value="state3")})}			
				    ),
				   @StateSpec(name="state2",
				    transitions={@TransitionSpec(event="A", params={@Param(name="toState",value="state3")}),
					      	    @TransitionSpec(isDefaultTransition=true, params={@Param(name="toState", value="state2")})
							}
				    ),
		           @StateSpec(name="state3", isFinal=true,
				    transitions={@TransitionSpec(event="A",params={@Param(name="toState",value="state1")}),
					     		 @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
						    	 @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})})
		}
	)
	public AnnotatedStateMachine<TestEnum> machine;
	...
```
The difference is only in placement of the annotation.

*AnnotatedStateMachine* provides 4 constructors for creation of FSM, two for situation of explicit specification of annotation placement and 2 for 
"annotation autowiring", that is runtime search for appropriate annotation.
First two are:

* *AnnotatedStateMachine(Class<? extends Enum<EventType>> enumClass,Class<?> annotatedClass)* this one takes annotation from annotatedClass
* *AnnotatedStateMachine(Class<? extends Enum<EventType>> enumClass,Class<?> myClass,String fieldName)* this one looks for specification in member *fieldname* of class *myClass*

Auto-wiring ones are:
* *AnnotatedStateMachine(Class<? extends Enum<EventType>> enumClass)* and 
* *AnnotatedStateMachine(Class<? extends Enum<EventType>> enumClass, String stateMachineName)*

The constructor with only one parameter is for first use case and the second one treats all the autowiring situations.

Autowiring in both is based on searching stack for appropriate annotation. In first use case, the constructor is called
with super(), in the second use case the constructor is called when member is initialized (typically in constructor of containing class).
In any case class containing annotation is in one of stack frames. So AnnotatedStateMachine(enumClass) goes through the stack and
searches class that is assignable from *this.getClass()*, and looks for class annotation @States.

*AnnotatedStateMachine(Class&lt;? extends Enum&lt;EventType&gt;&gt; enumClass, String stateMachineName)* looks for class annotation and goes through declared field of each class
for correct member. Match is found if member is of correct class and name field of @States annotation matches *stateMachineName*.
The name matching allows disambiguation in case of several possible candidates.


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

## Thread safe state machines: SynchronizedStateMachine and ConcurrentStateMachine

*SimpleStateMachine* is not thread safe. In many cases FSM is accessed from single thread, in which case thread safety is not an issue.
But sometimes FSM must or should operate in multi-threaded environment. If events are coming from several threads, FSM must be thread-safe.
Sometimes even if events come from single thread, results of FSM execution are not required immediately, and in such case one can benefit
from asynchronous execution of FSM logic and checking state/ retreiving results later. FSM4Java provides two concurrency handling classes.

*SynchronizedStateMachine* is simple wrapper that makes all relevant methods synchronized. It operates in very similar manner to what SynchronizedCollection does to
collections.

*ConcurrentStateMachine* provide ways for asynchronous execution, speculative event processing, thread safety and more.
FSM logic performed in dedicated thread, and events held in concurrent message queue before being executed. This ensures partial order,
that is events from each thread are processed in order they submitted by *transit()*, *CAStransit()*, *asyncTransitAndGetFutureState()* or *asyncTransit()*.

Events are submitted via following methods:

* *transit(StateMachineEvent&lt;EventType&gt;)* when this method invoked, "regular" behavior of single-threaded FSM emulated, that is
calling thread is paused until processing of the event passed as parameter is completed.
* *asyncTransit(StateMachineEvent&lt;EventType&gt; event)* this method submits the event and immediately returns.
* *StampedState&lt;EventType&gt; transitAndGetResultingState(StateMachineEvent&lt;EventType&gt; event)* works similar to transit, but
returns current state after event processing finished. The operation is atomic w.r.t. FSM operation, that is returned state is current state
of the FSM after transition is finished and before any other event is processed.
* *StampedState&lt;EventType&gt; CAStransit(StateMachineEvent&lt;EventType&gt; event, int generation)* is atomic transit which either executed entirely or
rolled back. The transition happens if generation provided in argument is 0 ( enumeration of generations starts from 1) or the same as 
current generation (time stamp) of FSM. generation can be obtained by invoking *getCurrentStampedState* or from objects returned
by *CAStransit* or *transitAndGetResultingState*. Generation is increased at each processed event, so if you invoke CAStransit
with non-zero generation, event is processed only if no other event processed between obtaining generation and the event processing started.
* *Future&lt;StampedState&lt;EventType&gt;&gt; asyncTransitAndGetFutureState(StateMachineEvent&lt;EventType&gt; event)* put event for asynchronous processing
and return Future object that provide resulting state when processing is done
 

Notes on other methods:
* *getCurrentStampedState()* returns current state wrapped to StampedState( that is with generation). However, in multithreaded environment
it is quite possible that generation is out-of-date by time caller get the object
* *completeInitializtion(...)* this method completes initialization of underlying FSM and starts dedicated thread of the class.
This method must be invoked before events start coming.Without calling this method event processing doesn't happen, events 
just sit in a queue and not processed..

## Example: arithmetic expressions parser

The following example demonstrates ways of using the FSM4Java. The example computes values of expression consisting of integer literals, + - * / operations and brackets.
Source code can be found in tests section in package *org.blitvin.statemachine.expressionparser*. Code is rather simple IMHO, and hopefully self explaining.
I simplified the example by not treating error conditions irrelevant to the purpose of the example (i.e. demo of state machine usage) e.g. there is no treatment
of overflows, no treatment of floating point etc. Also in some cases I "cut corners" by making instance variables package visible instead of creating getters and 
setters, put related classes together etc. I did it for brevity sake. Code style certainly can be improved, I just wanted to demonstrate certain techniques , not 
to try an achieve ideal style :-). Now let's look into this example. 

 
The language is as following:

* expression ::= factor
* expression ::= factor + expression
* expression ::= factor - expression
* factor ::= literal
* factor ::= literal * factor
* factor ::= literal / factor
* factor ::= ( expression)
* factor ::= (expression) * factor
* factor ::= (expression) / factor
* literal ::= [-]?[0-9]+

Parsing is done by two FSMs, first one is TokenizerFSM and second one ExpressionTreeFSM. One can think of those as analog of flex and yacc.
TokenizerFSM scans input stream and produces syntax tokens ExpressionTreeFSM consumes. ExpressionTreeFSM builds expression tree ( as you can
guess by its name) and computes values of nodes on the fly. To make example simpler I made it not to store entire expression tree, but only
current path that is required to compute  the expression value.


Here is State machine diagrams

![TokenizerFSM](https://github.com/blitvin/fsm4java/blob/master/TokenizerFSM.jpg "Tokenizer")

![ExpressionTreeFSM](https://github.com/blitvin/fsm4java/blob/master/ExpressionTreeFSM.jpg "Expression tree parser")

Let's see what each FSM does:
Sometimes decomposition of task to several "simpler" state machines brings clearer code, like in this case.
In this case TokenizerFSM converts stream of chars to tokens which is easy to process in syntax parsing. Once
the token recognized it is sent to the second state machine for processing. Most "difficult" part for this FSM
is distinguish between '-' as part of negative literal and operation. It is achieved by introducing two states
one "minusIsToken" and second "minus can be start of literal". Note that "business logic" are the same for both
states, it is implemented  by *RegularState* class. The only difference is transitions of the states.
Another point I'd like to show w.r.t. TokenizerFSM is chaining "technique", note that you can create pipelines
of FSMs even though their alphabet (in other words event types sets) are different one from another.
The FSM is specified as follows:
```xml
<stateMachine name="tokenizerFSM" eventTypeClass="org.blitvin.statemachine.expressionparser.TokensEnum" 
				  class="org.blitvin.statemachine.expressionparser.TokenizerFSM">
		<state name="RegularState" isInitial="true" isFinal="true" class="org.blitvin.statemachine.expressionparser.RegularState">
			<transition event="CLOSING_BRACKET" toState="MinusIsToken"/>
			<transition event="DIGIT" toState="DigitState"/>
			<transition event="OTHER" toState="ErrorState"/>
			<transition event="MINUS" toState="MinusState"/>
			<other_events_transition toState="RegularState"/>
 		</state>
 		<state name="MinusState" isFinal="false" class="org.blitvin.statemachine.expressionparser.MinusState">
 			<transition event="DIGIT" toState="DigitState"/>
 			<transition event="OTHER" toState="ErrorState"/>
 			<transition event="MINUS" toState="MinusState"/>
 			<other_events_transition toState="RegularState"/>
 		</state>
 		<state name="MinusIsToken" isFinal="true" class="org.blitvin.statemachine.expressionparser.RegularState">
 			<transition event="DIGIT" toState="DigitState"/>
 			<transition event="OTHER" toState="ErrorState"/>
 			<transition event="CLOSING_BRACKET" toState="MinusIsToken"/>
 			<other_events_transition toState="RegularState"/>
 		</state>
 		<state name="DigitState" isFinal="true" class="org.blitvin.statemachine.expressionparser.DigitState">
 			<transition event="DIGIT" toState="DigitState"/>
 			<transition event="OTHER" toState="ErrorState"/>
 			<other_events_transition toState="MinusIsToken"/>
  		</state>
 		<state name="ErrorState" isFinal="true" class="org.blitvin.statemachine.expressionparser.ErrorState">
 			<other_events_transition toState=""/>
 		</state>
	</stateMachine>
```    

'Business logic' of the FSM resides on callbacks of classes that implement state, so to learn more just look into sources
of classes specified in *class* tags of the state. As you can see actually programming doesn't require too much lines of code. The FSM
builds expression tree. However, the tree is not stored , only relevant portion is held. The required part is path from root to currently
constructed node. When subtree value is computed it is returned to previous level, and there is no need to store any information on the 
subtree anymore. It is enough for the task, changes for storing entire tree is IMHO trivial, but it'd "pollute" example with code irrelevant
to FSM per se operation.

Now I'd like to point out some interesting things on how second FSM operates. Similarly to TokenizerFSM, "business logic" code 
 is in state classes callbacks. Probably it is a typical situation for FSM4Java usage. I suggest to put the logic to states callback 
and write custom transition classes only if you need non trivial transition mechanisms.

```xml
<stateMachine name="expressionTreeFSM" eventTypeClass="org.blitvin.statemachine.expressionparser.SyntaxTokensEnum"
				class="org.blitvin.statemachine.expressionparser.ExpressionTreeFSM">
		<state name="startOfExpression" isInitial="true" isFinal="false" class="org.blitvin.statemachine.expressionparser.StartingExpression">
			<transition event="LITERAL" toState="startOfFactor"/>
			<transition event="OPEN_BRACKET" toState="startOfFactor"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="startOfFactor" isFinal="false" class="org.blitvin.statemachine.expressionparser.StartingFactor">
			<transition event="LITERAL" toState="factor"/>
			<transition event="OPEN_BRACKET" toState="startOfExpression"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="multOrDiv" isFinal="false" class="org.blitvin.statemachine.expressionparser.MultDivState">
			<transition event="LITERAL" toState="factor"/>
			<transition event="OPEN_BRACKET" toState="startOfExpression"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="factor" isFinal="false" class="org.blitvin.statemachine.expressionparser.Factor">
			<transition event="MULTIPLY_DIVIDE" toState="multOrDiv"/>
			<transition event="ADD_SUBSTRACT" toState="expression"/>
			<transition event="CLOSING_BRACKET" toState="expression"/>
			<transition event="END_OF_INPUT" toState="expression"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="addOrSubst" isFinal="false" class="org.blitvin.statemachine.expressionparser.AddSubstState">
			<transition event="LITERAL" toState="startOfFactor"/>
			<transition event="OPEN_BRACKET" toState="startOfFactor"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="expression" isFinal="true" class="org.blitvin.statemachine.expressionparser.Expression">
			<transition event="LITERAL" toState="startOfFactor"/>
			<transition event="OPEN_BRACKET" toState="startOfFactor"/>
			<transition event="CLOSING_BRACKET" toState="factor"/>
			<transition event="ADD_SUBSTRACT" toState="addOrSubst"/>
			<other_events_transition toState="error"/>
		</state>
		<state name="error" isFinal="true" class="org.blitvin.statemachine.expressionparser.SyntaxError">
			<other_events_transition toState=""/>
		</state>
	</stateMachine>
``` 

ExpressionTreeFSM states use internal transitions, that is transition on events generated during execution 
of business logic (which is placed in stateBecomesCurrent callbacks). E.g. when FSM determines that new 
expression level detected (e.g. by encountering '(' token) the expression tree must be extended with new node.
New node must be added in case of new factor too. States startingExpression and startingFactor responsible
for this. But once they finished their part of work, additional operations are required : e.g. if factor starts
with literal, this literal must be processed. So event is consumed by state startFactor and new event triggers
transition to Factor state. Another example of internal transition is syntax error handling : if the callback
detects premature end of input new event of syntax error generated and processed. See Expression class for
details.

So, now hopefully you have enough information to use the library. Hope you enjoy incorporating it in your
projects. Please feel free to contact me for questions and suggestions on FSM4Java.

Boris
