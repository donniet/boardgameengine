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
package org.apache.commons.scxml;

import java.net.URL;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.scxml.env.jsp.ELContext;
import org.apache.commons.scxml.env.jsp.ELEvaluator;
import org.apache.commons.scxml.model.State;

/**
 * Unit tests for namespace prefixes in XPaths pointing bits in a &lt;data&gt;.
 */
public class NamespacePrefixedXPathsTest extends TestCase {

    /**
     * Construct a new instance of NamespacePrefixedXPathsTest with
     * the specified name
     */
    public NamespacePrefixedXPathsTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(NamespacePrefixedXPathsTest.class);
        suite.setName("Namespace Prefixed XPaths Tests");
        return suite;
    }

    // Test data
    private URL datamodel03jexl, datamodel03jsp;
    private SCXMLExecutor exec01, exec02;

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() {
        datamodel03jexl = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml/env/jexl/datamodel-03.xml");
        datamodel03jsp = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml/env/jsp/datamodel-03.xml");
        exec01 = SCXMLTestHelper.getExecutor(datamodel03jexl);
        exec02 = SCXMLTestHelper.getExecutor(datamodel03jsp, new ELContext(), new ELEvaluator());
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        datamodel03jexl = datamodel03jsp = null;
        exec01 = exec02 = null;
    }

    /**
     * Test the XPath evaluation
     */
    // JEXL
    public void testNamespacePrefixedXPathsJexl() {
        runtest(exec01);
    }

    // EL
    public void testNamespacePrefixedXPathsEL() {
        runtest(exec02);
    }

    // Same test, since same documents (different expression languages)
    private void runtest(SCXMLExecutor exec) {

        try {

            // must be in state "ten" at the onset
            Set currentStates = exec.getCurrentStatus().getStates();
            assertEquals(1, currentStates.size());
            assertEquals("ten", ((State)currentStates.iterator().
                next()).getId());

            // should move to "twenty"
            currentStates = SCXMLTestHelper.fireEvent(exec, "ten.done");
            assertEquals(1, currentStates.size());
            assertEquals("twenty", ((State)currentStates.iterator().
                next()).getId());

            // This is set while exiting "ten"
            Double retval = (Double) exec.getRootContext().get("retval");
            assertEquals(Double.valueOf("11"), retval);

            // On to "thirty"
            currentStates = SCXMLTestHelper.fireEvent(exec, "twenty.done");
            assertEquals(1, currentStates.size());
            assertEquals("thirty", ((State)currentStates.iterator().
                next()).getId());
            exec = SCXMLTestHelper.testExecutorSerializability(exec);

            // Tests XPath on SCXML actions, set while exiting "twenty"
            String retvalstr = (String) exec.getRootContext().get("retval");
            assertEquals("Equal to 20", retvalstr);

            // and so on ...
            currentStates = SCXMLTestHelper.fireEvent(exec, "thirty.done");
            assertEquals(1, currentStates.size());
            assertEquals("forty", ((State)currentStates.iterator().
                next()).getId());

            currentStates = SCXMLTestHelper.fireEvent(exec, "forty.done");
            assertEquals(1, currentStates.size());
            assertEquals("fifty", ((State)currentStates.iterator().
                next()).getId());

            currentStates = SCXMLTestHelper.fireEvent(exec, "fifty.done");
            assertEquals(1, currentStates.size());
            assertEquals("sixty", ((State)currentStates.iterator().
                next()).getId());

            currentStates = SCXMLTestHelper.fireEvent(exec, "sixty.done");
            assertEquals(1, currentStates.size());
            assertEquals("seventy", ((State)currentStates.iterator().
                next()).getId());

            // done
            assertTrue(exec.getCurrentStatus().isFinal());

        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    public static void main(String args[]) {
        TestRunner.run(suite());
    }

}

