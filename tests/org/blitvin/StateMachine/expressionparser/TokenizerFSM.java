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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.DOMStateMachineFactory;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.InvalidFactoryImplementation;
import org.blitvin.statemachine.SimpleStateMachine;
import org.blitvin.statemachine.SimpleTransition;
import org.blitvin.statemachine.State;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineBuilder;
import org.blitvin.statemachine.StateMachineEvent;

public class TokenizerFSM extends SimpleStateMachine<TokensEnum> {

	
	StateMachine<SyntaxTokensEnum> syntaxFSM = null;
	final private Reader expressionReader;
	
	public static final String DigitStateName = "DigitState";
	public static final String MinusStateName = "MinusState";
	
	public static class Token implements StateMachineEvent<TokensEnum>{

		final private TokensEnum type;
		final private int position;
		final private char value;
	
		
		
		private static final HashMap<Character,TokensEnum> char2token;
		static {
			
			Properties p = new Properties();
			char2token = new HashMap<>();
			try {
				p.load(ClassLoader.getSystemResourceAsStream("char2token.map"));	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(Object s : p.keySet()) {
				// here all keys are one-characters
				char2token.put(((String)s).charAt(0), TokensEnum.valueOf(p.getProperty((String) s)));
			}
		}
		
		public Token(char character, int position){
			value = character;
			TokensEnum e = char2token.get(character);
			type = (e == null)?TokensEnum.OTHER:e;
			this.position = position;
		}
		
		public Token(int pos) {
			value =  '\n';
			position = pos;
			type = TokensEnum.END_OF_INPUT;
		}
		@Override
		public TokensEnum getEventType() {
			return type;
		}
		
		public int getPosition() {
			return position;
		}
		
		public char value(){
			return value;
		}
		
	}
	public void setStateMachine(StateMachine<SyntaxTokensEnum> syntaxFSM) {
		if (this.syntaxFSM == null)
			this.syntaxFSM = syntaxFSM;
	}
	
	public TokenizerFSM(HashMap<String, State<TokensEnum>> states,
			State<TokensEnum> initialState,Object constructorArgs) throws BadStateMachineSpecification {
		super(states, initialState);
		expressionReader = (Reader)constructorArgs;
	}

	public TokenizerFSM(HashMap<String, State<TokensEnum>> states,State<TokensEnum> initialState,
			HashMap<Object,HashMap<Object,Object>>initializer,Object constructorArgs) throws BadStateMachineSpecification {
		super(states,initialState,initializer);
		expressionReader = (Reader)constructorArgs;
	}
	
	public void run() throws InvalidEventType,IOException{
		int pos = 0;
		while(true) {
			int next = expressionReader.read();
			if (next == -1) {
				transit(new Token(pos));
				return;
			} else {
				transit(new Token((char) next, pos++));
			}
		}
	}

}
