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
package org.apache.james.protocols.smtp.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.ProtocolSessionFactory;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.impl.AbstractChannelUpstreamHandler;
import org.apache.james.protocols.impl.NettyProtocolTransport;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.slf4j.Logger;

/**
 * {@link ChannelUpstreamHandler} which is used by the SMTPServer and other line based protocols
 */
@Sharable
public class BasicChannelUpstreamHandler extends AbstractChannelUpstreamHandler {
    protected final Logger logger;
    protected final SSLContext context;
    protected String[] enabledCipherSuites;
    protected ProtocolSessionFactory sessionFactory;

    public BasicChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger) {
        this(chain, sessionFactory, logger, null, null);
    }

    public BasicChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger, SSLContext context, String[] enabledCipherSuites) {
        super(chain);
        this.sessionFactory = sessionFactory;
        this.logger = logger;
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites;
    }

    @Override
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
                    ctx.getChannel().close();
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
