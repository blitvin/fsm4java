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

import java.util.Map;
import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.FSMStateView;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.utils.onStateBecomesCurrent;
import org.blitvin.statemachine.utils.onStateIsNoLongerCurrent;
import org.blitvin.statemachine.utils.onStateMachineInitialized;

/**
 * Tokenizer defines callbacks for TokenizerFSM state machine for converting
 * characters stream into syntax tokens used by syntaxFSM. The callbacks initiate
 * transitions of syntaxFSM once tokens available
 * @author blitvin
 */
public class Tokenizer {

    private StateMachine<SyntaxTokensEnum> expressionTreeFSM = null;

    private State<TokensEnum> digitState = null;
    private int minusPosition = -1;

    int absValue = 0;
    boolean isPositive = false;
    int startPosition = 0;
    State<TokensEnum> minusState = null;

    public Tokenizer(StateMachine<SyntaxTokensEnum> expressionTreeFSM) {
        this.expressionTreeFSM = expressionTreeFSM;
    }

    @onStateBecomesCurrent(state = ExpressionParser.DIGIT_STATE)
    public void setLiteralValue(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState) {
        if (prevState != digitState) { // start of number literal
            ExpressionParser.Token curToken = (ExpressionParser.Token) theEvent;
            absValue = curToken.value() - '0';
            startPosition = curToken.getPosition();
            if (prevState == minusState) {// control came from minus - the literal is negative, 
                // also don't bother to track down -0 as error
                isPositive = false;
                startPosition--; // start position of the literal is at minus char
            } else {
                isPositive = true;
            }
        } else { // continuation of number literal
            absValue = absValue * 10 + (((ExpressionParser.Token) theEvent).value() - '0');
        }
    }

    @onStateIsNoLongerCurrent(state = ExpressionParser.DIGIT_STATE)
    public void processCompleteLiteral(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> nextState) {
        if (nextState != digitState) { //end of number literal
            if (theEvent.getEventType() == TokensEnum.OTHER) {
                return;
            }

            try {
                expressionTreeFSM.transit(new SyntaxToken.LiteralToken(startPosition, isPositive ? absValue : -absValue));
            } catch (InvalidEventException e) {
                throw new ExpressionParserException("got unexpected exception", e,
                        ExpressionParserException.INTERNAL_ERROR, ((ExpressionParser.Token) theEvent).getPosition());
            }
        }
    }

    @onStateMachineInitialized(state = ExpressionParser.DIGIT_STATE)
    public void storeMinusStateReference(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
        minusState = containingMachine.getStateByName(ExpressionParser.MINUS_STATE);
        if (minusState == null) {
            throw new BadStateMachineSpecification("Cannot find state " + ExpressionParser.MINUS_STATE);
        }
    }

    @onStateBecomesCurrent(state = "ErrorState")
    public void handleError(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState) {
        ExpressionParser.Token errToken = (ExpressionParser.Token) theEvent;
        throw new ExpressionParserException("Unexpected symbol " + errToken.value(),
                ExpressionParserException.UNEXPECTED_SYMBOL, errToken.getPosition());
    }

    @onStateBecomesCurrent(state = ExpressionParser.MINUS_STATE)
    public void gotMinusLiteral(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState) {
        minusPosition = ((ExpressionParser.Token) theEvent).getPosition();
    }

    @onStateIsNoLongerCurrent(state = ExpressionParser.MINUS_STATE)
    public void processMinusLiteral(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> nextState) {
        if (nextState != digitState) {
            if (theEvent.getEventType() == TokensEnum.OTHER) {
                return;
            }
            try {
                expressionTreeFSM.transit(new SyntaxToken.AddSubstToken(minusPosition, false));
            } catch (InvalidEventException e) {
                throw new ExpressionParserException("got unexpected exception", e,
                        ExpressionParserException.INTERNAL_ERROR, ((ExpressionParser.Token) theEvent).getPosition());
            }

        }
    }

    @onStateMachineInitialized(state = ExpressionParser.MINUS_STATE)
    public void setDigitStateReference(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
        digitState = containingMachine.getStateByName(ExpressionParser.DIGIT_STATE);
        if (digitState == null) {
            throw new BadStateMachineSpecification("Cannot find state " + ExpressionParser.DIGIT_STATE);
        }
    }

    @onStateBecomesCurrent(state = "MinusIsToken")
    public void handleMinusAsToken(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState) {
        dispatchSingleLiteralToken(theEvent, prevState);
    }

    @onStateBecomesCurrent(state = "RegularState")
    public void dispatchSingleLiteralToken(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState) {
        if (theEvent.getEventType() != TokensEnum.WHITESPACE) {

            try {
                SyntaxToken syntaxToken = null;
                ExpressionParser.Token token = (ExpressionParser.Token) theEvent;
                if (token.getEventType() == TokensEnum.END_OF_INPUT) {
                    syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(), SyntaxTokensEnum.END_OF_INPUT);
                } else {
                    switch (token.value()) {
                        case '-':
                            syntaxToken = new SyntaxToken.AddSubstToken(token.getPosition(), false);
                            break;
                        case '+':
                            syntaxToken = new SyntaxToken.AddSubstToken(token.getPosition(), true);
                            break;
                        case '*':
                            syntaxToken = new SyntaxToken.MultDivToken(token.getPosition(), true);
                            break;
                        case '/':
                            syntaxToken = new SyntaxToken.MultDivToken(token.getPosition(), false);
                            break;
                        case '(':
                            syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(), SyntaxTokensEnum.OPEN_BRACKET);
                            break;
                        case ')':
                            syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(), SyntaxTokensEnum.CLOSING_BRACKET);
                            break;
                    }
                }
                expressionTreeFSM.transit(syntaxToken);
            } catch (InvalidEventException e) {
                throw new ExpressionParserException("got unexpected exception", e,
                        ExpressionParserException.INTERNAL_ERROR, ((ExpressionParser.Token) theEvent).getPosition());

            }
        }
    }
}