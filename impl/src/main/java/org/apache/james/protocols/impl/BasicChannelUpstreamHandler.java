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

import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.ProtocolSessionFactory;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.ConnectHandler;
import org.apache.james.protocols.api.handler.ConnectHandlerResultHandler;
import org.apache.james.protocols.api.handler.DisconnectHandler;
import org.apache.james.protocols.api.handler.LineHandler;
import org.apache.james.protocols.api.handler.LineHandlerResultHandler;
import org.apache.james.protocols.api.handler.ProtocolHandlerChain;
import org.apache.james.protocols.impl.NettyProtocolTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.slf4j.Logger;

/**
 * {@link ChannelUpstreamHandler} which is used by the SMTPServer and other line based protocols
 */
@Sharable
public class BasicChannelUpstreamHandler extends SimpleChannelUpstreamHandler {
    protected final Logger logger;
    protected final SSLContext context;
    protected String[] enabledCipherSuites;
    protected ProtocolSessionFactory sessionFactory;
    protected ProtocolHandlerChain chain;

    public BasicChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger) {
        this(chain, sessionFactory, logger, null, null);
    }

    public BasicChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger, SSLContext context, String[] enabledCipherSuites) {
        this.chain = chain;
        this.sessionFactory = sessionFactory;
        this.logger = logger;
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites;
    }


    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.setAttachment(createSession(ctx));
        super.channelBound(ctx, e);
    }



    /**
     * Call the {@link ConnectHandler} instances which are stored in the {@link ProtocolHandlerChain}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        List<ConnectHandler> connectHandlers = chain.getHandlers(ConnectHandler.class);
        List<ConnectHandlerResultHandler> resultHandlers = chain.getHandlers(ConnectHandlerResultHandler.class);
        ProtocolSession session = (ProtocolSession) ctx.getAttachment();
        session.getLogger().info("Connection established from " + session.getRemoteHost() + " (" + session.getRemoteIPAddress()+ ")");
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                ConnectHandler cHandler = connectHandlers.get(i);
                
                long start = System.currentTimeMillis();
                connectHandlers.get(i).onConnect(session);
                long executionTime = System.currentTimeMillis() - start;
                
                for (int a = 0; a < resultHandlers.size(); a++) {
                    resultHandlers.get(a).onResponse(session, executionTime, cHandler);
                }
               
            }
        }
        super.channelConnected(ctx, e);
    }



    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        List<DisconnectHandler> connectHandlers = chain.getHandlers(DisconnectHandler.class);
        ProtocolSession session = (ProtocolSession) ctx.getAttachment();
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                connectHandlers.get(i).onDisconnect(session);
            }
        }
        super.channelDisconnected(ctx, e);
    }


    /**
     * Call the {@link LineHandler} 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ProtocolSession pSession = (ProtocolSession) ctx.getAttachment();
        LinkedList<LineHandler> lineHandlers = chain.getHandlers(LineHandler.class);
        LinkedList<LineHandlerResultHandler> resultHandlers = chain.getHandlers(LineHandlerResultHandler.class);

        
        if (lineHandlers.size() > 0) {
        
            ChannelBuffer buf = (ChannelBuffer) e.getMessage();      
            byte[] line;
            
            if (buf.hasArray()) {
                line = buf.array();
            } else {
                // copy the ChannelBuffer to a byte array to process the LineHandler
                line = new byte[buf.capacity()];
                buf.getBytes(0, line);
            }
            
            LineHandler lHandler=  (LineHandler) lineHandlers.getLast();
            long start = System.currentTimeMillis();            
            lHandler.onLine(pSession,line);
            long executionTime = System.currentTimeMillis() - start;

            for (int i = 0; i < resultHandlers.size(); i++) {
                resultHandlers.get(i).onResponse(pSession, executionTime, lHandler);
            }

        }
        
        super.messageReceived(ctx, e);
    }


    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ProtocolSession session = (ProtocolSession) ctx.getAttachment();
        if (session != null) {
            session.getLogger().info("Connection closed for " + session.getRemoteHost() + " (" + session.getRemoteIPAddress()+ ")");
        }
        cleanup(ctx);

        super.channelClosed(ctx, e);
    }

    /**
     * Cleanup the channel
     * 
     * @param channel
     */
    protected void cleanup(ChannelHandlerContext ctx) {
        ProtocolSession session = (ProtocolSession) ctx.getAttachment();
        if (session != null) {
            session.resetState();
            session = null;
        }
    }

    
    
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        SSLEngine engine = null;
        if (context != null) {
            engine = context.createSSLEngine();
            if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                engine.setEnabledCipherSuites(enabledCipherSuites);
            }
        }
        
        return sessionFactory.newSession(new NettyProtocolTransport(ctx.getChannel(), engine));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channel channel = ctx.getChannel();
        ProtocolSession session = (ProtocolSession) ctx.getAttachment();
        if (e.getCause() instanceof TooLongFrameException) {
            Response r = session.newLineTooLongResponse();
            if (r != null) ctx.getChannel().write(r);
        } else {
            if (channel.isConnected()) {
                Response r = session.newFatalErrorResponse();
                if (r != null) {
                    ctx.getChannel().write(r).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.getChannel().write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
            if (session != null) {
                session.getLogger().debug("Unable to process request", e.getCause());
            } else {
                logger.debug("Unable to process request", e.getCause());
            }
            cleanup(ctx);            
        }
    }

}
