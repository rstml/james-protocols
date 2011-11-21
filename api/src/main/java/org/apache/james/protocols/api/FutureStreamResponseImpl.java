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

import java.io.IOException;
import java.io.InputStream;

public class FutureStreamResponseImpl extends FutureResponseImpl implements StreamResponse{

    private static final  EmptyInputStream EMPTY = new EmptyInputStream();
    
    private InputStream in = EMPTY;
    public FutureStreamResponseImpl(AbstractResponse response) {
        super(response);
        if ((response instanceof StreamResponse) == false) {
            throw new IllegalArgumentException("Given Response must be a StreamResponse");
        }
    }

    public synchronized void setStream(InputStream in) {
        if (isReady()) {
            throw new IllegalStateException("FutureResponse MUST NOT get modified after its ready");
        }
        this.in = in;
    }
    @Override
    public InputStream getStream() {
        checkReady();
        return in;
        
    }

    private final static class EmptyInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return -1;
        }
        
    }
}
