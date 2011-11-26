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

package org.apache.james.protocols.api;

import org.apache.james.protocols.api.handler.ProtocolHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic {@link Protocol} implementation 
 *
 */
public class ProtocolImpl implements Protocol{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final ProtocolHandlerChain chain;

    public ProtocolImpl(ProtocolHandlerChain chain) {
        this.chain = chain;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.Protocol#getProtocolChain()
     */
    public ProtocolHandlerChain getProtocolChain() {
        return chain;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.Protocol#newSession(org.apache.james.protocols.api.ProtocolTransport)
     */
    public ProtocolSession newSession(ProtocolTransport transport) {
        return new ProtocolSessionImpl(logger, transport);
    }

}
