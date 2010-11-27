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

import org.apache.james.protocols.api.ConnectHandler;
import org.apache.james.protocols.api.ConnectHandlerResultHandler;
import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.api.LineHandlerResultHandler;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

/**
 * This abstract {@link ChannelUpstreamHandler} handling the calling of ConnectHandler and LineHandlers
 * on the right events.
 * 
 *
 */
public abstract class AbstractChannelUpstreamHandler extends SimpleChannelUpstreamHandler implements ChannelAttributeSupport{
    
    private ProtocolHandlerChain chain;

    public AbstractChannelUpstreamHandler(ProtocolHandlerChain chain) {
        this.chain = chain;
    }


    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        attributes.set(ctx.getChannel(),createSession(ctx));
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
        ProtocolSession session = (ProtocolSession) attributes.get(ctx.getChannel());
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                ConnectHandler cHandler = connectHandlers.get(i);
                
                long start = System.currentTimeMillis();
                boolean disconnect = connectHandlers.get(i).onConnect(session);
                long executionTime = System.currentTimeMillis() - start;
                
                for (int a = 0; a < resultHandlers.size(); a++) {
                    disconnect = resultHandlers.get(i).onResponse(session, disconnect, executionTime, cHandler);
                }
                if (disconnect)  {
                    ctx.getChannel().disconnect();
                    break;
                }
            }
        }
        super.channelConnected(ctx, e);
    }



    /**
     * Call the {@link LineHandler} 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ProtocolSession pSession = (ProtocolSession) attributes.get(ctx.getChannel());
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
            boolean disconnect = lHandler.onLine(pSession,line);
            long executionTime = System.currentTimeMillis() - start;

            for (int i = 0; i < resultHandlers.size(); i++) {
                disconnect = resultHandlers.get(i).onResponse(pSession, disconnect, executionTime, lHandler);
            }
            if (disconnect) ctx.getChannel().disconnect();

        }
        
        super.messageReceived(ctx, e);
    }


    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        cleanup(ctx.getChannel());
        
        super.channelClosed(ctx, e);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if ((e.getCause() instanceof TooLongFrameException) == false) {
            cleanup(ctx.getChannel());
        }
    }

    /**
     * Cleanup the channel
     * 
     * @param channel
     */
    protected void cleanup(Channel channel) {
        ProtocolSession session = (ProtocolSession) attributes.remove(channel);
        if (session != null) {
            session.resetState();
            session = null;
        }
    }

    /**
     * Create a new "protocol" session 
     * 
     * @param session ioSession
     * @return ctx
     * @throws Exception
     */
    protected abstract ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception;


}
