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

package org.apache.james.protocols.pop3;

import java.io.IOException;

/**
 * 
 *
 */
public interface MailboxFactory {

	/**
	 * Returns the {@link Mailbox} for the {@link POP3Session}. If the user does not exist or the password is not valid it will return <code>null</code>
	 * 
	 * @param user
	 * @param password
	 * @return mailbox or null if not valid user or password was given
	 * @throws IOException
	 */
	public Mailbox getMailbox(POP3Session session, String password) throws IOException;
	
}
