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
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Abstract base class for Servers which want to use async io
 *
 */
public abstract class AbstractAsyncServer {

    private int backlog = 250;
    
    private int port;

    private int timeout = 120;

    private ServerBootstrap bootstrap;

    private boolean started;

    private String ip;
    
    private ChannelGroup channels = new DefaultChannelGroup();

    
    /**
     * Set the ip on which the Server should listen on
     * 
     * @param ip
     */
    public void setIP(String ip) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.ip = ip;
    }
    
    
    /**
     * Set the port on which the Server should listen on
     * 
     * @param ip
     */
    public void setPort(int port) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.port = port;
    }
    
    /**
     * Start the server
     * 
     * @throws Exception 
     * 
     */
    public synchronized void start() throws Exception {
        if (started) throw new IllegalStateException("Server running already");

        if (port < 1) throw new RuntimeException("Please specify a port to which the server should get bound!");

        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        ChannelPipelineFactory factory = createPipelineFactory();
        
        // add the channel group handler
        factory.getPipeline().addFirst("channelGroupHandler", new ChannelGroupHandler());

        // Configure the pipeline factory.
        bootstrap.setPipelineFactory(factory);

        // Bind and start to accept incoming connections.
        bootstrap.setOption("backlog", backlog);
        bootstrap.setOption("reuseAddress", true);
        Channel serverChannel;
        if (getIP() == null) {
            serverChannel = bootstrap.bind(new InetSocketAddress(port));
        } else {
            serverChannel = bootstrap.bind(new InetSocketAddress(ip, port));
        }
          
        channels.add(serverChannel);
        started = true;

    }

    /**
     * Stop the server
     */
    public synchronized void stop() {
        channels.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
        started = false;
    }
    
    
    
    /**
     * Return the ip on which the server listen for connections
     * 
     * @return ip
     */
    public String getIP() {
        return ip;
    }
    
    /**
     * Return the port this server will listen on
     * 
     * @return port
     */
    public int getPort() {
        return port;
    }
    
    
    
    /**
     * Create ChannelPipelineFactory to use by this Server implementation
     * 
     * @return factory
     */
    protected abstract ChannelPipelineFactory createPipelineFactory();

    /**
     * Set the read/write timeout for the server. This will throw a {@link IllegalStateException} if the
     * server is running.
     * 
     * @param timeout
     */
    public void setTimeout(int timeout) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.timeout = timeout;
    }
    
    
    /**
     * Set the Backlog for the socket. This will throw a {@link IllegalStateException} if the server is running.
     * @param backlog
     */
    public void setBacklog(int backlog) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.backlog = backlog;
    }
    
    /**
     * Return the backlog for the socket
     * 
     * @return backlog
     */
    public int getBacklog() {
        return backlog;
    }
    
    /**
     * Return the read/write timeout for the socket.
     * @return
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Add channels to the channel group after the channel was opened
     *
     */
    @ChannelPipelineCoverage("all")
    private final class ChannelGroupHandler extends SimpleChannelUpstreamHandler {
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            // Add all open channels to the global group so that they are
            // closed on shutdown.
            channels.add(e.getChannel());
        }

    }
}
