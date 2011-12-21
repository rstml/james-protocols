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

package org.apache.james.protocols.api;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;


import org.apache.james.protocols.api.FutureResponse.ResponseListener;


/**
 * Abstract base class for {@link ProtocolTransport} implementation which already takes care of all the complex
 * stuff when handling {@link Response}'s. 
 * 
 * 
 *
 */
public abstract class AbstractProtocolTransport implements ProtocolTransport{
    
    private final static String CRLF = "\r\n";

    
    // TODO: Should we limit the size ?
    private final ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<Response>();
    private final AtomicBoolean write = new AtomicBoolean(false);
    
    /**
     * @see org.apache.james.protocols.api.ProtocolTransport#writeResponse(org.apache.james.protocols.api.Response, org.apache.james.protocols.api.ProtocolSession)
     */
    public final void writeResponse(Response response, final ProtocolSession session) {
        // just add the response to the queue. We will trigger the write operation later
        responses.add(response);
             
        // trigger the write
        writeQueuedResponses(session);
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
        Response queuedResponse = null;
        
        if (write.compareAndSet(false, true)){
            boolean listenerAdded = false;
            // dequeue Responses until non is left
            while ((queuedResponse = responses.poll()) != null) {
                    
                // check if we need to take special care of FutureResponses
                if (queuedResponse instanceof FutureResponse) {
                    FutureResponse futureResponse =(FutureResponse) queuedResponse;
                    if (futureResponse.isReady()) {
                        // future is ready so we can write it without blocking the IO-Thread
                        writeResponseToClient(queuedResponse, session);
                    } else {
                            
                        // future is not ready so we need to write it via a ResponseListener otherwise we MAY block the IO-Thread
                        futureResponse.addListener(new ResponseListener() {
                                
                            public void onResponse(FutureResponse response) {
                                writeResponseToClient(response, session);
                                if (write.compareAndSet(true, false)) {
                                    writeQueuedResponses(session);
                                }
                            }
                        });
                        listenerAdded = true;
                        // just break here as we will trigger the dequeue later
                        break;
                    }
                        
                } else {
                    // the Response is not a FutureResponse, so just write it back the the remote peer
                    writeResponseToClient(queuedResponse, session);
                }
                
            }
            // Check if a ResponseListener was added before. If not we can allow to write
            // responses again. Otherwise the writing will get triggered from the listener
            if (listenerAdded == false) {
                write.set(false);
            }
        }

        
    }
    
    /**
     * Write the {@link Response} to the client
     * 
     * @param response
     * @param session
     */
    protected void writeResponseToClient(Response response, ProtocolSession session) {
        if (response != null) {
            boolean startTLS = false;
            if (response instanceof StartTlsResponse) {
                if (isStartTLSSupported()) {
                    startTLS = true;
                }
            }
            
            
            if (response instanceof StreamResponse) {
                writeToClient(toBytes(response), session, false);
                writeToClient(((StreamResponse) response).getStream(), session, startTLS);
            } else {
                writeToClient(toBytes(response), session, startTLS);
            }
            // reset state on starttls
            if (startTLS) {
                session.resetState();
            }
            
            if (response.isEndSession()) {
                // close the channel if needed after the message was written out
                close();
           } 
         }        
    }
    

    /**
     * Take the {@link Response} and encode it to a <code>byte</code> array
     * 
     * @param response
     * @return bytes
     */
    protected static byte[] toBytes(Response response) {
        StringBuilder builder = new StringBuilder();
        List<CharSequence> lines = response.getLines();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(lines.get(i));
            if (i < lines.size()) {
                builder.append(CRLF);
            }
        }
        try {
            return builder.toString().getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No US-ASCII ?");
        }
    }
    

    /**
     * Write the given <code>byte's</code> to the remote peer
     * 
     * @param bytes    the bytes to write 
     * @param session  the {@link ProtocolSession} for the write request
     * @param startTLS true if startTLS should be started after the bytes were written to the client
     */
    protected abstract void writeToClient(byte[] bytes, ProtocolSession session, boolean startTLS);
    
    /**
     * Write the given {@link InputStream} to the remote peer
     * 
     * @param in       the {@link InputStream} which should be written back to the client
     * @param session  the {@link ProtocolSession} for the write request
     * @param startTLS true if startTLS should be started after the {@link InputStream} was written to the client
     */
    protected abstract void writeToClient(InputStream in, ProtocolSession session, boolean startTLS);

    
    /**
     * Close the Transport
     */
    protected abstract void close();
}

