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
package org.apache.james.protocols.pop3.utils;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.commons.net.pop3.POP3Reply;
import org.apache.commons.net.pop3.POP3SClient;

public class AdvancedPOP3SClient extends POP3SClient {

    public AdvancedPOP3SClient() {
        super();
    }

    public AdvancedPOP3SClient(boolean implicit, SSLContext ctx) {
        super(implicit, ctx);
    }

    public AdvancedPOP3SClient(boolean implicit) {
        super(implicit);
    }

    public AdvancedPOP3SClient(SSLContext context) {
        super(context);
    }

    public AdvancedPOP3SClient(String proto, boolean implicit, SSLContext ctx) {
        super(proto, implicit, ctx);
    }

    public AdvancedPOP3SClient(String proto, boolean implicit) {
        super(proto, implicit);
    }

    public AdvancedPOP3SClient(String proto) {
        super(proto);
    }

    public boolean capa() throws IOException {
        
        int code = sendCommand("CAPA");
        if (code == POP3Reply.OK) {
            getAdditionalReply();
            return true;
        }
        return false;
        
    }
}
