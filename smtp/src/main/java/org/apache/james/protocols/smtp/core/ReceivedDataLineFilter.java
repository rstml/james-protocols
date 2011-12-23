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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.james.protocols.api.ProtocolSession.State;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.LineHandler;
import org.apache.james.protocols.smtp.MailAddress;
import org.apache.james.protocols.smtp.SMTPSession;

public class ReceivedDataLineFilter implements DataLineFilter {

    private final static String CHARSET = "US-ASCII";
    
    private static final ThreadLocal<DateFormat> DATEFORMAT = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            // See RFC822 for the format
            return new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z (zzz)", Locale.US);
        }
        
    };

    private final static String HEADERS_WRITTEN = "HEADERS_WRITTEN";



    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.smtp.core.DataLineFilter#onLine(org.apache.james.protocols.smtp.SMTPSession, java.nio.ByteBuffer, org.apache.james.protocols.api.handler.LineHandler)
     */
    public Response onLine(SMTPSession session,  ByteBuffer line, LineHandler<SMTPSession> next) {
        if (session.getAttachment(HEADERS_WRITTEN, State.Transaction) == null) {
            Response response = addNewReceivedMailHeaders(session, next);

            session.setAttachment(HEADERS_WRITTEN, true, State.Transaction);
            
            if (response != null) {
                return response;
            }
        }
        Response resp =  next.onLine(session, line);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private Response addNewReceivedMailHeaders(SMTPSession session, LineHandler<SMTPSession> next) {
        try {
            StringBuilder headerLineBuffer = new StringBuilder();

            String heloMode = (String) session.getAttachment(SMTPSession.CURRENT_HELO_MODE, State.Connection);
            String heloName = (String) session.getAttachment(SMTPSession.CURRENT_HELO_NAME, State.Connection);

            // Put our Received header first
            headerLineBuffer.append("Received: from ").append(session.getRemoteAddress().getHostName());

            if (heloName != null) {
                headerLineBuffer.append(" (").append(heloMode).append(" ").append(heloName).append(") ");
            }

            headerLineBuffer.append(" ([").append(session.getRemoteAddress().getAddress().getHostAddress()).append("])").append("\r\n");

            Response response = next.onLine(session, ByteBuffer.wrap(headerLineBuffer.toString().getBytes(CHARSET)));
            if (response != null) {
                return response;
            }
            headerLineBuffer.delete(0, headerLineBuffer.length());

            headerLineBuffer.append("          by ").append(session.getConfiguration().getHelloName()).append(" (").append(session.getConfiguration().getSoftwareName()).append(") with ").append(getServiceType(session, heloMode));

           
            headerLineBuffer.append(" ID ").append(session.getSessionID());

            if (((Collection<?>) session.getAttachment(SMTPSession.RCPT_LIST, State.Transaction)).size() == 1) {
                // Only indicate a recipient if they're the only recipient
                // (prevents email address harvesting and large headers in
                // bulk email)
                headerLineBuffer.append("\r\n");
                next.onLine(session, ByteBuffer.wrap(headerLineBuffer.toString().getBytes(CHARSET)));
                headerLineBuffer.delete(0, headerLineBuffer.length());

                headerLineBuffer.delete(0, headerLineBuffer.length());
                headerLineBuffer.append("          for <").append(((List<MailAddress>) session.getAttachment(SMTPSession.RCPT_LIST, State.Transaction)).get(0).toString()).append(">;").append("\r\n");
                response = next.onLine(session, ByteBuffer.wrap(headerLineBuffer.toString().getBytes(CHARSET)));

                if (response != null) {
                    return response;
                }
                headerLineBuffer.delete(0, headerLineBuffer.length());
                headerLineBuffer.delete(0, headerLineBuffer.length());

            } else {
                // Put the ; on the end of the 'by' line
                headerLineBuffer.append(";");
                headerLineBuffer.append("\r\n");

                response = next.onLine(session, ByteBuffer.wrap(headerLineBuffer.toString().getBytes(CHARSET)));
                if (response != null) {
                    return response;
                }
                headerLineBuffer.delete(0, headerLineBuffer.length());
            }
            headerLineBuffer = null;
            return next.onLine(session, ByteBuffer.wrap(("          " + DATEFORMAT.get().format(new Date()) + "\r\n").getBytes(CHARSET)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No US-ASCII support ?");
        }
    }
    
    
    /**
     * Return the service type which will be used in the Received headers
     * 
     * @param session
     * @param heloMode
     * @return type
     */
    protected String getServiceType(SMTPSession session, String heloMode) {
     // Check if EHLO was used
        if ("EHLO".equals(heloMode)) {
            // Not successful auth
            if (session.getUser() == null) {
                return "ESMTP";
            } else {
                // See RFC3848
                // The new keyword "ESMTPA" indicates the use of ESMTP when
                // the
                // SMTP
                // AUTH [3] extension is also used and authentication is
                // successfully
                // achieved.
                return "ESMTPA";
            }
        } else {
            return "SMTP";
        }
    }
}
