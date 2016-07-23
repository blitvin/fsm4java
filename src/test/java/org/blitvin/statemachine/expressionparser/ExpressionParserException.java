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
/* expression parser is used in unitest of the library, so error code here is for asserts on correct errors are yielded 
  on incorrect inputs */
public class ExpressionParserException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int UNEXPECTED_SYMBOL = 1;
	public static final int END_OF_INPUT = 2;
	public static final int UNEXPECTED_TOKEN = 3;
	public static final int DIVISION_BY_0 = 4;
	public static final int INTERNAL_ERROR = 5;
	
	final int position;
	final int errorCode;
	
	public int getExceptionPosition() {return position;}
	
	public ExpressionParserException(String message, int errorCode,int position) {
		super(message);
		this.errorCode = errorCode;
		this.position = position;
	}

	public ExpressionParserException(Throwable cause, int errorCode,int position) {
		super(cause);
		this.errorCode = errorCode;
		this.position = position;
	}

	public ExpressionParserException(String message, Throwable cause, int errorCode, int position) {
		super(message, cause);
		this.errorCode = errorCode;
		this.position = position;
	}

	@Override
	public String toString() {
		return "Exception "+errorCode+" at position "+position+":"+super.getMessage();
	}
}