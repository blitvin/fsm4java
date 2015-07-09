package org.blitvin.statemachine;

import org.blitvin.statemachine.annotated.AnnotatedStateMachineTest;
import org.blitvin.statemachine.concurrent.ThreadSafeMachineTest;
import org.blitvin.statemachine.expressionparser.ExpressionParserNegativeTests;
import org.blitvin.statemachine.expressionparser.ExpressionParserPositiveTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DomFactoryTest.class, StateMachineBuilderTest.class, AnnotatedStateMachineTest.class,
				ThreadSafeMachineTest.class, ExpressionParserNegativeTests.class, ExpressionParserPositiveTests.class })
public class StateMachineTests {

}
