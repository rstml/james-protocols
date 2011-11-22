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

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FutureResponse} implementation which wraps a {@link AbstractResponse} implementation
 * 
 *
 */
public class FutureResponseImpl implements FutureResponse{

    protected Response response;
    private List<ResponseListener> listeners;
    private int waiters;

    protected final synchronized void checkReady() {
        while (!isReady()) {
            try {
                waiters++;
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                waiters--;
            }
        }
    }
    @Override
    public synchronized void addListener(ResponseListener listener) {
        if (isReady()) {
            listener.onResponse(this);
        } else {
            if (listeners == null) {
                listeners = new ArrayList<ResponseListener>();
            }
            listeners.add(listener);
        }
    }

    @Override
    public synchronized void removeListener(ResponseListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public synchronized boolean isReady() {
        return response != null;
    }
    
    @Override
    public List<CharSequence> getLines() {
        checkReady();
        return response.getLines();
    }


    @Override
    public String getRetCode() {
        checkReady();
        return response.getRetCode();
    }


    @Override
    public boolean isEndSession() {
        checkReady();
        return response.isEndSession();
    }

    @Override
    public synchronized String toString() {
        checkReady();
        return response.toString();
    }
    
    public synchronized void setResponse(Response response) {
        if (!isReady()) {
        	this.response = response;
            
            if (waiters > 0) {
                notifyAll();
            }
            for (ResponseListener listener: listeners) {
                listener.onResponse(this);
            }
            listeners = null;
        }
    }

}
