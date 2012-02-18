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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.scxml.Builtin;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.SCXMLTestHelper;
import org.w3c.dom.Node;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ParallelTest extends TestCase {

    public ParallelTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ParallelTest.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { ParallelTest.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // Test data
    private URL parallel01, parallel02, parallel03;
    private SCXMLExecutor exec;
    
    public void setUp() {
        parallel01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml/model/parallel-01.xml");
        parallel02 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml/model/parallel-02.xml");
        parallel03 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml/model/parallel-03.xml");
    }

    public void tearDown() {
        parallel01 = parallel02 = parallel03 = null;
        exec = null;
    }

    public void testParallel01() {
        SCXML scxml = SCXMLTestHelper.parse(parallel01);
        assertNotNull(scxml);
        exec = SCXMLTestHelper.getExecutor(scxml);
        assertNotNull(exec);
        try {
            SCXMLTestHelper.assertPostTriggerState(exec, "foo", "end");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testParallel02() {
        SCXML scxml = SCXMLTestHelper.parse(parallel02);
        assertNotNull(scxml);
        exec = SCXMLTestHelper.getExecutor(scxml);
        assertNotNull(exec);
        try {
            SCXMLTestHelper.assertPostTriggerStates(exec, "dummy.event", new String[] { "state01", "state02" });
            SCXMLTestHelper.assertPostTriggerState(exec, "event1", "state1");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testParallel03() {
        SCXML scxml = SCXMLTestHelper.parse(parallel03);
        assertNotNull(scxml);
        exec = SCXMLTestHelper.getExecutor(scxml);
        assertNotNull(exec);
        try {
            SCXMLTestHelper.assertPostTriggerStates(exec, "dummy.event", new String[] { "para11", "para21" });
            Node data = (Node) exec.getRootContext().get("root");
            Map namespaces = new HashMap();
            namespaces.put("", "http://www.w3.org/2005/07/scxml");
            Object count = Builtin.data(namespaces, data, "root/count");
            assertEquals("5.0", count.toString());
            SCXMLTestHelper.assertPostTriggerStates(exec, "foo", new String[] { "para12", "para21" });
            count = Builtin.data(namespaces, data, "root/count");
            assertEquals("7.0", count.toString());
            SCXMLTestHelper.assertPostTriggerState(exec, "bar", "end");
            count = Builtin.data(namespaces, data, "root/count");
            assertEquals("14.0", count.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
