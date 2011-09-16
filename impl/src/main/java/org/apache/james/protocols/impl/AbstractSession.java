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

package org.apache.james.protocols.impl;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.StartTlsResponse;
import org.apache.james.protocols.api.TLSSupportedSession;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;

/**
 * Abstract implementation of TLSSupportedSession which use Netty
 * 
 * 
 */
public abstract class AbstractSession implements TLSSupportedSession {
    protected Channel channel;
    protected InetSocketAddress socketAddress;
    private Logger logger;
    private SessionLog pLog = null;
    
    protected SSLEngine engine;
    protected String user;

    private String id;

    
    public AbstractSession(Logger logger, Channel channel, SSLEngine engine) {
        this.channel = channel;
        this.socketAddress = (InetSocketAddress) channel.getRemoteAddress();
        this.logger = logger;
        this.engine = engine;
        this.id = channel.getId() + "";
    }

    public AbstractSession(Logger logger, Channel channel) {
        this(logger, channel, null);
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return socketAddress.getHostName();
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return socketAddress.getAddress().getHostAddress();
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getUser()
     */
    public String getUser() {
        return user;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#setUser(java.lang.String)
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Return underlying {@link Channel}
     * 
     * @return session
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return engine != null;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        
        if (isStartTLSSupported()) {
            return channel.getPipeline().get("sslHandler") != null;
        }
        
        return false;
    }

    /**
     * @see org.apache.james.api.protocol.ProtocolSession#getLogger()
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
        if (response != null && channel.isConnected()) {
           ChannelFuture cf = channel.write(response);
           if (response.isEndSession()) {
                // close the channel if needed after the message was written out
                cf.addListener(ChannelFutureListener.CLOSE);
           } 
           if (response instanceof StartTlsResponse) {
               if (isStartTLSSupported()) {
                   channel.setReadable(false);
                   SslHandler filter = new SslHandler(engine);
                   filter.getEngine().setUseClientMode(false);
                   resetState();
                   channel.getPipeline().addFirst("sslHandler", filter);
                   channel.setReadable(true);
               }
           }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getSessionID()
     */
    public String getSessionID() {
        return id;
    }
    
    

}
