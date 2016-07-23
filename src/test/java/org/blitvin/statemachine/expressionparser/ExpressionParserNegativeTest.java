/*
 * (C) Copyright Boris Litvin 2014 - 2016
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FSM4Java is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine.expressionparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.DOMStateMachineFactory;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.InvalidFactoryImplementation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionParserNegativeTest {

    static DOMStateMachineFactory factory;

    @BeforeClass
    public static void createFactory() {
        try {
            factory = new DOMStateMachineFactory("expressionparser.xml");

        } catch (InvalidFactoryImplementation e) {
            factory = null;
        }

    }

    @Parameters
    public static Collection expressions() {
        return Arrays.asList(new Object[][]{
            {"34 +&^ 78 *5", ExpressionParserException.UNEXPECTED_SYMBOL, 4},/* unexpected symbol */
            {"11 *", ExpressionParserException.END_OF_INPUT, 4},/*unexpected end of expression */
            {"", ExpressionParserException.END_OF_INPUT, 0},/* empty input */
            {"11+5 * ( 5*3 +(17 - 3*4)", ExpressionParserException.END_OF_INPUT, 24},/* ) missing */
            {"(5 +3))", ExpressionParserException.UNEXPECTED_TOKEN, 6},/* unexpected ) */
            {"8*7 - - -5", ExpressionParserException.UNEXPECTED_TOKEN, 6},/* unexpected - */
            {"89 + 8 9", ExpressionParserException.UNEXPECTED_TOKEN, 7},/* unexpected 9 */
            {" 7/( 5 -5)", ExpressionParserException.DIVISION_BY_0, 9}, /* division by 0 */});
    }
    int position;
    int errorCode;

    ExpressionParser parser;

    public ExpressionParserNegativeTest(String expression, Integer errorCode, Integer position) throws BadStateMachineSpecification, InvalidFactoryImplementation {
        parser = new ExpressionParser(expression);
        this.position = position;
        this.errorCode = errorCode;
    }

    @Test
    public void runNegativeTest() throws BadStateMachineSpecification, InvalidEventException, IOException {
        try {
            parser.compute();
            fail("test supposed to throw ExpressionParserException");
        } catch (ExpressionParserException e) {
            assertEquals("Wrong error code ", errorCode, e.errorCode);
            assertEquals("Wrong position", position, e.position);
        }
    }

}