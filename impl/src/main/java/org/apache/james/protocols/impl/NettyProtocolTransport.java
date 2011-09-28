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
import java.util.LinkedList;

import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.FutureResponse;
import org.apache.james.protocols.api.FutureResponse.ResponseListener;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.ProtocolTransport;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.StartTlsResponse;
import org.apache.james.protocols.api.handler.LineHandler;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * A Netty implementation of a ProtocolTransport
 */
public class NettyProtocolTransport implements ProtocolTransport {
    
    private final Channel channel;
    private final SSLEngine engine;
    private int lineHandlerCount = 0;
    
    // TODO: Should we limit the size ?
    private final LinkedList<Response> responses = new LinkedList<Response>();
    
    public NettyProtocolTransport(Channel channel, SSLEngine engine) {
        this.channel = channel;
        this.engine = engine;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#getRemoteAddress()
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#getId()
     */
    public String getId() {
        return channel.getId() + "";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#isTLSStarted()
     */
    public boolean isTLSStarted() {
        if (isStartTLSSupported()) {
            return channel.getPipeline().get("sslHandler") != null;
        } 
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return engine != null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#writeResponse(org.apache.james.protocols.api.Response, org.apache.james.protocols.api.ProtocolSession)
     */
    public void writeResponse(Response response, final ProtocolSession session) {
        synchronized (responses) {
            // just add the response to the queue. We will trigger the write operation later
            responses.add(response);
             
            // trigger the write
            writeQueuedResponses(session);
        }

    }
    
    /**
     * Helper method which tries to write all queued {@link Response}'s to the remote client. This method is aware of {@link FutureResponse} and makes sure the {@link Response}'s are written
     * in the correct order
     * 
     * This is related to PROTOCOLS-36
     * 
     * @param session
     */
    private  void writeQueuedResponses(final ProtocolSession session) {
        synchronized (responses) {
            Response queuedResponse = null;
            
            // dequeue Responses until non is left
            while ((queuedResponse = responses.poll()) != null) {
                
                // check if we need to take special care of FutureResponses
                if (queuedResponse instanceof FutureResponse) {
                    FutureResponse futureResponse =(FutureResponse) queuedResponse;
                    if (futureResponse.isReady()) {
                        // future is ready so we can write it without blocking the IO-Thread
                        writeResponseToChannel(queuedResponse, session);
                    } else {
                        
                        // future is not ready so we need to write it via a ResponseListener otherwise we MAY block the IO-Thread
                        futureResponse.addListener(new ResponseListener() {
                            
                            public void onResponse(FutureResponse response) {
                                writeResponseToChannel(response, session);
                                writeQueuedResponses(session);
                            }
                        });
                        
                        // just break here as we will trigger the dequeue later
                        break;
                    }
                    
                } else {
                    // the Response is not a FutureResponse, so just write it back the the remote peer
                    writeResponseToChannel(queuedResponse, session);
                }
            }
        }
        
    }
    
    private void writeResponseToChannel(Response response, ProtocolSession session) {
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
                    session.resetState();
                    channel.getPipeline().addFirst("sslHandler", filter);
                    channel.setReadable(true);
                }
            }
         }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#popLineHandler()
     */
    public void popLineHandler() {
        if (lineHandlerCount > 0) {
            channel.getPipeline().remove("lineHandler" + lineHandlerCount);
            lineHandlerCount--;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#pushLineHandler(org.apache.james.protocols.api.handler.LineHandler, org.apache.james.protocols.api.ProtocolSession)
     */
    public <T extends ProtocolSession> void pushLineHandler(LineHandler<T> overrideCommandHandler,
            T session) {
        lineHandlerCount++;
        // Add the linehandler in front of the coreHandler so we can be sure 
        // it is executed with the same ExecutorHandler as the coreHandler (if one exist)
        // 
        // See JAMES-1277
        channel.getPipeline().addBefore("coreHandler", "lineHandler" + lineHandlerCount, new LineHandlerUpstreamHandler<T>(session, overrideCommandHandler));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolTransport#getPushedLineHandlerCount()
     */
    public int getPushedLineHandlerCount() {
        return lineHandlerCount;
    }
    
}
