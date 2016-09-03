#FSM4Java

FSM4Java is a library that allows creation and usage of deterministic finite state machine ( aka finite state automata aka state machine aka FSM).
More details on what FSM is can be found e.g. on [http://en.wikipedia.org/wiki/Finite-state_machine](wikipedia).
Intended users of the library are developers of projects that can benefit from usage of State machine / State pattern e.g. arranging business rules, document
life cycle management etc. but consider solutions like jbpm too "heavy". The library design to allow developer incorporate large set of features provided by FSM4Java
to the projects without requiring significant time to study technicalities. It provides sensible defaults and readily available solutions for common use cases. It also
provide some utility classes for development assistance. Yet, for those need more configuration, FSM4Java provides ways to modify most of configuration settings.

FSM4Java provides (among other things) the following features:
- creation of FSM programmatically using builder ( TBD groovy builder support is planned to be added. It will simplify FSM description syntax)
- FSM creation via declarative programming using XML definitions file and annotations
- augmentation of FSM by Wrappers mechanism
- thread safe FSM. Implementation hides all thread related details and one can use such a thread safe
FSM transparently - it implements the same interface as "regular" FSM and ensures thread safety behind the scene. Such concurrent FSMs also support asyncroneous
and speculative processing of events, guarantee of event ordering of those sent from the same thread and  more..
- thread pool attachable FSM. Such an FSM executes it's business logic as tasks submitted to the thread pool. Multiple FSMs can be attached to the same thread
pool, hence reducing overall resource consumption. Those attachable FSMs support the same functionality as the above concurrent state machines (asyncroneous,
speculative execution support of Future<> etc.)
- aspect enabled FSM
- on-the-fly reconfiguration of FSM
- (TBD, will be added later) transaction aware FSM i.e. one can make FSM to partisipate in transaction including commit and rollback to FSM state existed at 
transaction beginning.

FSM4Java designed to be easy to use. Most implementation details are hidden from the library user, with clearly defined interfaces betwen FSM and outside world 
and business logic logic code and rest of FSM. I tried to make business logic code as much decoupled from internal details of FSM as possible.
Convention-over-configuration principle is used as wide as possible in the library. In most use cases, FSMs can be used out-of-box with minimal efforts.  Also,
utility classes are provided to help with typical tasks.Yet, the library allows configuring a lot of paramers in case defaults are not suited to an use case. 

The FSM4Java's state machine have low overhead, e.g. for several years old laptop ( 4th generation i7 with 4GB memory) basic ( not thread safe)
FSM processed one billion event transitions in ~9.5 seconds. For concurrent implementation throughput of async event processing varies around 4.5 -5  million
transitions per second on 20 concurrent producers. For multiple FSMs test show for 20 FSMS and 200 producers overal throughput is about 8 million transitions
per second. Synchroneous processing ( that is resulting state info sent back to calling thread) requires a little more processing
and throughput is around 500000-600000 transitions per second. To run tests on your hardware please see files in tests org.blitvin.fsm4java.performancetests.
Those are not tests per se, but stand alone apps measuring performance in different scenarios..


Please look into documentation directory of the repository to get more info:

- [Using FSM4Java in your projects](../master/docs/api.md)
- [Creating FSM](../master/docs/creating_fsm.md)
- [ConcurrentFSMs](../master/docs/concurrent_fsms.md)
- [Extending FSM functionality with wrappers](../master/docs/wrapper.md)
- [Complete example](../master/docs/expression_parser.md): arithmetic expression parser using state machine

