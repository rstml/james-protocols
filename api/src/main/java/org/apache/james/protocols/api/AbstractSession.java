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


import org.apache.james.protocols.api.ProtocolTransport;
import org.slf4j.Logger;

/**
 * Abstract implementation of {@link ProtocolSession}
 * 
 * 
 */
public abstract class AbstractSession implements ProtocolSession {

    private final InetSocketAddress remoteAddress;
    private Logger logger;
    private SessionLog pLog = null;
    
    protected String user;

    private String id;
    protected ProtocolTransport transport;

    private final Map<String, Object> connectionState;
    private final Map<String, Object> sessionState;
	private final InetSocketAddress localAddress;

    
    public AbstractSession(Logger logger, ProtocolTransport transport) {
        this.transport = transport;
        this.remoteAddress = transport.getRemoteAddress();
        this.localAddress = transport.getRemoteAddress();
        this.logger = logger;
        this.id = transport.getId();
        this.connectionState = new HashMap<String, Object>();
        this.sessionState = new HashMap<String, Object>();

    }

    @Override
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
     * @see org.apache.james.protocols.api.ProtocolSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return remoteAddress.getHostName();
    }

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return remoteAddress.getAddress().getHostAddress();
    }

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getUser()
     */
    public String getUser() {
        return user;
    }

    /**
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

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return transport.isStartTLSSupported();
    }

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        return transport.isTLSStarted();
    }


    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getLogger()
     */
    public Logger getLogger() {
        if (pLog == null) {
            pLog = new SessionLog(getSessionID(), logger);
        }
        return pLog;
    }
    

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getSessionID()
     */
    public String getSessionID() {
        return id;
    }
    
    
    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getConnectionState()
     */
    public Map<String, Object> getConnectionState() {
        return connectionState;
    }

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#getState()
     */
    public Map<String, Object> getState() {
        return sessionState;
    }

}
