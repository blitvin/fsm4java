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
import org.blitvin.statemachine.expressionparser.SyntaxToken.SimpleToken;

public class RegularState extends State<TokensEnum> {

	StateMachine<SyntaxTokensEnum> syntaxFSM = null;
	
	public RegularState(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
	}
	
	
	@Override
	public void stateBecomesCurrentCallback(StateMachineEvent<TokensEnum> theEvent, State<TokensEnum> prevState){
		if (theEvent.getEventType() != TokensEnum.WHITESPACE) {
			if (syntaxFSM == null) {
				syntaxFSM = ((TokenizerFSM)getContatiningStateMachine()).syntaxFSM;
			}
			try {
				SyntaxToken syntaxToken = null;
				TokenizerFSM.Token token = (TokenizerFSM.Token)theEvent;
				if (token.getEventType() == TokensEnum.END_OF_INPUT)
					syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(),SyntaxTokensEnum.END_OF_INPUT);
				else {
					switch (token.value()){
						case '-':
							syntaxToken = new SyntaxToken.AddSubstToken(token.getPosition(), false);
							break;
						case '+':
							syntaxToken = new SyntaxToken.AddSubstToken(token.getPosition(), true);
							break;
						case '*' :
							syntaxToken = new SyntaxToken.MultDivToken(token.getPosition(), true);
							break;
						case '/' :
							syntaxToken = new SyntaxToken.MultDivToken(token.getPosition(), false);
							break;
						case '(' :
							syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(), SyntaxTokensEnum.OPEN_BRACKET);
							break;
						case ')' :
							syntaxToken = new SyntaxToken.SimpleToken(token.getPosition(), SyntaxTokensEnum.CLOSING_BRACKET);
							break;
					}
				}
				syntaxFSM.transit(syntaxToken);
			} catch (InvalidEventType e) {
				throw new ExpressionParserException("got unexpected exception",e,
						ExpressionParserException.INTERNAL_ERROR, ((TokenizerFSM.Token)theEvent).getPosition());

			}
		}
	}
}
