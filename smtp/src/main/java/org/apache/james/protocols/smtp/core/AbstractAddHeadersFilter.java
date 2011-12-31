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
package org.apache.james.protocols.smtp.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.james.protocols.api.ProtocolSession.State;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.LineHandler;
import org.apache.james.protocols.smtp.SMTPSession;

/**
 * Abstract base class for {@link SeparatingDataLineFilter} implementations that add headers to a message
 * 
 *
 */
public abstract class AbstractAddHeadersFilter extends SeparatingDataLineFilter{

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    
    private final String headersPrefixAdded = "HEADERS_PREFIX_ADDED" + COUNTER.incrementAndGet();
    
    enum Location{
        Prefix,
        Suffix
    }
    
    /**
     * Return the {@link Location} to add the headers in
     * 
     * @return location
     */
    protected abstract Location getLocation();
    
    
    @Override
    protected Response onSeparatorLine(SMTPSession session, ByteBuffer line, LineHandler<SMTPSession> next) {
        if (getLocation() == Location.Suffix && session.getAttachment(headersPrefixAdded, State.Transaction) == null) { 
            session.setAttachment(headersPrefixAdded, Boolean.TRUE, State.Transaction);
            return addHeaders(session, line, next);
        }
        return super.onSeparatorLine(session, line, next);
    }

    @Override
    protected Response onHeadersLine(SMTPSession session, ByteBuffer line, LineHandler<SMTPSession> next) {
        if (getLocation() == Location.Prefix) {
            return addHeaders(session, line, next);
        }
        return super.onHeadersLine(session, line, next);
    }
   
    /**
     * Add headers to the message
     * 
     * @param session
     * @param line
     * @param next
     * @return response
     */
    private Response addHeaders(SMTPSession session, ByteBuffer line, LineHandler<SMTPSession> next) {
        Response response;
        for (Header header: headers(session)) {
            response = header.transferTo(session, next);
            if (response != null) {
                return response;
            }
        }
        return next.onLine(session, line);
    }
    
    /**
     * Return the {@link Header}'s to operate on
     * 
     * @return headers
     */
    protected abstract Collection<Header> headers(SMTPSession session);
    
    public final static class Header {
        public final String name;
        public final String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String toString() {
             return name + ": " + value + "\r\n";
        }
        
        /**
         * Transfer the content of the {@link Header} to the given {@link LineHandler}.
         * 
         * This is done for each line of the {@link Header} until the end is reached or the {@link LineHandler#onLine(org.apache.james.protocols.api.ProtocolSession, ByteBuffer)}
         * return <code>non-null</code>
         * 
         * @param session
         * @param handler
         * @return response
         */
        public Response transferTo(SMTPSession session, LineHandler<SMTPSession> handler) {
            try {
                String[] lines = toString().split("\r\n");
                Response response = null;
                for (int i = 0; i < lines.length; i++) {
                     response = handler.onLine(session, ByteBuffer.wrap((lines[i] + "\r\n").getBytes("US-ASCII")));
                     if (response != null) {
                         break;
                     }
                }
                return response;
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("NO US-ASCII ?", e);
            }
        }
    }
    
    
}
