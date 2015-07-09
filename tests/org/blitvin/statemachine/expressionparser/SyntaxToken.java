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

import org.blitvin.statemachine.StateMachineEvent;

public abstract class SyntaxToken implements StateMachineEvent<SyntaxTokensEnum>{
	final protected int position;
	public SyntaxToken(int position){
		this.position = position;
	}
	@Override
	public String toString() {
				return "[position in input:"+position+"]";
	}
	public static class LiteralToken extends SyntaxToken {
		final int value;
		public LiteralToken(int position, int value){
			super(position);
			this.value = value;
		}
		@Override
		public String toString(){
			return "Literal "+value+ super.toString();
		}
		@Override
		public SyntaxTokensEnum getEventType() { return SyntaxTokensEnum.LITERAL;}
	}
	
	public static class AddSubstToken extends SyntaxToken {
		final boolean isAddition;
		public AddSubstToken(int position, boolean isAddition){
			super(position);
			this.isAddition = isAddition;
		}
		
		@Override
		public String toString(){
			return (isAddition? "'+'" : "'-'")+ super.toString();
		}
		
		@Override
		public SyntaxTokensEnum getEventType() { return SyntaxTokensEnum.ADD_SUBSTRACT;}
	}
	
	public static class MultDivToken extends SyntaxToken {
		final boolean isMultiplication;
		public MultDivToken(int position, boolean isMultiplication){
			super(position);
			this.isMultiplication = isMultiplication;
		}
		
		@Override
		public String toString(){
			return (isMultiplication? "'*'" : "'/'")+ super.toString();
		}
		
		@Override
		public SyntaxTokensEnum getEventType() { return SyntaxTokensEnum.MULTIPLY_DIVIDE;}
	}

	
	public static class SimpleToken extends SyntaxToken {
		final SyntaxTokensEnum token;
		public SimpleToken(int position, SyntaxTokensEnum token) {
			super(position);
			this.token = token;
		}
		@Override
		public SyntaxTokensEnum getEventType() {
			return token;
		}
		
		@Override
		public String toString(){
			switch(token) {
			case OPEN_BRACKET: return "'('" + super.toString();
			case CLOSING_BRACKET: return "')'"+super.toString();
			case END_OF_INPUT: return "END OF INPUT TOKEN";
			default:
			  return "WARNING:incorrect use of SimpleToken - token "+token.toString() + super.toString();
			}
		}
	}
	
	public static class ErrorToken extends SyntaxToken {

		final SyntaxToken originalToken;
		public ErrorToken(SyntaxToken originalToken){
			super(originalToken.position);
			this.originalToken=originalToken;
		}
		@Override
		public SyntaxTokensEnum getEventType() {
			return SyntaxTokensEnum.UNEXPECTED_TOKEN;
		}
		
	}
	
}
