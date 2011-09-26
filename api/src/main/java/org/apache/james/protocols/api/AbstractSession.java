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
import java.util.HashMap;
import java.util.Map;


import org.apache.james.protocols.api.FutureResponse.ResponseListener;
import org.apache.james.protocols.api.ProtocolTransport;
import org.apache.james.protocols.api.Response;
import org.slf4j.Logger;

/**
 * Abstract implementation of {@link ProtocolSession}
 * 
 * 
 */
public abstract class AbstractSession implements ProtocolSession {

    protected InetSocketAddress socketAddress;
    private Logger logger;
    private SessionLog pLog = null;
    
    protected String user;

    private String id;
    protected ProtocolTransport transport;

    private Map<String, Object> connectionState;
    private Map<String, Object> sessionState;

    
    public AbstractSession(Logger logger, ProtocolTransport transport) {
        this.transport = transport;
        this.socketAddress = transport.getRemoteAddress();
        this.logger = logger;
        this.id = transport.getId();
        this.connectionState = new HashMap<String, Object>();
        this.sessionState = new HashMap<String, Object>();

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return socketAddress.getHostName();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return socketAddress.getAddress().getHostAddress();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getUser()
     */
    public String getUser() {
        return user;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#setUser(java.lang.String)
     */
    public void setUser(String user) {
        this.user = user;
    }

    /*
     * 
     */
    public ProtocolTransport getProtocolTransport() {
        return transport;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return transport.isStartTLSSupported();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        return transport.isTLSStarted();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getLogger()
     */
    public Logger getLogger() {
        if (pLog == null) {
            pLog = new SessionLog(getSessionID(), logger);
        }
        return pLog;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.ProtocolSession#writeResponse(org.apache.james.api.protocol.Response)
     */
    public void writeResponse(final Response response) {
        if (response instanceof FutureResponse) {
            ((FutureResponse) response).addListener(new ResponseListener() {

                public void onResponse(Response response) {
                    transport.writeResponse(response, AbstractSession.this);
                }
            });
        } else {
            transport.writeResponse(response, this);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getSessionID()
     */
    public String getSessionID() {
        return id;
    }
    
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getConnectionState()
     */
    public Map<String, Object> getConnectionState() {
        return connectionState;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getState()
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getState() {
        return sessionState;
    }

}
