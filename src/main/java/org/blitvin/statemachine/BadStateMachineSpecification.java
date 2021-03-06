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
package org.blitvin.statemachine;

/**
 * BadStateSpecification is exception thrown by state machine factory upon 
 * encounter of problems during state machine constructor. In most case original
 * cause (e.g. IllegalArgumentException in attempt to convert string to enum constant
 *  during parsing of machine specification by DOMStateMachineFactory)
 * @author blitvin
 *
 */
public class BadStateMachineSpecification extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public BadStateMachineSpecification(String msg, Exception ex){
		super(msg,ex);
	}
	public BadStateMachineSpecification(String msg){
		super(msg);
	}
}