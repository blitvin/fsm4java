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
import org.blitvin.statemachine.FSMSupportingInternalEvents;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.utils.onStateBecomesCurrent;
import org.blitvin.statemachine.utils.onStateMachineInitialized;

/**
 * ExpressionTree is a representation of, well, expression tree built during
 * computation of an expression. Actually, only path from root to currently 
 * evaluated leaf is held. Once value of a sub-tree can be computed, the subtree
 * is replaced with its value. The class defines callbacks for FSM states that 
 * are called during evaluation of tokens stream by the FSM.
 * @author blitvin
 */
public class ExpressionTree {

    private class ExpressionNode {

        FactorNode parent = null;
        int value = 0;
        boolean gotInitialValue = false;
        boolean lastOpIsAdd = false;
    }

    private class FactorNode {

        ExpressionNode parent = null;
        int value = 0;
        boolean gotInitialValue = false;
        boolean lastOpIsMult = false;
    }
    
    private ExpressionNode curExpression;
    private FactorNode curFactor;
    private FSMSupportingInternalEvents fsm = null;

    public ExpressionTree() {
        curExpression = new ExpressionNode();
        curFactor = null;
    }

    public int value() {
        return curExpression.value;
    }

    @onStateBecomesCurrent(state = "addOrSubst")
    public void handleAdditionOrSubstraction(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        curExpression.lastOpIsAdd = ((SyntaxToken.AddSubstToken) theEvent).isAddition;
    }

    private void updateExpressionValue() {
        int newVal = curFactor.value;
        if (curExpression.gotInitialValue) {
            if (curExpression.lastOpIsAdd) {
                curExpression.value += newVal;
            } else {
                curExpression.value -= newVal;
            }
        } else {
            curExpression.value = newVal;
            curExpression.gotInitialValue = true;
        }
    }

    @onStateBecomesCurrent(state = "expression")
    public void handleExpression(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        try {
            switch (theEvent.getEventType()) {
                case CLOSING_BRACKET:
                    updateExpressionValue();
                    if (curExpression.parent == null) {
                        fsm.generateInternalEvent(new SyntaxToken.ErrorToken((SyntaxToken) theEvent));
                    } else {
                        curFactor = curExpression.parent;
                        try {
                            fsm.generateInternalEvent(theEvent);
                        } catch (InvalidEventException ex) {
                        }
                    }
                    break;
                case END_OF_INPUT:
                    updateExpressionValue();
                    if (curExpression.parent != null)// missing closing bracket(s)
                    {
                        fsm.generateInternalEvent(theEvent);
                    }
                    break;
                case ADD_SUBSTRACT:
                    updateExpressionValue();
                    fsm.generateInternalEvent(theEvent);
            }
        } catch (InvalidEventException ex) {
            throw new ExpressionParserException("got unexpected exception", ex,
                    ExpressionParserException.INTERNAL_ERROR, -1);
        }
    }

    @onStateMachineInitialized(state = "expression")
    public void setExpressionTreeFSMReference(Map<?, ?> initializer, FSMStateView containingMachine) throws BadStateMachineSpecification {
        fsm = (FSMSupportingInternalEvents) containingMachine;
    }

    @onStateBecomesCurrent(state = "factor")
    public void handleFactor(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {

        int newVal = 0;

        if (theEvent.getEventType() == SyntaxTokensEnum.LITERAL) {
            newVal = ((SyntaxToken.LiteralToken) theEvent).value;
        } else if (theEvent.getEventType() == SyntaxTokensEnum.CLOSING_BRACKET) {// end of expression
            newVal = curExpression.value;
            curExpression = curFactor.parent; //remove syntax tree lower level expression
        }
        if (curFactor.gotInitialValue) {
            if (curFactor.lastOpIsMult) {
                curFactor.value *= newVal;
            } else {
                if (newVal == 0) {
                    throw new ExpressionParserException("division by 0", ExpressionParserException.DIVISION_BY_0, ((SyntaxToken) theEvent).position);
                }
                curFactor.value /= newVal;
            }
        } else {
            curFactor.value = newVal;
            curFactor.gotInitialValue = true;
        }
    }

    @onStateBecomesCurrent(state = "multOrDiv")
    public void handleMultiplicationOrDivision(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        curFactor.lastOpIsMult = ((SyntaxToken.MultDivToken) theEvent).isMultiplication;
    }

    @onStateBecomesCurrent(state = "startOfExpression")
    public void startingExpression(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        ExpressionNode node = new ExpressionNode();
        node.parent = curFactor;
        curExpression = node;
    }

    @onStateBecomesCurrent(state = "startOfFactor")
    public void startingFactor(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        FactorNode node = new FactorNode();
        node.parent = curExpression;
        curFactor = node;
        try {
            fsm.generateInternalEvent(theEvent);
        } catch (InvalidEventException ex) {
            throw new ExpressionParserException("got unexpected exception", ex,
                    ExpressionParserException.INTERNAL_ERROR, -1);
        }
    }
    
    @onStateBecomesCurrent(state = "error")
    public void handleError(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState) {
        SyntaxToken tok = (SyntaxToken) theEvent;
        if (tok.getEventType() == SyntaxTokensEnum.END_OF_INPUT) {
            throw new ExpressionParserException("unexpected end of input ", ExpressionParserException.END_OF_INPUT, tok.position);
        } else {
            throw new ExpressionParserException("unexpected token " + tok, ExpressionParserException.UNEXPECTED_TOKEN, tok.position);
        }
    }
}