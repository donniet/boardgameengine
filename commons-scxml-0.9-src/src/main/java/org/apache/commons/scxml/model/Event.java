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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.TriggerEvent;

/**
 * The class in this SCXML object model that corresponds to the
 * &lt;event&gt; SCXML element.
 *
 * @since 0.7
 */
public class Event extends Action {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the derived event to be generated.
     */
    private String name;

    /**
     * Constructor.
     */
    public Event() {
        super();
    }

    /**
     * Get the event name.
     *
     * @return Returns the name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the event name.
     *
     * @param name The event name to set.
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(final EventDispatcher evtDispatcher,
            final ErrorReporter errRep, final SCInstance scInstance,
            final Log appLog, final Collection derivedEvents)
    throws ModelException, SCXMLExpressionException {

        if (appLog.isDebugEnabled()) {
            appLog.debug("<event>: Adding event named '" + name
                + "' to list of derived events.");
        }
        TriggerEvent ev = new TriggerEvent(name, TriggerEvent.SIGNAL_EVENT);
        derivedEvents.add(ev);

    }

}

