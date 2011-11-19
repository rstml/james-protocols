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

package org.apache.james.protocols.impl;

import javax.net.ssl.SSLContext;

public final class Secure {

    private final SSLContext context;
    private final boolean starttls;

    private Secure(SSLContext context, boolean starttls) {
        this.context = context;
        this.starttls = starttls;
    }
    
    public static Secure createTls(SSLContext context) {
        return new Secure(context, false);
    }
    
    public static Secure createStartTls(SSLContext context) {
        return new Secure(context, true);
    }
    
    public SSLContext getContext() {
        return context;
    }
    
    public boolean isStartTLS() {
        return starttls;
    }
}
