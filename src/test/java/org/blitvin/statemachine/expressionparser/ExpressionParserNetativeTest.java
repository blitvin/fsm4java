package org.blitvin.statemachine.expressionparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
public class ExpressionParserNegativeTests {

	
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
				{"34 +&^ 78 *5",ExpressionParserException.UNEXPECTED_SYMBOL,4},/* unexpected symbol */
				{"11 *",ExpressionParserException.END_OF_INPUT,4},/*unexpected end of expression */
				{"",ExpressionParserException.END_OF_INPUT,0},/* empty input */
				{"11+5 * ( 5*3 +(17 - 3*4)",ExpressionParserException.END_OF_INPUT,24},/* ) missing */
				{"(5 +3))",ExpressionParserException.UNEXPECTED_TOKEN, 6},/* unexpected ) */
				{"8*7 - - -5",ExpressionParserException.UNEXPECTED_TOKEN, 6},/* unexpected - */
				{"89 + 8 9",ExpressionParserException.UNEXPECTED_TOKEN,7},/* unexpected 9 */
				{" 7/( 5 -5)",ExpressionParserException.DIVISION_BY_0, 9}, /* division by 0 */
		});
	}
	int position;
	int errorCode;
	TokenizerFSM fsm;
	ExpressionTreeFSM expressionTreeFSM;
	StringReader reader;
	public ExpressionParserNegativeTests(String expression, Integer errorCode, Integer position) {
		reader = new StringReader(expression);
		this.position = position.intValue();
		this.errorCode = errorCode.intValue();
	}

	@Test
	public void runNegativeTest() throws BadStateMachineSpecification, InvalidEventType, IOException{
		fsm = (TokenizerFSM) factory.getStateMachine("tokenizerFSM", reader);
		expressionTreeFSM = (ExpressionTreeFSM) factory.getStateMachine("expressionTreeFSM");
		fsm.setStateMachine(expressionTreeFSM);
		try {
			fsm.run();
			fail("test supposed to throw ExpressionParserException");
		}
		catch (ExpressionParserException e) {
			assertEquals("Wrong error code ",errorCode,e.errorCode);
			assertEquals("Wrong position",position, e.position);
		}
	}

}
