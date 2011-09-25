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

package org.apache.james.protocols.api;

import java.util.Map;

import org.slf4j.Logger;

/**
 * Session for a protocol. Every new connection generates a new session
 * 
 *
 */
public interface ProtocolSession {
   
    /**
     * Gets the context sensitive log for this session.
     * @return log, not null
     */
    Logger getLogger();
    
    
    /**
     * Return Map which can be used to store objects within a session
     * 
     * @return state
     */
    Map<String, Object> getState();
    
    
    /**
     * Returns Map that consists of the state of the {@link ProtocolSession} per connection
     *
     * @return map of the current {@link ProtocolSession} state per connection
     */
    Map<String,Object> getConnectionState();

    
    /**
     * Reset the state
     */
    void resetState();
    
    /**
     * Write the response back to the client. Special care MUST be take to handle {@link StartTlsResponse} instances.
     * 
     * 
     * @param response
     */
    void writeResponse(Response response);

    
    /**
     * Returns host name of the client
     *
     * @return hostname of the client
     */
    String getRemoteHost();

    /**
     * Returns host ip address of the client
     *
     * @return host ip address of the client
     */
    String getRemoteIPAddress();
    
    /**
     * Return the ID for the session
     * 
     * @return id
     */
    String getSessionID();

    /**
     * Define a response object to be used as reply for a too long input line
     * @return Response
     */
    Response newLineTooLongResponse();

    /**
     * Define a response object to be used as reply during a fatal error.
     * Connection will be closed after this response.
     * @return Response
     */
    Response newFatalErrorResponse();
    
    /**
     * Returns the user name associated with this interaction.
     *
     * @return the user name
     */
    String getUser();

    /**
     * Sets the user name associated with this interaction.
     *
     * @param user the user name
     */
    void setUser(String user);

    /**
     * Return true if StartTLS is supported by the configuration
     * 
     * @return supported
     */
    boolean isStartTLSSupported();
    
    /**
     * Return true if the starttls was started
     * 
     * @return true
     */
    boolean isTLSStarted();

}
