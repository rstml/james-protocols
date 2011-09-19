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

import java.net.InetSocketAddress;

import org.apache.james.protocols.api.handler.LineHandler;

/**
 * ProtocolTransport is used by each ProtocolSession to communicate with the underlying transport.
 * Transport implementations will provide their own implementation of the transport.
 */
public interface ProtocolTransport {

    InetSocketAddress getRemoteAddress();

    String getId();

    boolean isTLSStarted();

    boolean isStartTLSSupported();
    
    void writeResponse(Response response, ProtocolSession session);

    void popLineHandler();

    <T extends ProtocolSession> void pushLineHandler(LineHandler<T> overrideCommandHandler, T smtpNettySession);

    int getPushedLineHandlerCount();

}
