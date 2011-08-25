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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;

import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.TLSSupportedSession;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedNioFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.slf4j.Logger;

/**
 * Abstract implementation of TLSSupportedSession which use Netty
 * 
 * 
 */
public abstract class AbstractSession implements TLSSupportedSession {
    protected ChannelHandlerContext handlerContext;
    protected InetSocketAddress socketAddress;
    private Logger logger;
    private SessionLog pLog = null;
    
    protected SSLEngine engine;
    protected String user;

    private String id;
	private boolean zeroCopy;

    public AbstractSession(Logger logger, ChannelHandlerContext handlerContext, SSLEngine engine) {
        this(logger, handlerContext, engine, true);
    }
    
    public AbstractSession(Logger logger, ChannelHandlerContext handlerContext, SSLEngine engine, boolean zeroCopy) {
        this.handlerContext = handlerContext;
        this.socketAddress = (InetSocketAddress) handlerContext.getChannel().getRemoteAddress();
        this.logger = logger;
        this.engine = engine;
        this.id = handlerContext.getChannel().getId() + "";
        this.zeroCopy = zeroCopy;

    }

    public AbstractSession(Logger logger, ChannelHandlerContext handlerContext) {
        this(logger, handlerContext, null);
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
     * Return underlying IoSession
     * 
     * @return session
     */
    public ChannelHandlerContext getChannelHandlerContext() {
        return handlerContext;
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
            return getChannelHandlerContext().getPipeline().get("sslHandler") != null;
        }
        
        return false;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#startTLS()
     */
    public void startTLS() throws IOException {
        if (isStartTLSSupported() && isTLSStarted() == false) {
            getChannelHandlerContext().getChannel().setReadable(false);
            SslHandler filter = new SslHandler(engine);
            filter.getEngine().setUseClientMode(false);
            resetState();
            getChannelHandlerContext().getPipeline().addFirst("sslHandler", filter);
            getChannelHandlerContext().getChannel().setReadable(true);
        }
        
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
    	Channel channel = getChannelHandlerContext().getChannel();
        if (response != null && channel.isConnected()) {
           ChannelFuture cf = channel.write(response);
           if (response.isEndSession()) {
                // close the channel if needed after the message was written out
                cf.addListener(ChannelFutureListener.CLOSE);
           }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#writeStream(java.io.InputStream)
     */
    public void writeStream(InputStream stream) {
        Channel channel = getChannelHandlerContext().getChannel();
        if (stream != null && channel.isConnected()) {

            if (stream instanceof FileInputStream  && channel.getFactory() instanceof NioServerSocketChannelFactory) {
                FileChannel fc = ((FileInputStream) stream).getChannel();
                try {
                    if (zeroCopy) {
                        channel.write(new DefaultFileRegion(fc, fc.position(), fc.size()));
                    } else {
                        channel.write(new ChunkedNioFile(fc, 8192));
                    }
                } catch (IOException e) {
                    // Catch the exception and just pass it so we get the exception later
                    channel.write(new ChunkedStream(stream));
                }
            } else {
                channel.write(new ChunkedStream(stream));
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
