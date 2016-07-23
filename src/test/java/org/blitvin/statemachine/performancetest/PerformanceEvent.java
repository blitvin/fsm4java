/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blitvin.statemachine.performancetest;

import org.blitvin.statemachine.StateMachineEvent;

/**
 *
 * @author blitvin
 */
public class PerformanceEvent implements StateMachineEvent<PerformanceEnum>{

    private PerformanceEnum eventType;
    @Override
    public PerformanceEnum getEventType() {
        return eventType;
    }
    
    public PerformanceEvent(PerformanceEnum eventType){
        this.eventType= eventType;
    }
    
    public void setEventType(PerformanceEnum eventType){
        this.eventType = eventType;
    }
    
}