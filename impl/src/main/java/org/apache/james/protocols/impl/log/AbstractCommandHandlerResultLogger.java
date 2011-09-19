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
package org.apache.james.protocols.impl.log;

import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;
import org.apache.james.protocols.api.handler.CommandHandlerResultHandler;

/**
 * 
 * 
 *
 */
public abstract class AbstractCommandHandlerResultLogger<R extends Response, S extends ProtocolSession> implements CommandHandlerResultHandler<R, S> {

    

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandlerResultHandler#onResponse(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Response, long, org.apache.james.protocols.api.CommandHandler)
     */
    public Response onResponse(ProtocolSession session, R response, long executionTime, CommandHandler<S> handler) {
        String code = response.getRetCode();
        String msg = handler.getClass().getName() + ": " + response.toString();
        
        // check if the response should log with info 
        if (logWithInfo(code)) {
            session.getLogger().info(msg);
        } else {
            session.getLogger().debug(msg);
        }
        return response;
    }
    
    protected abstract boolean logWithInfo(String retCode);

}
