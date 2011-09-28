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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.dsn.DSNStatus;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.hook.UnknownHook;

/**
  * Default command handler for handling unknown commands
  */
public class UnknownCmdHandler extends AbstractHookableCmdHandler<UnknownHook>{

    /**
     * The name of the command handled by the command handler
     */
    public static final String UNKNOWN_COMMAND = "UNKNOWN";

    /**
     * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add(UNKNOWN_COMMAND);

        return implCommands;
    }

    @Override
    protected SMTPResponse doCoreCmd(SMTPSession session, String command, String parameters) {
        StringBuilder result = new StringBuilder();
        result.append(DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_CMD)).append(" Command ").append(command).append(" unrecognized.");
        return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, result);
    }

    @Override
    protected SMTPResponse doFilterChecks(SMTPSession session, String command, String parameters) {
        session.getState().put("CURR_COMMAND", command);
        return null;
    }

    @Override
    protected HookResult callHook(UnknownHook rawHook, SMTPSession session, String parameters) {
        return rawHook.doUnknown(session, (String) session.getState().get("CURR_COMMAND"));
    }

    @Override
    protected Class<UnknownHook> getHookInterface() {
        return UnknownHook.class;
    }
}
