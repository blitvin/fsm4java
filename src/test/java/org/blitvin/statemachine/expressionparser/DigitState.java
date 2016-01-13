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
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

public class DigitState extends State<TokensEnum> {

	
	private int absValue;
	private boolean isPositive;
	private int startPosition; // for error reporting etc.
	private State<TokensEnum> minusState;
	private StateMachine<SyntaxTokensEnum> syntaxFSM = null;
 	public DigitState(String stateName,Boolean isFinal)  {
		super(TokenizerFSM.DigitStateName, isFinal);
	}
 	
	@Override
	public void onStateBecomesCurrent(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState){
		if (prevState != this) { // start of number literal
			TokenizerFSM.Token curToken = (TokenizerFSM.Token) theEvent;
			absValue = curToken.value() - '0';
			startPosition = curToken.getPosition();
			if (prevState == minusState) {// control came from minus - the literal is negative, 
				                         // also don't bother to track down -0 as error
				isPositive = false;
				startPosition--; // start position of the literal is at minus char
			}
			else isPositive = true;
		} else { // continuation of number literal
			absValue = absValue *10 + (((TokenizerFSM.Token)theEvent).value() - '0');
		}
	}
	
	@Override
	public void onStateIsNoLongerCurrent(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> nextState){
		if ( nextState != this) { //end of number literal
			if (theEvent.getEventType() == TokensEnum.OTHER)
				return;
			if (syntaxFSM == null)
				syntaxFSM = ((TokenizerFSM)getContatiningStateMachine()).syntaxFSM;
			try {
				syntaxFSM.transit(new SyntaxToken.LiteralToken(startPosition, isPositive?absValue:-absValue));
			} catch (InvalidEventType e) {
				throw new ExpressionParserException("got unexpected exception",e,
						ExpressionParserException.INTERNAL_ERROR, ((TokenizerFSM.Token)theEvent).getPosition());
			}
		}
	}

	@Override
	public void onStateMachineInitialized(Map<Object,Object>  initializer) throws BadStateMachineSpecification
	{
		super.onStateMachineInitialized(initializer);
		minusState = getContatiningStateMachine().getStateByName(TokenizerFSM.MinusStateName);
		if (minusState == null)
			throw new BadStateMachineSpecification("Cannot find state "+TokenizerFSM.MinusStateName);
	}
}
