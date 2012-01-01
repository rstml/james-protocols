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
package org.apache.james.protocols.pop3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.james.protocols.api.handler.WiringException;
import org.apache.james.protocols.netty.NettyServer;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MailboxFactory;

import org.junit.Test;

public class POP3ServerTest {

    private POP3Protocol createProtocol(MailboxFactory factory) throws WiringException {
        return new POP3Protocol(new POP3ProtocolHandlerChain(factory), new POP3Configuration(), new MockLogger());
    }
    @Test
    public void testInvalidAuth() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            server = new NettyServer(createProtocol(new MockMailboxFactory()));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertFalse(client.login("invalid", "invalid"));
           
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    private final class MockMailboxFactory implements MailboxFactory {

        public Mailbox getMailbox(POP3Session session, String password) throws IOException {
            return null;
        }
        
    }
}
