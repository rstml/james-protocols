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

package org.apache.james.protocols.pop3.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.pop3.POP3Response;
import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MailboxFactory;

/**
 * Handles PASS command
 */
public class PassCmdHandler extends RsetCmdHandler {
    private static final Collection<String> COMMANDS = Collections.unmodifiableCollection(Arrays.asList("PASS"));

    private MailboxFactory mailboxManager;

    public void setMailboxFactory(MailboxFactory manager) {
        this.mailboxManager = manager;
    }

    /**
     * Handler method called upon receipt of a PASS command. Reads in and
     * validates the password.
     */
    public Response onCommand(POP3Session session, Request request) {
        String parameters = request.getArgument();
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.AUTHENTICATION_USERSET && parameters != null) {
            String passArg = parameters;
            try {
                Mailbox mailbox = mailboxManager.getMailbox(session, passArg);
                if (mailbox != null) {
                	session.setUserMailbox(mailbox);
                	stat(session);
                
                	StringBuilder responseBuffer = new StringBuilder(64).append("Welcome ").append(session.getUser());
                	response = new POP3Response(POP3Response.OK_RESPONSE, responseBuffer.toString());
                	session.setHandlerState(POP3Session.TRANSACTION);
                } else {
                	response = new POP3Response(POP3Response.ERR_RESPONSE, "Authentication failed.");
                	session.setHandlerState(POP3Session.AUTHENTICATION_READY);
                }
            } catch (IOException e) {
                session.getLogger().error("Unexpected error accessing mailbox for " + session.getUser(), e);
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Unexpected error accessing mailbox");
                session.setHandlerState(POP3Session.AUTHENTICATION_READY);
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Authentication failed.");

            session.setHandlerState(POP3Session.AUTHENTICATION_READY);
        }

        return response;
    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
    	return COMMANDS;
    }

}
