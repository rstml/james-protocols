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
package org.apache.james.protocols.smtp.netty;


import java.nio.charset.Charset;

import javax.net.ssl.SSLContext;

import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.impl.AbstractAsyncServer;
import org.apache.james.protocols.impl.AbstractResponseEncoder;
import org.apache.james.protocols.impl.AbstractSSLAwareChannelPipelineFactory;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPProtocolHandlerChain;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPServerMBean;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NIO SMTPServer which use Netty
 */
public class SMTPServer extends AbstractAsyncServer implements SMTPServerMBean {

    private ProtocolHandlerChain chain;
    
    private Logger logger = LoggerFactory.getLogger(SMTPServer.class);

    private SSLContext context;

    private boolean starttls;
    
    private ExecutionHandler eHandler;

   
    /**
     * The configuration data to be passed to the handler
     */
    private final SMTPConfiguration theConfigData;


    private final static OneToOneEncoder SMTP_RESPONSE_ENCODER = new AbstractResponseEncoder(SMTPResponse.class, Charset.forName("US-ASCII"));
    private ChannelUpstreamHandler coreHandler;

   
    
    public SMTPServer(SMTPConfiguration theConfigData, SMTPProtocolHandlerChain chain) {
        this(theConfigData, chain, null, false);
    }
    
    
    public SMTPServer(SMTPConfiguration theConfigData, SMTPProtocolHandlerChain chain, SSLContext context, boolean starttls) {
        super();
        this.chain  = chain;
        this.context = context;
        this.starttls = starttls;
        if (context != null && starttls) {
            this.theConfigData = new StartTLSSMTPConfiguration(theConfigData);
        } else {
            this.theConfigData = theConfigData;
        }
    }
    
    protected ExecutionHandler createExecutionHandler(int size) {
        return new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(size, 0, 0));
    }
    
    
    public void setUseExecutionHandler(boolean useHandler, int size) {
        if (isBound()) throw new IllegalStateException("Server running already");
        if (useHandler) {
            eHandler =createExecutionHandler(size);
        } else {
            if (eHandler != null) {
                eHandler.releaseExternalResources();
            }
            eHandler = null;
        }
    }
    
  
    protected ChannelUpstreamHandler createCoreHandler() {
        return coreHandler;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.smtp.SMTPServerMBean#isEnabled()
     */
    public boolean isEnabled() {
        return isBound();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.smtp.SMTPServerMBean#getSocketType()
     */
    public String getSocketType() {
        if (context != null && !starttls) {
            return "ssl";
        } else {
            return "plain";
        }
    }

    
    @Override
    public synchronized void bind() throws Exception {
        coreHandler = new SMTPChannelUpstreamHandler(chain, theConfigData, logger, context, null);
        super.bind();
    }


    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new AbstractSSLAwareChannelPipelineFactory(getTimeout(), 0, getBacklog(), group, eHandler) {

            @Override
            protected ChannelUpstreamHandler createHandler() {
                return coreHandler;
            }

            @Override
            protected OneToOneEncoder createEncoder() {
                return SMTP_RESPONSE_ENCODER;
            }

            @Override
            protected boolean isSSLSocket() {
                return context != null && !starttls;
            }

            @Override
            protected SSLContext getSSLContext() {
                return context;
            }
        };

    }

    private final static class StartTLSSMTPConfiguration implements SMTPConfiguration {

        private SMTPConfiguration config;

        public StartTLSSMTPConfiguration(SMTPConfiguration config) {
            this.config = config;
        }
        
        public String getHelloName() {
            return config.getHelloName();
        }

        public int getResetLength() {
            return config.getResetLength();
        }

        public long getMaxMessageSize() {
            return config.getMaxMessageSize();
        }

        public boolean isRelayingAllowed(String remoteIP) {
            return config.isRelayingAllowed(remoteIP);
        }

        public boolean isAuthRequired(String remoteIP) {
            return config.isAuthRequired(remoteIP);
        }

        public boolean useHeloEhloEnforcement() {
            return config.useHeloEhloEnforcement();
        }

        public String getSMTPGreeting() {
            return config.getSMTPGreeting();
        }

        public boolean useAddressBracketsEnforcement() {
            return config.useAddressBracketsEnforcement();
        }

        public boolean isStartTLSSupported() {
            return true;
        }
        
    }
}
