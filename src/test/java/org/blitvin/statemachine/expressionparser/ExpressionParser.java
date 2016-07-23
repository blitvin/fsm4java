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
import org.blitvin.statemachine.FSMStateFactory;
import org.blitvin.statemachine.InvalidEventException;
import org.blitvin.statemachine.InvalidFactoryImplementation;
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineEvent;
import org.blitvin.statemachine.utils.AnnotatedObjectStateFactory;

public class ExpressionParser {

    final static String SYNTAX_FSM = "syntaxFSM";
    final static String TOKENIZER_FSM = "tokenizerFSM";
    final static String FSM_DEFINITIONS_FILE = "expressionparser.xml";
    final static String CHAR_MAP_FILE = "char2token.map";

    static final String DIGIT_STATE = "DigitState";
    static final String MINUS_STATE = "MinusState";
    
    StateMachine<SyntaxTokensEnum> syntaxFSM = null;
    StateMachine<TokensEnum> tokenizerFSM = null;
    final private Reader expressionReader;
    ExpressionTree tree = null;

    

    public static class Token implements StateMachineEvent<TokensEnum> {

        final private TokensEnum type;
        final private int position;
        final private char value;

        private static final HashMap<Character, TokensEnum> char2token;

        static {

            Properties p = new Properties();
            char2token = new HashMap<>();
            try {
                p.load(ClassLoader.getSystemResourceAsStream(CHAR_MAP_FILE));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for (Object s : p.keySet()) {
                // here all keys are one-characters
                char2token.put(((String) s).charAt(0), TokensEnum.valueOf(p.getProperty((String) s)));
            }
        }

        public Token(char character, int position) {
            value = character;
            TokensEnum e = char2token.get(character);
            type = (e == null) ? TokensEnum.OTHER : e;
            this.position = position;
        }

        public Token(int pos) {
            value = '\n';
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

        public char value() {
            return value;
        }

    }

    public ExpressionParser(Reader expressionReader) throws BadStateMachineSpecification, InvalidFactoryImplementation {
        this.expressionReader = expressionReader;
        DOMStateMachineFactory factory = new DOMStateMachineFactory(FSM_DEFINITIONS_FILE);
        tree = new ExpressionTree();
        syntaxFSM = (StateMachine<SyntaxTokensEnum>) factory.getBuilder(SYNTAX_FSM)
                .build((FSMStateFactory) new AnnotatedObjectStateFactory<SyntaxTokensEnum>(tree), false);
        tokenizerFSM = (StateMachine<TokensEnum>) factory.getBuilder(TOKENIZER_FSM)
                .build((FSMStateFactory) new AnnotatedObjectStateFactory<SyntaxTokensEnum>(new Tokenizer(syntaxFSM)), false);
    }

    public ExpressionParser(String expression) throws BadStateMachineSpecification, InvalidFactoryImplementation {
        this(new StringReader(expression));
    }

    public int compute() throws ExpressionParserException, IOException {
        int pos = 0;
        try {
            while (true) {

                int next = expressionReader.read();
                if (next == -1) {
                    tokenizerFSM.transit(new Token(pos));
                    return tree.value();
                } else {
                    tokenizerFSM.transit(new Token((char) next, pos++));
                }
            }
        } catch (InvalidEventException ex) {
            throw new ExpressionParserException("InvalidEventType exception is thrown during transition:" + ex.getMessage(),
                    ExpressionParserException.INTERNAL_ERROR, pos);
        }

    }

}