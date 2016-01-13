package org.blitvin.statemachine.expressionparser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.DOMStateMachineFactory;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.InvalidFactoryImplementation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionParserPositiveTest {

	static DOMStateMachineFactory factory;
	
	@BeforeClass
	public static void createFactory(){
		try {
			factory = new DOMStateMachineFactory("expressionparser.xml");
			
		} catch (InvalidFactoryImplementation e) {
			factory = null;
		}
		
	}
	
	@Parameters
	public static Collection expressions() {
		return Arrays.asList(new Object[][] {
				{"2 + 3 * 4", 14}, /* correctness of priority * over + */
				{"4+5*(7-8)", -1}, /* check of () */
				{"(2*3*4*5)",120}, /* expression->factor->expression->factor */
				{"((((4+3)*2 +(5+6)*2)*3 - -1) / 5 +18)-4", 35},/* inclosed () */
				{"-128",-128}, /*single terminal */
				{"-5 * -6 -7",23},/*correctness of - parsing after digit*/
				{"(3+4)-5",2 },/* correctness of - parsing after )*/
				{"7+ 0/ (4 - 6)", 7 },/* zero division */
				{"8-5",3},/* minus as token */
				{"9 -(2+3)*2",-1},/*minus before other token */
		});
	}
	int expectedResult;
	TokenizerFSM fsm;
	ExpressionTreeFSM expressionTreeFSM;
	StringReader reader;
	
	public ExpressionParserPositiveTest(String expression, Integer expectedValue)  {
		expectedResult = expectedValue.intValue();
		reader = new StringReader(expression);
		

	}
	
	@Test
	public void runPositiveTest() throws BadStateMachineSpecification, InvalidEventType, IOException{
		fsm = (TokenizerFSM) factory.getStateMachine("tokenizerFSM", reader);
		expressionTreeFSM = (ExpressionTreeFSM) factory.getStateMachine("expressionTreeFSM");
		fsm.setStateMachine(expressionTreeFSM);
		fsm.run();
		assertEquals(expectedResult,expressionTreeFSM.value());
	}

}
