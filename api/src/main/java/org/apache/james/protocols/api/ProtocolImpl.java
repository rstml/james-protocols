package org.apache.james.protocols.api;

import org.apache.james.protocols.api.handler.ProtocolHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolImpl implements Protocol{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private ProtocolHandlerChain chain;

    public ProtocolImpl(ProtocolHandlerChain chain) {
        this.chain = chain;
    }
    
    @Override
    public ProtocolHandlerChain getProtocolChain() {
        return chain;
    }

    @Override
    public ProtocolSession newSession(ProtocolTransport transport) {
        return new ProtocolSessionImpl(logger, transport);
    }

}
