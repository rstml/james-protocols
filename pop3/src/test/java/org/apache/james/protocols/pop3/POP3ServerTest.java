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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.protocols.api.handler.WiringException;
import org.apache.james.protocols.netty.NettyServer;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MailboxFactory;
import org.apache.james.protocols.pop3.mailbox.MessageMetaData;

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
    
    @Test
    public void testEmptyInbox() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            MockMailboxFactory factory = new MockMailboxFactory();
            
            factory.add("valid", new MockMailbox(identifier));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            POP3MessageInfo[] info = client.listMessages();
            assertEquals(0, info.length);
            
            info = client.listUniqueIdentifiers();
            assertEquals(0, info.length);
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    private final class MockMailboxFactory implements MailboxFactory {
        private final Map<String, Mailbox> mailboxes = new HashMap<String, Mailbox>();
       
        public void add(String username, Mailbox mailbox) {
            mailboxes.put(username, mailbox);
        }
        public Mailbox getMailbox(POP3Session session, String password) throws IOException {
            return mailboxes.get(session.getUser());
        }
        
    }
    
    private final class MockMailbox implements Mailbox {

        private final Map<Long, Message> messages = new HashMap<Long, POP3ServerTest.Message>();
        private final String identifier;

        public MockMailbox(String identifier, Message... messages) {
            this.identifier = identifier;
            for (Message m: messages) {
                this.messages.put(m.meta.getUid(), m);
            }
        }
        
        public MockMailbox(String identifier) {
            this(identifier, new Message[0]);
        }
        public InputStream getMessageBody(long uid) throws IOException {
            Message m = messages.get(uid);
            if (m == null) {
                return null;
            }
            return new ByteArrayInputStream(m.body.getBytes("US-ASCII"));
        }

        public InputStream getMessageHeaders(long uid) throws IOException {
            Message m = messages.get(uid);
            if (m == null) {
                return null;
            }
            return new ByteArrayInputStream((m.headers + "\r\n").getBytes("US-ASCII"));
        }

        public InputStream getMessage(long uid) throws IOException {
            InputStream body = getMessageBody(uid);
            InputStream headers = getMessageHeaders(uid);
            if (body == null || headers == null) {
                return null;
            }
            return new SequenceInputStream(headers, body);
        }

        public List<MessageMetaData> getMessages() throws IOException {
            List<MessageMetaData> meta = new ArrayList<MessageMetaData>();
            for (Message m: messages.values()) {
                meta.add(m.meta);
            }
            return meta;
        }

        public void remove(long... uids) throws IOException {
            for (long uid: uids) {
                messages.remove(uid);
            }
        }

        public String getIdentifier() throws IOException {
            return identifier;
        }

        public void close() throws IOException {
            // nothing
        }
        
    }
    
    private static final class Message {
        private static final AtomicLong UIDS = new AtomicLong(0);
        public final String headers;
        public final String body;
        public final MessageMetaData meta;

        public Message(String headers, String body) {
            this.headers = headers;
            this.body = body;
            this.meta = new MessageMetaData(UIDS.incrementAndGet(), headers.length() + body.length() + 2);
        }
        
    }
}
