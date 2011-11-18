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

package org.apache.james.protocols.smtp.core.esmtp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.AbstractHookableCmdHandler;
import org.apache.james.protocols.smtp.dsn.DSNStatus;
import org.apache.james.protocols.smtp.hook.HeloHook;
import org.apache.james.protocols.smtp.hook.HookResult;

/**
 * Handles EHLO command
 */
public class EhloCmdHandler extends AbstractHookableCmdHandler<HeloHook> implements EhloExtension{

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "EHLO";
    private final static Collection<String> COMMANDS = Collections.unmodifiableCollection(Arrays.asList(COMMAND_NAME));
    // see http://issues.apache.org/jira/browse/JAMES-419
    private final static List<String> ESMTP_FEATURES = Collections.unmodifiableList(Arrays.asList("PIPELINING", "ENHANCEDSTATUSCODES", "8BITMIME"));
    private List<EhloExtension> ehloExtensions;

    /**
     * Handler method called upon receipt of a EHLO command. Responds with a
     * greeting and informs the client whether client authentication is
     * required.
     * 
     * @param session
     *            SMTP session object
     * @param argument
     *            the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doEHLO(SMTPSession session, String argument) {
        SMTPResponse resp = new SMTPResponse(SMTPRetCode.MAIL_OK, new StringBuilder(session.getHelloName()).append(" Hello ").append(argument)
                .append(" [")
                .append(session.getRemoteAddress().getAddress().getHostAddress()).append("])"));
        
        session.getConnectionState().put(SMTPSession.CURRENT_HELO_MODE,
                COMMAND_NAME);

        processExtensions(session, resp);


 
        return resp;

    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        return COMMANDS;
    }

    /**
     * @see org.apache.james.protocols.api.handler.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> classes = super.getMarkerInterfaces();
        classes.add(EhloExtension.class);
        return classes;
    }

    /**
     * @see org.apache.james.protocols.api.handler.ExtensibleHandler#wireExtensions(java.lang.Class,
     *      java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) {
        super.wireExtensions(interfaceName, extension);
        if (EhloExtension.class.equals(interfaceName)) {
            this.ehloExtensions = extension;
        }
    }

    /**
     * Process the ehloExtensions
     * 
     * @param session SMTPSession 
     * @param resp SMTPResponse
     */
    private void processExtensions(SMTPSession session, SMTPResponse resp) {
        if (ehloExtensions != null) {
            int count = ehloExtensions.size();
            for (int i = 0; i < count; i++) {
                List<String> lines = ((EhloExtension) ehloExtensions.get(i))
                        .getImplementedEsmtpFeatures(session);
                if (lines != null) {
                    for (int j = 0; j < lines.size(); j++) {
                        resp.appendLine(lines.get(j));
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.james.protocols.smtp.core.AbstractHookableCmdHandler#doCoreCmd(org.apache.james.protocols.smtp.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doCoreCmd(SMTPSession session, String command,
            String parameters) {
        return doEHLO(session, parameters);
    }

    /**
     * @see org.apache.james.protocols.smtp.core.AbstractHookableCmdHandler#doFilterChecks(org.apache.james.protocols.smtp.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doFilterChecks(SMTPSession session, String command,
            String parameters) {
        session.resetState();

        if (parameters == null) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,
                    DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.DELIVERY_INVALID_ARG)
                            + " Domain address required: " + COMMAND_NAME);
        } else {
            // store provided name
            session.getState().put(SMTPSession.CURRENT_HELO_NAME, parameters);
            return null;
        }
    }

    /**
     * @see org.apache.james.protocols.smtp.core.AbstractHookableCmdHandler#getHookInterface()
     */
    protected Class<HeloHook> getHookInterface() {
        return HeloHook.class;
    }

    /**
     * {@inheritDoc}
     */
    protected HookResult callHook(HeloHook rawHook, SMTPSession session, String parameters) {
        return rawHook.doHelo(session, parameters);
    }



    /**
     * @see org.apache.james.protocols.smtp.core.esmtp.EhloExtension#getImplementedEsmtpFeatures(org.apache.james.protocols.smtp.SMTPSession)
     */
    public List<String> getImplementedEsmtpFeatures(SMTPSession session) {
        return ESMTP_FEATURES;
    }

}
