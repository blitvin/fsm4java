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

import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachineEvent;

public class SyntaxError extends State<SyntaxTokensEnum> {

	public SyntaxError(String stateName, Boolean isFinal) {
		super(stateName, isFinal);
	}
	
	@Override
	public void stateBecomesCurrentCallback(StateMachineEvent<SyntaxTokensEnum> theEvent, State<SyntaxTokensEnum> prevState){
		SyntaxToken tok = (SyntaxToken) theEvent;
		if (tok.getEventType() == SyntaxTokensEnum.END_OF_INPUT)
			throw new ExpressionParserException("unexpected end of input ", ExpressionParserException.END_OF_INPUT,tok.position);
		else
			throw new ExpressionParserException("unexpected token "+tok, ExpressionParserException.UNEXPECTED_TOKEN,  tok.position);
	}

}
