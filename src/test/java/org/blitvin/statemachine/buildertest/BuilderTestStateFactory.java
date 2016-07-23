/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.buildertest;

import java.util.HashMap;
import org.blitvin.statemachine.FSMStateFactory;
import org.blitvin.statemachine.State;

/**
 *
 * @author blitvin
 */
public class BuilderTestStateFactory<EventType extends Enum<EventType>> implements FSMStateFactory<EventType>{

    @Override
    public State<EventType> get(String state, HashMap<Object, Object> initializers) {
        if (state.equals("state2"))
            return new BuilderTestState<>();
        else
            return null;
    }
    
}