# Creation of state machine

There are two ways to build FSM with FSM4Java: programmaticaly and using declarative programming.

## Programmatic creation of FSM: class StateMachineBuilder

*StateMachineBuilder*, as one can guess, implements the builder pattern for creation of FSM objects.
Design of the library hides internal details of FSM implementation - it is completely decoupled from business
logic objects. FSM state machine don't have public constructors, creation of any state machine supposed to be
through StateMachineBuilder. Configuration and annotation driven creation of FSM  described in below section
in the end uses  the builder too.

StateMachineBuilder is parameterized with event type (that is, alphabet) of the state machines to be created.
E.g. creating the builder looks like

```java
StateMachineBuilder<EVENTS_TYPE> builder = new StateMachineBuilder<>(type, EVENTS_TYPE.class)
```

First argument is desired FSM type, which defines which capabilities are enabled, currently supported following types:
- BASIC - FSM without support of internal event
- SIMPLE - FSM with support of single internal event at given time i.e. current state can yield one internal event.
- MULTI_INTERNAL_EVENTS - FSM with support of emitting multiple event during processing of "original" transition
- ASPECT - Simple FSM with support of ASPECT. See discussion in [Using FSM4Java in your projects](api.md)

Second parameter is required because of erasure - builder must have an access to literals of event type...

Once builder object created, one can start supply details of the state machine, that is states, transitions
between states and parameters for state and transition creation. Calls can be chained e.g. typical snippet
for creation of state object with all its transitions  can look like

```java
		builder.addState("state1").markStateAsInitial()
                        .addProperty("class", org.blitvin.statemachine.buildertest.BuilderTestState.class)
			.addTransition(STM_EVENTS.STM_A).addProperty("toState", "state2");
		builder.addTransition(STM_EVENTS.STM_B).addProperty("toState", "state3")
			.addState("state2")
			.addTransition(STM_EVENTS.STM_A, "state3")
			.addDefaultTransition().addProperty("toState", "state2");
		
		BuilderTestState<STM_EVENTS> state3 = new BuilderTestState<>();
		
		builder.addState("state3",state3).addProperty("myAttribute", "isSet")
			.addTransition(STM_EVENTS.STM_A).addProperty("toState", "state1")
			.addTransition(STM_EVENTS.STM_B,StateMachineBuilder.TRANSITION_TYPE.NULL)
			.addTransition(STM_EVENTS.STM_C).addProperty("toState", "state2").markStateAsFinal();
		
                HashMap<Object,Object> fsmInit = new HashMap<>();
                fsmInit.put(StateMachineBuilder.STATE_FACTORY_IN_GLOBAL_PROPERTIES, new BuilderTestStateFactory<>());
		StateMachine<STM_EVENTS> machine = builder.addFSMProperties(fsmInit).build();

```

In this example, we create new state named "state1", and then make it initial state 
(that is current state that is current before events start to come in).
Next chained call defines a property named "class". I explain this particular
property in section describing ways to set state business objects below, but for
now let's just say, that one can define map of properties. This map is passed to
states, transitions etc. for proper configuration. For state object the map is
passed as 'initializer' parameter of *onStateAttachedToFSM()* method(see discussion
on obtaining StateMachine). FSM properties is such a map defining FSM wide
properties.Note that those properties' purpose is proper initialization, and they
are not available after FSM is fully constructed.

After one provides an information relevant to state itself, it is time to define
transitions from newly constucted state. This is done by invoking *addTransition()*.
There are several overloaded versions of this method. The one used here creates
"simple" transition that just transits to state specified by property "toState".
There is a shortcut, two methods invocation can be replaced by single e.g. 

```java
addTransition(STM_EVENTS.STM_A, "state3")
```

specifies simple transition triggered by event with type "STM_A" and with target
state "state3".

In a way similar to (*) transition in computer science finite state machine,
FSM4Java supports "catch-all-others" transition. That is, if a transition defined
for particular event type, this transition is used to determine which state becomes
"current". If no such explicit transition defined, default transition like
```java
.addDefaultTransition().addProperty("toState", "state2");
```
is used. If default transition is not defined, event is invalid (*InvalidEvent*
exception is thrown by *transit()*)

### Types of transitions

Following types of transition are defined by FSM4Java

- BASIC - this is "simple" transition with pre-defined statically set target state.
Examples above describe such transitions. It is default transition type. So if no
type is specified transitions of this type are constructed
- NULL - this transition is defined if event of particular type should be "swallowed".
See discussion in [Using FSM4Java in your projects](api.md)
- CUSTOMIZED - this transition type allows dynamic behavior in chosing next state.
Business object of state defining such transition must implement
*CustomizedTransitionsLogicState* interface. When this transaction is chosen by
FSM (according to proceeded event), transition calls *stateToTransitTo()* in state
object, and perform transit to state identified by returned name.

### Specifying state object (business logic) for state

FSM can be visualized as graph with states as nodes, and transitions as edges 
(well this doesn't work for NULL and customized transition, but let's put this
aside for a moment). Each node has reference to business logic object
(*org.blitvin.statemachine.State*), which does actual work. One can provide such
object in the following ways

- specify it explicitely in *addState(String name, State obj). state3 in code 
snippet above is defined in such a way. Note that if the same builder is used for
creation of several FSMs, all FSMs share this object
- specify property "class" to state as done in definition of 'state1'. In this
case default constructor of the class specified in the property invoked to get
state object
- state object can be retrieved from FSM property with name "<state name>BusinessObject"
- the object can be obtained from states factory (*org.blitvin.statemachine.FSMStateFactory*).
The state factory should be placed in property with name "stateFactory". Builder
invokes method *get()* with parameter name of the state to obtain object for this
state. If factory does not provide state object for particular state, it should return null.
- the same as above, but state factory is defeined in FSM properties with name "globalFactory"

The object retrieval works as follows - if object is explicitely specified, this
object is used. If state factory exists in properties of the node, the factory is
consulted. If object is not obtained, global property "<state name>BusinessObject"
is checked. If object is not there, global state factory is checked. If still no luck,
object is constructed using default constructor of class specified in "class"
property of the state.

Note that Builder specifies build() methods with override flag. If this flag is 
true, than factories and FSM properties object take precedence.

### Obtaining StateMachine 

After all parameter are specified, one can invoke *build*  method of the Builder,
which actually constructs state machine object. build() checks validity of specification
and actually creates state machine object.

```java
	HashMap<Object,Object> fsmInit = new HashMap<>();
        fsmInit.put(StateMachineBuilder.STATE_FACTORY_IN_GLOBAL_PROPERTIES, new BuilderTestStateFactory<>());
	StateMachine<STM_EVENTS> machine = builder.addFSMProperties(fsmInit).build();
```

An example of invalid specification is creating transition without target state property 

```java
	.addTransition(STM_EVENTS.STM_A).addTransition(STM_EVENTS.STM_B)....
```
yields an error because it is impossible to know where to transit to if STM_A 
event arrives...

If everything is OK and there is no problem detected, *build()* constructs actual
object, creates all required internal objects obtains state business objects and
finalizes initialization. E.g. for BASIC transitions target state reference computed
according to "toState" property. For state objects, builder invokes *onStateAttachedToFSM()*
life cycle callback with properties map and reference to state view of the FSM.
Note that it is possible to construct several instances of state machine according
to "specifications" supplied to Builder by just invoking *build()* several times.
This is exactly what happens if the builder invoked from a state machine factory.
Also, there are *build()* method for creating state machine with new "topology"
of state nodes and transitions, but same state objects, see javadoc of StateMachineBuilder
for more information.

## Declarative programming: creating FSM from XML configuration

FSM4Java allows creation of state machine corresponding to provided xml specification.
Class *DOMStateMachineFactory* handles all the details. You can either explicitely
specify location of XML file, or use argument-less constructor,which uses value
specified by system property *org.blitvin.statemachine.DOMStateMachineFactoryImplementation.defaultXmlFileName*.
FSM object can be obtained by invocation of *getStateMachine(String name)* where
name is name of the state machine specification in the XML file.

```java
DOMStateMachineFactory factory = ....;
....
SimpleStateMachine<TestEnum> machine =(SimpleStateMachine<TestEnum>)factory.getStateMachine("myFSM");
```

*DOMStateMachineFactory* also provides default factory object that can be obtained
by invocation static method *DOMStateMachineFactory.getDefaultFactory()*.
*getStateMachine()* always creates new object, so one can obtain multiple instances
corresponding to the same FSM spec (in our example myFSM) and use them independently
one from another.

Now let's see what the XML file looks like.  Below is a little example.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<stateMachines>
    <stateMachine name="myFSM" eventTypeClass="org.blitvin.statemachine.domfactorytest.TestEnum" type="SIMPLE">
        <state name="state1" isInitial="true" class="org.blitvin.statemachine.domfactorytest.TestState" stateSpecificProperty="value">
            <transition event="enum1" toState="state2"/>
            <transition event="enum2" toState="state3"/>
            <other_events_transition toState="state1"/>
        </state>
        <state name="state2" isFinal="false" class="org.blitvin.statemachine.domfactorytest.TestState">
            <transition event="enum1" toState="state1" />
            <transition event="enum2" type="NULL" />
            <other_events_transition toState="state3"/>
        </state>
        <state name="state3" isFinal="true" class="org.blitvin.statemachine.domfactorytest.TestState">
            <other_events_transition toState="state2"/>
        </state>
    </stateMachine>
</stateMachines>
```

The file must contain node *stateMachines* which includes list of nodes *stateMachine*.
Each such a node defines specification of an FSM. Let's go over the stateMachine
tage in the example.

* *name* attribute provides name of the FSM. This is a name to be supplied to 
 *getStateMachine*, this is how the factory knows which FSM to construct. 
 This attribute is mandatory.
* *eventTypeClass* is another mandatory attribute specifying class name of the
 Alphabet (a.k.a. type of events consumed by the state machine)
* *type* is optional attribute specifying type of the state machine. 

*stateMachine* node contains list of *state* nodes. Those nodes have the following attributes:

* *name* the state's name. It can be used to allocate appropriate state. The name
 must be unique in the list of states of the FSM.
* *isFinal* boolean property that marks the state as final
* *isInitial* boolean property that marks the state is initial i.e. it is a state
 of FSM before first event arrives. Exactly one state must be marked as initial
* *class* is optional attribute defining the class of the state object.

The state contains list of transitions and optionally *other_events_transition*
node representing default (*) transition. Transition has the following attributes:

* *event* event on which the transition is invoked
* *type* optional defining transition type. If omitted, BASIC transition is assumed

*other_events_transition* can contain *type* , but not *event* attribute.

Nodes of "statesMachine", like state, transition can define additional attributes.
When new FSM constructed, those additional attributes are passed to builder as
properties of the entity (e.g. see toState attribute for transitions).

The file format is defined by *state_machines.xsd* provided with the library.

## Declarative programming: Annotation driven FSM creation

FSM4Java provides two ways of creating FSM from specification provided in
annotations - factory and stand alone classes. 
Annotation factory class is *org.blitvin.statemachine.annotated.AnnotationStateMachineFactory*.
Annotation used by factory , *@StateMachines* describes list of state machine
specifications. State machine can be obtained from the factory by providing
the FSM specification name, similar to *DOMStateMachineFactory* operation.

E.g.

```java
@StateMachines({
    @StateMachineSpec(name = "MyStateMachine", type=BASIC,
            eventTypeClass = TestEnum.class,
            states = {
                @StateSpec(name = "state1", isFinal = false, isInitial = true,
                        implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state2")}),
                            @TransitionSpec(event = "B", params = {
                                @Param(name = "toState", value = "state3")})}
                ),
                @StateSpec(name = "state2",implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state3")}),
                            @TransitionSpec(isDefaultTransition = true, params = {
                                @Param(name = "toState", value = "state2")})
                        }
                ),
                @StateSpec(name = "state3", isFinal = true,implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {
                                @Param(name = "toState", value = "state1")}),
                            @TransitionSpec(event = "B", params = {
                                @Param(name = "toState", value = "state1")}),
                            @TransitionSpec(event = "C", params = {
                                @Param(name = "toState", value = "state2")})
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


FSM4java doesn't require usage of factory. It provides a way to put annotation in
the FSM defining class itself. There are two possible use cases: one is FSM is
distinct class, and second one is FSM is member of some other class. Actually there
is a third possible option - FSM is local variable,
but unfortunately, due to bug in JVM specification, annotations of local variables
are not stored in class files and therefore not available at runtime.
There is a JSR redefining annotation in progress, so hopefully at some point in
the future this last use case will be supported.

Let's see how FSM class can be constructed using specification in annotations.
The class must be subclassed from *org.blitvin.statemachine.annotated.AnnotatedStateMachine*.
In order to fully define FSM The only missing part is states specification as class
itself and event type enum are provided in code. The annotated class looks like

```java
@StateMachineSpec(eventTypeClass = TestEnum.class, name = "myStateMachine", type = BASIC,
            states = {
                @StateSpec(name = "state1", isFinal = false, isInitial = true, implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {@Param(name = "toState", value = "state2")}),
                            @TransitionSpec(event = "B", params = {@Param(name="toState", value="state3")})
                        }),
                @StateSpec(name="state2", implClass = TestState.class, transitions = {
                    @TransitionSpec(event= "A", params={@Param(name="toState", value="state3")}),
                    @TransitionSpec(isDefaultTransition = true, params={@Param(name = "toState", value="state2")})
                    }),
                @StateSpec(name="state3",isFinal=true, implClass = TestState.class, transitions= {
                    @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
                    @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})
                })
            }
    )
    public AnnotatedStateMachine<TestEnum> machine;
```

If FSM is member of the class (second use case ) the definition looks like

```java
public class ClassWithFSMMember {
    ....
@StateMachineSpec(eventTypeClass = TestEnum.class, name = "myStateMachine", type = BASIC,
            states = {
                @StateSpec(name = "state1", isFinal = false, isInitial = true, implClass = TestState.class,
                        transitions = {
                            @TransitionSpec(event = "A", params = {@Param(name = "toState", value = "state2")}),
                            @TransitionSpec(event = "B", params = {@Param(name="toState", value="state3")})
                        }),
                @StateSpec(name="state2", implClass = TestState.class, transitions = {
                    @TransitionSpec(event= "A", params={@Param(name="toState", value="state3")}),
                    @TransitionSpec(isDefaultTransition = true, params={@Param(name = "toState", value="state2")})
                    }),
                @StateSpec(name="state3",isFinal=true, implClass = TestState.class, transitions= {
                    @TransitionSpec(event="B",params={@Param(name="toState",value="state1")}),
                    @TransitionSpec(event="C",params={@Param(name="toState",value="state2")})
                })
            }
    )
    public AnnotatedStateMachine<TestEnum> machine;
	...
```

The difference is only in placement of the annotation.

*AnnotatedStateMachine* provides several constructors for creation of FSM, for
situation of explicit specification of annotation placement and for "annotation 
autowiring", that is runtime search for appropriate annotation.


Autowiring is based on searching stack for appropriate annotation. In first use
case, the constructor is called with super(), in the second use case the constructor
is called when member is initialized (typically in constructor of containing class).
In any case class containing annotation is in one of stack frames. So constructor
*AnnotatedStateMachine(enumClass)* goes through the stack and
searches class that is assignable from *this.getClass()*, and looks for class
annotation @States.

*AnnotatedStateMachine(Class&lt;? extends Enum&lt;EventType&gt;&gt; enumClass, String stateMachineName)*
looks for class annotation and goes through declared field of each class
for correct member. Match is found if member is of correct class and name field
of @States annotation matches *stateMachineName*. The name matching allows
disambiguation in case of several possible candidates.

## TBD taking best from both declarative and programatic worlds - groovy builder
groovy builder is not yet ready, this section will be filled upon its completion
