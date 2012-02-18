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
package org.apache.commons.scxml.env;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SimpleContextTest extends TestCase {

    public SimpleContextTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SimpleContextTest.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { SimpleContextTest.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }

    private SimpleContext context;

    protected void setUp() throws Exception {
        context = new SimpleContext();
    }
    
    public void testHasTrue() {
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        
        assertTrue(context.has("key"));
    }

    public void testHasNullParent() {
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        
        assertFalse(context.has("differentKey"));
    }
    
    public void testHasParentWrongKey() {
        Map parentVars = new HashMap();
        parentVars.put("key", "value");
        
        SimpleContext parentContext = new SimpleContext(parentVars);
        
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        context = new SimpleContext(parentContext, parentVars);
        
        assertFalse(context.has("differentKey"));
    }

    public void testHasParentCorrectKey() {
        Map parentVars = new HashMap();
        parentVars.put("differentKey", "value");
        
        SimpleContext parentContext = new SimpleContext(parentVars);
        
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        context = new SimpleContext(parentContext, parentVars);
        
        assertTrue(context.has("differentKey"));
    }
    
    public void testGetNull() {
        Object value = context.get("key");
        
        assertNull(value);
    }
    
    public void testGetValue() {
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        
        assertEquals("value", context.get("key"));
    }
    
    public void testGetParentValue() {
        Map parentVars = new HashMap();
        parentVars.put("differentKey", "differentValue");
        
        SimpleContext parentContext = new SimpleContext(parentVars);
        
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        context = new SimpleContext(parentContext, parentVars);
        
        assertEquals("differentValue", context.get("differentKey"));
    }
    
    public void testGetParentNull() {
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        
        assertNull(context.get("differentKey"));
    }
    
    public void testGetParentWrongValue() {
        Map parentVars = new HashMap();
        parentVars.put("differentKey", "differentValue");
        
        SimpleContext parentContext = new SimpleContext(parentVars);
        
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        context = new SimpleContext(parentContext, parentVars);
        
        assertNull(context.get("reallyDifferentKey"));
    }

    public void testSetVarsChangeValue() {
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        
        context.set("key", "newValue");
        
        assertEquals("newValue", context.get("key"));
    }

    public void testSetVarsEmpty() {
        Map vars = new HashMap();
        context.setVars(vars);
        
        context.set("key", "newValue");
        
        assertEquals("newValue", context.get("key"));
    }
    
    public void testSetVarsParent() {
        Map parentVars = new HashMap();
        parentVars.put("differentKey", "differentValue");
        
        SimpleContext parentContext = new SimpleContext(parentVars);
        
        Map vars = new HashMap();
        vars.put("key", "value");
        
        context.setVars(vars);
        context = new SimpleContext(parentContext, parentVars);
        
        context.set("differentKey", "newValue");
        
        assertEquals("newValue", context.get("differentKey"));
    }
}
