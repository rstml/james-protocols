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

import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.api.ProtocolSession;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * {@link ChannelUpstreamHandler} implementation which will call a given {@link LineHandler} implementation
 *
 * @param <S>
 */
public class LineHandlerUpstreamHandler<S extends ProtocolSession> extends SimpleChannelUpstreamHandler {

    private final LineHandler<S> handler;
    private final S session;
    
    public LineHandlerUpstreamHandler(S session, LineHandler<S> handler) {
        this.handler = handler;
        this.session = session;
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {        
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();      
        byte[] line;
        if (buf.hasArray()) {
            line = buf.array();
        } else {
            // copy the ChannelBuffer to a byte array to process the LineHandler
            line = new byte[buf.capacity()];
            buf.getBytes(0, line);
        }

        boolean disconnect = handler.onLine(session, line);
        if (disconnect) ctx.getChannel().write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        
    }

}
