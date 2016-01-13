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
import org.blitvin.statemachine.StateMachine;
import org.blitvin.statemachine.StateMachineAspects;
import org.blitvin.statemachine.StateMachineEvent;

public class ExpressionTreeAspect implements StateMachineAspects<SyntaxTokensEnum> {

	@Override
	public boolean onTransitionStart(StateMachineEvent<SyntaxTokensEnum> event) {
		System.out.println("starting transition with token "+ event);
		return true;
	}

	@Override
	public void onNullTransition(StateMachineEvent<SyntaxTokensEnum> event) {
		System.out.println("null transition with event "+event);
		
	}

	@Override
	public boolean onControlLeavesState(
			StateMachineEvent<SyntaxTokensEnum> event,
			State<SyntaxTokensEnum> currentState,
			State<SyntaxTokensEnum> newState) {
	//	System.out.println("other state becomes current "+event +" cur state "+currentState + " new state" + newState);
		return true;
	}

	@Override
	public boolean onControlEntersState(
			StateMachineEvent<SyntaxTokensEnum> event,
			State<SyntaxTokensEnum> currentState,
			State<SyntaxTokensEnum> prevState) {
		System.out.println("SBC "+event +" cur "+currentState + " prev " + prevState);
		return true;
	}

	@Override
	public void onTransitionFinish(StateMachineEvent<SyntaxTokensEnum> event,
			State<SyntaxTokensEnum> currentState,
			State<SyntaxTokensEnum> prevState) {
		//System.out.println("send transition "+event +" cur state "+currentState + " prev state" + prevState);
		

	}

	@Override
	public void setContainingMachine(StateMachine<SyntaxTokensEnum> machine) {
		// TODO Auto-generated method stub
		
	}
	
}