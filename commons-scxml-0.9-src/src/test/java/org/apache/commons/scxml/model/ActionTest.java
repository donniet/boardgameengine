/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.scxml.model;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ActionTest extends TestCase {

    public ActionTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ActionTest.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { ActionTest.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }
    
    private Action action;
    
    public void setUp() {
        action = new Assign();
    }
    
    public void testGetParentStateIsState() throws Exception {
        Transition transition = new Transition();
        
        State state = new State();
        state.setId("on");
        
        transition.setParent(state);
        action.setParent(transition);

        TransitionTarget returnValue = action.getParentTransitionTarget();
        
        assertEquals("on", returnValue.getId());
    }

    public void testGetParentStateIsParallel() throws Exception {
        Transition transition = new Transition();
        
        Parallel parallel = new Parallel();
        parallel.setId("on");
 
        State state = new State();
        state.setId("off");
        
        parallel.setParent(state);

        transition.setParent(parallel);
        action.setParent(transition);

        TransitionTarget returnValue = action.getParentTransitionTarget();
        
        assertEquals("on", returnValue.getId());
    }
    
    public void testGetParentStateIsHistory() throws Exception {
        Transition transition = new Transition();
        
        History history = new History();
        history.setId("on");
 
        State state = new State();
        state.setId("off");
        
        history.setParent(state);

        transition.setParent(history);
        action.setParent(transition);

        TransitionTarget returnValue = action.getParentTransitionTarget();
        
        assertEquals("off", returnValue.getId());
    }

    public void testGetParentStateIsInitial() throws Exception {
        Transition transition = new Transition();
        
        Initial initial = new Initial();
        initial.setId("on");

        State state = new State();
        state.setId("off");

        initial.setParent(state);

        transition.setParent(initial);
        action.setParent(transition);

        TransitionTarget returnValue = action.getParentTransitionTarget();

        assertEquals("off", returnValue.getId());
    }
}
