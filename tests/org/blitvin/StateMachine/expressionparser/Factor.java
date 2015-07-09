/*
 * (C) Copyright Boris Litvin 2014, 2015
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
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

public class Factor extends State<SyntaxTokensEnum> {

	private ExpressionTreeFSM fsm = null;

	public Factor(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
	}

	@Override
	public void stateBecomesCurrentCallback(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState){
		
		int newVal = 0;
		if (theEvent.getEventType() == SyntaxTokensEnum.LITERAL)
			newVal = ((SyntaxToken.LiteralToken)theEvent).value;
		else if (theEvent.getEventType() == SyntaxTokensEnum.CLOSING_BRACKET) {// end of expression
			newVal = fsm.curExpression.value;
			fsm.curExpression = fsm.curFactor.parent; //remove syntax tree lower level expression
		}
		
		if (fsm.curFactor.gotInitialValue) {
			if (fsm.curFactor.lastOpIsMult) 
				fsm.curFactor.value *= newVal;
			else {
				if (newVal == 0) {
					throw new ExpressionParserException("division by 0", ExpressionParserException.DIVISION_BY_0,((SyntaxToken)theEvent).position);
				}
				fsm.curFactor.value /=  newVal; 
			}
		} else {
			fsm.curFactor.value = newVal;
			fsm.curFactor.gotInitialValue = true;
		}
	}
	
	@Override
	public void stateMachineInitializedCallback(Map<Object,Object>  initializer) throws BadStateMachineSpecification
	{
		fsm = (ExpressionTreeFSM) getContatiningStateMachine();
	}
}
