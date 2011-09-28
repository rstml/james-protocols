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


package org.apache.james.protocols.smtp.core.fastfail;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import org.apache.james.protocols.smtp.BaseFakeSMTPSession;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookReturnCode;

import junit.framework.TestCase;

public class MaxUnknownCmdHandlerTest extends TestCase{

    
    public void testRejectAndClose() throws ParseException {
        SMTPSession session = new BaseFakeSMTPSession() {
            private final HashMap<String,Object> state = new HashMap<String,Object>();

            public Map<String,Object> getState() {
                return state;
            }
        };
        
        
        MaxUnknownCmdHandler handler = new MaxUnknownCmdHandler();
        handler.setMaxUnknownCmdCount(2);
        int resp = handler.doUnknown(session, "what").getResult();
        assertEquals(HookReturnCode.DECLINED, resp);

        resp = handler.doUnknown(session, "what").getResult();
        assertEquals(HookReturnCode.DECLINED, resp);
        
        resp = handler.doUnknown(session, "what").getResult();
        assertEquals(HookReturnCode.DENY | HookReturnCode.DISCONNECT, resp);
    }
}
