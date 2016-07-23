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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.InvalidFactoryImplementation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionParserPositiveTest {

    @Parameters
    public static Collection expressions() {
        return Arrays.asList(new Object[][]{
            {"2 + 3 * 4", 14}, /* correctness of priority * over + */
            {"4+5*(7-8)", -1}, /* check of () */
            {"(2*3*4*5)", 120}, /* expression->factor->expression->factor */
            {"((((4+3)*2 +(5+6)*2)*3 - -1) / 5 +18)-4", 35},/* inclosed () */
            {"-128", -128}, /*single terminal */
            {"-5 * -6 -7", 23},/*correctness of - parsing after digit*/
            {"(3+4)-5", 2},/* correctness of - parsing after )*/
            {"7+ 0/ (4 - 6)", 7},/* zero division */
            {"8-5", 3},/* minus as token */
            {"9 -(2+3)*2", -1},/*minus before other token */});
    }
    int expectedResult;
    ExpressionParser parser;

    public ExpressionParserPositiveTest(String expression, Integer expectedValue) throws BadStateMachineSpecification, InvalidFactoryImplementation {
        parser = new ExpressionParser(expression);
        expectedResult = expectedValue;

    }

    @Test
    public void runPositiveTest() throws BadStateMachineSpecification, InvalidEventException, IOException {
        assertEquals(expectedResult, parser.compute());
    }
    
}