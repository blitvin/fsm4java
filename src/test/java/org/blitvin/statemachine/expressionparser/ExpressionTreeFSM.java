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
import java.io.StringReader;
import java.util.HashMap;

import org.blitvin.statemachine.BadStateMachineSpecification;
import org.blitvin.statemachine.DOMStateMachineFactory;
import org.blitvin.statemachine.InvalidEventType;
import org.blitvin.statemachine.InvalidFactoryImplementation;
//import org.blitvin.statemachine.AspectEnabledStateMachine;
import org.blitvin.statemachine.SimpleStateMachine;
import org.blitvin.statemachine.State;

public class ExpressionTreeFSM extends SimpleStateMachine<SyntaxTokensEnum> {
	
	ExpressionNode startNode = new ExpressionNode();
	ExpressionNode curExpression = startNode;
	FactorNode curFactor = null;
	
	public ExpressionTreeFSM(HashMap<String, State<SyntaxTokensEnum>> states,
			State<SyntaxTokensEnum> initialState)
			throws BadStateMachineSpecification {
		super(states, initialState);
		//setAspects(new ExpressionTreeAspect());
	}

	public ExpressionTreeFSM(HashMap<String, State<SyntaxTokensEnum>> states,State<SyntaxTokensEnum> initialState,
			HashMap<Object,HashMap<Object,Object>>initializer) throws BadStateMachineSpecification {
		super(states,initialState,initializer);
		//setAspects(new ExpressionTreeAspect());
	}
	
	public int value() { return startNode.value;}
	
	static ExpressionTreeFSM expressionTreeFSM = null;
	static DOMStateMachineFactory factory;
	static {
		try {
			factory = new DOMStateMachineFactory("expressionparser.xml");
			
		} catch (InvalidFactoryImplementation e) {
			expressionTreeFSM =null;
			factory = null;
		}
		
	}

}
