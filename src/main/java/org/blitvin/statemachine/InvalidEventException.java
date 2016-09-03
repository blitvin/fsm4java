/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;
/**
 * this exception is thrown if FSM can't process the event e.g. 
 * transition map doesn't contain event type passed to the state machine
 * @author blitvin
 *
 */
public class InvalidEventException extends Exception {

	private static final long serialVersionUID = -6589959736381224582L;
        public InvalidEventException(){
            super();
        }
        public InvalidEventException(String msg) {
            super(msg);
        }
        public InvalidEventException(String msg, Throwable cause){
            super(msg, cause);
        }

}