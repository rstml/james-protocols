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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import static junit.framework.Assert.*;

import org.apache.james.protocols.api.ProtocolSession.State;
import org.apache.james.protocols.smtp.BaseFakeDNSService;
import org.apache.james.protocols.smtp.BaseFakeSMTPSession;
import org.apache.james.protocols.smtp.DNSService;
import org.apache.james.protocols.smtp.MailAddress;
import org.apache.james.protocols.smtp.MailAddressException;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.fastfail.ValidSenderDomainHandler;
import org.apache.james.protocols.smtp.hook.HookReturnCode;
import org.junit.Test;

public class ValidSenderDomainHandlerTest {
    
    private DNSService setupDNSServer() {
    	DNSService dns = new BaseFakeDNSService(){

            public Collection<String> findMXRecords(String hostname) {
                Collection<String> mx = new ArrayList<String>();
                if (hostname.equals("test.james.apache.org")) {
                    mx.add("mail.james.apache.org");
                }
                return mx;
            }
            
        };
        return dns;
    }
    
    private SMTPSession setupMockedSession(final MailAddress sender) {
        SMTPSession session = new BaseFakeSMTPSession() {
            HashMap<String,Object> map = new HashMap<String,Object>();

            public Map<String,Object> getState() {

                map.put(SMTPSession.SENDER, sender);

                return map;
            }
            
            public boolean isRelayingAllowed() {
                return false;
            }
            /*
             * (non-Javadoc)
             * @see org.apache.james.protocols.api.ProtocolSession#setAttachment(java.lang.String, java.lang.Object, org.apache.james.protocols.api.ProtocolSession.State)
             */
            public Object setAttachment(String key, Object value, State state) {
                if (state == State.Connection) {
                    throw new UnsupportedOperationException();

                } else {
                    if (value == null) {
                        return getState().remove(key);
                    } else {
                        return getState().put(key, value);
                    }
                }
            }

            /*
             * (non-Javadoc)
             * @see org.apache.james.protocols.api.ProtocolSession#getAttachment(java.lang.String, org.apache.james.protocols.api.ProtocolSession.State)
             */
            public Object getAttachment(String key, State state) {
                if (state == State.Connection) {
                    throw new UnsupportedOperationException();
                } else {
                    return getState().get(key);
                }
            }
            
        };
        return session;
    }
    
    
    // Test for JAMES-580
    @Test
    public void testNullSenderNotReject() {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(setupMockedSession(null),null).getResult();
        
        assertEquals("Not blocked cause its a nullsender",response,HookReturnCode.DECLINED);
    }

    @Test
    public void testInvalidSenderDomainReject() throws MailAddressException {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        SMTPSession session = setupMockedSession(new MailAddress("invalid@invalid"));
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(session,(MailAddress) session.getAttachment(SMTPSession.SENDER, State.Transaction)).getResult();
        
        assertEquals("Blocked cause we use reject action", response,HookReturnCode.DENY);
    }
}
