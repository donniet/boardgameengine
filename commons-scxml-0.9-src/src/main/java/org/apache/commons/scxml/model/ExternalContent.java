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

import java.util.List;

/**
 * An <code>ExternalContent</code> implementation represents an
 * element in the SCXML document that may contain &quot;body
 * content&quot;, which means an arbitrary number of child nodes
 * belonging to external namespaces.
 *
 */
public interface ExternalContent {

    /**
     * Return the list of external namespaced children as
     * DOM node instances.
     *
     * @return The list of (external namespaced) child nodes.
     */
    List getExternalNodes();

}

