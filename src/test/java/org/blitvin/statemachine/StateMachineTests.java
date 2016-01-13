package org.blitvin.statemachine;

import org.blitvin.statemachine.annotated.AnnotatedStateMachineTest;
import org.blitvin.statemachine.concurrent.ThreadSafeMachineTest;
import org.blitvin.statemachine.expressionparser.ExpressionParserNegativeTest;
import org.blitvin.statemachine.expressionparser.ExpressionParserPositiveTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DomFactoryTest.class, StateMachineBuilderTest.class, AnnotatedStateMachineTest.class,
				ThreadSafeMachineTest.class, ExpressionParserNegativeTest.class, ExpressionParserPositiveTest.class })
public class StateMachineTests {

}
