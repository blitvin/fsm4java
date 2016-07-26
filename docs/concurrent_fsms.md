# Thread safe state machines: SynchronizedStateMachine and ConcurrentStateMachine
THIS DOCUMENT IS OUT OF DATE AND DESCRIBES 1.5 version of FSM4JAVA
IT WILL BE UPDATED TO v2.0 ASAP. Sorry for inconvenience. 


StateMachine created by *StateMachineBuilder* are not thread safe. In many cases FSM is accessed from single thread, in which case thread safety is not an issue.
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


## FSMPool
TBD thread pool for FSM