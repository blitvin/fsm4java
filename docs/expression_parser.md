#Example: arithmetic expressions parser

The following example demonstrates ways of using the FSM4Java. The example computes
values of expression consisting of integer literals, + - * / operations and brackets.
Source code can be found in tests section in package *org.blitvin.statemachine.expressionparser*.
Code is rather simple IMHO, and hopefully self explaining. I simplified the example
by not treating error conditions irrelevant to the purpose of the example (i.e.
demo of state machine usage) e.g. there is no treatment of overflows, no treatment
of floating point etc. Also in some cases I "cut corners" by making instance variables
package visible instead of creating getters and setters, put related classes together etc.
I did it for brevity sake. Code style certainly can be improved, I just wanted to
demonstrate certain techniques , not to try an achieve ideal style :-). This example
covers a lot of functionality of the library, so I use it for unitesting, you can
use *ExpressionParserNegativeTest* and *ExpressionParserPositiveTest* as entry point
of the example code.

Now let's look into this example. 

 
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

Parsing is done by two FSMs, first one is TokenizerFSM and second one ExpressionTreeFSM.
One can think of those as analog of flex and yacc. TokenizerFSM scans input
stream and produces syntax tokens ExpressionTreeFSM consumes. ExpressionTreeFSM
builds expression tree ( as you can guess by its name) and computes values of 
nodes on the fly. To make example simpler I made it not to store entire expression
tree, but only current path that is required to compute  the expression value.


Here is State machine diagrams

![TokenizerFSM](https://github.com/blitvin/fsm4java/blob/master/docs/TokenizerFSM.jpg "Tokenizer")

![ExpressionTreeFSM](https://github.com/blitvin/fsm4java/blob/master/docs/ExpressionTreeFSM.jpg "Expression tree parser")

Let's see what each FSM does:
Sometimes decomposition of task to several "simpler" state machines brings clearer code, like in this case.
In this case TokenizerFSM converts stream of chars to tokens which is easy to process in syntax parsing. Once
the token recognized, it is sent to the second state machine for processing. Most "difficult" part for this FSM
is distinguish between '-' as part of negative literal and arithmetic operation. It is achieved by introducing two states
one "minusIsToken" and second "minus can be start of literal". Note that "business logic" is the same for both
states. The only difference is transitions of the states.
Another point I'd like to show w.r.t. TokenizerFSM is chaining "technique". You can create pipelines
of FSMs even though their alphabet (in other words event types sets) are different one from another.
The FSM is specified as follows:

```xml
<stateMachines>
    <stateMachine name="tokenizerFSM" eventTypeClass="org.blitvin.statemachine.expressionparser.TokensEnum" type="SIMPLE">
        <state name="RegularState" isInitial="true" isFinal="true">
            <transition event="CLOSING_BRACKET" toState="MinusIsToken"/>
            <transition event="DIGIT" toState="DigitState"/>
            <transition event="OTHER" toState="ErrorState"/>
            <transition event="MINUS" toState="MinusState"/>
            <other_events_transition toState="RegularState"/>
        </state>
        <state name="MinusState" isFinal="false">
            <transition event="DIGIT" toState="DigitState"/>
            <transition event="OTHER" toState="ErrorState"/>
            <transition event="MINUS" toState="MinusState"/>
            <other_events_transition toState="RegularState"/>
        </state>
        <state name="MinusIsToken" isFinal="true">
            <transition event="DIGIT" toState="DigitState"/>
            <transition event="OTHER" toState="ErrorState"/>
            <transition event="CLOSING_BRACKET" toState="MinusIsToken"/>
            <other_events_transition toState="RegularState"/>
        </state>
        <state name="DigitState" isFinal="true">
            <transition event="DIGIT" toState="DigitState"/>
            <transition event="OTHER" toState="ErrorState"/>
            <other_events_transition toState="MinusIsToken"/>
        </state>
        <state name="ErrorState" isFinal="true">
            <other_events_transition type="NULL"/>
        </state>
    </stateMachine>
    <stateMachine name="syntaxFSM" eventTypeClass="org.blitvin.statemachine.expressionparser.SyntaxTokensEnum"
                      type="SIMPLE">
        <state name="startOfExpression" isInitial="true" isFinal="false">
            <transition event="LITERAL" toState="startOfFactor"/>
            <transition event="OPEN_BRACKET" toState="startOfFactor"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="startOfFactor" isFinal="false">
            <transition event="LITERAL" toState="factor"/>
            <transition event="OPEN_BRACKET" toState="startOfExpression"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="multOrDiv" isFinal="false">
            <transition event="LITERAL" toState="factor"/>
            <transition event="OPEN_BRACKET" toState="startOfExpression"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="factor" isFinal="false">
            <transition event="MULTIPLY_DIVIDE" toState="multOrDiv"/>
            <transition event="ADD_SUBSTRACT" toState="expression"/>
            <transition event="CLOSING_BRACKET" toState="expression"/>
            <transition event="END_OF_INPUT" toState="expression"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="addOrSubst" isFinal="false">
            <transition event="LITERAL" toState="startOfFactor"/>
            <transition event="OPEN_BRACKET" toState="startOfFactor"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="expression" isFinal="true">
            <transition event="LITERAL" toState="startOfFactor"/>
            <transition event="OPEN_BRACKET" toState="startOfFactor"/>
            <transition event="CLOSING_BRACKET" toState="factor"/>
            <transition event="ADD_SUBSTRACT" toState="addOrSubst"/>
            <other_events_transition toState="error"/>
        </state>
        <state name="error" isFinal="true">
            <other_events_transition type="NULL"/>
        </state>
    </stateMachine>
</stateMachines>

```

"Business logic" of the FSM resides on callbacks of classes that implement state.
In many cases only one or two callbacks are used, and code for states
of the FSM should be in the same place. One of helper classes of FSM4Java, *AnnotatedObjectStateFactory* allows
easy construction of FSM states using annotated methods. *ExpressionTree* class
contains callbacks for ExpressionTreeFSM and *Tokenizer* contains logic of TokenizerFSM.
As you can see actually programming doesn't require too much lines of code. The ExpressionTreeFSM
builds expression tree. Note that the tree is not actually stored. FSM holds only portion required for computation of values representing by subtrees  from root to currently consturcted node. In other words, 
the required part is path from root to currently
constructed node. When subtree value is computed it is returned to previous level, and there is no need to store any information on the 
subtree anymore. It is enough for the task, changes for storing entire tree is IMHO trivial, but it'd "pollute" example with code irrelevant
to FSM operation.

Now I'd like to point out some interesting things on how second FSM operates. Similarly to TokenizerFSM, "business logic" code 
is in state classes callbacks. Probably it is a typical situation for FSM4Java usage. 
ExpressionTreeFSM states use internal transitions, that is transition on events generated during execution 
of business logic (placed in stateBecomesCurrent callbacks). E.g. when FSM determines that new 
expression level detected (e.g. by encountering '(' token) the expression tree must be extended with new node.
New node must be added in case of new factor too. States startingExpression and startingFactor responsible
for this. But once they finished their part of work, additional operations are required : e.g. if factor starts
with literal, this literal must be processed. So event is consumed by state startFactor and new event triggers
transition to Factor state. Another example of internal transition is syntax error handling, e.g. if the callback
detects premature end of input new event of syntax error generated and processed. See "handleExpression()" for
details.

So, this example shows how to:
- construct FSM from XML file
- use annotated class to provide callbacks for FSM states
- use chaining techniques, when transitions in one FSM generate events to another
- provide result to outside world using fields of class defining callback. This
won't work if callbacks defined in multipile files. Use FSM properties (*setProperty()*
and *getProperty()* in interface StateMachine and FSMStateView) to send data in and out
of FSM
