# Creation of state machine
THIS DOCUMENT IS OUT OF DATE AND DESCRIBES 1.5 version of FSM4JAVA
IT WILL BE UPDATED TO v2.0 ASAP. Sorry for inconvenience. 

There are two ways to build FSM with FSM4Java: programmaticaly and using declarative programming.

## Programmatic creation of FSM: class StateMachineBuilder

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

## Declarative programming: creating FSM from XML configuration

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

## Declarative programming: Annotation driven FSM creation

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

## TBD taking best from both declarative and programatic worlds - groovy builder
groovy builder is not yet ready, this section will be filled upon its completion