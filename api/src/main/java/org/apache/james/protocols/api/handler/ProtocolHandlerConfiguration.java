/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.protocols.api.handler;


import java.util.Iterator;


public interface ProtocolHandlerConfiguration {

    /**
     * Returns the value of the named initialization parameter,
     * or null if the parameter does not exist.
     *
     * @param name the name of the initialization parameter
     * @return the value of the initialization parameter, or null
     */
    String getInitParameter(String name);

    /**
     * Returns the names of the mailet's initialization parameters as an
     * Iterator of String objects, or an empty Iterator if the {@link ProtocolHandler} has
     * no initialization parameters.
     *
     * @return an Iterator of String objects containing the names of the
     *      mailet's initialization parameters
     */
    Iterator<String> getInitParameterNames();
}
