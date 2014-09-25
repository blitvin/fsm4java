/*
 * (C) Copyright Boris Litvin 2014
 * This file is part of StateMachine library.
 *
 *  StateMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   NioServer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with StateMachine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;
/**
 * this exception is thrown if transition map doesn't contain event type passed to the state machine
 * @author blitvin
 *
 */
public class InvalidEventType extends Exception {

	private static final long serialVersionUID = -6589959736381224582L;

}
