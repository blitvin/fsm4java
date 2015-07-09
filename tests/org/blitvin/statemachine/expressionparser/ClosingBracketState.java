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

import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;

public class ClosingBracketState extends State<TokensEnum> {

	private StateMachine<SyntaxTokensEnum> syntaxFSM = null;
	public ClosingBracketState(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
	}

	
	@Override
	public void stateBecomesCurrentCallback(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState){
		if (theEvent.getEventType() != TokensEnum.WHITESPACE) {
			if (syntaxFSM == null) {
				syntaxFSM = ((TokenizerFSM)getContatiningStateMachine()).syntaxFSM;
			}
			try {
				syntaxFSM.transit(new SyntaxToken.SimpleToken(((TokenizerFSM.Token)theEvent).getPosition(),
						SyntaxTokensEnum.CLOSING_BRACKET));
			} catch (InvalidEventType e) {
				throw new ExpressionParserException("got unexpected exception",e,
							ExpressionParserException.INTERNAL_ERROR, ((TokenizerFSM.Token)theEvent).getPosition());
			}
		}
	}
}
