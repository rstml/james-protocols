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



package org.apache.james.protocols.smtp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;


/**
 * Provides abstraction for DNS resolutions. The interface is Mail specific.
 * It may be a good idea to make the interface more generic or expose 
 * commonly needed DNS methods.
 *
 */
public interface DNSService {

    /**
     * The component role used by components implementing this service
     */
     String ROLE = "org.apache.james.smtpserver.protocol.DNSService";

    /**
     * <p>Return a prioritized unmodifiable list of host handling mail
     * for the domain.</p>
     * 
     * <p>First lookup MX hosts, then MX hosts of the CNAME adress, and
     * if no server is found return the IP of the hostname</p>
     *
     * @param hostname domain name to look up
     *
     * @return a unmodifiable list of handling servers corresponding to
     *         this mail domain name
     * @throws TemporaryResolutionException get thrown on temporary problems 
     */
    Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException;

    /**
     * Get a collection of DNS TXT Records
     * 
     * @param hostname The hostname to check
     * @return collection of strings representing TXT record values
     */
    Collection<String> findTXTRecords(String hostname);

    
    /**
     * @see java.net.InetAddress#getAllByName(String)
     */
    InetAddress[] getAllByName(String host) throws UnknownHostException;
 
    /**
     * @see java.net.InetAddress#getByName(String)
     */
    InetAddress getByName(String host) throws UnknownHostException;

    /**
     * Determines the hostname for an address
     * @param addr
     *             the address record
     * @return
     *             the hostname defined in the address record
     */
    String getHostName(InetAddress addr);
    

    /**
     * get the local hosts {@link InetAddress}
     * @return
     *             the localhosts inet address
     * @throws UnknownHostException
     */
    InetAddress getLocalHost() throws UnknownHostException;
}
