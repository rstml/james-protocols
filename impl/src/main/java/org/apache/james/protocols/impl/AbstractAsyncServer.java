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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Abstract base class for Servers which want to use async io
 *
 */
public abstract class AbstractAsyncServer {

    public static final int DEFAULT_IO_WORKER_COUNT = Runtime.getRuntime().availableProcessors() * 2;
    private int backlog = 250;
    
    private int port;

    private int timeout = 120;

    private ServerBootstrap bootstrap;

    private boolean started;

    private String ip;
    
    private ChannelGroup channels = new DefaultChannelGroup();

    private int ioWorker = DEFAULT_IO_WORKER_COUNT;
    
    /**
     * Set the ip on which the Server should listen on
     * 
     * @param ip
     */
    public synchronized void setIP(String ip) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.ip = ip;
    }
    
    
    /**
     * Set the port on which the Server should listen on
     * 
     * @param ip
     */
    public synchronized void setPort(int port) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.port = port;
    }
    
    /**
     * Set the IO-worker thread count to use. Default is nCores * 2
     * 
     * @param ioWorker
     */
    public synchronized void setIoWorkerCount(int ioWorker) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.ioWorker = ioWorker;
    }
    
    /**
     * Return the IO worker thread count to use
     * 
     * @return ioWorker
     */
    public synchronized int getIoWorkerCount() {
        return ioWorker;
    }
    
    /**
     * Start the server
     * 
     * @throws Exception 
     * 
     */
    public synchronized void bind() throws Exception {
        if (started) throw new IllegalStateException("Server running already");

        if (port < 1) throw new RuntimeException("Please specify a port to which the server should get bound!");

        bootstrap = new ServerBootstrap(createSocketChannelFactory());
        ChannelPipelineFactory factory = createPipelineFactory(channels);
        
        // Configure the pipeline factory.
        bootstrap.setPipelineFactory(factory);

        // Bind and start to accept incoming connections.
        bootstrap.setOption("backlog", backlog);
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        Channel serverChannel;
        if (getIP() == null) {
            serverChannel = bootstrap.bind(new InetSocketAddress(port));
        } else {
            serverChannel = bootstrap.bind(new InetSocketAddress(ip, port));
        }
          
        channels.add(serverChannel);
        started = true;

    }

    protected ServerSocketChannelFactory createSocketChannelFactory() {
        return new NioServerSocketChannelFactory(createBossExecutor(), createWorkerExecutor(), ioWorker);
    }
    
    /**
     * Stop the server
     */
    public synchronized void unbind() {
        if (started == false) return;
        channels.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
        started = false;
    }
    
    
    
    /**
     * Return the ip on which the server listen for connections
     * 
     * @return ip
     */
    public synchronized String getIP() {
        return ip;
    }
    
    /**
     * Return the port this server will listen on
     * 
     * @return port
     */
    public synchronized int getPort() {
        return port;
    }
    
    
    
    /**
     * Create ChannelPipelineFactory to use by this Server implementation
     * 
     * @return factory
     */
    protected abstract ChannelPipelineFactory createPipelineFactory(ChannelGroup group);

    /**
     * Set the read/write timeout for the server. This will throw a {@link IllegalStateException} if the
     * server is running.
     * 
     * @param timeout
     */
    public synchronized void setTimeout(int timeout) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.timeout = timeout;
    }
    
    
    /**
     * Set the Backlog for the socket. This will throw a {@link IllegalStateException} if the server is running.
     * 
     * @param backlog
     */
    public synchronized void setBacklog(int backlog) {
        if (started) throw new IllegalStateException("Can only be set when the server is not running");
        this.backlog = backlog;
    }
    
    /**
     * Return the backlog for the socket
     * 
     * @return backlog
     */
    public synchronized int getBacklog() {
        return backlog;
    }
    
    /**
     * Return the read/write timeout for the socket.
     * @return
     */
    public synchronized int getTimeout() {
        return timeout;
    }
    
    /**
     * Create a new {@link Executor} used for dispatch messages to the workers. One Thread will be used per port which is bound.
     * This can get overridden if needed, by default it use a {@link Executors#newCachedThreadPool()}
     * 
     * @return bossExecutor
     */
    protected Executor createBossExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Create a new {@link Executor} used for workers. This can get overridden if needed, by default it use a {@link Executors#newCachedThreadPool()}
     * 
     * @return workerExecutor
     */
    protected Executor createWorkerExecutor() {
        return Executors.newCachedThreadPool();
    }
    
    /**
     * return true if the server is bound 
     * 
     * @return bound
     */
    public synchronized boolean isBound() {
        return started;
    }
}
