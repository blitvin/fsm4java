# Thread safe state machines: SynchronizedStateMachine and ConcurrentStateMachine

StateMachine created by *StateMachineBuilder* are not thread safe. In many cases
FSM is accessed from single thread, in which case thread safety is not an issue.
But sometimes FSM must or should operate in multi-threaded environment. If events
are coming from several threads, FSM must be thread-safe. Sometimes even if events
come from single thread, results of FSM execution are not required immediately,
and in such case one can benefit from asynchronous execution of FSM logic and
checking state/ retrieving results later. FSM4Java provides two concurrency handling classes.

*SynchronizedStateMachine* is simple wrapper that makes all relevant methods
synchronized. It operates in very similar manner to what SynchronizedCollection
does to collections.

*ConcurrentStateMachine* provide ways for asynchronous execution, speculative event
processing, thread safety and more. FSM logic performed in dedicated thread, and
events held in concurrent message queue before being executed. This ensures partial
order, that is events from each thread are processed in order they submitted by
*transit()*, *CAStransit()* or *asyncTransit()*.

Events are submitted via following methods:

* *transit(StateMachineEvent&lt;EventType&gt;)* when this method invoked, "regular"
behavior of single-threaded FSM emulated, that is calling thread is paused until
processing of the event passed as parameter is completed.
* *asyncTransit(StateMachineEvent&lt;EventType&gt; event)* this method submits the
event and immediately returns.
* *StampedState&lt;EventType&gt; transitAndGetResultingState(StateMachineEvent&lt;EventType&gt; event)*
works similar to transit, but returns current state after event processing finished.
The operation is atomic w.r.t. FSM operation, that is returned state is current state
of the FSM after transition is finished and before any other event is processed.
* *StampedState&lt;EventType&gt; CAStransit(StateMachineEvent&lt;EventType&gt; event, int generation)*
is atomic transit which either executed entirely or rolled back. The transition
happens if generation provided in argument is 0 ( enumeration of generations starts
from 1) or the same as current generation (time stamp) of FSM. generation can be
obtained by invoking *getCurrentStampedState* or from objects returned by *CAStransit*
or *transitAndGetResultingState*. Generation is increased at each processed event,
so if you invoke CAStransit with non-zero generation, event is processed only if
no other event processed between obtaining generation and the event processing started.
* *Future&lt;StampedState&lt;EventType&gt;&gt; asyncTransitAndGetFutureState(StateMachineEvent&lt;EventType&gt; event)* put event for asynchronous processing
and return Future object that provide resulting state when processing is done
 

Notes on other methods:
* *getCurrentStampedState()* returns current state wrapped to StampedState( that
is, with generation). However, in multithreaded environment it is quite possible
that generation is out-of-date by time caller get the object
* *completeInitializtion(...)* this method completes initialization of underlying
FSM and starts dedicated thread of the class. This method must be invoked before
events start coming.Without calling this method event processing doesn't happen,
events just sit in a queue and not processed.. One also can invoke *start()* method
to complete initialization, this one is logically similar to *start()* of Thread
class

TBD describe effect different modes of queuing - unlimited, limited that pauses
execution if queue is full, limited throwing exception if queue is full...
For now, please look javadoc of different constructors, this is described there

## FSMs that uses thread pools

*ConcurrentStateMachine* is good when there is a relatively few FSMs in the project.
Each instance of this class creates dedicated thread. In case of many concurrent
FSMs the system overhead is just too big... FSM4Java provides solution for this
use case : it allows execution of FSM related code in thread pool, so the same pool
used for many FSMs, thus drastically reducing per-thread overhead.

FSM4Java provides two classes that together provide this capability: *FSMThreadPoolFacade*
and *PooledStateMachineProxy*.

 *FSMThreadPoolFacade* is "mediator" between FSM and thread pool, it implements
all the behind-the-scene magic. However, one doesn't use it for interaction with
the state machine.  *PooledStateMachineProxy* is used for all the stuff like sending
new events. It implements *StateMachine* interface as well as *AsyncStateMachine*.
That means, it can be used as any other FSM ,e.g. its functionality can be augmented
with wrapper, e.g. one can use *ReconfigurableStateMachine* with object of this
class and reconfiguration works correctly on underlying FSM. Moreover, all additional
functionality of asynchronous execution and speculative event processing is supported
exactly as in *ConcurrentStataMachine*.


Let's see typical use case of thread pool attached FSM:

First of all one needs  thread pool and a queue. One should configure both according
to particular use case. See e.g. "Java concurrency in practice" for more information
on configuration/tuning of those..
Once one have those

```java
FSMThreadPoolFacade<TestEnum> pooled = new FSMThreadPoolFacade<>(myFSM,pool,
                new LinkedBlockingQueue<FSMQueueSubmittable>());

....

AsyncStateMachine<TestEnum> proxy = pooled.getProxy();

...

proxy.transit();
...
Future<StatmpedState> f = proxy.asyncTransit(..);

..
if (f.isDone()...) {
try {
 f.get();
}
catch(CancellationException ex){
            ...    
}
catch(ExecutionException ex) {
 .......              
}
}
```

Of cause one can attache different fsms for the same thread pool as well as
get multiple proxies for the same attached fsm. The implementation is thread
safe, so there is no problem sending events from different threads...
