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
package org.apache.james.protocols.smtp;

import java.util.Collection;

import org.apache.james.protocols.api.ProtocolSessionImpl;
import org.apache.james.protocols.api.ProtocolTransport;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.LineHandler;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.mailet.MailAddress;
import org.slf4j.Logger;

/**
 * {@link SMTPSession} implementation
 */
public class SMTPSessionImpl extends ProtocolSessionImpl implements SMTPSession {
    public final static String SMTP_SESSION = "SMTP_SESSION";

    private boolean relayingAllowed;


    private SMTPConfiguration theConfigData;

    public SMTPSessionImpl(SMTPConfiguration theConfigData, Logger logger, ProtocolTransport transport) {
        super(logger, transport);
        this.theConfigData = theConfigData;
        relayingAllowed = theConfigData.isRelayingAllowed(getRemoteIPAddress());
    }


    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#isRelayingAllowed()
     */
    public boolean isRelayingAllowed() {
        return relayingAllowed;
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#resetState()
     */
    public void resetState() {
        // remember the ehlo mode between resets
        Object currentHeloMode = getState().get(CURRENT_HELO_MODE);

        getState().clear();

        // start again with the old helo mode
        if (currentHeloMode != null) {
            getState().put(CURRENT_HELO_MODE, currentHeloMode);
        }
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#popLineHandler()
     */
    public void popLineHandler() {
        getProtocolTransport().popLineHandler();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#pushLineHandler(LineHandler)
     */
    public void pushLineHandler(LineHandler<SMTPSession> overrideCommandHandler) {
        getProtocolTransport().pushLineHandler(overrideCommandHandler, this);
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#getHelloName()
     */
    public String getHelloName() {
        return theConfigData.getHelloName();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#getMaxMessageSize()
     */
    public long getMaxMessageSize() {
        return theConfigData.getMaxMessageSize();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#getRcptCount()
     */
    @SuppressWarnings("unchecked")
    public int getRcptCount() {
        int count = 0;

        // check if the key exists
        if (getState().get(SMTPSession.RCPT_LIST) != null) {
            count = ((Collection<MailAddress>) getState().get(SMTPSession.RCPT_LIST)).size();
        }

        return count;
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#getSMTPGreeting()
     */
    public String getSMTPGreeting() {
        return theConfigData.getSMTPGreeting();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#isAuthSupported()
     */
    public boolean isAuthSupported() {
        return theConfigData.isAuthRequired(getRemoteAddress().getAddress().getHostAddress());
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#setRelayingAllowed(boolean)
     */
    public void setRelayingAllowed(boolean relayingAllowed) {
        this.relayingAllowed = relayingAllowed;
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#useAddressBracketsEnforcement()
     */
    public boolean useAddressBracketsEnforcement() {
        return theConfigData.useAddressBracketsEnforcement();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPSession#useHeloEhloEnforcement()
     */
    public boolean useHeloEhloEnforcement() {
        return theConfigData.useHeloEhloEnforcement();
    }

    /**
     * @see
     * org.apache.james.protocols.smtp.SMTPSession#getPushedLineHandlerCount()
     */
    public int getPushedLineHandlerCount() {
        return getProtocolTransport().getPushedLineHandlerCount();
    }

    public Response newLineTooLongResponse() {
        return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, "Line length exceeded. See RFC 2821 #4.5.3.1.");
    }

    public Response newFatalErrorResponse() {
        return new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to process request");
    }
}
