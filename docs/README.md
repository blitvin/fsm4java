#FSM4Java

FSM4Java is a library that allows creation and usage of deterministic finite state machine ( aka finite state automata aka state machine aka FSM).
More details on what FSM is can be found e.g. on [http://en.wikipedia.org/wiki/Finite-state_machine](wikipedia). 

FSM4Java provides (among other things) the following features:
- creation of FSM programmatically using builder ( TBD groovy builder support is planned to be added. It will simplify FSM description syntax)
- FSM creation via declarative programming using XML definitions file and annotations
- augmentation of FSM by Wrappers mechanism
- thread safe FSM. Implementation hides all thread related details and one can use such a thread safe
FSM transparently - it implements the same interface as "regular" FSM and ensures thread safety behind the scene. Such concurrent FSMs support asyncroneous
and speculative processing of events, guarantee of event ordering of those sent from the same thread and  more..
(TBD include specialized thread pool - FSM pool)
- aspect enabled FSM
- on-the-fly reconfiguration of FSM
- (TBD, will be added later) transaction aware FSM i.e. one can make FSM to partisipate in transaction including commit and rollback to FSM state of
transaction beginning. JEE resource template is provided.

The FSM4Java's state machine have low overhead, e.g. for several years old laptop ( 4th generation i7 with 4GB memory) basic ( not thread safe)
FSM processed one billion event transitions in ~9.5 seconds. For concurrent implementation throughput of async event processing varies around 4.5 -5  million
transitions per second on 20 concurrent producers. For multiple FSMs test show for 20 FSMS and 200 producers overal throughput is about 8 million transitions
per second. Synchroneous processing ( that is resulting state info sent back to colling thread) requires a little more processing
and throughput is around 500000-600000 transitions per second. To run tests on your hardware please see files in tests org.blitvin.fsm4java.performancetests.
Those are not tests per se, but stand alone apps measuring performance in different scenarios..


Please look into documentation directory of the repository to get more info:
[Using FSM4Java in your projects](../blob/master/docs/api.md) in your project

[Creating FSM](../blob/master/docs/creating_fsm.md)

[ConcurrentFSMs](../blob/master/docs/concurrent_fsms.md)

[Extending FSM with wrappers](../blob/master/docs/wrapper.md)

[Complete example](../blob/master/docs/expression.md) expression parser using state machine
