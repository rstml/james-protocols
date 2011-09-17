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


import javax.net.ssl.SSLContext;

import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPProtocolHandlerChain;
import org.apache.james.protocols.smtp.SMTPServerMBean;

/**
 * NIO SMTPServer which use Netty
 */
public class SMTPServer extends NettyServer implements SMTPServerMBean {

    private boolean starttls;

    public SMTPServer(SMTPConfiguration theConfigData, SMTPProtocolHandlerChain chain) {
        this(theConfigData, chain, null, false);
    }
    
    
    public SMTPServer(SMTPConfiguration theConfigData, SMTPProtocolHandlerChain chain, SSLContext context, boolean starttls) {
        super(new SMTPProtocol(chain, new StartTLSSMTPConfiguration(theConfigData, starttls)), context);
        this.starttls = starttls;
    }
    
    /**
     * @see org.apache.james.protocols.smtp.SMTPServerMBean#isEnabled()
     */
    public boolean isEnabled() {
        return isBound();
    }

    /**
     * @see org.apache.james.protocols.smtp.SMTPServerMBean#getSocketType()
     */
    public String getSocketType() {
        if (context != null && !starttls) {
            return "ssl";
        } else {
            return "plain";
        }
    }

    private final static class StartTLSSMTPConfiguration implements SMTPConfiguration {

        private SMTPConfiguration config;
        private boolean startTls;

        public StartTLSSMTPConfiguration(SMTPConfiguration config, boolean startTls) {
            this.config = config;
            this.startTls = startTls;
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
            return this.startTls;
        }
        
    }

}
