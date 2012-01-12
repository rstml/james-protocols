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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3Reply;
import org.apache.james.protocols.api.handler.WiringException;
import org.apache.james.protocols.netty.NettyServer;
import org.apache.james.protocols.pop3.core.AbstractApopCmdHandler;
import org.apache.james.protocols.pop3.core.AbstractPassCmdHandler;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MessageMetaData;

import org.junit.Test;

public class POP3ServerTest {

    private static final Message MESSAGE1 = new Message("Subject: test\r\nX-Header: value\r\n", "My Body\r\n");
    private static final Message MESSAGE2 = new Message("Subject: test2\r\nX-Header: value2\r\n", "My Body with a DOT.\r\n.\r\n");

    private POP3Protocol createProtocol(AbstractPassCmdHandler handler) throws WiringException {
        return new POP3Protocol(new POP3ProtocolHandlerChain(handler), new POP3Configuration(), new MockLogger());
    }
    @Test
    public void testInvalidAuth() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            server = new NettyServer(createProtocol(new TestPassCmdHandler()));
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
            TestPassCmdHandler handler = new TestPassCmdHandler();
            
            handler.add("valid", new MockMailbox(identifier));
            server = new NettyServer(createProtocol(handler));
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
    
    @Test
    public void testInboxWithMessages() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler handler = new TestPassCmdHandler();
            
            handler.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(handler));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            POP3MessageInfo[] info = client.listMessages();
            assertEquals(2, info.length);
            assertEquals((int) MESSAGE1.meta.getSize(), info[0].size);
            assertEquals((int) MESSAGE2.meta.getSize(), info[1].size);
            assertEquals(1, info[0].number);
            assertEquals(2, info[1].number);

            POP3MessageInfo mInfo = client.listMessage(1);
            assertEquals((int) MESSAGE1.meta.getSize(), mInfo.size);
            assertEquals(1, mInfo.number);

            // try to retrieve message that not exist
            mInfo = client.listMessage(10);
            assertNull(mInfo);

            info = client.listUniqueIdentifiers();
            assertEquals(2, info.length);
            assertEquals(identifier + "-" + MESSAGE1.meta.getUid(), info[0].identifier);
            assertEquals(identifier + "-" + MESSAGE2.meta.getUid(), info[1].identifier);
            assertEquals(1, info[0].number);
            assertEquals(2, info[1].number);

            mInfo = client.listUniqueIdentifier(1);
            assertEquals(identifier + "-" + MESSAGE1.meta.getUid(), mInfo.identifier);
            assertEquals(1, mInfo.number);

            // try to retrieve message that not exist
            mInfo = client.listUniqueIdentifier(10);
            assertNull(mInfo);
            
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testRetr() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            Reader reader = client.retrieveMessage(1);
            assertNotNull(reader);
            checkMessage(MESSAGE1, reader);
            reader.close();
            
            // does not exist
            reader = client.retrieveMessage(10);
            assertNull(reader);
            
            
            // delete and check for the message again, should now be deleted
            assertTrue(client.deleteMessage(1));
            reader = client.retrieveMessage(1);
            assertNull(reader);

            
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testTop() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            Reader reader = client.retrieveMessageTop(1, 1000);
            assertNotNull(reader);
            checkMessage(MESSAGE1, reader);
            reader.close();
            
            reader = client.retrieveMessageTop(2, 1);
            assertNotNull(reader);
            checkMessage(MESSAGE2, reader,1);
            reader.close();
            
            // does not exist
            reader = client.retrieveMessageTop(10,100);
            assertNull(reader);
            
            // delete and check for the message again, should now be deleted
            assertTrue(client.deleteMessage(1));
            reader = client.retrieveMessageTop(1, 1000);
            assertNull(reader);

            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testDele() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            POP3MessageInfo[] info = client.listMessages();
            assertEquals(2, info.length);
            
            assertTrue(client.deleteMessage(1));
            info = client.listMessages();
            assertEquals(1, info.length);

            
            assertFalse(client.deleteMessage(1));
            info = client.listMessages();
            assertEquals(1, info.length);
            
            
            assertTrue(client.deleteMessage(2));
            info = client.listMessages();
            assertEquals(0, info.length);
            
            // logout so the messages get expunged
            assertTrue(client.logout());

            client.connect(address.getAddress().getHostAddress(), address.getPort());
  
            assertTrue(client.login("valid", "valid"));
            info = client.listMessages();
            assertEquals(0, info.length);

            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testNoop() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            assertTrue(client.noop());
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testRset() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            assertEquals(1, client.listMessages().length);
            assertTrue(client.deleteMessage(1));
            assertEquals(0, client.listMessages().length);
            
            // call RSET. After this the deleted mark should be removed again
            assertTrue(client.reset());
            assertEquals(1, client.listMessages().length);

            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    @Test
    public void testStat() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            
            assertTrue(client.login("valid", "valid"));
            POP3MessageInfo info = client.status();
            assertEquals((int)(MESSAGE1.meta.getSize() + MESSAGE2.meta.getSize()), info.size);
            assertEquals(2, info.number);
            
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    @Test
    public void testDifferentStates() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            String identifier = "id";
            TestPassCmdHandler factory = new TestPassCmdHandler();
            
            factory.add("valid", new MockMailbox(identifier, MESSAGE1, MESSAGE2));
            server = new NettyServer(createProtocol(factory));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            assertNull(client.listMessages());
            assertNull(client.listUniqueIdentifiers());
            assertFalse(client.deleteMessage(1));
            assertNull(client.retrieveMessage(1));
            assertNull(client.retrieveMessageTop(1, 10));
            assertNull(client.status());
            assertFalse(client.reset());
            client.logout();
            
            client.connect(address.getAddress().getHostAddress(), address.getPort());

            assertTrue(client.login("valid", "valid"));
            assertNotNull(client.listMessages());
            assertNotNull(client.listUniqueIdentifiers());
            Reader reader = client.retrieveMessage(1);
            assertNotNull(reader);
            reader.close();
            assertNotNull(client.status());
            reader = client.retrieveMessageTop(1, 1);
            assertNotNull(reader);
            reader.close();
            assertTrue(client.deleteMessage(1));
            assertTrue(client.reset());

            assertTrue(client.logout());

        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    
    
    @Test
    public void testAPop() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", TestUtils.getFreePort());
        
        NettyServer server = null;
        try {
            TestApopCmdHandler handler = new TestApopCmdHandler();
            server = new NettyServer(createProtocol(handler));
            server.setListenAddresses(address);
            server.bind();
            
            POP3Client client =  new POP3Client();
            client.connect(address.getAddress().getHostAddress(), address.getPort());
            String welcomeMessage = client.getReplyString();
            
            // check for valid syntax that include all info needed for APOP
            assertTrue(welcomeMessage.trim().matches("\\+OK \\<\\d+\\.\\d+@.+\\> .+"));
            
            int reply = client.sendCommand("APOP invalid invalid");
            assertEquals(POP3Reply.ERROR, reply);
            
            handler.add("valid", new MockMailbox("id"));
            reply = client.sendCommand("APOP valid valid");
            assertEquals(POP3Reply.OK, reply);
            
            assertTrue(client.logout());
           
        } finally {
            if (server != null) {
                server.unbind();
            }
        }
        
    }
    private void checkMessage(Message message, Reader reader) throws IOException {
        int read = 0;
        int i = -1;
        String content = message.toString();
        while ((i = reader.read()) != -1) {
            assertEquals(content.charAt(read++), (char)i);
        }
        assertEquals(content.length(), read);
    }
    
    private void checkMessage(Message message, Reader reader, int lines) throws IOException {
        int read = 0;
        String headers = message.headers + "\r\n";
        
        while (read < headers.length()) {
            assertEquals(headers.charAt(read++), reader.read());
        }
        assertEquals(headers.length(), read);
        
        BufferedReader bufReader = new BufferedReader(reader);
        String line = null;
        int linesRead = 0;
        String parts[] = message.body.split("\r\n");
        while ((line = bufReader.readLine()) != null) {
            assertEquals(parts[linesRead++], line);
            
            if (linesRead == lines) {
                break;
            }
        }
        
        assertEquals(lines, linesRead);
        
    }
    
    private final class TestPassCmdHandler extends AbstractPassCmdHandler {
        private final Map<String, Mailbox> mailboxes = new HashMap<String, Mailbox>();
       
        public void add(String username, Mailbox mailbox) {
            mailboxes.put(username, mailbox);
        }
        
        protected Mailbox auth(POP3Session session, String username, String password) throws Exception{
            return mailboxes.get(username);
        }

        
    }
    
    private final class TestApopCmdHandler extends AbstractApopCmdHandler {
        private final Map<String, Mailbox> mailboxes = new HashMap<String, Mailbox>();
       
        public void add(String username, Mailbox mailbox) {
            mailboxes.put(username, mailbox);
        }

        @Override
        protected Mailbox auth(POP3Session session, String apopTimestamp, String user, String digest) throws Exception {
            return mailboxes.get(user);
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
        
        public String toString() {
            return headers + "\r\n" + body;
        }
        
    }
}
