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
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

public class Expression extends State<SyntaxTokensEnum> {

	private ExpressionTreeFSM fsm = null;

	
	public Expression(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
		// TODO Auto-generated constructor stub
	}


	private void updateExpressionValue() {
		int newVal = fsm.curFactor.value;
		if (fsm.curExpression.gotInitialValue) {
			if (fsm.curExpression.lastOpIsAdd) 
				fsm.curExpression.value += newVal;
			else
				fsm.curExpression.value -= newVal;
		}
		else {
			fsm.curExpression.value = newVal;
			fsm.curExpression.gotInitialValue = true;
		}
	}
	@Override
	public void stateBecomesCurrentCallback(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState){
	
		switch(theEvent.getEventType()) {
		case CLOSING_BRACKET:
			updateExpressionValue();
			if (fsm.curExpression.parent ==  null ){
				fsm.generateInternalEvent(new SyntaxToken.ErrorToken((SyntaxToken) theEvent));
			} else {
				fsm.curFactor = fsm.curExpression.parent;
				fsm.generateInternalEvent(theEvent);
			}
			break;
		case END_OF_INPUT:
			updateExpressionValue();
			if (fsm.curExpression.parent != null)// missing closing bracket(s)
					fsm.generateInternalEvent(theEvent);
			break;
		case ADD_SUBSTRACT:
			updateExpressionValue();
			fsm.generateInternalEvent(theEvent);
		}
	}
	
	@Override
	public void stateMachineInitializedCallback(Map<Object,Object>  initializer) throws BadStateMachineSpecification
	{
		fsm = (ExpressionTreeFSM) getContatiningStateMachine();
	}

}
