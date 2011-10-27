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


import javax.net.ssl.SSLContext;

import org.apache.james.protocols.api.Protocol;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic NettyServer
 */
public class NettyServer extends AbstractAsyncServer {

    protected Protocol protocol;
    
    protected Logger logger = LoggerFactory.getLogger(NettyServer.class);

    protected SSLContext context;

    private ExecutionHandler eHandler;
    
    private ChannelUpstreamHandler coreHandler;

    public NettyServer(Protocol protocol) {
        this(protocol, null);
    }
    
    
    public NettyServer(Protocol protocol, SSLContext context) {
        super();
        this.protocol = protocol;
        this.context = context;
    }
    
    protected ExecutionHandler createExecutionHandler(int size) {
        return new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(size, 0, 0));
    }
    
    
    public void setUseExecutionHandler(boolean useHandler, int size) {
        if (isBound()) throw new IllegalStateException("Server running already");
        if (useHandler) {
            eHandler = createExecutionHandler(size);
        } else {
            if (eHandler != null) {
                eHandler.releaseExternalResources();
            }
            eHandler = null;
        }
    }
    
  
    protected ChannelUpstreamHandler createCoreHandler() {
        return new BasicChannelUpstreamHandler(protocol, logger, context, null);
    }
    
    @Override
    public synchronized void bind() throws Exception {
        coreHandler = createCoreHandler();
        super.bind();
    }


    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new AbstractSSLAwareChannelPipelineFactory(getTimeout(), 0, getBacklog(), group, eHandler) {

            @Override
            protected ChannelUpstreamHandler createHandler() {
                return coreHandler;
            }

            @Override
            protected boolean isSSLSocket() {
                return context != null && !protocol.isStartTLSSupported();
            }

            @Override
            protected SSLContext getSSLContext() {
                return context;
            }
        };

    }

}
